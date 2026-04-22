package com.blackboxpro.forge.action.container

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireInt
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.world.item.ItemStack
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket

/**
 * 鍒涢€犳ā寮忚缃Ы浣嶃€傚綋鍓嶅疄鐜颁粎鏀寔娓呯┖妲戒綅锛堣缃负 EMPTY锛夈€?
 * 瀹屾暣鐨勭墿鍝佽缃渶瑕?ItemStack 鐨?NBT 鍙嶅簭鍒楀寲鏀寔銆?
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

