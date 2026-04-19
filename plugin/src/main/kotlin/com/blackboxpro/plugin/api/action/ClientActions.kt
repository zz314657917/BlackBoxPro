package com.blackboxpro.plugin.api.action

import com.blackboxpro.plugin.api.BlackBoxApi
import com.blackboxpro.common.protocol.ResponseMessage
import com.google.gson.JsonObject
import org.bukkit.entity.Player
import java.util.concurrent.CompletableFuture

/**
 * 客户端设置与信息行为快捷 API。
 */
object ClientActions {

    fun clientInformation(
        player: Player,
        locale: String = "en_us",
        viewDistance: Int = 12,
        chatMode: Int = 0,
        chatColors: Boolean = true,
        skinParts: Int = 127,
        mainHand: Int = 1,
        textFiltering: Boolean = false,
        allowServerListings: Boolean = true
    ): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "client_information", JsonObject().apply {
            addProperty("locale", locale); addProperty("viewDistance", viewDistance)
            addProperty("chatMode", chatMode); addProperty("chatColors", chatColors)
            addProperty("skinParts", skinParts); addProperty("mainHand", mainHand)
            addProperty("textFiltering", textFiltering); addProperty("allowServerListings", allowServerListings)
        })

    fun playerAbilities(player: Player, flying: Boolean): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "player_abilities", JsonObject().apply {
            addProperty("flying", flying)
        })

    fun resourcePackResponse(player: Player, uuid: String, result: String): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "resource_pack_response", JsonObject().apply {
            addProperty("uuid", uuid); addProperty("result", result)
        })

    fun openInventory(player: Player): CompletableFuture<ResponseMessage> =
        BlackBoxApi.sendAsync(player, "open_inventory")
}
