package com.blackboxpro.forge.action.client

import com.blackboxpro.common.runtime.dispatcher.RuntimeResponseSender
import com.blackboxpro.common.runtime.scheduler.RuntimeTickScheduler
import com.blackboxpro.forge.action.ActionExecutor
import com.blackboxpro.forge.action.ActionResult
import com.blackboxpro.forge.util.ScreenKeyboardHelper
import com.blackboxpro.forge.util.getIntOrDefault
import com.blackboxpro.forge.util.requireString
import com.google.gson.JsonObject

class TypeTextAction : ActionExecutor {

    override fun execute(params: JsonObject): ActionResult = execute(params, "")

    override fun execute(params: JsonObject, commandId: String): ActionResult {
        val text = params.requireString("text")
        val intervalTicks = params.getIntOrDefault("intervalTicks", 0).coerceAtLeast(0)

        if (intervalTicks <= 0 || text.length <= 1 || commandId.isEmpty()) {
            val state = ScreenKeyboardHelper.typeText(text)
            val data = state.toJson().apply {
                addProperty("textLength", text.length)
                addProperty("intervalTicks", intervalTicks)
            }
            return ActionResult.ok("Typed ${text.length} character(s)", data)
        }

        typeNext(commandId, text, intervalTicks, index = 0)
        return ActionResult.async()
    }

    private fun typeNext(commandId: String, text: String, intervalTicks: Int, index: Int) {
        if (index >= text.length) {
            val data = ScreenKeyboardHelper.KeyboardState(
                mode = "screen_type_text",
                key = null,
                keyCode = -1,
                typedChar = null,
                pressTicks = 0,
                typedCount = text.length
            ).toJson().apply {
                addProperty("textLength", text.length)
                addProperty("intervalTicks", intervalTicks)
            }
            RuntimeResponseSender.sendResponse(commandId, "success", "Typed ${text.length} character(s)", data)
            return
        }

        runCatching {
            ScreenKeyboardHelper.typeCharacter(text[index])
        }.onFailure {
            RuntimeResponseSender.sendResponse(commandId, "failure", it.message ?: "Failed to type text")
            return
        }

        RuntimeTickScheduler.schedule(intervalTicks) {
            typeNext(commandId, text, intervalTicks, index + 1)
        }
    }
}
