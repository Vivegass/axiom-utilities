**Axiom Utilities** is a small quality-of-life addon for the [Axiom](https://modrinth.com/mod/axiom) editor. Everything lives inside Axiom's own *Tool Options* window and *Select Block* dialog — no new keybinds, no extra screens.

*Unofficial — not affiliated with or endorsed by Moulberry or the Axiom project.*

## Features

### 🎯 Offset target point — for every tool
The Modelling and Shape tools can aim at the block **next to** the face you look at instead of the block itself. Axiom Utilities adds that *Offset target point* checkbox to every other tool, remembered per tool.

### 🖌️ Paint in air next to blocks
Painter, Noise Painter and Gradient Painter normally only recolour existing blocks. With **Paint in air next to blocks** they paint into the air layer touching the surface instead — keeping block percentages, noise, gradients and masks. *Layer thickness* (1–6) sets how far the layer reaches.

### 💾 Global presets
Save the current state and bring it back in one click:

- **Brush settings** — everything in the tool's options
- **Placement** — the options above
- **Tool mask** — the mask or mask script from the Tool Masks window
- **Active block**

Choose what goes into each preset and what to apply when loading. Picking a preset loads it instantly and switches to its tool. Browse presets as a **list** or as **block tiles** — the preset's main block with the tool's icon in the corner. Presets are plain files in `config/axiom-utilities/presets/`, easy to back up and share.

### 💧 Block pipette
The *Select Block* dialog gets a **Pick from world** button: click it, then click any block in the world to put it — with its block state — into that slot. `Esc` cancels.

## Requirements

- Minecraft **26.3**, Fabric Loader 0.16+
- **Axiom 6.1.x** (required)
- Client-side only

Axiom Utilities hooks into Axiom's internals, so each release targets a specific Axiom version line. If a newer Axiom changes something, the game still starts and only the affected feature disappears until an update.

## Source & issues

Open source under the MIT license: [github.com/Vivegass/axiom-utilities](https://github.com/Vivegass/axiom-utilities). Bug reports and ideas are welcome in the [issue tracker](https://github.com/Vivegass/axiom-utilities/issues).
