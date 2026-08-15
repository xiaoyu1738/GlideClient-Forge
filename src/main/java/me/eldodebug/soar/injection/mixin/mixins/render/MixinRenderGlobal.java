package me.eldodebug.soar.injection.mixin.mixins.render;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import me.eldodebug.soar.injection.interfaces.IMixinRenderGlobal;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.RenderGlobal;

@Mixin(RenderGlobal.class)
public class MixinRenderGlobal implements IMixinRenderGlobal {

    @Shadow
    private WorldClient theWorld;
    
	@Override
	public WorldClient glide$getWorldClient() {
		return theWorld;
	}
}
