# GlideClient Forge

An independent Forge 1.8.9 port derived from
[GlideClient](https://github.com/GlideClient/client).

This project targets Forge only. The final build is one client-side mod JAR
for Minecraft 1.8.9; there is no Vanilla LaunchWrapper artifact or custom
launcher JSON in this repository. Mixins remain an implementation detail for
features that have not yet been moved to Forge events and APIs.

## Project Status

The current Forge mod build targets:

- Minecraft 1.8.9
- Forge 11.15.1.2318
- OptiFine 1.8.9 HD U M6 pre2
- Java 8

The Forge artifact builds successfully and has completed an automated mixed-Mod
world-entry smoke test. The tested setup loaded 11 Forge Mod containers,
including OneConfig, Essential, Keystrokes, NotEnoughUpdates, PolyPatcher,
SimpleToggleSprint, and GlideClient, with the Myau coremod also present.
Keystrokes, sprint status, Glide CPS, and Glide's modern hotbar rendered at the
same time. This is a regression baseline, not a claim that every Forge Mod is
compatible.

This is an independent community project. It is not an official GlideClient,
Minecraft, Forge, or OptiFine release.

## Current Compatibility Work

- Provides a Forge-ordered Mixin tweaker and regular `@Mod` lifecycle entry
  point.
- Loads directly from the normal Forge `mods` directory.
- Starts the embedded Mixin subsystem only after Forge installs its runtime
  deobfuscation transformer.
- Uses the Forge `searge` obfuscation context.
- Bridges client ticks, render ticks, keyboard and mouse input, world lifecycle,
  player and living updates, attacks, jumps, FOV changes, sound playback, menu
  opening, world-last/player/living rendering, block highlights, first-person
  overlays, and HUD elements through Forge events.
- Preserves Forge overlay `Post` notifications when Glide replaces a vanilla HUD
  element, allowing third-party HUD listeners to keep rendering.
- Isolates individual Glide event-handler, NanoVG, blur, and modern-hotbar
  failures so one failed feature does not suppress the complete Forge HUD.
- Works around conflicting OptiFine Forge API stubs and Vecmath class loading.

## Roadmap

1. Expand automated smoke coverage across additional independent Mod sets and
   both survival and creative HUD states.
2. Move remaining hooks to Forge events wherever Forge 1.8.9 exposes an
   equivalent contract.
3. Reduce and eventually remove classpath transformers as equivalent Forge APIs
   become available.
4. Add artifact-structure and Windows runtime smoke tests to CI.

The migration will be incremental. Replacing all Mixins at once would create a
large regression surface across rendering, GUI, networking, and performance
modules.

## Building

### Requirements

- JDK 8
- Git
- Network access for legacy ForgeGradle and Maven dependencies

The Gradle wrapper is included. On Linux or macOS:

```bash
./gradlew clean build --console=plain
```

On Windows:

```bat
gradlew.bat clean build --console=plain
```

The build writes one artifact to `build/libs/`:

- `GlideClient-Forge-7.2-forge.1.jar`

## Installing The Forge Build

1. Install Minecraft Forge `1.8.9-11.15.1.2318`.
2. Put `GlideClient-Forge-7.2-forge.1.jar` in the instance's `mods` directory.
3. Start the normal Forge 1.8.9 profile.

The Forge artifact contains both the early coremod bootstrap required by the
remaining Mixins and the regular `glideclient` Forge mod container. It is
client-only and does not need to be installed on a server.

For an IntelliJ development workspace:

```bash
./gradlew setupDecompWorkspace
./gradlew genIntellijRuns
```

ForgeGradle 2.1 and Gradle 4.10.3 are legacy toolchains. Use Java 8 for this
project; newer Java runtimes are not supported by the current build.

## Launch Architecture

The JAR manifest registers `GlideMixinTweaker` after Forge's primary tweaker.
It verifies that Forge runtime deobfuscation is active, prepares the narrow
Vecmath/LWJGL class-loading compatibility boundary, delegates to the embedded
Mixin tweaker, and registers `mixins.soar.json`. Forge then discovers
`GlideForgeMod` through normal `@Mod` scanning and registers `ForgeEventBridge`
on the Forge and FML event buses.

This remains a hybrid implementation: installation, lifecycle, input, world
events, and the shared HUD path use Forge, while low-level rendering and game
behavior without a Forge 1.8.9 event still use Mixins. All supported launches
use this Forge Mod path.

## Upstream and Attribution

This repository is based on
[`GlideClient/client`](https://github.com/GlideClient/client), starting from
upstream commit `695c208`. GlideClient is itself described by its maintainers as
an updated version of Soar Client.

The original project was distributed under the GNU General Public License
version 3. This derivative remains under the same license. Existing third-party
license files and notices are retained in their respective source and asset
directories. See [NOTICE.md](NOTICE.md) for the modification and attribution
notice.

## Contributing

Keep changes focused and test the Forge artifact when touching shared Mixin,
rendering, or bootstrap code. Bug reports should include the Minecraft, Forge,
OptiFine, Java, and operating system versions together with the first relevant
exception from the log.

## License

GlideClient Forge is distributed under the
[GNU General Public License version 3](LICENSE). Source distributions and
modified binaries must continue to comply with GPLv3, including its source-code
availability requirements.
