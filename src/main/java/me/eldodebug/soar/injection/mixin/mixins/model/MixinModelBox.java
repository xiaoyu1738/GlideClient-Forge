package me.eldodebug.soar.injection.mixin.mixins.model;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.model.ModelBox;
import net.minecraft.client.renderer.WorldRenderer;

@Mixin(ModelBox.class)
public class MixinModelBox {

	@Unique
	private boolean glide$restoreDisabledCullFace;

	@Inject(method = "render", at = @At("HEAD"))
    private void glide$enableCullFace(WorldRenderer renderer, float scale, CallbackInfo ci) {
		glide$restoreDisabledCullFace = !GL11.glIsEnabled(GL11.GL_CULL_FACE);
		if (glide$restoreDisabledCullFace) {
			GL11.glEnable(GL11.GL_CULL_FACE);
		}
    }

	@Inject(method = "render", at = @At("RETURN"))
    private void glide$restoreCullFace(WorldRenderer renderer, float scale, CallbackInfo ci) {
		if (glide$restoreDisabledCullFace) {
			GL11.glDisable(GL11.GL_CULL_FACE);
		}
	}
}
