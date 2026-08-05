# GlideClient Forge

An experimental Forge 1.8.9 compatibility project derived from
[GlideClient](https://github.com/GlideClient/client).

This repository currently adapts GlideClient's existing LaunchWrapper and
Mixin architecture so it can coexist with Minecraft Forge 1.8.9. It is not yet
a conventional Forge mod. The longer-term goal is to move GlideClient into a
normal Forge mod structure and reduce its dependence on a custom version JSON,
custom tweak ordering, and classpath replacement logic.

## Project Status

The current compatibility build can start with:

- Minecraft 1.8.9
- Forge 11.15.1.2318
- OptiFine 1.8.9 HD U M6 pre2
- Java 8

The Forge path has been tested on Windows 11. The original vanilla
LaunchWrapper path remains in the codebase, but it has not yet been fully
regression-tested after the Forge compatibility work.

This is an independent community project. It is not an official GlideClient,
Minecraft, Forge, or OptiFine release.

## Current Compatibility Work

- Detects Forge launches and defers Glide's Mixin bootstrap until Forge has
  installed its runtime deobfuscation transformer.
- Uses the `notch` obfuscation context for vanilla and `searge` for Forge.
- Produces separate vanilla and Forge artifacts.
- Bridges Glide's 2D HUD events into Forge's `GuiIngameForge` render path.
- Preserves legacy Glide tweaker class names for existing launcher profiles.
- Works around conflicting OptiFine Forge API stubs and Vecmath class loading.
- Avoids duplicate LaunchWrapper arguments when FML owns the launch process.

## Roadmap

1. Stabilize Forge 1.8.9 startup, rendering, input, and OptiFine coexistence.
2. Add automated checks for vanilla and Forge artifact structure.
3. Introduce a standard Forge mod entry point and lifecycle.
4. Move suitable event hooks from the custom event bus to Forge events.
5. Reduce classpath transformers and version-JSON-specific bootstrap logic.
6. Package GlideClient as a normal Forge mod while preserving user data and
   module behavior where practical.

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
./gradlew clean build reobfForgeJar --console=plain
```

On Windows:

```bat
gradlew.bat clean build reobfForgeJar --console=plain
```

Build artifacts are written to `build/libs/`:

- `GlideClient-Release.jar`: vanilla LaunchWrapper artifact
- `GlideClient-Release-Forge.jar`: Forge SRG-reobfuscated artifact

For an IntelliJ development workspace:

```bash
./gradlew setupDecompWorkspace
./gradlew genIntellijRuns
```

ForgeGradle 2.1 and Gradle 4.10.3 are legacy toolchains. Use Java 8 for this
project; newer Java runtimes are not supported by the current build.

## Launch Architecture

The current Forge build is still loaded as a LaunchWrapper tweaker. A Forge
profile must start FML before Glide:

```text
--tweakClass net.minecraftforge.fml.common.launcher.FMLTweaker
--tweakClass me.eldodebug.soar.injection.tweaker.GlideTweaker
```

Glide then waits for Forge's deobfuscation transformer before registering its
Mixin configuration. This bootstrap is transitional and is expected to be
replaced as the Forge mod port progresses.

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

Keep changes focused and test both output artifacts when touching shared Mixin,
rendering, or bootstrap code. Bug reports should include the Minecraft, Forge,
OptiFine, Java, and operating system versions together with the first relevant
exception from the log.

## License

GlideClient Forge is distributed under the
[GNU General Public License version 3](LICENSE). Source distributions and
modified binaries must continue to comply with GPLv3, including its source-code
availability requirements.
