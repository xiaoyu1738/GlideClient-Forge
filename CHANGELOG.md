# Changelog

## 7.2-forge.2 - 2026-08-21

- Added a normal Forge 1.8.9 lifecycle and Forge event bridge while retaining
  only the low-level Mixins needed for behavior without a Forge event.
- Preserved Forge overlay notifications so Glide and third-party HUDs can
  render in the same frame.
- Isolated HUD, blur, NanoVG, and individual event-listener failures.
- Added Forge/OptiFine class-loading compatibility and safe Vecmath selection.
- Added Linux and macOS desktop integration and Discord Unix-socket IPC.
- Disabled Windows-only LWJGL injection outside Windows and kept vanilla
  fullscreen behavior on Linux Wayland.
- Added malformed health, potion, and Discord IPC input guards.
- Added unit tests, real Linux Unix-socket tests, a separate runtime smoke Mod,
  production JAR auditing, and Linux/Windows CI builds.
- Added embedded dependency license notices and reproducible Linux runtime
  validation tools.
- Hardened the Linux smoke controller against menu focus loss from external
  Mod browsers, and made its screenshot gate require an in-world screen.
- Runtime-tested OptiFine 1.8.9 HD U M6 pre2 on Arch Linux KDE Wayland through
  XWayland, including world entry, HUD coexistence, screenshot capture, and
  clean shutdown. Added an explicit `--require-optifine` smoke gate.
- Added a core-only OptiFine smoke pass to distinguish Glide/OptiFine startup
  failures from warnings emitted by external Mixin stacks.
- Removed unused LWJGL Yoga and Zstd Java/native payloads from the production
  JAR to reduce its footprint and avoid unrelated native loading conflicts.
- Hardened Unix Discord IPC handling for negative and otherwise invalid native
  read lengths, preserving a clear I/O failure instead of leaking an array
  bounds error.
