package me.eldodebug.soar.injection.tweaker;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

import me.eldodebug.soar.injection.transformer.ForgeClasspathTransformer;
import me.eldodebug.soar.injection.transformer.LwjglTransformer;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.launchwrapper.LogWrapper;

public class GlideTweaker implements ITweaker {

    private static final String FORGE_TWEAKER = "net.minecraftforge.fml.common.launcher.FMLTweaker";
    private static final String FORGE_DEOBFUSCATION_TRANSFORMER =
            "net.minecraftforge.fml.common.asm.transformers.DeobfuscationTransformer";
    private static final String MIXIN_CONFIG = "mixins.soar.json";
    private static final String LEGACY_TWEAKER_PACKAGE = "me.eldodebug.soar.injection.mixin";
    private static final String VECMATH_MATRIX = "javax.vecmath.Matrix4f";

    private static final String[] LWJGL2_CLASS_LOADER_EXCLUSIONS = {
            "org.lwjgl.input.",
            "org.lwjgl.openal.",
            "org.lwjgl.opencl.",
            "org.lwjgl.opengl.",
            "org.lwjgl.opengles.",
            "org.lwjgl.util.",
            "org.lwjgl.BufferChecks",
            "org.lwjgl.BufferUtils",
            "org.lwjgl.DefaultSysImplementation",
            "org.lwjgl.J2SESysImplementation",
            "org.lwjgl.LWJGLException",
            "org.lwjgl.LWJGLUtil",
            "org.lwjgl.LinuxSysImplementation",
            "org.lwjgl.MacOSXSysImplementation",
            "org.lwjgl.MemoryUtil",
            "org.lwjgl.MemoryUtilSun",
            "org.lwjgl.PointerBuffer",
            "org.lwjgl.PointerWrapper",
            "org.lwjgl.Sys",
            "org.lwjgl.SysImplementation",
            "org.lwjgl.WindowsSysImplementation"
    };

    private final List<String> launchArguments = new ArrayList<>();
    private boolean forgeLaunch;

    private static boolean mixinConfigured;

    public static boolean hasOptifine = false;

    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        forgeLaunch = isForgeLaunch();
        hasOptifine = Launch.classLoader.getResource("optifine/Patcher.class") != null;

        LogWrapper.info("Glide detected %s launch; deferring to Forge: %s",
                forgeLaunch ? "Forge" : "vanilla", forgeLaunch);

        // FMLTweaker owns the launch argument map. Returning the same arguments
        // from both tweakers makes single-value options such as --width invalid.
        if (forgeLaunch) {
            return;
        }

        this.launchArguments.addAll(args);

        addOptionIfMissing("--version", profile);
        addOptionIfMissing("--assetsDir", assetsDir == null ? null : assetsDir.getAbsolutePath());
        addOptionIfMissing("--gameDir", gameDir == null ? null : gameDir.getAbsolutePath());
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        if (forgeLaunch) {
            if (!hasTransformer(classLoader, ForgeClasspathTransformer.class.getName())) {
                classLoader.registerTransformer(ForgeClasspathTransformer.class.getName());
            }
            preloadForgeVecmath(classLoader);
            queueTweak(GlideForgeTweaker.class.getName());
            return;
        }

