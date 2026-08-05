package me.eldodebug.soar.hooks;

import eu.shoroa.contrib.render.ShBlur;
import me.eldodebug.soar.gui.GuiEditHUD;
import me.eldodebug.soar.gui.modmenu.GuiModMenu;
import me.eldodebug.soar.management.event.impl.EventRender2D;
import me.eldodebug.soar.management.event.impl.EventRenderDamageTint;
import me.eldodebug.soar.management.event.impl.EventRenderNotification;
import net.minecraft.client.Minecraft;

public final class IngameOverlayRenderer {

    private IngameOverlayRenderer() {
    }

    public static void render(float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();

        ShBlur.getInstance().render();
        new EventRenderDamageTint(partialTicks).call();

        if (!(mc.currentScreen instanceof GuiEditHUD)) {
            new EventRender2D(partialTicks).call();

            if (!(mc.currentScreen instanceof GuiModMenu)) {
                new EventRenderNotification().call();
            }
        }
    }
}
