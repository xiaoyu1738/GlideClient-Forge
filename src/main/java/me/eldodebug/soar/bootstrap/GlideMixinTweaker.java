package me.eldodebug.soar.bootstrap;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 * Starts Mixin after Forge has installed runtime deobfuscation.
 *
 * LaunchWrapper delegates every class in an ITweaker's package to its parent
 * class loader. Keep this package limited to bootstrap-only classes so Forge
 * can load the actual mod and all Minecraft-facing code normally.
 */
public final class GlideMixinTweaker implements ITweaker {

    private static final String MIXIN_TWEAKER = "org.spongepowered.asm.launch.MixinTweaker";
    private static final String MIXINS = "org.spongepowered.asm.mixin.Mixins";
    private static final String MIXIN_CONFIG_SOURCE =
            "org.spongepowered.asm.mixin.extensibility.IMixinConfigSource";
    private static final String MIXIN_CONFIG = "mixins.soar.json";
    private static final String WINDOWS_MIXIN_CONFIG = "mixins.soar.windows.json";

    private final ITweaker delegate;

    public GlideMixinTweaker() {
        LaunchClassLoader classLoader = Launch.classLoader;

        try {
            Class<?> tweakerClass = Class.forName(MIXIN_TWEAKER, true, classLoader);
            delegate = (ITweaker) tweakerClass.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to create the embedded Mixin tweaker", e);
        }
    }

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir,
            String profile) {
        delegate.acceptOptions(args, gameDir, assetsDir, profile);
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        ForgeMixinBootstrap.prepare(classLoader);
        delegate.injectIntoClassLoader(classLoader);
        registerMixinConfiguration(MIXIN_CONFIG);
        boolean windows = isWindows();
        boolean wayland = isWaylandSession();
        net.minecraft.launchwrapper.LogWrapper.info(
                "Glide platform bootstrap detected os=%s, wayland=%s",
                System.getProperty("os.name", "unknown"), wayland);
        if (windows) {
            registerMixinConfiguration(WINDOWS_MIXIN_CONFIG);
        } else {
            net.minecraft.launchwrapper.LogWrapper.info(
                    "Glide skipped Windows-only Mixin configuration");
        }
    }

    @Override
    public String getLaunchTarget() {
        return delegate.getLaunchTarget();
    }

    @Override
    public String[] getLaunchArguments() {
        return delegate.getLaunchArguments();
    }

    private void registerMixinConfiguration(String config) {
        try {
            ClassLoader mixinClassLoader = delegate.getClass().getClassLoader();
            Class<?> mixinsClass = Class.forName(MIXINS, true, mixinClassLoader);
            Class<?> configSourceClass = Class.forName(
                    MIXIN_CONFIG_SOURCE, true, mixinClassLoader);
            Method addConfiguration = mixinsClass.getMethod(
                    "addConfiguration", String.class, configSourceClass);
            addConfiguration.invoke(null, config, null);
            net.minecraft.launchwrapper.LogWrapper.info(
                    "Glide registered Mixin configuration %s", config);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Unable to register Glide Mixins", e);
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static boolean isWaylandSession() {
        String sessionType = System.getenv("XDG_SESSION_TYPE");
        if (sessionType != null && "wayland".equalsIgnoreCase(sessionType.trim())) {
            return true;
        }
        String display = System.getenv("WAYLAND_DISPLAY");
        return display != null && !display.trim().isEmpty();
    }
}
