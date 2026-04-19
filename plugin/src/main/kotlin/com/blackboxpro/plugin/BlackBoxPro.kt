package com.blackboxpro.plugin

import com.blackboxpro.plugin.api.BlackBoxApi
import taboolib.common.platform.Plugin
import taboolib.common.platform.function.info
import taboolib.common.util.unsafeLazy
import taboolib.platform.util.bukkitPlugin

/**
 * BlackBoxPro 服务端插件主入口。
 *
 * 通过 TabooLib 自动加载，无需手动注册。
 * ChannelHandler 通过 @Awake 自动注册通道。
 */
object BlackBoxPro : Plugin() {

    val VERSION by unsafeLazy { bukkitPlugin.description.version }

    override fun onEnable() {
        info("[BlackBoxPro] Server plugin v$VERSION enabled.")
    }

    override fun onDisable() {
        BlackBoxApi.shutdown()
        info("[BlackBoxPro] Server plugin disabled.")
    }
}
