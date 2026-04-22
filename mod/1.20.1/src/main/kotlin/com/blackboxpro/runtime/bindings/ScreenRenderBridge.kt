package com.blackboxpro.runtime.bindings

/**
 * 骞冲彴鏃犲叧鐨?Screen 娓叉煋鍥炶皟妗ユ帴銆?
 *
 * NeoForge 绔€氳繃 ScreenEvent.Render.Post 浜嬩欢瀹炵幇锛?
 * Fabric 绔€氳繃 ScreenEvents.afterRender 瀹炵幇銆?
 *
 * 鐢ㄤ簬 ScreenshotTooltipAction 鍦?screen 娓叉煋瀹屾垚鍚庢敞鍏?tooltip 缁樺埗銆?
 */
object ScreenRenderBridge {

    /**
     * 娉ㄥ唽涓€娆℃€х殑 screen 娓叉煋鍚庡洖璋冦€?
     * 鍥炶皟鍦ㄤ笅涓€甯?screen 娓叉煋瀹屾垚鍚庤Е鍙戯紝浠呰Е鍙戜竴娆°€?
     *
     * @param callback 鎺ユ敹褰撳墠甯х殑 DrawContext/GuiGraphics 瀹炰緥锛堝钩鍙扮浉鍏筹紝闇€鑷 cast锛?
     */
    fun interface AfterRenderCallback {
        fun onAfterRender(drawContext: Any)
    }

    fun interface Provider {
        fun registerOneShotAfterRender(callback: AfterRenderCallback)
    }

    @Volatile
    private var provider: Provider? = null

    fun bind(provider: Provider) {
        this.provider = provider
    }

    fun registerOneShotAfterRender(callback: AfterRenderCallback) {
        val p = provider ?: throw IllegalStateException("ScreenRenderBridge not bound")
        p.registerOneShotAfterRender(callback)
    }
}

