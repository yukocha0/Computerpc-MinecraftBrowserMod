# Computerpc

<img width="256" height="256" alt="Computerpc  256" src="https://github.com/user-attachments/assets/5f0962c2-10f8-4e4a-9cb6-f8e64900c8e6" />

Computerpc adds placeable browser screens to Minecraft Fabric 1.21.11+.

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
- Multiplayer-friendly syncing so other nearby players see the same display activity (url sync)
- Automatic browser runtime initialization on startup

## Requirements

- Minecraft `1.21.11+`
- Fabric Loader `0.18.5+`
- Fabric API

## Installation

1. Install **Fabric Loader** for Minecraft `1.21.11+`.
2. Download and install **Fabric API**.
3. Download **Computer PC**.
4. Install the files in the correct `mods` folder:
5. Launch the game.

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
- In multiplayer the url gets synced with other players. Its not a screen share or streaming.

## License & Attribution

**ComputerPC** is released into the Public Domain under the **CC0 1.0 Universal License**. You are absolutely free to view, use, modify, and distribute the code of this mod without needing permission.

### MCEF (Third-Party Library)
To function, this mod dynamically downloads and links to **MCEF [Keksuccino's Fork]** at runtime. MCEF is an independent third-party library and is **NOT** under the CC0 license.

In compliance with its licensing, please note the following:
* **License:** MCEF is licensed under the **GNU Lesser General Public License v2.1 (LGPL-2.1)**.
* **Credits:** MCEF [Keksuccino's Fork] is maintained by **Keksuccino** https://modrinth.com/mod/mcef-keksuccino . It is a fork of the original MCEF created by **montoyo**, **ds58**, and the **CinemaMod Group**. All rights and credits for MCEF belong to these respective developers.
* **Source Code:** You can find the open-source code for MCEF [Keksuccino's Fork] here: [https://github.com/Keksuccino/mcef](https://github.com/Keksuccino/mcef)