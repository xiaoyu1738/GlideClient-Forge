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

This repository contains modifications made beginning in August 2026 to add an
experimental Minecraft Forge 1.8.9 compatibility path. The changes include
Forge-aware LaunchWrapper initialization, runtime mapping selection, Forge HUD
render integration, compatibility handling for selected OptiFine API stubs,
and separate Forge artifact generation.

The project is currently a compatibility port based on GlideClient's custom
LaunchWrapper architecture. It is not yet a complete or conventional Forge
mod. Future work intends to migrate the client incrementally to Forge mod
lifecycle and event APIs.

## Independence

This project is not endorsed by or affiliated with the original GlideClient
maintainers, Mojang Studios, Microsoft, Minecraft Forge, or OptiFine.

## Third-Party Material

Third-party components and assets retain their own license notices. In
particular, additional notices are present under:

- `src/main/java/eu/shoroa/contrib/LICENSE.txt`
- `src/main/resources/assets/minecraft/soar/fonts/inter/LICENSE.txt`
- `src/main/resources/assets/minecraft/soar/fonts/unifont/LICENSE.txt`

The root `LICENSE` file applies to the GlideClient-derived project as a whole.
