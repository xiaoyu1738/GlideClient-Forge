package me.eldodebug.soar.platform;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class PlatformDesktopTest {

    @Test
    public void linuxUsesXdgOpenWithoutShellParsing() {
        assertArrayEquals(new String[] {"xdg-open", "file:/tmp/a b.png"},
                PlatformDesktop.getOpenCommand(PlatformUtils.OperatingSystem.LINUX,
                        "file:/tmp/a b.png"));
    }

    @Test
    public void windowsUsesTheFileProtocolHandler() {
        assertArrayEquals(new String[] {"rundll32", "url.dll,FileProtocolHandler",
                        "https://example.com/a?b=c"},
                PlatformDesktop.getOpenCommand(PlatformUtils.OperatingSystem.WINDOWS,
                        "https://example.com/a?b=c"));
    }

    @Test
    public void unsupportedSystemsDoNotGuessACommand() {
        assertArrayEquals(new String[0],
                PlatformDesktop.getOpenCommand(PlatformUtils.OperatingSystem.OTHER,
                        "https://example.com"));
    }
}
