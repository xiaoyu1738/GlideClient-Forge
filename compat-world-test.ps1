param(
    [Parameter(Mandatory = $true)]
    [string]$GameDir,
    [int]$Width = 1280,
    [int]$Height = 720,
    [int]$StartupTimeoutSeconds = 180
)

$ErrorActionPreference = 'Stop'
$launchScript = Join-Path $PSScriptRoot 'compat-launch.ps1'
$logName = 'compat-world-console.log'
$logPath = Join-Path $GameDir $logName

Add-Type @'
using System;
using System.Runtime.InteropServices;
using System.Threading;

public static class GlideTestWindow {
    [StructLayout(LayoutKind.Sequential)]
    public struct Rect {
        public int Left;
        public int Top;
        public int Right;
        public int Bottom;
    }

    [StructLayout(LayoutKind.Sequential)]
    public struct Point {
        public int X;
        public int Y;
    }

    [DllImport("user32.dll")]
    public static extern bool GetClientRect(IntPtr window, out Rect rect);

    [DllImport("user32.dll")]
    public static extern bool ClientToScreen(IntPtr window, ref Point point);

    [DllImport("user32.dll")]
    public static extern bool SetForegroundWindow(IntPtr window);

    [DllImport("user32.dll")]
    public static extern IntPtr GetForegroundWindow();

    [DllImport("user32.dll")]
    public static extern uint GetWindowThreadProcessId(IntPtr window, IntPtr processId);

    [DllImport("kernel32.dll")]
    public static extern uint GetCurrentThreadId();

    [DllImport("user32.dll")]
    public static extern bool AttachThreadInput(uint idAttach, uint idAttachTo, bool attach);

    [DllImport("user32.dll")]
    public static extern bool BringWindowToTop(IntPtr window);

    [DllImport("user32.dll")]
    public static extern bool ShowWindow(IntPtr window, int command);

    [DllImport("user32.dll")]
    public static extern bool SetWindowPos(
        IntPtr window,
        IntPtr insertAfter,
        int x,
        int y,
        int width,
        int height,
        uint flags);

    [DllImport("user32.dll")]
    public static extern bool SetCursorPos(int x, int y);

    [DllImport("user32.dll")]
    public static extern void mouse_event(uint flags, uint dx, uint dy, uint data, UIntPtr extraInfo);

    [DllImport("user32.dll")]
    public static extern void keybd_event(byte virtualKey, byte scanCode, uint flags, UIntPtr extraInfo);

    [DllImport("user32.dll")]
    public static extern bool PostMessage(IntPtr window, uint message, IntPtr wParam, IntPtr lParam);

    public static void PrepareWindow(IntPtr window) {
        ShowWindow(window, 3); // SW_MAXIMIZE
        ActivateWindow(window);
        Thread.Sleep(500);
    }

    private static void ActivateWindow(IntPtr window) {
        const uint SWP_NOSIZE = 0x0001;
        const uint SWP_NOMOVE = 0x0002;
        const uint SWP_SHOWWINDOW = 0x0040;
        IntPtr HWND_TOPMOST = new IntPtr(-1);
        IntPtr HWND_NOTOPMOST = new IntPtr(-2);

        for (int attempt = 0; attempt < 5; attempt++) {
            IntPtr foreground = GetForegroundWindow();
            uint currentThread = GetCurrentThreadId();
            uint foregroundThread = foreground == IntPtr.Zero
                ? 0
                : GetWindowThreadProcessId(foreground, IntPtr.Zero);
            bool attached = foregroundThread != 0 && foregroundThread != currentThread &&
                AttachThreadInput(currentThread, foregroundThread, true);

            try {
                ShowWindow(window, 3); // SW_MAXIMIZE
                SetWindowPos(window, HWND_TOPMOST, 0, 0, 0, 0,
                    SWP_NOMOVE | SWP_NOSIZE | SWP_SHOWWINDOW);
                BringWindowToTop(window);
                SetForegroundWindow(window);
                SetWindowPos(window, HWND_NOTOPMOST, 0, 0, 0, 0,
                    SWP_NOMOVE | SWP_NOSIZE | SWP_SHOWWINDOW);
            } finally {
                if (attached) {
                    AttachThreadInput(currentThread, foregroundThread, false);
                }
            }

            Thread.Sleep(250);
            if (GetForegroundWindow() == window) {
                return;
            }
        }

        throw new InvalidOperationException(
            "Minecraft could not be brought to the foreground before input");
    }

