package me.eldodebug.soar.platform;

import java.util.Locale;
import java.util.Map;

/** Platform checks that are safe to use during the early Mixin bootstrap. */
public final class PlatformUtils {

    public enum OperatingSystem {
        WINDOWS,
        MACOS,
        LINUX,
        OTHER
    }

    private PlatformUtils() {
    }

    public static OperatingSystem getOperatingSystem() {
        return detectOperatingSystem(System.getProperty("os.name", ""));
    }

    public static boolean isWindows() {
        return getOperatingSystem() == OperatingSystem.WINDOWS;
    }

    public static boolean isMacOS() {
        return getOperatingSystem() == OperatingSystem.MACOS;
    }

    public static boolean isLinux() {
        return getOperatingSystem() == OperatingSystem.LINUX;
    }

    public static boolean isWaylandSession() {
        return isWaylandSession(System.getenv());
    }

    public static boolean supportsBorderlessFullscreen() {
        return !isLinux() || !isWaylandSession();
    }

    static OperatingSystem detectOperatingSystem(String osName) {
        String normalized = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        if (normalized.contains("mac") || normalized.contains("darwin")) {
            return OperatingSystem.MACOS;
        }
        if (normalized.contains("win")) {
            return OperatingSystem.WINDOWS;
        }
        if (normalized.contains("linux")) {
            return OperatingSystem.LINUX;
        }
        return OperatingSystem.OTHER;
    }

    static boolean isWaylandSession(Map<String, String> environment) {
        if (environment == null) {
            return false;
        }

        String sessionType = environment.get("XDG_SESSION_TYPE");
        if (sessionType != null && "wayland".equalsIgnoreCase(sessionType.trim())) {
            return true;
        }

        String waylandDisplay = environment.get("WAYLAND_DISPLAY");
        return waylandDisplay != null && !waylandDisplay.trim().isEmpty();
    }
}
