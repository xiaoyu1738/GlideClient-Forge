# GlideClient Forge

[简体中文](README.md)

GlideClient Forge is an independent Minecraft Forge 1.8.9 port derived from
[GlideClient](https://github.com/GlideClient/client).

This project targets Forge only. Its final artifact is a single client-side mod
JAR installed in the normal `mods` directory. It does not provide a Vanilla
LaunchWrapper build or custom launcher JSON. Low-level features without a Forge
1.8.9 equivalent still use a limited set of Mixins.

This is an independent community project. It is not an official or endorsed
release of GlideClient, Minecraft, Forge, or OptiFine.

## Project Status

The current release candidate is `7.2-forge.2`, targeting:

- Minecraft 1.8.9
- Forge 11.15.1.2318
- OptiFine 1.8.9 HD U M6 pre2
- Java 8

A clean build runs unit tests, a Linux Unix-domain socket integration test, and
an automatic production JAR structure audit.

The Arch Linux runtime baseline uses Java 8u502, KDE Wayland with Minecraft
running through XWayland, and an NVIDIA GPU. One combined regression loads
OptiFine, Myau, Essential/OneConfig, Keystrokes, NotEnoughUpdates, PolyPatcher,
and SimpleToggleSprint. It verifies world rendering, the Myau Radar and module
list, Keystrokes, the Glide crosshair, the Glide modern hotbar, and a normal
client shutdown.

This is evidence for the tested combination, not a guarantee for every Forge
mod or OptiFine version. Essential/OneConfig still emits known third-party
Mixin warnings in the mixed setup, but they do not prevent world entry, HUD
rendering, or shutdown.

## Current Implementation

- Loads from the normal Forge `mods` directory.
- Provides a regular Forge `@Mod` lifecycle entry and a Forge-ordered Mixin
  tweaker.
- Initializes embedded Mixin after Forge installs runtime deobfuscation and
  uses the `searge` obfuscation context.
- Bridges client/render ticks, keyboard and mouse input, world lifecycle,
  player and living updates, attacks, jumps, FOV, sound, menus, world/player/
  living rendering, block highlights, first-person overlays, and HUD rendering
  through Forge events.
- Preserves Forge overlay `Post` events when Glide replaces a vanilla HUD
  element, allowing third-party HUD listeners to continue rendering.
- Isolates individual Glide event-handler, NanoVG, blur, and modern-hotbar
  failures so one feature cannot suppress the complete HUD.
- Handles conflicting OptiFine Forge API stubs and Vecmath class loading.
- Uses separate common and Windows-only Mixin configurations.
- Supports Discord IPC through Windows named pipes and Linux/macOS Unix-domain
  sockets with bounded payloads and complete short-read/write handling.

## Platform Support

- **Linux/X11 and Linux/Wayland through XWayland:** runtime tested on Arch
  Linux. On Wayland, Borderless Fullscreen leaves fullscreen handling to
  Minecraft/LWJGL instead of applying X11 window positioning.
- **Windows 10/11:** retained as a supported target. CI compiles and runs
  platform-independent tests on Windows, but a real Minecraft runtime
  regression is still required before a stable release.
- **macOS:** desktop integration and Discord Unix sockets are implemented, but
  a full Minecraft runtime is not currently in the release test matrix.

Minecraft 1.8.9 must use Java 8. LWJGL 2 has no native Wayland backend, so
Wayland desktops require XWayland.

No project can guarantee compatibility with arbitrary Forge mods that replace
the same Minecraft methods or embed incompatible Mixin runtimes. Compatibility
reports should include Minecraft, Forge, OptiFine, Java, and operating-system
versions together with the first relevant exception from the log.

## Installation

1. Install Forge `1.8.9-11.15.1.2318`.
2. Put `GlideClient-Forge-7.2-forge.2.jar` in the instance's `mods` directory.
3. Start the normal Forge 1.8.9 profile.

The JAR contains both the early bootstrap required by the remaining Mixins and
the regular `glideclient` Forge mod container. It is client-only and does not
need to be installed on a server.

## Building

### Requirements

- JDK 8
- Git
- Network access for legacy ForgeGradle and Maven dependencies

Linux or macOS:

```bash
./gradlew clean build --console=plain
```

Windows:

```bat
gradlew.bat clean build --console=plain
```

The distributable artifact is written to:

```text
build/libs/GlideClient-Forge-7.2-forge.2.jar
```

`./gradlew smokeJar` additionally creates a test-only controller JAR. It must
not be distributed as a mod or installed in a normal user instance.

For an IntelliJ development workspace:

```bash
./gradlew setupDecompWorkspace
./gradlew genIntellijRuns
```

ForgeGradle 2.1 and Gradle 4.10.3 are legacy toolchains. Always use Java 8.

## Runtime Verification

The Linux smoke runner deploys the production JAR and test controller to an
isolated game directory, enters a named single-player world, runs Mixin's
runtime audit, captures the XWayland window, scans the log, and waits for a
normal shutdown:

```bash
./gradlew clean build smokeJar --console=plain
tools/linux/run-world-smoke.sh \
  --version-dir "/path/to/.minecraft/versions/GlideClient-Forge" \
  --game-dir "/path/to/isolated-game-dir" \
  --world "New World"
```

For an OptiFine regression, put the selected Forge 1.8.9 OptiFine JAR in the
isolated directory's `mods` folder and add `--require-optifine`. The verifier
then requires both the OptiFine version marker and Forge's OptiFine detection
line.

The isolated game directory must already contain the selected save and mod
set. Screenshot verification requires `xdotool` and ImageMagick. The runner
terminates the client it launched on failure or timeout. Windows helpers live
under `tools/windows/`.

## Launch Architecture

The JAR manifest registers `GlideMixinTweaker` after Forge's primary tweaker.
The bootstrap:

1. verifies that Forge runtime deobfuscation is active;
2. prepares the narrow Vecmath/LWJGL class-loading compatibility boundary;
3. delegates to the embedded Mixin tweaker;
4. registers `mixins.soar.json`;
5. registers `mixins.soar.windows.json` only on Windows; and
6. lets Forge discover `GlideForgeMod`, which registers `ForgeEventBridge` on
   the Forge and FML event buses.

This remains a hybrid implementation: installation, lifecycle, input, world
events, and the shared HUD path use Forge, while low-level rendering and game
behavior without Forge 1.8.9 equivalents still use Mixins.

## Roadmap

1. Expand automated regression coverage across additional independent mod sets
   and survival/creative HUD states.
2. Move remaining hooks to Forge events wherever Forge 1.8.9 offers an
   equivalent contract.
3. Reduce classpath transformers and Mixins incrementally without removing
   existing features.
4. Add a real Windows Minecraft runtime worker; GitHub CI currently covers
   Windows compilation and unit tests only.

## Upstream and Attribution

This repository is based on
[`GlideClient/client`](https://github.com/GlideClient/client), starting from
upstream commit `695c208`. GlideClient is described by its maintainers as an
updated version of Soar Client.

The upstream project was distributed under GNU General Public License version
3, and this derivative remains under the same license. Existing third-party
licenses and notices are retained in their source and asset directories. See
[NOTICE.md](NOTICE.md) for modification and attribution information and
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for embedded dependencies.
Dependency license texts are also included under `META-INF/licenses/` in the
production JAR.

## Contributing

Keep changes focused. Changes to shared Mixins, rendering, or bootstrap code
must build the production JAR and run the relevant regression tests.

## License

GlideClient Forge is distributed under the
[GNU General Public License version 3](LICENSE). Source distributions and
modified binaries must continue to comply with GPLv3, including its
corresponding-source requirements.
