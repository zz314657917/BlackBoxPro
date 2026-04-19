package com.blackboxpro.neoforge.action.advanced

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket

class RenameItemAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val name = params.requireString("name")

        if (name.length > 50) {
            return ActionResult.fail("Name too long: ${name.length} > 50")
        }

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.send(ServerboundRenameItemPacket(name))
        return ActionResult.ok("Renamed item to '$name'")
    }
}
