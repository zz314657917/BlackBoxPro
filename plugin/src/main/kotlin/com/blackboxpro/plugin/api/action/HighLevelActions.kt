package com.blackboxpro.plugin.api.action

import com.blackboxpro.common.protocol.ResponseMessage
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture
import kotlin.math.*

/**
 * 面向场景的语义化高级 API。
 *
 * 内部组合底层 action，为开发者提供直观的操作方法。
 * 所有方法均返回 CompletableFuture<ResponseMessage>，支持链式调用。
 */
object HighLevelActions {

    // ======================== 移动类 ========================

    /**
     * 向玩家当前朝向前进指定格数。
     * 通过玩家 yaw 计算目标坐标，发送 player_move_look。
     */
    fun moveForward(player: Player, blocks: Double): CompletableFuture<ResponseMessage> {
        val loc = player.location
        val rad = Math.toRadians(loc.yaw.toDouble())
        val dx = -sin(rad) * blocks
        val dz = cos(rad) * blocks
        return MovementActions.playerMoveLook(
            player,
            loc.x + dx, loc.y, loc.z + dz,
            loc.pitch
        )
    }

    /**
     * 向指定方向移动指定格数。
     * @param direction 方向: "north"(-Z), "south"(+Z), "east"(+X), "west"(-X), "up"(+Y), "down"(-Y)
     */
    fun moveDirection(player: Player, direction: String, blocks: Double): CompletableFuture<ResponseMessage> {
        val loc = player.location
        var dx = 0.0; var dy = 0.0; var dz = 0.0
        when (direction.lowercase()) {
            "north" -> dz = -blocks
            "south" -> dz = blocks
            "east" -> dx = blocks
            "west" -> dx = -blocks
            "up" -> dy = blocks
            "down" -> dy = -blocks
            else -> error("Unknown direction: $direction (expected: north/south/east/west/up/down)")
        }
        return MovementActions.playerMove(player, loc.x + dx, loc.y + dy, loc.z + dz)
    }

    /**
     * 移动到指定坐标并自动转向目标位置。
     */
    fun teleportTo(player: Player, x: Double, y: Double, z: Double): CompletableFuture<ResponseMessage> {
        val loc = player.location
        val dx = x - loc.x
        val dz = z - loc.z
        val dy = y - (loc.y + 1.62) // 眼睛高度
        val distXZ = sqrt(dx * dx + dz * dz)
        val pitch = Math.toDegrees(-atan2(dy, distXZ)).toFloat()
        return MovementActions.playerMoveLook(player, x, y, z, pitch)
    }

    // ======================== 视角类 ========================

    /**
     * 看向指定方块坐标（方块中心）。
     */
    fun lookAtBlock(player: Player, x: Int, y: Int, z: Int): CompletableFuture<ResponseMessage> =
        CompositeActions.lookAt(player, x + 0.5, y + 0.5, z + 0.5)

    /**
     * 看向指定精确坐标。
     */
    fun lookAt(player: Player, x: Double, y: Double, z: Double): CompletableFuture<ResponseMessage> =
        CompositeActions.lookAt(player, x, y, z)

    /**
     * 看向指定实体。
     */
    fun lookAtEntity(player: Player, entityId: Int): CompletableFuture<ResponseMessage> =
        CompositeActions.lookAtEntity(player, entityId)

    // ======================== 容器点击类 ========================

    /**
     * 左键点击槽位（普通拾取/放置）。
     * mode=0, button=0
     */
    fun leftClick(player: Player, windowId: Int, stateId: Int, slot: Int): CompletableFuture<ResponseMessage> =
        ContainerActions.clickSlot(player, windowId, stateId, slot, button = 0, mode = 0)

    /**
     * 右键点击槽位（拾取一半/放置一个）。
     * mode=0, button=1
     */
    fun rightClick(player: Player, windowId: Int, stateId: Int, slot: Int): CompletableFuture<ResponseMessage> =
        ContainerActions.clickSlot(player, windowId, stateId, slot, button = 1, mode = 0)

    /**
     * Shift+左键点击槽位（快速移动）。
     * mode=1, button=0
     */
    fun shiftClick(player: Player, windowId: Int, stateId: Int, slot: Int): CompletableFuture<ResponseMessage> =
        ContainerActions.clickSlot(player, windowId, stateId, slot, button = 0, mode = 1)

