package me.eldodebug.soar.injection.mixin.mixins.layer;

import me.eldodebug.soar.management.mods.impl.EarsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.entity.layers.LayerDeadmau5Head;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LayerDeadmau5Head.class)
public abstract class MixinLayerDeadmau5Head {

    @Final @Shadow private RenderPlayer playerRenderer;

    @Inject(method = "doRenderLayer", at = @At("HEAD"), cancellable = true)
    private void glide$renderEars(AbstractClientPlayer e, float p_177141_2_, float p_177141_3_, float partialTicks, float p_177141_5_, float p_177141_6_, float p_177141_7_, float scale, CallbackInfo ci) {
        if (e == Minecraft.getMinecraft().thePlayer && !e.isInvisible() && EarsMod.getInstance().isToggled()) {
            EarsMod.drawLeft(e, partialTicks, playerRenderer);
            EarsMod.drawRight(e, partialTicks, playerRenderer);
            ci.cancel();
        }
    }

}
