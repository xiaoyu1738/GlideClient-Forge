package me.eldodebug.soar.injection.mixin.mixins.gui;

import me.eldodebug.soar.injection.interfaces.IMixinGuiIngame;
import me.eldodebug.soar.management.event.impl.EventRenderScoreboard;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.item.ItemStack;
import net.minecraft.scoreboard.ScoreObjective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Retains only hooks for which Forge 1.8.9 has no equivalent event. Overlay
 * elements implemented by GuiIngameForge are bridged from ForgeEventBridge.
 */
@Mixin(GuiIngame.class)
public abstract class MixinGuiIngame implements IMixinGuiIngame {

    @Shadow
    private int updateCounter;

    @Shadow
    private int remainingHighlightTicks;

    @Shadow
    private ItemStack highlightingItemStack;

    @Inject(method = "renderScoreboard", at = @At("HEAD"), cancellable = true)
    private void glide$renderScoreboard(ScoreObjective objective,
            ScaledResolution resolution, CallbackInfo ci) {
        EventRenderScoreboard event = new EventRenderScoreboard(objective);
        event.call();
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Override
    public int glide$getUpdateCounter() {
        return updateCounter;
    }

    @Override
    public int glide$getRemainingHighlightTicks() {
        return remainingHighlightTicks;
    }

    @Override
    public ItemStack glide$getHighlightingItemStack() {
        return highlightingItemStack;
    }
}
