package com.blackboxpro.plugin.command

import com.blackboxpro.plugin.api.action.*
import com.blackboxpro.common.protocol.ResponseMessage
import com.blackboxpro.plugin.command.testframework.BlackBoxActionTestCase
import com.blackboxpro.plugin.command.testframework.BlackBoxActionTestResult
import com.blackboxpro.plugin.command.testframework.BlackBoxTestCatalog
import com.blackboxpro.plugin.command.testframework.BlackBoxTestContext
import com.blackboxpro.plugin.command.testframework.BlackBoxTestProfile
import com.blackboxpro.plugin.command.testframework.BlackBoxTestStatus
import com.blackboxpro.plugin.command.testframework.BlackBoxLoaderProfile
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import taboolib.common.platform.function.submit
import java.util.concurrent.CompletableFuture

/**
 * 集成测试运行器。
 *
 * 框架层不自动截图。截图完全由测试用例自身决定：
 * - 需要视觉验证的用例在 execute() 或 verify() 里主动调用 screenshot action。
 * - screenshot action 本身的测试用例会验证截图是否落盘。
 */
object BlackBoxTestRunner {

    /**
     * @param start 启动动作，返回 Future
     * @param finish 收尾动作（持续性动作），为 null 表示瞬时动作
     */
    private data class TestCase(
        val name: String,
        val id: String,
        val start: (Player) -> CompletableFuture<ResponseMessage>,
        val finish: ((Player) -> CompletableFuture<ResponseMessage>)? = null
    )

    fun runAll(player: Player, sender: CommandSender) {
        val cases = buildCases(player)
        val testId = "integration_${System.currentTimeMillis() / 1000}"

        sender.sendMessage("§6[BlackBoxPro Test] §f开始执行 ${cases.size} 个测试用例...")
        sender.sendMessage("§7  模式: smoke")
        sender.sendMessage("§7  截图会话: $testId")
        sender.sendMessage("")

        val results = mutableListOf<Triple<String, Boolean, String?>>()
        val startTime = System.currentTimeMillis()

        var chain = screenshot(player, testId, "00_test_start")

        cases.forEachIndexed { index, case ->
            chain = chain.thenCompose { delay(800L) }.thenCompose {
                val num = (index + 1).toString().padStart(2, '0')
                val tag = "${num}_${case.id}"
                val t0 = System.currentTimeMillis()

                screenshot(player, testId, "${tag}_1_before").thenCompose {
                    case.start(player)
                }.thenCompose { startResponse ->
                    if (!startResponse.isSuccess) {
                        screenshot(player, testId, "${tag}_2_during").thenCompose {
                            screenshot(player, testId, "${tag}_3_FAILED")
                        }.thenApply { startResponse }
                    } else if (case.finish != null) {
                        screenshot(player, testId, "${tag}_2_during").thenCompose {
                            case.finish.invoke(player)
                        }.thenCompose { finishResponse ->
                            val suffix = if (finishResponse.isSuccess) "3_after" else "3_FAILED"
                            screenshot(player, testId, "${tag}_${suffix}").thenApply { finishResponse }
                        }
                    } else {
                        screenshot(player, testId, "${tag}_2_during").thenCompose {
                            screenshot(player, testId, "${tag}_3_after")
                        }.thenApply { startResponse }
                    }
                }.thenApply { finalResponse ->
                    val elapsed = System.currentTimeMillis() - t0
                    val passed = finalResponse.isSuccess
                    val reason = if (!passed) (finalResponse.message ?: finalResponse.status) else null
                    results.add(Triple(case.name, passed, reason))
                    if (passed) {
                        sender.sendMessage("§a[BlackBoxPro Test] §a✓ §f#${index + 1} ${case.name} §7(${elapsed}ms)")
                    } else {
                        sender.sendMessage("§c[BlackBoxPro Test] §c✗ §f#${index + 1} ${case.name} §c- $reason §7(${elapsed}ms)")
                    }
                }
            }
        }

        chain.thenCompose {
            delay(300L)
        }.thenCompose {
            screenshot(player, testId, "99_test_end")
        }.thenRun {
            val totalTime = System.currentTimeMillis() - startTime
            val passed = results.count { it.second }
            val failed = results.size - passed
            sender.sendMessage("")
            sender.sendMessage("§6[BlackBoxPro Test] §f完成: §a$passed/${results.size} 通过§f, §c$failed 失败 §7(总耗时 ${totalTime}ms)")
            if (failed > 0) {
                results.filter { !it.second }.forEach { (name, _, reason) ->
                    sender.sendMessage("§c  ✗ $name: $reason")
                }
            }
        }.exceptionally { ex ->
            ScreenshotActions.screenshot(player, testId, "99_test_exception", player.name).exceptionally { null }
            sender.sendMessage("§c[BlackBoxPro Test] 测试异常中断: ${ex.message}")
            null
        }
    }

