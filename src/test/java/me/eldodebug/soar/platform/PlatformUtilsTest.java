package me.eldodebug.soar.platform;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlatformUtilsTest {

    @Test
    public void operatingSystemsAreDetectedWithoutCaseSensitivity() {
        assertEquals(PlatformUtils.OperatingSystem.WINDOWS,
                PlatformUtils.detectOperatingSystem("Windows 11"));
        assertEquals(PlatformUtils.OperatingSystem.MACOS,
                PlatformUtils.detectOperatingSystem("Mac OS X"));
        assertEquals(PlatformUtils.OperatingSystem.MACOS,
                PlatformUtils.detectOperatingSystem("Darwin"));
        assertEquals(PlatformUtils.OperatingSystem.LINUX,
                PlatformUtils.detectOperatingSystem("LINUX"));
        assertEquals(PlatformUtils.OperatingSystem.OTHER,
                PlatformUtils.detectOperatingSystem("FreeBSD"));
    }

    @Test
    public void waylandIsDetectedFromSessionType() {
        Map<String, String> environment = new HashMap<>();
        environment.put("XDG_SESSION_TYPE", "Wayland");
        assertTrue(PlatformUtils.isWaylandSession(environment));
    }

    @Test
    public void waylandIsDetectedFromDisplaySocket() {
        Map<String, String> environment = new HashMap<>();
        environment.put("WAYLAND_DISPLAY", "wayland-0");
        assertTrue(PlatformUtils.isWaylandSession(environment));
    }

    @Test
    public void x11AndEmptyEnvironmentAreNotWayland() {
        Map<String, String> x11 = new HashMap<>();
        x11.put("XDG_SESSION_TYPE", "x11");
        x11.put("WAYLAND_DISPLAY", "  ");
        assertFalse(PlatformUtils.isWaylandSession(x11));
        assertFalse(PlatformUtils.isWaylandSession(Collections.emptyMap()));
        assertFalse(PlatformUtils.isWaylandSession(null));
    }
}
