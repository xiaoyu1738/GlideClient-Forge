package me.eldodebug.soar.bootstrap;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import net.minecraft.launchwrapper.LogWrapper;

final class ForgeMixinBootstrap {

    private static final String FORGE_DEOBFUSCATION_TRANSFORMER =
            "net.minecraftforge.fml.common.asm.transformers.DeobfuscationTransformer";
    private static final String VECMATH_MATRIX = "javax.vecmath.Matrix4f";
    private static final String LWJGL_TRANSFORMER =
            "me.eldodebug.soar.injection.transformer.LwjglTransformer";
    private static final String OPTIFINE_STATE_KEY = "glide.optifineLoaded";
    private static final String CORE_MOD_MANAGER =
            "net.minecraftforge.fml.relauncher.CoreModManager";
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

    private static boolean prepared;

    private ForgeMixinBootstrap() {
    }

    static synchronized void prepare(LaunchClassLoader classLoader) {
        if (prepared) {
            return;
        }
        if (!hasTransformer(classLoader, FORGE_DEOBFUSCATION_TRANSFORMER)) {
            throw new IllegalStateException(
                    "Forge runtime deobfuscation must be installed before Glide Mixins");
        }

        ensureForgeModDiscovery();
        Launch.blackboard.put(OPTIFINE_STATE_KEY,
                classLoader.getResource("optifine/Patcher.class") != null);
        configureLwjglClassLoading(classLoader, false);
        if (!hasTransformer(classLoader, LWJGL_TRANSFORMER)) {
            classLoader.registerTransformer(LWJGL_TRANSFORMER);
        }
        preloadForgeVecmath(classLoader);

        prepared = true;
        LogWrapper.info("Glide prepared Forge class loading before Mixin initialization");
    }

    private static void ensureForgeModDiscovery() {
        try {
            File source = new File(GlideMixinTweaker.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());
            ClassLoader bootstrapLoader = GlideMixinTweaker.class.getClassLoader();
            Class<?> coreModManager = Class.forName(
                    CORE_MOD_MANAGER, true, bootstrapLoader);
            Method getIgnoredMods = coreModManager.getMethod("getIgnoredMods");
            @SuppressWarnings("unchecked")
            List<String> ignoredMods = (List<String>) getIgnoredMods.invoke(null);

            if (ignoredMods.remove(source.getName())) {
                LogWrapper.info("Glide restored Forge mod discovery for %s", source.getName());
            }
        } catch (ReflectiveOperationException | URISyntaxException e) {
            throw new IllegalStateException("Unable to register Glide with Forge discovery", e);
        }
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
    private static void configureLwjglClassLoading(LaunchClassLoader classLoader,
            boolean useParentLwjgl3) {
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

            if (useParentLwjgl3) {
                classLoader.addClassLoaderExclusion("org.lwjgl.nanovg.");
                classLoader.addClassLoaderExclusion("org.lwjgl.system.");
                classLoader.addClassLoaderExclusion("org.lwjgl.stb.");
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Unable to configure LWJGL class loading", e);
        }
    }

}
