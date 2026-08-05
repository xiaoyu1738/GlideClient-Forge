package me.eldodebug.soar.forge;

import java.lang.reflect.Field;
import java.util.Set;

import me.eldodebug.soar.injection.transformer.LwjglTransformer;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.launchwrapper.LogWrapper;
import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;

final class ForgeMixinBootstrap {

    private static final String FORGE_DEOBFUSCATION_TRANSFORMER =
            "net.minecraftforge.fml.common.asm.transformers.DeobfuscationTransformer";
    private static final String MIXIN_CONFIG = "mixins.soar.json";
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

    private static boolean initialized;

    private ForgeMixinBootstrap() {
    }

    static synchronized void initialize(LaunchClassLoader classLoader) {
        if (initialized) {
            return;
        }
        if (!hasTransformer(classLoader, FORGE_DEOBFUSCATION_TRANSFORMER)) {
            throw new IllegalStateException(
                    "Forge runtime deobfuscation must be installed before Glide Mixins");
        }

        configureLwjglClassLoading(classLoader);
        if (!hasTransformer(classLoader, LwjglTransformer.class.getName())) {
            classLoader.registerTransformer(LwjglTransformer.class.getName());
        }
        preloadForgeVecmath(classLoader);

        MixinBootstrap.init();
        MixinEnvironment environment = MixinEnvironment.getDefaultEnvironment();
        environment.setSide(MixinEnvironment.Side.CLIENT);
        environment.setObfuscationContext("searge");
        Mixins.addConfiguration(MIXIN_CONFIG);

        initialized = true;
        LogWrapper.info("Glide initialized as a Forge mod with searge mappings");
    }

    private static boolean hasTransformer(LaunchClassLoader classLoader, String transformerClass) {
        for (Object transformer : classLoader.getTransformers()) {
            String className = transformer.getClass().getName();
            if (transformerClass.equals(className) || className.contains(transformerClass)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private static void preloadForgeVecmath(LaunchClassLoader classLoader) {
        try {
            Field transformerExceptions = LaunchClassLoader.class.getDeclaredField(
                    "transformerExceptions");
            transformerExceptions.setAccessible(true);
            Set<String> exclusions = (Set<String>) transformerExceptions.get(classLoader);
            boolean restoreJavaxExclusion = exclusions.remove("javax.");

            try {
                Class<?> matrixClass = Class.forName(VECMATH_MATRIX, false, classLoader);
                matrixClass.getConstructor();
            } finally {
                if (restoreJavaxExclusion) {
                    exclusions.add("javax.");
                }
            }
        } catch (NoSuchFieldException | IllegalAccessException |
                ClassNotFoundException | NoSuchMethodException e) {
            throw new IllegalStateException("Unable to preload Forge Vecmath", e);
        }
    }

    @SuppressWarnings("unchecked")
    private static void configureLwjglClassLoading(LaunchClassLoader classLoader) {
        try {
            Field classLoaderExceptions = LaunchClassLoader.class.getDeclaredField(
                    "classLoaderExceptions");
            classLoaderExceptions.setAccessible(true);
            Set<String> exclusions = (Set<String>) classLoaderExceptions.get(classLoader);

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
