package com.blackboxpro.forge.action.container

import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getIntOrDefault
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.play.client.CPacketCloseWindow

class CloseContainerAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val mc = Minecraft.getMinecraft()
        val player = mc.player
            ?: return ActionResult.fail("Player not available")

        // windowId 可选，默认使用当前打开的容器
        val windowId = params.getIntOrDefault("windowId", player.openContainer.windowId)

        val connection = mc.connection
            ?: return ActionResult.fail("Not connected to server")

        connection.sendPacket(CPacketCloseWindow(windowId))

        // 重置客户端容器状态（恢复到 inventoryContainer）并关闭 GUI
        player.closeScreen()
        mc.displayGuiScreen(null)

        return ActionResult.ok("Closed container window $windowId")
    }
}
