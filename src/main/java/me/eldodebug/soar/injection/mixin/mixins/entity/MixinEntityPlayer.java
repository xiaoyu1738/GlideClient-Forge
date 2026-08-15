package me.eldodebug.soar.injection.mixin.mixins.entity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.eldodebug.soar.injection.interfaces.IMixinEntityPlayer;
import me.eldodebug.soar.management.mods.impl.skin3d.render.CustomizableModelPart;
import me.eldodebug.soar.management.mods.impl.waveycapes.sim.StickSimulation;
import net.minecraft.entity.player.EntityPlayer;

@Mixin(EntityPlayer.class)
public class MixinEntityPlayer implements IMixinEntityPlayer {

    private CustomizableModelPart headLayer;
	private CustomizableModelPart[] skinLayer;
	
    private StickSimulation stickSimulation = new StickSimulation();
    
    @Inject(method = "onUpdate", at = @At("HEAD"))
    private void moveCloakUpdate(CallbackInfo info) {
        if((Object)this instanceof EntityPlayer) {
            glide$simulate((EntityPlayer)(Object)this);
        }
    }
    
    @Override
    public StickSimulation glide$getSimulation() {
        return stickSimulation;
    }
    
	@Override
	public CustomizableModelPart[] glide$getSkinLayers() {
		return skinLayer;
	}
	
	@Override
	public void glide$setupSkinLayers(CustomizableModelPart[] box) {
		this.skinLayer = box;
	}
	
	@Override
	public CustomizableModelPart glide$getHeadLayers() {
		return headLayer;
	}
	
	@Override
	public void glide$setupHeadLayers(CustomizableModelPart box) {
		this.headLayer = box;
	}
}
