package me.eldodebug.soar.injection.mixin.mixins.gui;

import me.eldodebug.soar.injection.interfaces.IMixinGuiIngame;
import me.eldodebug.soar.injection.interfaces.IMixinMinecraft;
import me.eldodebug.soar.management.event.impl.EventRenderSelectedItem;
import me.eldodebug.soar.management.event.impl.EventRenderTooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraftforge.client.GuiIngameForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Covers the selected-item overlay that Forge 1.8.9 copies instead of
 * delegating to GuiIngame. Forge exposes no event for this element.
 */
@Mixin(value = GuiIngameForge.class, remap = false)
public abstract class MixinGuiIngameForge {

    @Inject(method = "renderToolHightlight", at = @At("HEAD"),
			cancellable = true, remap = false)
    private void glide$renderToolHighlight(ScaledResolution resolution, CallbackInfo ci) {
        IMixinGuiIngame overlay = (IMixinGuiIngame) this;
        int remainingTicks = overlay.glide$getRemainingHighlightTicks();

        if (remainingTicks > 0 && overlay.glide$getHighlightingItemStack() != null) {
            int alpha = Math.min(255, (int) (remainingTicks * 256.0F / 10.0F));
            new EventRenderSelectedItem(0xFFFFFF | (alpha << 24)).call();
        }

        EventRenderTooltip tooltip = new EventRenderTooltip(
				((IMixinMinecraft) Minecraft.getMinecraft())
						.glide$getTimer().renderPartialTicks);
        tooltip.call();
        if (tooltip.isCancelled()) {
            ci.cancel();
        }
    }
}
