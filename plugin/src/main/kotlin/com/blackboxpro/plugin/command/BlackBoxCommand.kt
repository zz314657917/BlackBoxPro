package com.blackboxpro.plugin.command

import com.blackboxpro.common.protocol.ResponseMessage
import com.blackboxpro.plugin.BlackBoxPro
import com.blackboxpro.plugin.api.BlackBoxApi
import com.blackboxpro.plugin.config.BlackBoxSettings
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import taboolib.common.platform.command.CommandBody
import taboolib.common.platform.command.CommandHeader
import taboolib.common.platform.command.mainCommand
import taboolib.common.platform.command.subCommand
import taboolib.common.platform.command.suggestUncheck

@CommandHeader("blackbox", permission = "blackbox.admin")
object BlackBoxCommand {

    private val gson = Gson()

    @CommandBody
    val main = mainCommand {
        execute<CommandSender> { sender, _, _ ->
            sender.sendMessage("§6[BlackBoxPro] §fServer Plugin v${BlackBoxPro.VERSION}")
            sender.sendMessage("§7/blackbox send <player> <action> [params_json] §f- 发送指令 (JSON)")
            sender.sendMessage("§7/blackbox exec <player> <action> [key:value ...] §f- 发送指令 (扁平化)")
            sender.sendMessage("§7/blackbox test <player> [full|category <分类>|action <id>] §f- 执行黑盒测试")
            sender.sendMessage("§7/blackbox status §f- 查看状态")
            sender.sendMessage("§7/blackbox reload §f- 重载配置")
        }
    }

