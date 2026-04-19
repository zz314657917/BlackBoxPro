package com.blackboxpro.neoforge.mixin;

import com.blackboxpro.neoforge.action.client.ScreenshotTooltipAction;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在 GUI 渲染完成后（guiRenderer.render() 之后）注入截图点。
 * 此时主帧缓冲包含完整的 screen + tooltip 内容。
 */
@Mixin(GameRenderer.class)
public class GameRendererMixin {

    /**
     * 注入到 GameRenderer.render() 中 guiRenderer.incrementFrameNumber() 调用之前。
     * 此时 guiRenderer.render() 已执行完毕，帧缓冲包含完整 GUI。
     */
    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/render/GuiRenderer;incrementFrameNumber()V"))
    private void blackboxpro_afterGuiRender(CallbackInfo ci) {
        ScreenshotTooltipAction.Companion.onPostGuiRender();
    }
}
