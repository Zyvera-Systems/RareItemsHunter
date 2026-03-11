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
