# 🏆 RareItemsHunter

> A Minecraft Paper plugin to track and celebrate the rarest items and events in the game — inspired by BastiGHG.
> "DIE SELTENSTEN DINGE IN MINECRAFT FINDEN" -> Plugin
---

## ✨ Features

- **30+ built-in rare items & events**
- **Interactive GUI** — glass panes above/below items to click and confirm finds
- **Per-item timestamps** — each find is saved with the exact date and time
- **Progress bar** — turns orange when you reach 100%
- **Global chat announce** — only fires when a player collects all items
- **Localization** — German (`de`) and English (`en`) out of the box, fully editable
- **Custom items via config** — extend the list without touching the source code

---

## 📦 Installation

1. Download the latest `.jar` from [Releases](https://github.com/ThomasUgh/RareItemsHunter/releases)
2. Drop it into your server's `plugins/` folder
3. Restart or `/reload` your server
4. Open the GUI with `/ri`

**Requirements:** Paper 1.21.4+ · Java 21+

---

## 🎮 Commands

| Command | Description | Permission |
|---|---|---|
| `/ri` | Open the GUI | `rih.use` (default: true) |
| `/ri help` | Show command overview | `rih.use` |
| `/ri reset <player>` | Reset a player's progress | `rih.admin` (default: op) |
| `/ri reload` | Reload config & items | `rih.admin` |
| `/ri give <player> <id>` | Mark an item as found for a player | `rih.admin` |

---

## ⚙️ Configuration

**`config.yml`**
```yaml
# Language: de | en
language: de

# Firework power (0 = instant, 1 = normal, 2 = high)
firework-power: 1

# GUI title (supports & color codes)
gui-title: "&8» &6&lSeltene Items &8«"

# Global announce when a player collects ALL items
announce-find: true

# Add your own items here
custom-items:
  - id: "my_item"
    material: "DIAMOND_BLOCK"
    name: "&b&lDiamond Block"
    chance: "0.01 %"
    description: "Somehow found a full diamond block"
    rarity: 50
    occurrence-only: false
```

**Language files** are in `plugins/RareItemsHunter/lang/de.yml` and `en.yml` — every message, GUI label, and format string is editable there.

---
