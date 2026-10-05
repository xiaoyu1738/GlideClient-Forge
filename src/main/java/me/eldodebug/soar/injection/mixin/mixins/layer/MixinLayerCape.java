package me.eldodebug.soar.injection.mixin.mixins.layer;

import me.eldodebug.soar.management.mods.impl.waveycapes.layers.CustomCapeRenderLayer;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.layers.LayerCape;
import net.minecraft.entity.player.EnumPlayerModelParts;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LayerCape.class)
public class MixinLayerCape {

    @Inject(method = "doRenderLayer", at = @At("HEAD"), cancellable = true)
    private void skipVanillaCapeWhenWavey(
            AbstractClientPlayer player,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            float scale,
            CallbackInfo ci) {
        if (CustomCapeRenderLayer.shouldRender(player)) {
            ci.cancel();
        }
    }

    @Redirect(
            method = "doRenderLayer",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/entity/AbstractClientPlayer;isWearing(Lnet/minecraft/entity/player/EnumPlayerModelParts;)Z"))
    private boolean allowGlideCape(AbstractClientPlayer player, EnumPlayerModelParts part) {
        if (part == EnumPlayerModelParts.CAPE && CustomCapeRenderLayer.hasGlideCape(player)) {
            return true;
        }

        return player.isWearing(part);
    }
}
