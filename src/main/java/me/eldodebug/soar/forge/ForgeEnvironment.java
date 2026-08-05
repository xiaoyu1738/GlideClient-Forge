package me.eldodebug.soar.forge;

import net.minecraft.launchwrapper.LaunchClassLoader;

public final class ForgeEnvironment {

    private static volatile boolean optifineLoaded;

    private ForgeEnvironment() {
    }

    static void detect(LaunchClassLoader classLoader) {
        optifineLoaded = classLoader.getResource("optifine/Patcher.class") != null;
    }

    public static boolean isOptifineLoaded() {
        return optifineLoaded;
    }
}