    private static Rect ReadClientRect(IntPtr window) {
        Rect rect;
        if (!GetClientRect(window, out rect)) {
            throw new InvalidOperationException("Unable to read the Minecraft client bounds");
        }
        return rect;
    }

    public static Point GetClientSize(IntPtr window) {
        Rect rect = ReadClientRect(window);
        return new Point { X = rect.Right - rect.Left, Y = rect.Bottom - rect.Top };
    }

    private static void ClickClientPoint(IntPtr window, int x, int y) {
        PrepareWindow(window);
        Point point = new Point { X = x, Y = y };
        if (!ClientToScreen(window, ref point)) {
            throw new InvalidOperationException("Unable to translate Minecraft click coordinates");
        }

        SetCursorPos(point.X, point.Y);
        Thread.Sleep(100);
        SendMouseClick(window);
    }

    private static void SendMouseClick(IntPtr window) {
        // A browser opened by another mod can steal focus between cursor
        // positioning and the input event. Reclaim and verify it at the last
        // possible moment so the click can only reach Minecraft.
        ShowWindow(window, 3); // SW_MAXIMIZE
        ActivateWindow(window);
        SendMouseClickToForeground(window);
    }

    private static void SendMouseClickToForeground(IntPtr window) {
        if (GetForegroundWindow() != window) {
            throw new InvalidOperationException(
                "Minecraft lost foreground focus immediately before a click");
        }
        mouse_event(0x0002, 0, 0, 0, UIntPtr.Zero);
        Thread.Sleep(50);
        mouse_event(0x0004, 0, 0, 0, UIntPtr.Zero);
    }

    public static void ClickRatio(IntPtr window, double xRatio, double yRatio) {
        PrepareWindow(window);
        Rect rect = ReadClientRect(window);
        ClickClientPoint(
            window,
            (int)((rect.Right - rect.Left) * xRatio),
            (int)((rect.Bottom - rect.Top) * yRatio));
    }

    public static void DoubleClickRatio(IntPtr window, double xRatio, double yRatio) {
        PrepareWindow(window);
        Rect rect = ReadClientRect(window);
        Point point = new Point {
            X = (int)((rect.Right - rect.Left) * xRatio),
            Y = (int)((rect.Bottom - rect.Top) * yRatio)
        };
        if (!ClientToScreen(window, ref point)) {
            throw new InvalidOperationException("Unable to translate Minecraft click coordinates");
        }

        SetCursorPos(point.X, point.Y);
        Thread.Sleep(100);
        ShowWindow(window, 3); // SW_MAXIMIZE
        ActivateWindow(window);
        SendMouseClickToForeground(window);
        Thread.Sleep(100);
        SendMouseClickToForeground(window);
    }

    public static void ClickPlaySelectedWorld(IntPtr window) {
        PrepareWindow(window);
        Rect rect = ReadClientRect(window);
        int width = rect.Right - rect.Left;
        int height = rect.Bottom - rect.Top;
        ClickClientPoint(window, width / 2 - 158, height - 84);
    }

    public static void PressF2(IntPtr window) {
        PrepareWindow(window);
        if (GetForegroundWindow() != window) {
            throw new InvalidOperationException(
                "Minecraft lost foreground focus immediately before F2");
        }
        keybd_event(0x71, 0, 0, UIntPtr.Zero);
        Thread.Sleep(100);
        keybd_event(0x71, 0, 0x0002, UIntPtr.Zero);
    }

    public static void PressEnter(IntPtr window) {
        PrepareWindow(window);
        if (GetForegroundWindow() != window) {
            throw new InvalidOperationException(
                "Minecraft lost foreground focus immediately before Enter");
        }
        keybd_event(0x0D, 0, 0, UIntPtr.Zero);
        Thread.Sleep(100);
        keybd_event(0x0D, 0, 0x0002, UIntPtr.Zero);
    }

}
'@

