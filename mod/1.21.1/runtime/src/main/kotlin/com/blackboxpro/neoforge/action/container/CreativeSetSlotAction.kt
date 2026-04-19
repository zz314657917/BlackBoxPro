package com.blackboxpro.neoforge.action.container

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.world.item.ItemStack
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket

/**
 * 创造模式设置槽位。当前实现仅支持清空槽位（设置为 EMPTY）。
 * 完整的物品设置需要 ItemStack 的 NBT 反序列化支持。
 */
class CreativeSetSlotAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val slot = params.requireInt("slot")

        val handler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        handler.send(ServerboundSetCreativeModeSlotPacket(slot, ItemStack.EMPTY))
        return ActionResult.ok("Set creative slot $slot to empty")
    }
}
