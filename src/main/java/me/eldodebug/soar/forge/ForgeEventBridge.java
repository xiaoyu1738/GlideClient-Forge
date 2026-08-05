package me.eldodebug.soar.forge;

import me.eldodebug.soar.Glide;
import me.eldodebug.soar.hooks.IngameOverlayRenderer;
import me.eldodebug.soar.management.event.impl.EventPreRenderTick;
import me.eldodebug.soar.management.event.impl.EventRenderTick;
import me.eldodebug.soar.management.event.impl.EventTick;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Adapts Forge events to Glide's existing internal event bus while the client
 * is migrated subsystem by subsystem.
 */
public final class ForgeEventBridge {

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            Glide.getInstance().start();
        } else if (Glide.getInstance().isStarted()) {
            new EventTick().call();
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (!Glide.getInstance().isStarted()) {
            return;
        }

        if (event.phase == TickEvent.Phase.START) {
            new EventPreRenderTick().call();
        } else {
            new EventRenderTick().call();
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type == RenderGameOverlayEvent.ElementType.ALL
                && Glide.getInstance().isStarted()) {
            IngameOverlayRenderer.render(event.partialTicks);
        }
    }
}
