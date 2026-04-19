package com.blackboxpro.neoforge.action.container

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.getIntOrDefault
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket

class CloseContainerAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val client = Minecraft.getInstance()
        val player = client.player
            ?: return ActionResult.fail("Player not available")

        // windowId 可选，默认使用当前打开的容器
        val windowId = params.getIntOrDefault("windowId", player.containerMenu.containerId)

        val connection = client.connection
            ?: return ActionResult.fail("Not connected to server")

        connection.send(ServerboundContainerClosePacket(windowId))

        // 同步关闭客户端 GUI
        client.setScreen(null)

        return ActionResult.ok("Closed container window $windowId")
    }
}