function Get-ScaledHeight {
    param([IntPtr]$Window)

    $client = [GlideTestWindow]::GetClientSize($Window)
    $guiScale = 1000
    $optionsPath = Join-Path $GameDir 'options.txt'
    if (Test-Path $optionsPath) {
        $match = Select-String -Path $optionsPath -Pattern '^guiScale:(\d+)$' | Select-Object -First 1
        if ($null -ne $match) {
            $guiScale = [int]$match.Matches[0].Groups[1].Value
            if ($guiScale -eq 0) {
                $guiScale = 1000
            }
        }
    }

    $scaleFactor = 1
    while ($scaleFactor -lt $guiScale -and
            [math]::Floor($client.X / ($scaleFactor + 1.0)) -ge 320 -and
            [math]::Floor($client.Y / ($scaleFactor + 1.0)) -ge 240) {
        $scaleFactor++
    }

    return [math]::Ceiling($client.Y / [double]$scaleFactor)
}

function Get-TestJavaProcess {
    $escapedGameDir = [Regex]::Escape($GameDir)
    $candidate = Get-CimInstance Win32_Process | Where-Object {
        $_.Name -in @('java.exe', 'javaw.exe') -and
        $_.CommandLine -match $escapedGameDir
    } | Select-Object -First 1

    if ($null -eq $candidate) {
        return $null
    }

    return Get-Process -Id $candidate.ProcessId -ErrorAction SilentlyContinue
}

function Wait-Until {
    param(
        [scriptblock]$Condition,
        [int]$TimeoutSeconds,
        [string]$FailureMessage
    )

    $deadline = [DateTime]::UtcNow.AddSeconds($TimeoutSeconds)
    do {
        if (& $Condition) {
            return
        }
        Start-Sleep -Milliseconds 500
    } while ([DateTime]::UtcNow -lt $deadline)

    throw $FailureMessage
}

function Test-LogContains {
    param([string]$Pattern)

    if (-not (Test-Path $logPath)) {
        return $false
    }

    return [bool](Select-String -Path $logPath -Pattern $Pattern -Quiet)
}

function Disable-TestInstanceMenuPopups {
    $toggleSprintConfig = Join-Path $GameDir 'config\simpletogglesprint.toml'
    if (-not (Test-Path $toggleSprintConfig)) {
        return
    }

    $bytes = [System.IO.File]::ReadAllBytes($toggleSprintConfig)
    $hasUtf8Bom = $bytes.Length -ge 3 -and
            $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF
    $content = [System.IO.File]::ReadAllText($toggleSprintConfig)
    $updated = $content -replace '(?m)^(\s*show_config_on_menu\s*=\s*)true\s*$', '${1}false'
    if ($updated -ne $content -or $hasUtf8Bom) {
        $utf8WithoutBom = New-Object System.Text.UTF8Encoding($false)
        [System.IO.File]::WriteAllText($toggleSprintConfig, $updated, $utf8WithoutBom)
    }
}

