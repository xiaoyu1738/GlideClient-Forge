# Attribution and Modification Notice

GlideClient Forge is a modified, independent distribution of GlideClient.

## Upstream Project

- Project: GlideClient
- Repository: https://github.com/GlideClient/client
- Upstream revision used as the starting point: `695c208`
- Upstream license: GNU General Public License version 3

The upstream project describes GlideClient as an updated version of Soar
Client. Copyright in the original source, assets, names, and contributions
remains with the respective copyright holders.

## Modifications

This repository contains modifications made beginning in August 2026 to add a
Minecraft Forge 1.8.9 port. The changes include a standard Forge mod
container, a Forge-ordered tweaker for the remaining Mixins, Forge-aware runtime
mapping selection, Forge event-based lifecycle/input/HUD integration,
compatibility handling for selected OptiFine API stubs, and Forge-only artifact
generation, platform-specific Mixin selection, Linux/macOS Discord IPC, and
cross-platform desktop integration.

The Forge artifact can now be discovered from the normal `mods` directory, but
the project remains a hybrid port because substantial behavior still depends
on GlideClient's Mixins and delayed LaunchWrapper bootstrap. Future work will
continue moving lifecycle and game events to Forge APIs and removing that
transitional bootstrap.

## Independence

This project is not endorsed by or affiliated with the original GlideClient
maintainers, Mojang Studios, Microsoft, Minecraft Forge, or OptiFine.

## Third-Party Material

Third-party components and assets retain their own license notices. In
particular, additional notices are present under:

- `src/main/java/eu/shoroa/contrib/LICENSE.txt`
- `src/main/resources/assets/minecraft/soar/fonts/inter/LICENSE.txt`
- `src/main/resources/assets/minecraft/soar/fonts/unifont/LICENSE.txt`

The distributable JAR also embeds SpongePowered Mixin, ASM, and selected LWJGL
3 NanoVG/STB bindings and native libraries. Their license texts are packaged
under `META-INF/licenses/`; see `THIRD_PARTY_NOTICES.md` for the dependency
list.

The root `LICENSE` file applies to the GlideClient-derived project as a whole.
