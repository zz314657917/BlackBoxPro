package com.blackboxpro.neoforge.action.advanced

import com.blackboxpro.neoforge.action.ActionExecutor
import com.blackboxpro.neoforge.action.ActionResult
import com.blackboxpro.neoforge.util.requireInt
import com.blackboxpro.neoforge.util.requireString
import com.google.gson.JsonObject
import net.minecraft.client.Minecraft
import net.minecraft.network.protocol.game.ServerboundEditBookPacket
import java.util.Optional

class SignBookAction : ActionExecutor {
    override fun execute(params: JsonObject): ActionResult {
        val slot = params.requireInt("slot")
        val pagesArray = params.getAsJsonArray("pages")
            ?: return ActionResult.fail("Missing required field: pages")
        val title = params.requireString("title")

        if (pagesArray.size() > 200) {
            return ActionResult.fail("Too many pages: ${pagesArray.size()} > 200")
        }

        val pages = pagesArray.map { it.asString }
        pages.forEachIndexed { i, page ->
            if (page.length > 32767) {
                return ActionResult.fail("Page $i too long: ${page.length} > 32767")
            }
        }

        val networkHandler = Minecraft.getInstance().connection
            ?: return ActionResult.fail("Not connected to server")

        networkHandler.send(
            ServerboundEditBookPacket(slot, pages, Optional.of(title))
        )
        return ActionResult.ok("Signed book '$title' in slot $slot with ${pages.size} pages")
    }
}
