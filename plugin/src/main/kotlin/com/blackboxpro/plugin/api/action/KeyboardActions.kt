package com.blackboxpro.plugin.api.action

import com.blackboxpro.common.protocol.ResponseMessage
import com.blackboxpro.plugin.api.BlackBoxApi
import com.google.gson.JsonObject
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

object KeyboardActions {

    fun keyPress(
        player: Player,
        key: String? = null,
        keyCode: Int? = null,
        char: Char? = null,
        pressTicks: Int = 1
    ): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "key_press", JsonObject().apply {
            if (key != null) addProperty("key", key)
            if (keyCode != null) addProperty("keyCode", keyCode)
            if (char != null) addProperty("char", char.toString())
            addProperty("pressTicks", pressTicks)
        })

    fun typeText(
        player: Player,
        text: String,
        intervalTicks: Int = 0
    ): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "type_text", JsonObject().apply {
            addProperty("text", text)
            addProperty("intervalTicks", intervalTicks)
        })
}
