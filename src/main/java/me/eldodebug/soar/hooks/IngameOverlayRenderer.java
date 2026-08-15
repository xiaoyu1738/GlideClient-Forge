package me.eldodebug.soar.hooks;

import eu.shoroa.contrib.render.ShBlur;
import me.eldodebug.soar.Glide;
import me.eldodebug.soar.gui.GuiEditHUD;
import me.eldodebug.soar.gui.modmenu.GuiModMenu;
import me.eldodebug.soar.logger.GlideLogger;
import me.eldodebug.soar.management.event.impl.EventRender2D;
import me.eldodebug.soar.management.event.impl.EventRenderDamageTint;
import me.eldodebug.soar.management.event.impl.EventRenderNotification;
import net.minecraft.client.Minecraft;

public final class IngameOverlayRenderer {

	private static boolean blurAvailable = true;

    private IngameOverlayRenderer() {
    }

    public static void render(float partialTicks) {
        if (Glide.getInstance().getEventManager() == null) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();

		if (blurAvailable) {
			try {
				ShBlur.getInstance().render();
			} catch (VirtualMachineError error) {
				throw error;
			} catch (ThreadDeath death) {
				throw death;
			} catch (Throwable throwable) {
				blurAvailable = false;
				GlideLogger.getLogger().error(
						"[GC/ERROR] Disabling blur rendering after an unrecoverable failure",
						throwable);
			}
		}
		new EventRenderDamageTint(partialTicks).call();

        if (!(mc.currentScreen instanceof GuiEditHUD)) {
            new EventRender2D(partialTicks).call();

            if (!(mc.currentScreen instanceof GuiModMenu)) {
                new EventRenderNotification().call();
            }
        }
    }
}
