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

The drop-in Forge bootstrap has been structurally validated and builds
successfully. Runtime testing with OptiFine and other coremods is still ongoing.

This is an independent community project. It is not an official GlideClient,
Minecraft, Forge, or OptiFine release.

## Current Compatibility Work

- Provides a Forge `IFMLLoadingPlugin` and regular `@Mod` lifecycle entry point.
- Loads directly from the normal Forge `mods` directory.
- Detects Forge launches and defers Glide's Mixin bootstrap until Forge has
  installed its runtime deobfuscation transformer.
- Uses the Forge `searge` obfuscation context.
- Bridges Glide's 2D HUD events from Forge's `RenderGameOverlayEvent` into the
  existing internal event bus.
- Works around conflicting OptiFine Forge API stubs and Vecmath class loading.

## Roadmap

1. Stabilize the drop-in Forge mod bootstrap with OptiFine and other coremods.
2. Move input, tick, connection, and remaining render hooks to Forge events.
3. Move Glide startup and shutdown ownership into the Forge lifecycle.
4. Reduce and eventually remove classpath transformers as equivalent Forge APIs
   become available.
5. Add runtime smoke tests and artifact-structure checks to CI.

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

Forge discovers `GlideLoadingPlugin` from the Forge JAR manifest. The plugin
installs the OptiFine compatibility transformer and queues Glide's Mixin setup
after Forge's runtime deobfuscation transformer. Forge then discovers
`GlideForgeMod` through normal `@Mod` scanning and registers the event bridge.

This is a transitional port: installation and lifecycle discovery use Forge,
while many game hooks still use Mixin. All supported launches use this Forge
mod path.

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
