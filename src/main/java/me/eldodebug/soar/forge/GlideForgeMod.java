package me.eldodebug.soar.forge;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * Standard Forge lifecycle entry point for the incremental Glide port.
 */
@Mod(
        modid = GlideForgeMod.MOD_ID,
        name = GlideForgeMod.NAME,
        version = GlideForgeMod.VERSION,
        acceptedMinecraftVersions = "[1.8.9]",
        acceptableRemoteVersions = "*",
        clientSideOnly = true)
public final class GlideForgeMod {

    public static final String MOD_ID = "glideclient";
    public static final String NAME = "Glide Client";
    public static final String VERSION = "7.2-forge.2";

    private final ForgeEventBridge eventBridge = new ForgeEventBridge();

    @Mod.EventHandler
    public void preInitialize(FMLPreInitializationEvent event) {
        event.getModLog().info("Glide Client Forge lifecycle initialized from {}",
                event.getSourceFile());
    }

    @Mod.EventHandler
    public void initialize(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(eventBridge);
        FMLCommonHandler.instance().bus().register(eventBridge);
    }
}