    fun runFull(player: Player, sender: CommandSender): CompletableFuture<JsonObject> =
        runCatalog(player, sender, BlackBoxTestProfile.FULL)

    fun runSmoke(player: Player, sender: CommandSender): CompletableFuture<JsonObject> =
        runCatalog(player, sender, BlackBoxTestProfile.SMOKE)

    fun runCategory(player: Player, sender: CommandSender, category: String) {
        runCatalog(player, sender, BlackBoxTestProfile.FULL, category = category)
    }

    fun runAction(player: Player, sender: CommandSender, actionId: String) {
        runCatalog(player, sender, BlackBoxTestProfile.FULL, actionId = actionId)
    }

    fun categories(): List<String> = BlackBoxTestCatalog.getCategories()

    private fun runCatalog(
        player: Player,
        sender: CommandSender,
        profile: BlackBoxTestProfile,
        category: String? = null,
        actionId: String? = null
    ): CompletableFuture<JsonObject> {
        val loaderProfile = BlackBoxLoaderProfile.detect(Bukkit.getBukkitVersion())
        val cases = when {
            actionId != null -> listOfNotNull(BlackBoxTestCatalog.findAction(actionId))
            else -> BlackBoxTestCatalog.cases(profile, category)
        }

        if (cases.isEmpty()) {
            sender.sendMessage("§c[BlackBoxPro Test] 没有找到匹配的测试项。")
            return CompletableFuture.completedFuture(JsonObject().apply { addProperty("error", "No matching cases") })
        }

        val testId = buildString {
            append("integration_")
            append(profile.name.lowercase())
            if (category != null) append("_${category.lowercase()}")
            if (actionId != null) append("_${actionId.lowercase()}")
            append("_")
            append(System.currentTimeMillis() / 1000)
        }
        val ctx = BlackBoxTestContext(player, sender, testId, loaderProfile)
        val results = mutableListOf<BlackBoxActionTestResult>()
        val startTime = System.currentTimeMillis()

        sender.sendMessage("§6[BlackBoxPro Test] §f开始执行 ${cases.size} 个测试用例...")
        sender.sendMessage("§7  模式: ${profile.name.lowercase()}")
        sender.sendMessage("§7  客户端档位: ${loaderProfile.name.lowercase()}")
        if (category != null) sender.sendMessage("§7  分类: $category")
        if (actionId != null) sender.sendMessage("§7  Action: $actionId")
        sender.sendMessage("§7  截图会话: $testId")
        sender.sendMessage("")

        var chain = CompletableFuture.completedFuture(Unit)
        cases.forEachIndexed { index, case ->
            chain = chain.thenCompose {
                executeCatalogCase(ctx, case, index, results)
            }
        }

        return chain.thenCompose {
            ctx.delay(300L)
        }.thenApply {
            val totalTime = System.currentTimeMillis() - startTime
            val passed = results.count { it.status == BlackBoxTestStatus.PASSED }
            val failed = results.count { it.status == BlackBoxTestStatus.FAILED }
            val skipped = results.count { it.status == BlackBoxTestStatus.SKIPPED }
            sender.sendMessage("")
            sender.sendMessage("§6[BlackBoxPro Test] §f完成: §a$passed 通过§f, §c$failed 失败§f, §e$skipped 跳过 §7(总耗时 ${totalTime}ms)")
            if (failed > 0) {
                results.filter { it.status == BlackBoxTestStatus.FAILED }.forEach { result ->
                    sender.sendMessage("§c  ✗ ${result.case.displayName}: ${result.message}")
                }
            }
            if (skipped > 0) {
                sender.sendMessage("§e[BlackBoxPro Test] 跳过项已记录，可用 /blackbox test <player> action <id> 单独调试。")
            }
            JsonObject().apply {
                addProperty("passed", passed)
                addProperty("failed", failed)
                addProperty("skipped", skipped)
                addProperty("total", results.size)
                addProperty("totalMs", totalTime)
                add("results", JsonArray().apply {
                    results.forEach { r ->
                        add(JsonObject().apply {
                            addProperty("action", r.case.actionId)
                            addProperty("status", r.status.name.lowercase())
                            addProperty("message", r.message)
                            r.response?.data?.let { add("data", it) }
                        })
                    }
                })
            }
        }.exceptionally { ex ->
            sender.sendMessage("§c[BlackBoxPro Test] 测试异常中断: ${ex.message}")
            JsonObject().apply { addProperty("error", ex.message) }
        }
    }

