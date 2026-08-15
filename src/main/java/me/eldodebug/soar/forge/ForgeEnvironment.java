package me.eldodebug.soar.forge;

import net.minecraft.launchwrapper.Launch;

public final class ForgeEnvironment {

    private static final String OPTIFINE_STATE_KEY = "glide.optifineLoaded";

    private ForgeEnvironment() {
    }

    public static boolean isOptifineLoaded() {
        return Boolean.TRUE.equals(Launch.blackboard.get(OPTIFINE_STATE_KEY));
    }
}
