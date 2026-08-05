package me.eldodebug.soar.injection.mixin.mixins.gui;

import me.eldodebug.soar.hooks.IngameOverlayRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "net.minecraftforge.client.GuiIngameForge", remap = false)
public abstract class MixinGuiIngameForge {

    @Inject(
            method = "func_175180_a(F)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraftforge/client/GuiIngameForge;renderTitle(IIF)V",
                    shift = At.Shift.AFTER,
                    remap = false),
            remap = false)
    private void glide$renderOverlay(float partialTicks, CallbackInfo ci) {
        IngameOverlayRenderer.render(partialTicks);
    }
}