    private fun executeCatalogCase(
        ctx: BlackBoxTestContext,
        case: BlackBoxActionTestCase,
        index: Int,
        results: MutableList<BlackBoxActionTestResult>
    ): CompletableFuture<Unit> {
        val t0 = System.currentTimeMillis()

        return ctx.mainThread { ctx.fixtureManager.resetBaseline() }.thenCompose {
            ctx.sendAction("close_screen").thenApply { }.exceptionally { }
        }.thenCompose {
            ctx.delay(300L)
        }.thenCompose {
            case.prepare(ctx)
        }.thenCompose { prepareResult ->
            if (prepareResult.skipReason != null) {
                case.cleanup(ctx).thenApply {
                    val elapsed = System.currentTimeMillis() - t0
                    results += BlackBoxActionTestResult(case, BlackBoxTestStatus.SKIPPED, prepareResult.skipReason)
                    ctx.sender.sendMessage("§e[BlackBoxPro Test] §e↷ §f#${index + 1} ${case.displayName} §e- ${prepareResult.skipReason} §7(${elapsed}ms)")
                }
            } else {
                case.execute(ctx).handle { response, throwable -> response to throwable }.thenCompose { (response, throwable) ->
                    if (throwable != null || response == null) {
                        val reason = throwable?.message ?: "Unknown execution error"
                        case.cleanup(ctx).thenApply {
                            val elapsed = System.currentTimeMillis() - t0
                            results += BlackBoxActionTestResult(case, BlackBoxTestStatus.FAILED, reason)
                            ctx.sender.sendMessage("§c[BlackBoxPro Test] §c✗ §f#${index + 1} ${case.displayName} §c- $reason §7(${elapsed}ms)")
                        }
                    } else {
                        val verifyMessage = case.verify(ctx, response)
                        val status = if (verifyMessage == null) BlackBoxTestStatus.PASSED else BlackBoxTestStatus.FAILED
                        case.cleanup(ctx).thenApply {
                            val elapsed = System.currentTimeMillis() - t0
                            results += BlackBoxActionTestResult(case, status, verifyMessage ?: (response.message ?: response.status), response)
                            when (status) {
                                BlackBoxTestStatus.PASSED ->
                                    ctx.sender.sendMessage("§a[BlackBoxPro Test] §a✓ §f#${index + 1} ${case.displayName} §7(${elapsed}ms)")
                                BlackBoxTestStatus.FAILED ->
                                    ctx.sender.sendMessage("§c[BlackBoxPro Test] §c✗ §f#${index + 1} ${case.displayName} §c- $verifyMessage §7(${elapsed}ms)")
                                BlackBoxTestStatus.SKIPPED -> Unit
                            }
                        }
                    }
                }
            }
        }
    }

