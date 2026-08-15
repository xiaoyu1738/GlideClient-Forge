package me.eldodebug.soar.injection.transformer;

import org.objectweb.asm.Type;

/**
 * Keeps types exposed by a parent-loaded API identical when its implementation
 * lives in a child-first LWJGL loader. Only exact NanoVG types present in an
 * external interface descriptor are shared; the rest of each runtime remains
 * isolated.
 */
public final class LwjglClassLoadingBridge {

    public static final String PROPERTY_PREFIX = "glide.lwjgl.shared.";
    private static final String NANOVG_PREFIX = "org.lwjgl.nanovg.";

    private LwjglClassLoadingBridge() {
    }

    static void registerDescriptor(String descriptor) {
        for (Type argument : Type.getArgumentTypes(descriptor)) {
            registerType(argument);
        }
        registerType(Type.getReturnType(descriptor));
    }

    private static void registerType(Type type) {
        while (type.getSort() == Type.ARRAY) {
            type = type.getElementType();
        }
        if (type.getSort() != Type.OBJECT) {
            return;
        }

        String className = type.getClassName();
        if (className.startsWith(NANOVG_PREFIX)
                && LwjglClassLoadingBridge.class.getClassLoader().getResource(
                        className.replace('.', '/') + ".class") != null) {
            System.setProperty(PROPERTY_PREFIX + className, "true");
        }
    }
}
