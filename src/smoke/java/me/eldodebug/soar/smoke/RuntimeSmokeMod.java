package me.eldodebug.soar.smoke;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Test-only Forge mod used by the platform smoke scripts. */
@Mod(
        modid = RuntimeSmokeMod.MOD_ID,
        name = "Glide Runtime Smoke Controller",
        version = "1",
        acceptedMinecraftVersions = "[1.8.9]",
        clientSideOnly = true)
public final class RuntimeSmokeMod {

    public static final String MOD_ID = "glideruntimesmoke";
    private static final Logger LOGGER = LogManager.getLogger("Glide Runtime Smoke");
    private static final String WORLD_PROPERTY = "glide.smoke.world";
    private static final String READY_TICKS_PROPERTY = "glide.smoke.readyTicks";
    private static final String EXIT_TICKS_PROPERTY = "glide.smoke.exitTicks";

    private String worldFolder;
    private int readyTicks;
    private int exitTicks;

    @Mod.EventHandler
    public void initialize(FMLInitializationEvent event) {
        worldFolder = System.getProperty(WORLD_PROPERTY, "").trim();
        if (worldFolder.isEmpty()) {
            LOGGER.info("GLIDE_SMOKE_DISABLED: set -D{}=<save folder> to enable", WORLD_PROPERTY);
            return;
        }

        readyTicks = positiveIntegerProperty(READY_TICKS_PROPERTY, 160);
        exitTicks = Math.max(readyTicks + 100,
                positiveIntegerProperty(EXIT_TICKS_PROPERTY, 600));
        LOGGER.info("GLIDE_SMOKE_ARMED: world={}, readyTicks={}, exitTicks={}",
                worldFolder, readyTicks, exitTicks);
        Thread controller = new Thread(this::runSmoke, "Glide Runtime Smoke Controller");
        controller.setDaemon(true);
        controller.start();
    }

    private void runSmoke() {
        try {
            // Wait until Forge initialization, resource loading, and any menu
            // replacement have settled, then submit the launch on the client thread.
            Thread.sleep(10_000L);
            Minecraft minecraft = minecraft();
            schedule(minecraft, () -> {
                try {
                    launchWorld(minecraft);
                } catch (Throwable throwable) {
                    LOGGER.error("GLIDE_SMOKE_WORLD_LAUNCH_FAILED", throwable);
                }
            });

            long worldDeadline = System.currentTimeMillis() + 120_000L;
            while (System.currentTimeMillis() < worldDeadline
                    && (getField(minecraft, "field_71441_e", "theWorld") == null
                    || getField(minecraft, "field_71439_g", "thePlayer") == null
                    || integratedServer(minecraft) == null)) {
                Thread.sleep(250L);
            }
            if (getField(minecraft, "field_71441_e", "theWorld") == null) {
                throw new IllegalStateException("World did not load before the smoke timeout");
            }

            // A menu replacement or a launcher handoff can leave a screen
            // visible after the integrated world is ready. Clear it on the
            // client thread so the screenshot proves in-world rendering.
            schedule(minecraft, () -> {
                Object gameSettings = getField(minecraft, "field_71474_y", "gameSettings");
                setBooleanField(gameSettings, false, "field_82881_y", "pauseOnLostFocus");
                if (getField(minecraft, "field_71462_r", "currentScreen") != null) {
                    invoke(minecraft, new String[] {"func_147108_a", "displayGuiScreen"},
                            new Class<?>[] {net.minecraft.client.gui.GuiScreen.class},
                            new Object[] {null});
                }
                LOGGER.info("GLIDE_SMOKE_SCREEN_CLEARED");
            });
            Thread.sleep(500L);

            auditMixins();
            Thread.sleep(readyTicks * 50L);
            LOGGER.info("GLIDE_SMOKE_WORLD_READY: world={}, display={}x{}",
                    worldFolder,
                    getIntField(minecraft, "field_71443_c", "displayWidth"),
                    getIntField(minecraft, "field_71440_d", "displayHeight"));

            Thread.sleep((exitTicks - readyTicks) * 50L);
            LOGGER.info("GLIDE_SMOKE_SHUTDOWN: completed approximately {} in-world ticks",
                    exitTicks);
            schedule(minecraft, () -> shutdown(minecraft));
        } catch (Throwable throwable) {
            LOGGER.error("GLIDE_SMOKE_FAILED", throwable);
            try {
                Minecraft minecraft = minecraft();
                schedule(minecraft, () -> shutdown(minecraft));
            } catch (Throwable shutdownFailure) {
                LOGGER.error("GLIDE_SMOKE_FORCED_SHUTDOWN_FAILED", shutdownFailure);
            }
        }
    }

