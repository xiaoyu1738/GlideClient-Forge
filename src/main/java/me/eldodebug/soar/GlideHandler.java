package me.eldodebug.soar;

import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

import me.eldodebug.soar.management.event.impl.*;
import me.eldodebug.soar.management.mods.RestrictedMod;
import org.apache.commons.lang3.StringUtils;

import me.eldodebug.soar.gui.modmenu.GuiModMenu;
import me.eldodebug.soar.management.cape.CapeManager;
import me.eldodebug.soar.management.cape.impl.Cape;
import me.eldodebug.soar.management.event.EventTarget;
import me.eldodebug.soar.management.profile.Profile;
import me.eldodebug.soar.utils.OptifineUtils;
import me.eldodebug.soar.utils.TargetUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.play.server.S2EPacketCloseWindow;
import net.minecraft.util.ResourceLocation;

public class GlideHandler {

	private Minecraft mc = Minecraft.getMinecraft();
	
	private Glide instance;
	
	private String prevOfflineName;
	private ResourceLocation offlineSkin;
	
	public GlideHandler() {
		instance = Glide.getInstance();
	}
	
	@EventTarget
	public void onTick(EventTick event) {
		OptifineUtils.disableFastRender();
	}
	
	@EventTarget
	public void onJoinServer(EventJoinServer event) {
		for(Profile p : instance.getProfileManager().getProfiles()) {
			if(!p.getServerIp().isEmpty() && StringUtils.containsIgnoreCase(event.getIp(), p.getServerIp())) {
				instance.getModManager().disableAll();
				instance.getProfileManager().load(p.getJsonFile());
				break;
			}
		}

		instance.getRestrictedMod().joinServer(event.getIp());
	}

	@EventTarget
	public void onLoadWorld(EventLoadWorld event) {
		instance.getRestrictedMod().joinWorld();
	}
	
	@EventTarget
	public void onUpdate(EventUpdate event) {
		TargetUtils.onUpdate();
	}
	
	@EventTarget
	public void onClickMouse(EventClickMouse event) {
        if (mc.gameSettings.keyBindTogglePerspective.isPressed()) {
            mc.gameSettings.thirdPersonView = (mc.gameSettings.thirdPersonView + 1) % 3;
            mc.renderGlobal.setDisplayListEntitiesDirty();
        }
	}
	
	@EventTarget
	public void onReceivePacket(EventReceivePacket event) {
    	if(event.getPacket() instanceof S2EPacketCloseWindow && mc.currentScreen instanceof GuiModMenu) {
    		event.setCancelled(true);
    	}
	}
	
	@EventTarget
	public void onCape(EventLocationCape event) {
		
		CapeManager capeManager = instance.getCapeManager();
		
		if(event.getPlayerInfo() != null && event.getPlayerInfo().getGameProfile().getId().equals(mc.thePlayer.getGameProfile().getId())) {
			
			Cape currentCape = capeManager.getCurrentCape();
			
			if(!currentCape.equals(capeManager.getCapeByName("None"))) {
				event.setCancelled(true);
				event.setCape(currentCape.getCape());
			}
		}
	}
}
