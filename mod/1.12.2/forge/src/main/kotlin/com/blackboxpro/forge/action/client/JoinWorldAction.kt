package com.blackboxpro.forge.action.client

import com.blackboxpro.common.runtime.dispatcher.RuntimeResponseSender
import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import org.apache.logging.log4j.LogManager
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class JoinWorldAction : ActionExecutor {

    private val logger = LogManager.getLogger("BlackBoxPro-JoinWorldAction")

    override fun execute(params: JsonObject): ActionResult =
        ActionResult.fail("JoinWorldAction requires commandId, use execute(params, commandId)")

    override fun execute(params: JsonObject, commandId: String): ActionResult {
        val mc = Minecraft.getMinecraft()
        if (mc.world != null || mc.player != null) {
            return ActionResult.fail("Already in a world, leave the current world first")
        }

        val requestedWorldName = params.requireString("worldName").trim()
        if (requestedWorldName.isBlank()) {
            return ActionResult.fail("worldName cannot be blank")
        }

        val saveLoader = mc.getSaveLoader()
        val saveList = saveLoader.saveList

        val target = saveList.firstOrNull { save ->
            val normalized = requestedWorldName.lowercase(Locale.ROOT)
            save.fileName.lowercase(Locale.ROOT) == normalized ||
                save.displayName.lowercase(Locale.ROOT) == normalized
        }

        if (target == null) {
            return ActionResult.fail("World not found: $requestedWorldName")
        }

        val folderName = target.fileName
        val displayName = target.displayName
        val completed = AtomicBoolean(false)

        mc.addScheduledTask {
            try {
                mc.launchIntegratedServer(folderName, displayName, null)
            } catch (t: Throwable) {
                logger.error("Failed to join world: {}", displayName, t)
                if (completed.compareAndSet(false, true)) {
                    RuntimeResponseSender.sendResponse(
                        id = commandId,
                        status = "failure",
                        message = "Failed to join world: ${t.message}"
                    )
                }
            }
        }

        val scheduler = Executors.newSingleThreadScheduledExecutor { r ->
            Thread(r, "BlackBoxPro-JoinWorld").apply { isDaemon = true }
        }
        val startedAt = System.nanoTime()

        fun poll() {
            if (completed.get()) {
                scheduler.shutdown()
                return
            }
            val elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
            if (elapsed >= 90_000L) {
                if (completed.compareAndSet(false, true)) {
                    RuntimeResponseSender.sendResponse(
                        id = commandId,
                        status = "failure",
                        message = "Timed out waiting to join world: $requestedWorldName"
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
                            message = "Joined world: $displayName",
                            data = JsonObject().apply {
                                addProperty("worldId", folderName)
                                addProperty("worldName", displayName)
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