    private void launchWorld(Minecraft minecraft) {
        File levelFile = new File(new File(new File((File) getField(minecraft,
                "field_71412_D", "mcDataDir"), "saves"),
                worldFolder), "level.dat");
        if (!levelFile.isFile()) {
            LOGGER.error("GLIDE_SMOKE_WORLD_MISSING: {}", levelFile.getAbsolutePath());
            shutdown(minecraft);
            return;
        }

        LOGGER.info("GLIDE_SMOKE_WORLD_LAUNCH: {}", levelFile.getAbsolutePath());
        invoke(minecraft, new String[] {"func_71371_a", "launchIntegratedServer"},
                new Class<?>[] {String.class, String.class, net.minecraft.world.WorldSettings.class},
                worldFolder, worldFolder, null);
    }

    private static Minecraft minecraft() {
        return (Minecraft) invokeStatic(Minecraft.class,
                new String[] {"func_71410_x", "getMinecraft"}, new Class<?>[0]);
    }

    private static Object integratedServer(Minecraft minecraft) {
        return invoke(minecraft, new String[] {"func_71401_C", "getIntegratedServer"},
                new Class<?>[0]);
    }

    private static void shutdown(Minecraft minecraft) {
        invoke(minecraft, new String[] {"func_71400_g", "shutdownMinecraftApplet", "shutdown"},
                new Class<?>[0]);
    }

    private static void auditMixins() {
        try {
            Class<?> environmentClass = Class.forName(
                    "org.spongepowered.asm.mixin.MixinEnvironment");
            Object environment = environmentClass.getMethod("getCurrentEnvironment").invoke(null);
            environmentClass.getMethod("audit").invoke(environment);
            LOGGER.info("GLIDE_SMOKE_MIXIN_AUDIT_OK");
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Mixin runtime audit failed", exception);
        }
    }

    private static void schedule(Minecraft minecraft, Runnable task) {
        invoke(minecraft, new String[] {"func_152344_a", "addScheduledTask"},
                new Class<?>[] {Runnable.class}, task);
    }

    private static Object getField(Object target, String... names) {
        for (String name : names) {
            try {
                Field field = target.getClass().getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (ReflectiveOperationException ignored) {
                // Try the next mapping name.
            }
        }
        throw new IllegalStateException("Unable to resolve field " + java.util.Arrays.toString(names));
    }

    private static int getIntField(Object target, String... names) {
        return ((Number) getField(target, names)).intValue();
    }

    private static void setBooleanField(Object target, boolean value, String... names) {
        for (String name : names) {
            try {
                Field field = target.getClass().getDeclaredField(name);
                field.setAccessible(true);
                field.setBoolean(target, value);
                return;
            } catch (ReflectiveOperationException ignored) {
                // Try the next mapping name.
            }
        }
        throw new IllegalStateException("Unable to resolve boolean field "
                + java.util.Arrays.toString(names));
    }

    private static Object invokeStatic(Class<?> type, String[] names, Class<?>[] parameterTypes,
            Object... arguments) {
        for (String name : names) {
            Method method;
            try {
                method = type.getDeclaredMethod(name, parameterTypes);
            } catch (NoSuchMethodException ignored) {
                // Try the next mapping name.
                continue;
            }
            method.setAccessible(true);
            return invokeResolved(method, null, arguments);
        }
        throw new IllegalStateException("Unable to resolve static method "
                + java.util.Arrays.toString(names));
    }

    private static Object invoke(Object target, String[] names, Class<?>[] parameterTypes,
            Object... arguments) {
        for (String name : names) {
            Method method;
            try {
                method = target.getClass().getDeclaredMethod(name, parameterTypes);
            } catch (NoSuchMethodException ignored) {
                // Try the next mapping name.
                continue;
            }
            method.setAccessible(true);
            return invokeResolved(method, target, arguments);
        }
        throw new IllegalStateException("Unable to resolve method "
                + java.util.Arrays.toString(names));
    }

    private static Object invokeResolved(Method method, Object target, Object... arguments) {
        try {
            return method.invoke(target, arguments);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new IllegalStateException("Reflected method failed: " + method, cause);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to invoke " + method, exception);
        }
    }

    private static int positiveIntegerProperty(String name, int defaultValue) {
        try {
            return Math.max(1, Integer.parseInt(System.getProperty(name,
                    Integer.toString(defaultValue))));
        } catch (NumberFormatException exception) {
            LOGGER.warn("Invalid integer for {}; using {}", name, defaultValue);
            return defaultValue;
        }
    }
}
