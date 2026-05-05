package com.blackboxpro.plugin.api.action

import com.blackboxpro.common.protocol.ResponseMessage
import com.blackboxpro.common.protocol.CommandMessage
import com.blackboxpro.plugin.api.BlackBoxApi
import com.blackboxpro.plugin.http.ServerGermActions
import com.google.gson.JsonObject
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture
import java.util.UUID

object MouseActions {

    fun moveMouse(
        player: Player,
        x: Double,
        y: Double
    ): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "move_mouse", JsonObject().apply {
            addProperty("x", x)
            addProperty("y", y)
        })

    fun clickMouse(
        player: Player,
        button: Int = 0,
        clickCount: Int = 1
    ): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "click_mouse", JsonObject().apply {
            addProperty("button", button)
            addProperty("clickCount", clickCount)
        })

    fun clickScreenAt(
        player: Player,
        x: Double,
        y: Double,
        button: Int = 0,
        clickCount: Int = 1
    ): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "click_screen_at", JsonObject().apply {
            addProperty("x", x)
            addProperty("y", y)
            addProperty("button", button)
            addProperty("clickCount", clickCount)
        })

    fun clickGermComponent(
        player: Player,
        x: Double? = null,
        y: Double? = null,
        componentId: String? = null,
        button: Int = 0,
        clickCount: Int = 1,
        maxDepth: Int = 4,
        maxComponents: Int = 200,
        includeFields: Boolean = false,
        fallbackScreenClick: Boolean = false
    ): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "click_germ_component", JsonObject().apply {
            if (x != null) addProperty("x", x)
            if (y != null) addProperty("y", y)
            if (componentId != null) addProperty("componentId", componentId)
            addProperty("button", button)
            addProperty("clickCount", clickCount)
            addProperty("maxDepth", maxDepth)
            addProperty("maxComponents", maxComponents)
            addProperty("includeFields", includeFields)
            addProperty("fallbackScreenClick", fallbackScreenClick)
        })

    fun germGuiPartDos(
        player: Player,
        guiName: String,
        partId: String,
        dosType: String = "click",
        execute: Boolean = true,
        resolvePlaceholders: Boolean = true,
        mode: String = "command_util"
    ): CompletableFuture<ResponseMessage> =
        CompletableFuture.completedFuture(
            ServerGermActions.handle(
                CommandMessage(
                    id = "api-germ-gui-part-dos-${UUID.randomUUID()}",
                    action = "germ_gui_part_dos",
                    params = JsonObject().apply {
                        addProperty("guiName", guiName)
                        addProperty("partId", partId)
                        addProperty("dosType", dosType)
                        addProperty("execute", execute)
                        addProperty("resolvePlaceholders", resolvePlaceholders)
                        addProperty("mode", mode)
                    },
                    target = player.name
                )
            )
        )

    fun clickChatText(
        player: Player,
        match: String,
        index: Int = 0,
        execute: Boolean = true
    ): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "click_chat_text", JsonObject().apply {
            addProperty("match", match)
            addProperty("index", index)
            addProperty("execute", execute)
        })

    fun queryChatStyle(
        player: Player,
        match: String,
        index: Int = 0
    ): CompletableFuture<ResponseMessage> =
        QueryActions.queryChatStyle(player, match, index)

    fun hoverSlot(
        player: Player,
        windowId: Int,
        slot: Int,
        durationTicks: Int = 0
    ): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "hover_slot", JsonObject().apply {
            addProperty("windowId", windowId)
            addProperty("slot", slot)
            addProperty("durationTicks", durationTicks)
        })

    fun querySlotTooltip(
        player: Player,
        slot: Int,
        advanced: Boolean = false
    ): CompletableFuture<ResponseMessage> =
        QueryActions.querySlotTooltip(player, slot, advanced)

    fun queryCursorState(player: Player): CompletableFuture<ResponseMessage> =
        QueryActions.queryCursorState(player)

    fun queryGermScreen(
        player: Player,
        maxDepth: Int = 4,
        maxComponents: Int = 200,
        includeFields: Boolean = false
    ): CompletableFuture<ResponseMessage> =
        QueryActions.queryGermScreen(player, maxDepth, maxComponents, includeFields)

    fun queryGermHitTest(
        player: Player,
        x: Double? = null,
        y: Double? = null,
        maxDepth: Int = 4,
        maxComponents: Int = 200,
        includeFields: Boolean = false
    ): CompletableFuture<ResponseMessage> =
        QueryActions.queryGermHitTest(player, x, y, maxDepth, maxComponents, includeFields)

    fun queryTooltipState(player: Player): CompletableFuture<ResponseMessage> =
        QueryActions.queryTooltipState(player)
}
