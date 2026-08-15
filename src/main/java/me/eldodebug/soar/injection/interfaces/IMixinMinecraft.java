package me.eldodebug.soar.injection.interfaces;

import net.minecraft.client.resources.DefaultResourcePack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Session;
import net.minecraft.util.Timer;

public interface IMixinMinecraft {
	boolean glide$isRunning();
	Timer glide$getTimer();
	void glide$setSession(Session session);
	void glide$callClickMouse();
	void glide$callRightClickMouse();
	void glide$setLeftClickCounter(int value);
	DefaultResourcePack glide$getMcDefaultResourcePack();
    void glide$resizeWindow(int width, int height);
    Entity glide$getRenderViewEntity();
}