$launcher = $null
$javaProcess = $null
try {
    Disable-TestInstanceMenuPopups

    $launcherArgs = '-NoProfile -ExecutionPolicy Bypass ' +
            '-File "{0}" -GameDir "{1}" -Width {2} -Height {3} -LogName "{4}"' -f
            $launchScript, $GameDir, $Width, $Height, $logName
    $powershell = Join-Path $env:SystemRoot 'System32\WindowsPowerShell\v1.0\powershell.exe'
    $launcher = Start-Process $powershell -ArgumentList $launcherArgs -PassThru

    Wait-Until -TimeoutSeconds $StartupTimeoutSeconds `
        -FailureMessage 'Minecraft did not create a window before the startup timeout' `
        -Condition {
            if ($launcher.HasExited) {
                throw "Minecraft launcher exited with code $($launcher.ExitCode)"
            }
            $script:javaProcess = Get-TestJavaProcess
            if ($null -eq $script:javaProcess) {
                return $false
            }
            $script:javaProcess.Refresh()
            return $script:javaProcess.MainWindowHandle -ne [IntPtr]::Zero
        }

    Wait-Until -TimeoutSeconds $StartupTimeoutSeconds `
        -FailureMessage 'Glide did not finish initializing before the startup timeout' `
        -Condition { Test-LogContains '\[MODULE\]' }

    # The first module log line is emitted before the remaining saved module
    # state and menu setup have finished. Avoid clicking through that burst.
    Start-Sleep -Seconds 5

    $javaProcess.Refresh()
    $window = $javaProcess.MainWindowHandle
    [GlideTestWindow]::PrepareWindow($window)
    Start-Sleep -Seconds 2
    $scaledHeight = Get-ScaledHeight $window
    $singlePlayerY = (($scaledHeight / 2.0) - 12.0) / $scaledHeight
    $worldRowY = 50.0 / $scaledHeight
    $playButtonY = ($scaledHeight - 22.0) / $scaledHeight

    # Main menus from other mods commonly retain the vanilla first-button
    # position. Try that first, then Glide's dynamically scaled position. If
    # either click already opened the world list, the other lands in its empty
    # body without activating a destructive action.
    [GlideTestWindow]::PrepareWindow($window)
    [GlideTestWindow]::ClickRatio($window, 0.5, 0.367)
    Start-Sleep -Seconds 2
    [GlideTestWindow]::ClickRatio($window, 0.5, $singlePlayerY)
    Start-Sleep -Seconds 2

    # Vanilla world list: the isolated directory contains exactly one world.
    # Try a verified double-click first, then fall back to a held Enter key and
    # the screen's explicit confirmation button. Each path reclaims focus.
    [GlideTestWindow]::PrepareWindow($window)
    [GlideTestWindow]::DoubleClickRatio($window, 0.5, $worldRowY)
    Start-Sleep -Seconds 4
    if (-not (Test-LogContains 'Starting integrated minecraft server')) {
        [GlideTestWindow]::ClickRatio($window, 0.5, $worldRowY)
        Start-Sleep -Milliseconds 500
        [GlideTestWindow]::PressEnter($window)
        Start-Sleep -Seconds 4
    }
    if (-not (Test-LogContains 'Starting integrated minecraft server')) {
        [GlideTestWindow]::ClickRatio($window, 0.5, $worldRowY)
        Start-Sleep -Milliseconds 500
        [GlideTestWindow]::ClickRatio($window, 0.5, $playButtonY)
        Start-Sleep -Seconds 4
    }

    Wait-Until -TimeoutSeconds 90 `
        -FailureMessage 'The isolated world did not finish loading' `
        -Condition { Test-LogContains 'joined the game|logged in with entity id' }

    Start-Sleep -Seconds 8
    [GlideTestWindow]::PressF2($window)
    Start-Sleep -Seconds 3
} catch {
    if ($null -ne $javaProcess -and -not $javaProcess.HasExited) {
        $javaProcess.Refresh()
        if ($javaProcess.MainWindowHandle -ne [IntPtr]::Zero) {
            [GlideTestWindow]::PressF2($javaProcess.MainWindowHandle)
            Start-Sleep -Seconds 2
        }
    }
    throw
} finally {
    if ($null -ne $javaProcess -and -not $javaProcess.HasExited) {
        $javaProcess.Refresh()
        if ($javaProcess.MainWindowHandle -ne [IntPtr]::Zero) {
            [GlideTestWindow]::PostMessage(
                    $javaProcess.MainWindowHandle, 0x0010, [IntPtr]::Zero, [IntPtr]::Zero) | Out-Null
        }
    }

    $deadline = [DateTime]::UtcNow.AddSeconds(15)
    while ($null -ne $javaProcess -and -not $javaProcess.HasExited -and
            [DateTime]::UtcNow -lt $deadline) {
        Start-Sleep -Milliseconds 500
        $javaProcess.Refresh()
    }

    Get-CimInstance Win32_Process | Where-Object {
        $_.Name -in @('java.exe', 'javaw.exe') -and
        $_.CommandLine -match [Regex]::Escape($GameDir)
    } | ForEach-Object {
        Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
    }

    if ($null -ne $launcher -and -not $launcher.HasExited) {
        Stop-Process -Id $launcher.Id -Force -ErrorAction SilentlyContinue
    }
}
