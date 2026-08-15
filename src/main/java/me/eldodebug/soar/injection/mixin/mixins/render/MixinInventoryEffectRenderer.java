package me.eldodebug.soar.injection.mixin.mixins.render;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.eldodebug.soar.management.mods.impl.InventoryMod;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.InventoryEffectRenderer;
import net.minecraft.inventory.Container;

@Mixin(InventoryEffectRenderer.class)
public abstract class MixinInventoryEffectRenderer extends GuiContainer {

	public MixinInventoryEffectRenderer(Container inventorySlotsIn) {
		super(inventorySlotsIn);
	}

	@Inject(method = "updateActivePotionEffects", at = @At("RETURN"))
	private void centerInventoryWithPotionEffects(CallbackInfo ci) {
		if(InventoryMod.getInstance().isToggled() && InventoryMod.getInstance().getPreventPotionShiftSetting().isToggled()) {
			guiLeft = (width - xSize) / 2;
		}
	}
}
