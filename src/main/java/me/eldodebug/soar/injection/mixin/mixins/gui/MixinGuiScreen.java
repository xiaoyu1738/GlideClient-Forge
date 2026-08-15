package me.eldodebug.soar.injection.mixin.mixins.gui;

import me.eldodebug.soar.injection.interfaces.IMixinGuiScreen;
import net.minecraft.client.gui.GuiScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GuiScreen.class)
public abstract class MixinGuiScreen implements IMixinGuiScreen {

	@Override
	@Invoker("keyTyped")
	public abstract void glide$invokeKeyTyped(char typedChar, int keyCode);
}
