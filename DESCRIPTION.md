# ImagoCore

> An image display engine for Minecraft — render custom images on GUIs, entities, and more.

---

## Overview

**ImagoCore** is a lightweight, framework-style plugin that provides a unified API for rendering custom images inside Minecraft. It acts as a **display driver core** — other plugins depend on it to overlay images onto inventory GUIs, item frames, maps, and entities, with animation support.

The rendering pipeline uses **Unicode private-use characters** mapped through a **custom resource pack font**, enabling arbitrary bitmap images to appear in GUI titles, entity map views, and other surfaces — all without mods.

---

## Key Features

- **GUI Image Overlay** — Render full-size background images behind any inventory GUI using resource-pack-backed bitmap fonts and negative-offset character alignment
- **Multi-size GUI Support** — Built-in templates for 9 / 18 / 27 / 36 / 45 / 54 slot inventories
- **Resource Pack Generator** — Auto-build a compliant `build.zip` from registered GUI entries; no manual resource pack editing needed
- **Entity Image Display** — Display images on item frames and maps (framework ready, implementation in progress)
- **Animation Driver** — Frame-sequence animation with LOOP, ONCE, and PING-PONG modes
- **Spigot & Folia Compatible** — Unified `CompatibleScheduler` API; entity operations use region-aware scheduling on Folia
- **Hot-reload Config** — Configuration supports `/guildadmin reload` without server restart
- **Plugin API** — Other plugins integrate via soft-dependency; access `ImageRenderer`, `ImageDisplay`, and `AnimationDriver` interfaces

---

## How It Works

1. **Resource Pack** — A custom font maps Unicode private-use characters (`\uE800`+ range) to bitmap textures. Two additional characters provide horizontal alignment via negative font advances.
2. **GUI Title Rendering** — The inventory title is composed of shift-offset characters followed by the background character. The client renders this through the resource pack font as a background image.
3. **Registry & Auto-build** — Administrators drop PNG files into `plugins/ImagoCore/gui/{slots}/` directories, the plugin auto-assigns a unique Unicode character, and `/imagocore resource build` (or auto-build on restart) produces the final `build.zip`.

---

## Commands

| Command | Aliases | Description |
|---|---|---|
| `/imagocore test gui [id]` | `/ic t g [id]`, `/ic test g` | Open a test inventory with the specified GUI background |
| `/imagocore resource build` | `/ic r b`, `/ic resource b` | Generate `build.zip` from all registered GUIs |

---

## Getting Started

1. Drop `ImagoCore.jar` into your `plugins/` folder and start the server.
2. The plugin creates `plugins/ImagoCore/gui/` with template folders for each slot size.
3. Replace `default.png` in each folder with your own background image (or add new `entries` in the folder's `gui.yml`).
4. Run `/ic resource build` to generate the resource pack.
5. Configure your server to send `plugins/ImagoCore/build.zip` as the server resource pack.
6. Players join, accept the pack, and `/ic test gui 54-default` opens a GUI with your custom background.

---

## For Developers

Other plugins integrate by soft-depending on ImagoCore:

```java
ImagoCore core = (ImagoCore) Bukkit.getPluginManager().getPlugin("ImagoCore");
if (core == null) return;

// Use the title renderer
Component title = GuiTitleRenderer.build(entry, core.getGuiRegistry());
Inventory inv = Bukkit.createInventory(null, 54, title);
player.openInventory(inv);
```

See [ARCHITECTURE.md](ARCHITECTURE.md) for the full API and design decisions.

---

## Requirements

- **Java 21**
- **Paper 1.21+** (also runs on Spigot 1.21+, Folia)

---

## Development Status

- ✅ GUI image display (resource-pack-backed title rendering)
- ✅ Multi-size GUI registry with auto char-assignment
- ✅ Resource pack auto-generation (`build.zip`)
- ✅ Folia-compatible scheduler
- 🔨 Entity image display (API defined, renderer pending)
- 🔨 Animation integration on displays
- 🔨 Player-specific image overlays

---

## License

_To be determined._