    /**
     * Shift+右键点击槽位。
     * mode=1, button=1
     */
    fun shiftRightClick(player: Player, windowId: Int, stateId: Int, slot: Int): CompletableFuture<ResponseMessage> =
        ContainerActions.clickSlot(player, windowId, stateId, slot, button = 1, mode = 1)

    /**
     * 数字键交换槽位到快捷栏。
     * mode=2, button=hotbar(0-8)
     */
    fun swapToHotbar(player: Player, windowId: Int, stateId: Int, slot: Int, hotbar: Int): CompletableFuture<ResponseMessage> =
        ContainerActions.clickSlot(player, windowId, stateId, slot, button = hotbar, mode = 2)

    /**
     * 中键复制（创造模式）。
     * mode=3, button=2
     */
    fun middleClick(player: Player, windowId: Int, stateId: Int, slot: Int): CompletableFuture<ResponseMessage> =
        ContainerActions.clickSlot(player, windowId, stateId, slot, button = 2, mode = 3)

    /**
     * Q 丢弃槽位物品（单个）。
     * mode=4, button=0
     */
    fun dropSlot(player: Player, windowId: Int, stateId: Int, slot: Int): CompletableFuture<ResponseMessage> =
        ContainerActions.clickSlot(player, windowId, stateId, slot, button = 0, mode = 4)

    /**
     * Ctrl+Q 丢弃槽位全部物品。
     * mode=4, button=1
     */
    fun dropSlotAll(player: Player, windowId: Int, stateId: Int, slot: Int): CompletableFuture<ResponseMessage> =
        ContainerActions.clickSlot(player, windowId, stateId, slot, button = 1, mode = 4)

    /**
     * 双击收集同类物品到光标。
     * mode=6, button=0
     */
    fun doubleClick(player: Player, windowId: Int, stateId: Int, slot: Int): CompletableFuture<ResponseMessage> =
        ContainerActions.clickSlot(player, windowId, stateId, slot, button = 0, mode = 6)

    // ======================== 快捷栏 ========================

    /**
     * 切换快捷栏选中槽位（0-8）。
     */
    fun switchHotbar(player: Player, slot: Int): CompletableFuture<ResponseMessage> =
        ContainerActions.setCarriedItem(player, slot)

    // ======================== 聊天与命令 ========================

    /**
     * 发送聊天消息。
     */
    fun sendChat(player: Player, message: String): CompletableFuture<ResponseMessage> =
        ChatActions.chatMessage(player, message)

    /**
     * 执行命令（自动去除开头的 /）。
     */
    fun executeCommand(player: Player, command: String): CompletableFuture<ResponseMessage> =
        ChatActions.chatCommand(player, command.removePrefix("/"))

    // ======================== 飞行控制 ========================

    /**
     * 开始飞行（创造/旁观模式）。
     */
    fun startFlying(player: Player): CompletableFuture<ResponseMessage> =
        ClientActions.playerAbilities(player, flying = true)

    /**
     * 停止飞行。
     */
    fun stopFlying(player: Player): CompletableFuture<ResponseMessage> =
        ClientActions.playerAbilities(player, flying = false)

    // ======================== 物品操作 ========================

    /**
     * 丢弃手持物品（单个）。
     */
    fun dropItem(player: Player): CompletableFuture<ResponseMessage> =
        PlayerActions.dropItem(player)

    /**
     * 丢弃手持物品（整组）。
     */
    fun dropItemStack(player: Player): CompletableFuture<ResponseMessage> =
        PlayerActions.dropItemStack(player)

    /**
     * 交换主副手物品。
     */
    fun swapHands(player: Player): CompletableFuture<ResponseMessage> =
        PlayerActions.swapHands(player)

    // ======================== 截图 ========================

    /**
     * 触发客户端截图。
     */
    fun screenshot(
        player: Player,
        testId: String = "default",
        prefix: String? = null
    ): CompletableFuture<ResponseMessage> =
        ScreenshotActions.screenshot(player, testId, prefix, player.name)

    // ======================== 查询 ========================

    /**
     * 查询主手/副手物品。
     */
    fun queryHeldItem(player: Player, hand: String = "main_hand"): CompletableFuture<ResponseMessage> =
        QueryActions.queryHeldItem(player, hand)

    /**
     * 查询背包指定槽位物品。
     */
    fun queryInventorySlot(player: Player, slot: Int? = null): CompletableFuture<ResponseMessage> =
        QueryActions.queryInventorySlot(player, slot)