    private fun buildCases(player: Player): List<TestCase> {
        val loc = player.location
        return listOf(
            // === 通讯基础 ===
            TestCase("通讯基础 - PlayerOnGround", "ping",
                start = { p -> MovementActions.playerOnGround(p, true) }
            ),

            // === 移动控制 ===
            TestCase("移动控制 - PlayerMove", "move",
                start = { p -> MovementActions.playerMove(p, loc.x + 5, loc.y, loc.z, speed = 1.0, timeout = 100) }
            ),
            TestCase("移动控制 - PlayerMoveLook", "move_look",
                start = { p -> MovementActions.playerMoveLook(p, loc.x - 5, loc.y, loc.z, pitch = 0.0f, speed = 1.0, timeout = 100) }
            ),

            // === 视角控制 ===
            TestCase("视角控制 - LookAt", "look_at",
                start = { p -> CompositeActions.lookAt(p, 0.0, 100.0, 0.0) }
            ),
            TestCase("视角控制 - PlayerLook", "look",
                start = { p -> MovementActions.playerLook(p, 90.0f, -30.0f) }
            ),

            // === 玩家状态（持续性） ===
            TestCase("玩家状态 - 潜行", "sneak",
                start = { p ->
                    PlayerActions.sneakStart(p).thenCompose { CompositeActions.wait(p, 30) }
                },
                finish = { p -> PlayerActions.sneakStop(p) }
            ),
            TestCase("玩家状态 - 疾跑", "sprint",
                start = { p ->
                    PlayerActions.sprintStart(p).thenCompose { CompositeActions.wait(p, 30) }
                },
                finish = { p -> PlayerActions.sprintStop(p) }
            ),
            TestCase("玩家状态 - 跳跃", "jump",
                start = { p -> PlayerActions.jump(p) }
            ),
            TestCase("玩家状态 - 交换主副手", "swap_hands",
                start = { p -> PlayerActions.swapHands(p) }
            ),

            // === 实体交互 ===
            TestCase("手臂挥动 - 主手", "swing_main",
                start = { p -> EntityActions.swingArm(p, "main_hand") }
            ),
            TestCase("手臂挥动 - 副手", "swing_off",
                start = { p -> EntityActions.swingArm(p, "off_hand") }
            ),

            // === 聊天与命令 ===
            TestCase("聊天消息 - ChatMessage", "chat",
                start = { p -> ChatActions.chatMessage(p, "[BlackBoxPro] Integration test message") }
            ),
            TestCase("命令执行 - ChatCommand", "command",
                start = { p -> ChatActions.chatCommand(p, "me BlackBoxPro test") }
            ),

            // === 快捷栏（持续性） ===
            TestCase("快捷栏切换 - SetCarriedItem", "hotbar",
                start = { p -> ContainerActions.setCarriedItem(p, 4) },
                finish = { p -> ContainerActions.setCarriedItem(p, 0) }
            ),

            // === 查询 ===
            TestCase("查询 - 玩家状态", "query_state",
                start = { p -> QueryActions.queryPlayerState(p) }
            ),
            TestCase("查询 - 手持物品", "query_held",
                start = { p -> QueryActions.queryHeldItem(p) }
            ),
            TestCase("查询 - 附近实体", "query_entities",
                start = { p -> QueryActions.queryNearbyEntities(p, radius = 16.0) }
            ),
            TestCase("查询 - 聊天历史", "query_chat",
                start = { p -> QueryActions.queryChatHistory(p, count = 5) }
            ),
            TestCase("查询 - 药水效果", "query_effects",
                start = { p -> QueryActions.queryActiveEffects(p) }
            ),

            // === 复合行为 ===
            TestCase("复合行为 - Wait", "wait",
                start = { p -> CompositeActions.wait(p, 10) }
            ),
            TestCase("批量指令 - Batch", "batch",
                start = { p ->
                    CompositeActions.batch(p, listOf(
                        "sneak_start" to JsonObject(),
                        "wait" to JsonObject().apply { addProperty("ticks", 10) },
                        "swing_arm" to JsonObject().apply { addProperty("hand", "main_hand") },
                        "wait" to JsonObject().apply { addProperty("ticks", 10) },
                        "sneak_stop" to JsonObject()
                    ))
                }
            ),

            // === 截图 ===
            TestCase("截图功能 - Screenshot", "screenshot_test",
                start = { p -> ScreenshotActions.screenshot(p, "integration_verify", "final_check", p.name) }
            )
        )
    }

    /**
     * 截图并等待完成。串入 Future 链确保编号不冲突。
     */
    private fun screenshot(player: Player, testId: String, prefix: String): CompletableFuture<Unit> =
        ScreenshotActions.screenshot(player, testId, prefix, player.name)
            .thenApply { }
            .exceptionally { }

    private fun delay(ms: Long): CompletableFuture<Unit> {
        val future = CompletableFuture<Unit>()
        // 将毫秒转换为 tick（1 tick = 50ms），最少 1 tick
        val ticks = (ms / 50).coerceAtLeast(1)
        submit(async = true, delay = ticks) { future.complete(Unit) }
        return future
    }
}
