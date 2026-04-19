package com.blackboxpro.forge.action.client

import com.blackboxpro.common.runtime.dispatcher.RuntimeResponseSender
import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.getIntOrDefault
import com.blackboxpro.forge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiMainMenu
import net.minecraft.client.multiplayer.GuiConnecting
import net.minecraft.client.multiplayer.ServerData
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class ConnectToServerAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult =
        ActionResult.fail("ConnectToServerAction requires commandId, use execute(params, commandId)")

    override fun execute(params: JsonObject, commandId: String): ActionResult {
        val ip = params.requireString("ip")
        val port = params.getIntOrDefault("port", 25565)
        val address = if (port == 25565) ip else "$ip:$port"

        val mc = Minecraft.getMinecraft()
        if (mc.world != null) {
            return ActionResult.fail("Already in a world, disconnect first")
        }

        val completed = AtomicBoolean(false)

        mc.addScheduledTask {
            try {
                val serverData = ServerData("BlackBoxPro", address, false)
                mc.displayGuiScreen(GuiConnecting(mc.currentScreen ?: GuiMainMenu(), mc, serverData))
            } catch (t: Throwable) {
                if (completed.compareAndSet(false, true)) {
                    RuntimeResponseSender.sendResponse(
                        id = commandId,
                        status = "failure",
                        message = "Failed to connect: ${t.message}"
                    )
                }
            }
        }

        val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "BlackBoxPro-ConnectToServer").apply { isDaemon = true }
        }
        val startedAt = System.nanoTime()

        fun poll() {
            if (completed.get()) {
                scheduler.shutdown()
                return
            }
            val elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
            if (elapsed >= 30_000L) {
                if (completed.compareAndSet(false, true)) {
                    RuntimeResponseSender.sendResponse(
                        id = commandId,
                        status = "failure",
                        message = "Timed out waiting to connect to $address"
                    )
                }
                scheduler.shutdown()
                return
            }
            mc.addScheduledTask {
                if (completed.get()) return@addScheduledTask
                if (mc.world != null && mc.player != null) {
                    if (completed.compareAndSet(false, true)) {
                        RuntimeResponseSender.sendResponse(
                            id = commandId,
                            status = "success",
                            message = "Connected to $address",
                            data = JsonObject().apply {
                                addProperty("address", address)
                                addProperty("state", "in_world")
                            }
                        )
                    }
                    scheduler.shutdown()
                } else {
                    scheduler.schedule(::poll, 200, TimeUnit.MILLISECONDS)
                }
            }
        }

        scheduler.schedule(::poll, 500, TimeUnit.MILLISECONDS)
        return ActionResult.async()
    }
}
