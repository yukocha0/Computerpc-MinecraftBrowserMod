# Computer PC

Computer PC adds placeable browser screens to Minecraft Fabric 1.21.11.

Build display walls from **Display Blocks**, power them on, and control them with the **Browser Remote**. Each screen uses an embedded Chromium runtime, letting you open real web pages in-game, switch tabs, change resolution, and control media volume from a clean remote interface.

It is designed for modern builds, control rooms, media setups, and multiplayer bases where you want functional browser displays instead of static decoration.

## Features

- Placeable display blocks that connect into larger screen clusters
- Embedded Chromium-powered browser rendering
- Browser Remote for scanning and managing nearby screens
- Multiple tabs per display
- Back, forward, reload, home, and direct URL entry
- Resolution presets that adapt to the selected screen's aspect ratio
- Per-screen media volume control
- Cluster-wide power toggle
- Saved screen state, including tabs and settings
- Multiplayer-friendly syncing so other nearby players see the same display activity
- Automatic browser runtime initialization on startup

## Requirements

- Minecraft `1.21.11`
- Fabric Loader `0.18.5+`
- Fabric API
- Java `21`

## Installation

1. Install **Java 21**.
2. Install **Fabric Loader** for Minecraft `1.21.11`.
3. Download and install **Fabric API**.
4. Download **Computer PC**.
5. Install the files in the correct `mods` folder:
6. Launch the game.

### Where To Install It

- **Singleplayer:** put **Fabric API** and **Computer PC** in your client `mods` folder only.
- **Dedicated server:** put **Fabric API** and **Computer PC** in the server `mods` folder and in every player's client `mods` folder.
- The same **Computer PC** `.jar` is used on both sides.

### First Launch Note

Computer PC includes its browser integration, so you do **not** need to install a separate browser mod.

On first startup, the embedded Chromium runtime may need a moment to initialize or download its runtime files. If that happens, wait for it to finish before using the displays.

## Quick Start

1. Place one or more **Display Blocks** facing the same direction.
2. **Sneak + right-click** the front of a display to power the screen on or off.
3. Hold the **Browser Remote** and **right-click** to open the control screen.
4. Use **Scan** to find nearby displays.
5. Select a screen, enter a URL, and manage tabs, resolution, and volume from the remote UI.

## Notes

- The remote scans for displays near the player.
- Display clusters share the same screen state, so larger setups behave like one screen wall.
- If Chromium is still starting, the display will begin rendering once the runtime is ready.
