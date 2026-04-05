# Computer PC

<img width="256" height="256" alt="Computerpc  256" src="https://github.com/user-attachments/assets/5f0962c2-10f8-4e4a-9cb6-f8e64900c8e6" />

Computer PC is a Fabric mod that adds placeable multiblock browser displays to Minecraft. Build a screen wall from **Display Blocks**, power it on, and control it with the **Browser Remote**. Each display uses an embedded Chromium runtime, so you can open live web pages in-game instead of relying on static textures.

This repository contains the public source for the mod, including the Fabric setup, game assets, browser integration, and multiplayer sync logic.

## Highlights

- Placeable **Display Block** clusters that behave like one larger screen
- Embedded Chromium browser rendering through JCEF
- **Browser Remote** UI for scanning and controlling nearby displays
- Multiple tabs, direct URL entry, back, forward, reload, and home actions
- Resolution presets that adapt to the selected display aspect ratio
- Per-display media volume controls
- Cluster-wide power toggling
- Saved browser state for tabs and screen settings
- Multiplayer synchronization so nearby players see the same browser activity

## Requirements

- Minecraft `26.1` wip
- Fabric Loader `0.18.6+`
- Fabric API `0.143.12+26.1`
- Java `25`

The checked-in Gradle configuration currently targets Minecraft/Fabric version `26.1`. If you want to retarget the mod, update the values in `gradle.properties`.

## Installation

1. Install **Java 25**.
2. Install **Fabric Loader** for Minecraft `26.1`.
3. Put **Fabric API** and the **Computer PC** mod jar into your `mods` folder.
4. Launch the game.

For dedicated servers, install the same mod jar on the server and on every connecting client.

## First Launch

Computer PC bundles its browser integration, so you do not need a separate browser mod.

The first launch can take longer because the embedded browser runtime may need to initialize or download runtime files before the displays become active.

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
