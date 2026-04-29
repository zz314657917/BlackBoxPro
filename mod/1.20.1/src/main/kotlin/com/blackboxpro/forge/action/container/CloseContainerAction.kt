package com.blackboxpro.forge.action.container

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getIntOrDefault
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket

class CloseContainerAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val client = Minecraft.getInstance()
        val player = client.player
            ?: return ActionResult.fail("Player not available")

        // windowId 鍙€夛紝榛樿浣跨敤褰撳墠鎵撳紑鐨勫鍣?
        val windowId = params.getIntOrDefault("windowId", player.containerMenu.containerId)

        val connection = client.connection
            ?: return ActionResult.fail("Not connected to server")

        connection.send(ServerboundContainerClosePacket(windowId))

        // 鍚屾鍏抽棴瀹㈡埛绔?GUI
        client.setScreen(null)

        return ActionResult.ok("Closed container window $windowId")
    }
}

