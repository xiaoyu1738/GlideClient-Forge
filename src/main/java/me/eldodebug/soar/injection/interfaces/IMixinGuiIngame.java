package me.eldodebug.soar.injection.interfaces;

import net.minecraft.item.ItemStack;

public interface IMixinGuiIngame {
	int glide$getUpdateCounter();
	int glide$getRemainingHighlightTicks();
	ItemStack glide$getHighlightingItemStack();
}
