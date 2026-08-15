package me.eldodebug.soar.injection.mixin.mixins.multiplayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import me.eldodebug.soar.management.mods.impl.AnimationsMod;
import net.minecraft.client.multiplayer.PlayerControllerMP;

@Mixin(PlayerControllerMP.class)
public class MixinPlayerControllerMP {

    @Inject(method = "getIsHittingBlock", at = @At("HEAD"), cancellable = true)
    private void cancelHit(CallbackInfoReturnable<Boolean> cir) {
    	
    	AnimationsMod mod = AnimationsMod.getInstance();
    	
    	if(mod.isToggled() && mod.getPushingSetting().isToggled() && mod.getBlockHitSetting().isToggled()) {
    		cir.setReturnValue(false);
    	}
    }
}
