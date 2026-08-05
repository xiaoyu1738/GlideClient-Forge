package me.eldodebug.soar.utils;

import java.lang.reflect.Field;

import me.eldodebug.soar.forge.ForgeEnvironment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;

public class OptifineUtils {

    private static Field gameSettings_ofFastRender;
    private static boolean fastRenderFieldResolved;
    private static final Minecraft mc = Minecraft.getMinecraft();

    public static void disableFastRender() {
        if (ForgeEnvironment.isOptifineLoaded() && mc.gameSettings != null) {
            try {
                Field fastRenderField = getFastRenderField();
                if (fastRenderField != null) {
                    fastRenderField.setBoolean(mc.gameSettings, false);
                }
            } catch (IllegalArgumentException | IllegalAccessException ignored) {
            }
        }

        if (mc.gameSettings != null) {
            mc.gameSettings.useVbo = true;
            mc.gameSettings.fboEnable = true;
        }
    }

    private static Field getFastRenderField() {
        if (!fastRenderFieldResolved) {
            fastRenderFieldResolved = true;

            try {
                gameSettings_ofFastRender = GameSettings.class.getDeclaredField("ofFastRender");
                gameSettings_ofFastRender.setAccessible(true);
            } catch (NoSuchFieldException ignored) {
            }
        }

        return gameSettings_ofFastRender;
    }
}
