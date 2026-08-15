package me.eldodebug.soar.injection.mixin.mixins.world;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import me.eldodebug.soar.injection.interfaces.IMixinWorld;
import me.eldodebug.soar.management.language.TranslateText;
import me.eldodebug.soar.management.mods.impl.WeatherChangerMod;
import me.eldodebug.soar.management.mods.settings.impl.ComboSetting;
import me.eldodebug.soar.management.mods.settings.impl.combo.Option;
import net.minecraft.client.particle.EntityFX;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityTNTPrimed;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

@Mixin(World.class)
public abstract class MixinWorld implements IMixinWorld {

	@Shadow 
	@Final 
	public boolean isRemote;
	
	@Shadow
	protected abstract boolean isChunkLoaded(int x, int z, boolean allowEmpty);
	
    @Inject(method = "getRainStrength", at = @At("HEAD"), cancellable = true)
    public void preGetRainStrength(float delta, CallbackInfoReturnable<Float> cir) {
    	
    	WeatherChangerMod mod = WeatherChangerMod.getInstance();
    	ComboSetting setting = mod.getWeatherSetting();
    	Option weather = setting.getOption();
    	
        if (mod.isToggled() && weather.getTranslate().equals(TranslateText.CLEAR)) {
            cir.setReturnValue(0f);
        } else if (mod.isToggled()) {
            cir.setReturnValue(mod.getRainStrength().getValueFloat());
        }
    }

    @Inject(method = "getThunderStrength", at = @At("HEAD"), cancellable = true)
    public void preGgetThunderStrength(float delta, CallbackInfoReturnable<Float> cir) {
    	
    	WeatherChangerMod mod = WeatherChangerMod.getInstance();
    	ComboSetting setting = mod.getWeatherSetting();
    	Option weather = setting.getOption();
    	
        if (mod.isToggled() && !weather.getTranslate().equals(TranslateText.STORM)) {
            cir.setReturnValue(0f);
        } else if (mod.isToggled()) {
            cir.setReturnValue(mod.getThunderStrength().getValueFloat());
        }
    }
    
	@Override
	public boolean glide$isLoaded(int x, int z, boolean allowEmpty) {
		return isChunkLoaded(x, z, allowEmpty);
	}
	
    @ModifyVariable(method = "updateEntityWithOptionalForce", at = @At("STORE"), ordinal = 1)
    private boolean checkIfWorldIsRemoteBeforeForceUpdating(boolean isForced) {
        return isForced && !this.isRemote;
    }
    
    @Inject(method = "getCollidingBoundingBoxes", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getEntitiesWithinAABBExcludingEntity(Lnet/minecraft/entity/Entity;Lnet/minecraft/util/AxisAlignedBB;)Ljava/util/List;"), cancellable = true, locals = LocalCapture.CAPTURE_FAILSOFT)
    private void filterEntities(Entity entityIn, AxisAlignedBB bb, CallbackInfoReturnable<List<AxisAlignedBB>> cir, List<AxisAlignedBB> list) {
        if (entityIn instanceof EntityTNTPrimed || entityIn instanceof EntityFallingBlock || entityIn instanceof EntityItem || entityIn instanceof EntityFX) {
            cir.setReturnValue(list);
        }
    }
    
}
