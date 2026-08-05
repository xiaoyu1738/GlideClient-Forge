package me.eldodebug.soar.injection.tweaker;

import java.io.File;
import java.util.List;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 * Defers Glide's transformers until Forge has installed its runtime
 * deobfuscation transformer.
 */
public final class GlideForgeTweaker implements ITweaker {

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        if (GlideTweaker.isForgeDeobfuscationReady(classLoader)) {
            GlideTweaker.bootstrap(classLoader, "searge");
            return;
        }

        GlideTweaker.queueTweak(GlideForgeLateTweaker.class.getName());
    }

    @Override
    public String getLaunchTarget() {
        return "net.minecraft.client.main.Main";
    }

    @Override
    public String[] getLaunchArguments() {
        return new String[0];
    }
}