        bootstrap(classLoader, "notch");
    }

    @Override
    public String getLaunchTarget() {
        return "net.minecraft.client.main.Main";
    }

    @Override
    public String[] getLaunchArguments() {
        return launchArguments.toArray(new String[0]);
    }

    private void addOptionIfMissing(String option, String value) {
        if (value != null && !launchArguments.contains(option)) {
            launchArguments.add(option);
            launchArguments.add(value);
        }
    }

    static boolean isForgeDeobfuscationReady(LaunchClassLoader classLoader) {
        return hasTransformer(classLoader, FORGE_DEOBFUSCATION_TRANSFORMER);
    }

    static synchronized void bootstrap(LaunchClassLoader classLoader, String obfuscationContext) {
        LogWrapper.info("Glide is initializing Mixin with obfuscation context %s", obfuscationContext);
        configureLwjglClassLoading(classLoader);

        if (!hasTransformer(classLoader, LwjglTransformer.class.getName())) {
            classLoader.registerTransformer(LwjglTransformer.class.getName());
        }

        MixinBootstrap.init();

        MixinEnvironment environment = MixinEnvironment.getDefaultEnvironment();
        environment.setSide(MixinEnvironment.Side.CLIENT);
        environment.setObfuscationContext(obfuscationContext);

        if (!mixinConfigured) {
            Mixins.addConfiguration(MIXIN_CONFIG);
            mixinConfigured = true;
        }
    }

    @SuppressWarnings("unchecked")
    static void queueTweak(String tweakClass) {
        Object pendingTweaks = Launch.blackboard.get("TweakClasses");
        if (!(pendingTweaks instanceof List)) {
            throw new IllegalStateException("LaunchWrapper did not expose its pending tweak list");
        }

        List<String> tweakClasses = (List<String>) pendingTweaks;
        if (!tweakClasses.contains(tweakClass)) {
            tweakClasses.add(tweakClass);
        }
    }

    private static boolean isForgeLaunch() {
        if (Launch.classLoader.getResource(FORGE_TWEAKER.replace('.', '/') + ".class") != null) {
            return true;
        }

        Object pendingTweaks = Launch.blackboard.get("TweakClasses");
        if (pendingTweaks instanceof List && ((List<?>) pendingTweaks).contains(FORGE_TWEAKER)) {
            return true;
        }

        Object tweaks = Launch.blackboard.get("Tweaks");
        if (!(tweaks instanceof List)) {
            return false;
        }

        for (Object tweak : (List<?>) tweaks) {
            if (tweak != null && FORGE_TWEAKER.equals(tweak.getClass().getName())) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasTransformer(LaunchClassLoader classLoader, String transformerClass) {
        for (Object transformer : classLoader.getTransformers()) {
            if (transformerClass.equals(transformer.getClass().getName())) {
                return true;
            }
        }

        return false;
    }

    @SuppressWarnings("unchecked")
    private static void preloadForgeVecmath(LaunchClassLoader classLoader) {
        try {
            Field transformerExceptions = LaunchClassLoader.class.getDeclaredField("transformerExceptions");
            transformerExceptions.setAccessible(true);
            Set<String> exclusions = (Set<String>) transformerExceptions.get(classLoader);
            boolean restoreJavaxExclusion = exclusions.remove("javax.");

            try {
                Class<?> matrixClass = Class.forName(VECMATH_MATRIX, false, classLoader);
                matrixClass.getConstructor();
                LogWrapper.info("Glide preloaded authoritative %s before OptiFine can cache its stub",
                        VECMATH_MATRIX);
            } finally {
                if (restoreJavaxExclusion) {
                    exclusions.add("javax.");
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException |
                ClassNotFoundException | NoSuchMethodException e) {
            throw new IllegalStateException("Unable to preload the Forge Vecmath implementation", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void configureLwjglClassLoading(LaunchClassLoader classLoader) {
        try {
            Field transformerExceptions = LaunchClassLoader.class.getDeclaredField("classLoaderExceptions");
            transformerExceptions.setAccessible(true);
            Set<String> exclusions = (Set<String>) transformerExceptions.get(classLoader);

            // LaunchWrapper excludes every tweak class package. Older launcher
            // profiles still name the tweaker from this package, which would
            // otherwise also exclude all ...mixin.mixins implementation classes.
            exclusions.remove(LEGACY_TWEAKER_PACKAGE);

            if (exclusions.remove("org.lwjgl.")) {
                for (String exclusion : LWJGL2_CLASS_LOADER_EXCLUSIONS) {
                    classLoader.addClassLoaderExclusion(exclusion);
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Unable to configure LWJGL class loading", e);
        }
    }
}