    /**
     * 查询玩家完整状态。
     */
    fun queryPlayerState(player: Player): CompletableFuture<ResponseMessage> =
        QueryActions.queryPlayerState(player)

    /**
     * 查询附近实体。
     */
    fun queryNearbyEntities(player: Player, radius: Double = 10.0, type: String? = null): CompletableFuture<ResponseMessage> =
        QueryActions.queryNearbyEntities(player, radius, type)

    /**
     * 查询当前容器状态。
     */
    fun queryContainerState(player: Player): CompletableFuture<ResponseMessage> =
        QueryActions.queryContainerState(player)

    /**
     * 查询聊天历史。
     */
    fun queryChatHistory(player: Player, count: Int = 10, filter: String? = null): CompletableFuture<ResponseMessage> =
        QueryActions.queryChatHistory(player, count, filter)

    /**
     * 点击最新一条包含目标文本的聊天富文本。
     */
    fun clickChatText(player: Player, match: String): CompletableFuture<ResponseMessage> =
        MouseActions.clickChatText(player, match, index = 0, execute = true)

    /**
     * 查询最新一条包含目标文本的聊天样式。
     */
    fun queryChatStyle(player: Player, match: String): CompletableFuture<ResponseMessage> =
        MouseActions.queryChatStyle(player, match, index = 0)

    /**
     * 将鼠标悬停到当前容器中的指定槽位。
     */
    fun hoverSlot(player: Player, windowId: Int, slot: Int, durationTicks: Int = 0): CompletableFuture<ResponseMessage> =
        MouseActions.hoverSlot(player, windowId, slot, durationTicks)

    /**
     * 查询当前容器中指定槽位的 Tooltip。
     */
    fun querySlotTooltip(player: Player, slot: Int): CompletableFuture<ResponseMessage> =
        MouseActions.querySlotTooltip(player, slot, advanced = false)

    /**
     * 查询当前鼠标悬浮 Tooltip 状态。
     */
    fun queryTooltipState(player: Player): CompletableFuture<ResponseMessage> =
        MouseActions.queryTooltipState(player)

    /**
     * 先查样式，若存在 ClickEvent 再执行点击。
     */
    fun queryAndClickChatText(player: Player, match: String): CompletableFuture<ResponseMessage> =
        MouseActions.queryChatStyle(player, match).thenCompose { queryResponse ->
            if (queryResponse.status == "success" && queryResponse.data?.has("clickEvent") == true) {
                MouseActions.clickChatText(player, match)
            } else {
                CompletableFuture.completedFuture(queryResponse)
            }
        }

    /**
     * 玩家跳跃。
     */
    fun jump(player: Player): CompletableFuture<ResponseMessage> =
        PlayerActions.jump(player)

    /**
     * 查询指定坐标方块状态。
     */
    fun queryBlockState(player: Player, x: Int, y: Int, z: Int): CompletableFuture<ResponseMessage> =
        QueryActions.queryBlockState(player, x, y, z)

    /**
     * 查询世界全局状态。
     */
    fun queryWorldState(player: Player): CompletableFuture<ResponseMessage> =
        QueryActions.queryWorldState(player)

    /**
     * 查询 Tab 列表。
     */
    fun queryTabList(player: Player, limit: Int = 100): CompletableFuture<ResponseMessage> =
        QueryActions.queryTabList(player, limit)

    /**
     * 查询记分板状态。
     */
    fun queryScoreboard(player: Player, objective: String? = null): CompletableFuture<ResponseMessage> =
        QueryActions.queryScoreboard(player, objective)

    /**
     * 查询当前屏幕/GUI 状态。
     */
    fun queryScreenState(player: Player): CompletableFuture<ResponseMessage> =
        QueryActions.queryScreenState(player)

    /**
     * 查询 Boss Bar 信息。
     */
    fun queryBossBar(player: Player): CompletableFuture<ResponseMessage> =
        QueryActions.queryBossBar(player)

    // ======================== 导航与瞄准 ========================

    /**
     * 瞄准指定方块的指定面。
     */
    fun lookAtBlock(player: Player, x: Int, y: Int, z: Int, face: String = "top"): CompletableFuture<ResponseMessage> =
        NavigationActions.lookAtBlock(player, x, y, z, face)

    /**
     * AI 寻路移动到目标坐标。
     */
    fun navigateTo(player: Player, x: Double, y: Double, z: Double, speed: Double = 1.0): CompletableFuture<ResponseMessage> =
        NavigationActions.navigateTo(player, x, y, z, speed)
}
