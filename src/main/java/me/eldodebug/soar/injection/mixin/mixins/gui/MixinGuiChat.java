package me.eldodebug.soar.injection.mixin.mixins.gui;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import me.eldodebug.soar.hooks.GuiChatHook;
import me.eldodebug.soar.management.mods.impl.ChatTranslateMod;
import me.eldodebug.soar.utils.Multithreading;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

@Mixin(GuiChat.class)
public class MixinGuiChat extends GuiScreen {

	@Shadow
	protected GuiTextField inputField;
	
	@Redirect(method = "keyTyped", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiScreen;sendChatMessage(Ljava/lang/String;)V"))
	public void cancelSendMessage(GuiScreen screen, String ignoredMessage) {
		
        String s = this.inputField.getText().trim();
        
        if (s.length() > 0) {
        	
    		if(ChatTranslateMod.getInstance().isToggled() && GuiChatHook.isToggled()) {
    			Multithreading.runAsync(() -> {
    				GuiChatHook.sendTranslatedMessage(s);
    			});
    		} else {
    			this.sendChatMessage(s);
    		}
        }
	}
}
