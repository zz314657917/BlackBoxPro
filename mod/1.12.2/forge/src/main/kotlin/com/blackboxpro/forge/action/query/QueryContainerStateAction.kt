package com.blackboxpro.forge.action.query

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.ItemStackSerializer
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft

class QueryContainerStateAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult {
        val mc = Minecraft.getMinecraft()
        val player = mc.player
            ?: return ActionResult.fail("Player not available")

        val container = player.openContainer
        val isInventoryOnly = container === player.inventoryContainer

        val data = JsonObject().apply {
            addProperty("open", !isInventoryOnly)
            addProperty("windowId", container.windowId)
            addProperty("slotCount", container.inventorySlots.size)
            addProperty("stateId", -1) // 1.12.2 协议无 stateId，输出 -1 表示不可用

            // 容器类型
            addProperty("type", container.javaClass.simpleName)

            // 容器标题：从第一个非玩家 inventory 的 displayName 获取
            val title = container.inventorySlots
                .firstOrNull { it.inventory !== player.inventory }
                ?.inventory?.displayName?.unformattedText
            addProperty("title", title ?: "")

            // 光标上持有的物品（使用 ItemStackSerializer 保持与 1.21.11 端格式一致）
            add("carriedItem", ItemStackSerializer.serializeSlot(player.inventory.itemStack))
        }

        return ActionResult.ok("Container state queried", data)
    }
}
