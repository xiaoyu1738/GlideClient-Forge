package me.eldodebug.soar.platform;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;

import me.eldodebug.soar.logger.GlideLogger;

/** Opens local files and URIs through the user's desktop environment. */
public final class PlatformDesktop {

    private PlatformDesktop() {
    }

    public static boolean open(File file) {
        if (file == null) {
            return false;
        }

        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(file);
                return true;
            }
        } catch (Throwable throwable) {
            GlideLogger.getLogger().debug("Desktop API could not open {}", file, throwable);
        }

        return openExternally(file.toURI());
    }

    public static boolean browse(URI uri) {
        if (uri == null) {
            return false;
        }

        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(uri);
                return true;
            }
        } catch (Throwable throwable) {
            GlideLogger.getLogger().debug("Desktop API could not browse {}", uri, throwable);
        }

        return openExternally(uri);
    }

    private static boolean openExternally(URI uri) {
        String[] command = getOpenCommand(PlatformUtils.getOperatingSystem(), uri.toString());
        if (command.length == 0) {
            GlideLogger.warn("No desktop opener is available for " + uri);
            return false;
        }

        try {
            new ProcessBuilder(command).start();
            return true;
        } catch (IOException exception) {
            GlideLogger.error("Could not open " + uri, exception);
            return false;
        }
    }

    static String[] getOpenCommand(PlatformUtils.OperatingSystem operatingSystem,
            String target) {
        switch (operatingSystem) {
            case WINDOWS:
                return new String[] {"rundll32", "url.dll,FileProtocolHandler", target};
            case MACOS:
                return new String[] {"/usr/bin/open", target};
            case LINUX:
                return new String[] {"xdg-open", target};
            default:
                return new String[0];
        }
    }
}
