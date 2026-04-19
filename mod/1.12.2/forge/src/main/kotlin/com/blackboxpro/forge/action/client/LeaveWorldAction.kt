package com.blackboxpro.forge.action.client

import com.blackboxpro.common.runtime.dispatcher.RuntimeResponseSender
import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiMainMenu
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class LeaveWorldAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult =
        ActionResult.fail("LeaveWorldAction requires commandId, use execute(params, commandId)")

    override fun execute(params: JsonObject, commandId: String): ActionResult {
        val mc = Minecraft.getMinecraft()
        if (mc.world == null && mc.player == null && mc.currentScreen is GuiMainMenu) {
            return ActionResult.ok(
                "Already at main menu",
                JsonObject().apply { addProperty("state", "main_menu") }
            )
        }

        val completed = AtomicBoolean(false)

        mc.addScheduledTask {
            mc.loadWorld(null)
            mc.displayGuiScreen(GuiMainMenu())
        }

        val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "BlackBoxPro-LeaveWorld").apply { isDaemon = true }
        }
        val startedAt = System.nanoTime()

        fun poll() {
            if (completed.get()) {
                scheduler.shutdown()
                return
            }
            val elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
            if (elapsed >= 60_000L) {
                if (completed.compareAndSet(false, true)) {
                    RuntimeResponseSender.sendResponse(
                        id = commandId,
                        status = "failure",
                        message = "Timed out waiting to leave the current world"
                    )
                }
                scheduler.shutdown()
                return
            }
            mc.addScheduledTask {
                if (completed.get()) return@addScheduledTask
                if (mc.world == null && mc.player == null && mc.currentScreen is GuiMainMenu) {
                    if (completed.compareAndSet(false, true)) {
                        RuntimeResponseSender.sendResponse(
                            id = commandId,
                            status = "success",
                            message = "Returned to main menu",
                            data = JsonObject().apply { addProperty("state", "main_menu") }
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