    @CommandBody
    val send = subCommand {
        dynamic("player") {
            suggestion<CommandSender> { _, _ ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            dynamic("action") {
                execute<CommandSender> { sender, context, _ ->
                    val playerName = context["player"]
                    val action = context["action"]
                    val player = Bukkit.getPlayerExact(playerName)
                    if (player == null) {
                        sender.sendMessage("§c[BlackBoxPro] 玩家 $playerName 不在线。")
                        return@execute
                    }
                    val timeoutMs = BlackBoxSettings.responseTimeoutMs
                    BlackBoxApi.sendAsync(player, action, timeoutMs = timeoutMs).thenAccept { response ->
                        sendResponseFeedback(sender, response)
                    }
                    sender.sendMessage("§a[BlackBoxPro] 已发送指令 §f$action §a给 §f${player.name}§a，等待响应... (超时: ${timeoutMs}ms)")
                }
                dynamic("params") {
                    execute<CommandSender> { sender, context, _ ->
                        val playerName = context["player"]
                        val action = context["action"]
                        val paramsRaw = context["params"]
                        val player = Bukkit.getPlayerExact(playerName)
                        if (player == null) {
                            sender.sendMessage("§c[BlackBoxPro] 玩家 $playerName 不在线。")
                            return@execute
                        }
                        val params = try {
                            gson.fromJson(paramsRaw, JsonObject::class.java) ?: JsonObject()
                        } catch (e: JsonSyntaxException) {
                            sender.sendMessage("§c[BlackBoxPro] 无效的 JSON 参数: ${e.message}")
                            return@execute
                        }
                        // 使用配置的超时时间
                        val timeoutMs = BlackBoxSettings.responseTimeoutMs
                        BlackBoxApi.sendAsync(player, action, params, timeoutMs = timeoutMs).thenAccept { response ->
                            sendResponseFeedback(sender, response)
                        }
                        sender.sendMessage("§a[BlackBoxPro] 已发送指令 §f$action §a给 §f${player.name}§a，等待响应... (超时: ${timeoutMs}ms)")
                    }
                }
            }
        }
    }

    @CommandBody
    val exec = subCommand {
        dynamic("player") {
            suggestion<CommandSender> { _, _ ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            dynamic("action") {
                suggestUncheck { ActionParamRegistry.getActionIds() }
                execute<CommandSender> { sender, context, _ ->
                    executeFlat(sender, context["player"], context["action"], emptyList())
                }
                dynamic("params") {
                    suggestUncheck {
                        val action = ctx["action"]
                        val allParams = ActionParamRegistry.getParams(action) ?: return@suggestUncheck emptyList()
                        val currentInput = ctx.self()
                        val enteredKeys = currentInput.split(" ")
                            .filter { it.contains(':') }
                            .map { it.substringBefore(':') }
                            .toSet()
                        allParams.filterNot { it in enteredKeys }.map { "$it:" }
                    }
                    execute<CommandSender> { sender, context, _ ->
                        val rawParams = context["params"].split(" ").filter { it.isNotBlank() }
                        executeFlat(sender, context["player"], context["action"], rawParams)
                    }
                }
            }
        }
    }

    private fun executeFlat(sender: CommandSender, playerName: String, action: String, rawParams: List<String>) {
        val player = Bukkit.getPlayerExact(playerName)
        if (player == null) {
            sender.sendMessage("§c[BlackBoxPro] 玩家 $playerName 不在线。")
            return
        }
        val params = FlatParamParser.parse(rawParams)
        // 统一使用 sendAsync，确保有超时机制
        val timeoutMs = BlackBoxSettings.responseTimeoutMs
        BlackBoxApi.sendAsync(player, action, params, timeoutMs = timeoutMs).thenAccept { response ->
            sendResponseFeedback(sender, response)
        }
        if (params.size() == 0) {
            sender.sendMessage("§a[BlackBoxPro] 已发送指令 §f$action §a给 §f${player.name}§a，等待响应... (超时: ${timeoutMs}ms)")
        } else {
            sender.sendMessage("§a[BlackBoxPro] 已发送指令 §f$action §a给 §f${player.name}§a，参数: §7$params")
        }
    }

    /**
     * 统一的响应反馈输出。
     * debug 模式下输出完整 JSON 数据。
     */
    private fun sendResponseFeedback(sender: CommandSender, response: ResponseMessage) {
        sender.sendMessage("§6[BlackBoxPro] 响应: §f${response.status} §7${response.message ?: ""}")
        val data = response.data
        if (data != null && data.size() > 0) {
            if (BlackBoxSettings.debug) {
                val dataStr = gson.toJson(data)
                if (dataStr.length <= 500) {
                    sender.sendMessage("§6[BlackBoxPro] 数据: §f$dataStr")
                } else {
                    sender.sendMessage("§6[BlackBoxPro] 数据 (截断): §f${dataStr.take(500)}...")
                    sender.sendMessage("§7[BlackBoxPro] 完整数据已输出到控制台日志")
                }
            } else {
                sender.sendMessage("§7[BlackBoxPro] 响应包含数据 (${data.size()} 字段)，开启 debug 模式查看详情")
            }
        }
    }

    @CommandBody
    val test = subCommand {
        dynamic("player") {
            suggestion<CommandSender> { _, _ ->
                Bukkit.getOnlinePlayers().map { it.name }
            }
            execute<CommandSender> { sender, context, _ ->
                val playerName = context["player"]
                val player = Bukkit.getPlayerExact(playerName)
                if (player == null) {
                    sender.sendMessage("§c[BlackBoxPro] 玩家 $playerName 不在线。")
                    return@execute
                }
                BlackBoxTestRunner.runAll(player, sender)
            }
            dynamic("mode") {
                suggestUncheck { listOf("full", "category", "action") }
                execute<CommandSender> { sender, context, _ ->
                    val playerName = context["player"]
                    val player = Bukkit.getPlayerExact(playerName)
                    if (player == null) {
                        sender.sendMessage("§c[BlackBoxPro] 玩家 $playerName 不在线。")
                        return@execute
                    }
                    when (context["mode"].lowercase()) {
                        "full" -> BlackBoxTestRunner.runFull(player, sender)
                        else -> sender.sendMessage("§c[BlackBoxPro] 用法: /blackbox test <player> [full|category <分类>|action <id>]")
                    }
                }
                dynamic("value") {
                    suggestUncheck {
                        when (ctx["mode"].lowercase()) {
                            "category" -> BlackBoxTestRunner.categories()
                            "action" -> ActionParamRegistry.getActionIds()
                            else -> emptyList()
                        }
                    }
                    execute<CommandSender> { sender, context, _ ->
                        val playerName = context["player"]
                        val player = Bukkit.getPlayerExact(playerName)
                        if (player == null) {
                            sender.sendMessage("§c[BlackBoxPro] 玩家 $playerName 不在线。")
                            return@execute
                        }
                        when (context["mode"].lowercase()) {
                            "category" -> BlackBoxTestRunner.runCategory(player, sender, context["value"])
                            "action" -> BlackBoxTestRunner.runAction(player, sender, context["value"])
                            else -> sender.sendMessage("§c[BlackBoxPro] 用法: /blackbox test <player> [full|category <分类>|action <id>]")
                        }
                    }
                }
            }
        }
    }

    @CommandBody
    val status = subCommand {
        execute<CommandSender> { sender, _, _ ->
            sender.sendMessage("§6[BlackBoxPro] §f状态信息:")
            sender.sendMessage("§7  版本: §f${BlackBoxPro.VERSION}")
            sender.sendMessage("§7  调试模式: §f${BlackBoxSettings.debug}")
            sender.sendMessage("§7  测试模式: §f${BlackBoxSettings.testMode}")
            sender.sendMessage("§7  HTTP 端口: §f${BlackBoxSettings.httpPort}")
            sender.sendMessage("§7  响应超时: §f${BlackBoxSettings.responseTimeoutMs}ms")
            sender.sendMessage("§7  在线玩家数: §f${Bukkit.getOnlinePlayers().size}")
        }
    }

    @CommandBody
    val reload = subCommand {
        execute<CommandSender> { sender, _, _ ->
            BlackBoxSettings.conf.reload()
            sender.sendMessage("§a[BlackBoxPro] 配置已重载。")
        }
    }
}
