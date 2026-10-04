# Computer PC

<img width="256" height="256" alt="Computerpc  256" src="https://github.com/user-attachments/assets/5f0962c2-10f8-4e4a-9cb6-f8e64900c8e6" />

Computer PC is a Fabric mod that adds placeable multiblock browser displays to Minecraft. Build a screen wall from **Display Blocks**, power it on, and control it with the **Browser Remote**. Each display uses an embedded Chromium runtime, so you can open live web pages in-game instead of relying on static textures.

This repository contains the public source for the mod, including the Fabric setup, game assets, browser integration, and multiplayer sync logic.

## Highlights

- Placeable **Display Block** clusters that behave like one larger screen
- Embedded Chromium browser rendering through [MCEF Modern](https://modrinth.com/mod/mcef-modern)
- **Browser Remote** UI for scanning and controlling nearby displays
- Multiple tabs, direct URL entry, back, forward, reload, and home actions
- Resolution presets that adapt to the selected display aspect ratio
- Per-display media volume controls
- Cluster-wide power toggling
- Saved browser state for tabs and screen settings
- Multiplayer synchronization so nearby players see the same browser activity

## Requirements

- Minecraft `26.2`
- Fabric Loader `0.19.3+`
- Fabric API `0.154.0+26.2`
- Java `25`
- [MCEF Modern `0.3.3` for Minecraft 26.2](https://modrinth.com/mod/mcef-modern)

The checked-in Gradle configuration currently targets Minecraft/Fabric version `26.2`. The browser library version is configured in `gradle.properties`.

## Installation

1. Install **Java 25**.
2. Install **Fabric Loader** for Minecraft `26.2`.
3. Install **Fabric API**, **MCEF Modern for Minecraft 26.2**, and the **Computer PC** mod jar into your `mods` folder.
4. Launch the game.

For dedicated servers, install the Computer PC mod and Fabric API on the server and on every connecting client. Install MCEF Modern on clients only; it is a client-side browser library.

## First Launch

Computer PC requires MCEF Modern for its browser integration. Install the matching Minecraft 26.2 release on every client; without it, browser functionality is disabled and the game log explains the missing dependency. MCEF Modern is client-only, so dedicated servers do not need it.

The first launch downloads the embedded browser runtime before displays become active. The in-game status shows download progress when available. If progress remains unchanged, check your internet connection and the game log (`logs/latest.log`) for MCEF initialization errors.

## In-Game Usage

1. Place one or more **Display Blocks** facing the same direction.
2. **Sneak + right-click** the front of a display to toggle power.
3. Hold the **Browser Remote** and **right-click** to open the controller screen.
4. Scan for nearby displays, select one, then manage URLs, tabs, resolution, and volume.

## Development

- Build with `gradlew.bat build` on Windows or `./gradlew build` on Unix-like systems.
- The project uses the Gradle Java toolchain and is configured for Java `25`.
- Local helper content such as `refs/` and `tools/jdk25/` is intentionally excluded from the public repository.

## License

Licensed under `CC0 1.0 Universal`. See [LICENSE](LICENSE).
