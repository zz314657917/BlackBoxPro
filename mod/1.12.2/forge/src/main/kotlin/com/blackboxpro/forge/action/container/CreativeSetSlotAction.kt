package com.blackboxpro.forge.action.container

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.CPacketCreativeInventoryAction

/**
 * 创造模式设置槽位。当前实现仅支持清空槽位（设置为 EMPTY）。
 */
class CreativeSetSlotAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val slot = params.requireInt("slot")

        val connection = Minecraft.getMinecraft().connection
            ?: return ActionResult.fail("Not connected to server")

        connection.sendPacket(CPacketCreativeInventoryAction(slot, ItemStack.EMPTY))
        return ActionResult.ok("Set creative slot $slot to empty")
    }
}
