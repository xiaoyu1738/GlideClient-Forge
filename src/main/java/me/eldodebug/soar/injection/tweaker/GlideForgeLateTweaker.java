package me.eldodebug.soar.injection.tweaker;

import java.io.File;
import java.util.List;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 * Final Forge bootstrap stage. LaunchWrapper visits each tweak class once, so
 * this distinct class lets FMLDeobfTweaker run before Glide initializes Mixin.
 */
public final class GlideForgeLateTweaker implements ITweaker {

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        if (!GlideTweaker.isForgeDeobfuscationReady(classLoader)) {
            throw new IllegalStateException(
                    "Forge did not install its deobfuscation transformer before Glide started");
        }

        GlideTweaker.bootstrap(classLoader, "searge");
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
