package com.blackboxpro.neoforge.action.advanced

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.requireBoolean
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundLockDifficultyPacket

class LockDifficultyAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val locked = params.requireBoolean("locked")

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.send(ServerboundLockDifficultyPacket(locked))
        return ActionResult.ok("Difficulty lock set to $locked")
    }
}
