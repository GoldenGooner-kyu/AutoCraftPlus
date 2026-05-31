# AutoCraftPlus

<p align="center">
  <img src="./autocraftplus_banner.png" alt="AutoCraftPlus Banner" width="800">
</p>

<p align="center">
  <a href="https://www.spigotmc.org/resources/%E2%9C%A6-autocraftplus-%E2%9C%A6-advanced-automatic-crafting-engine-1-19-26-x.135744/"><img src="https://img.shields.io/badge/SpigotMC-AutoCraftPlus-00D2FF?style=for-the-badge&logo=minecraft&logoColor=white" alt="SpigotMC"></a>
  <a href="https://bstats.org/plugin/bukkit/AutoCraftPlus/31724"><img src="https://img.shields.io/badge/bStats-Metrics-3A7BD5?style=for-the-badge&logo=chart&logoColor=white" alt="bStats"></a>
  <img src="https://img.shields.io/badge/Version-1.0.1-FFAF7B?style=for-the-badge" alt="Version">
  <img src="https://img.shields.io/badge/Minecraft-1.19+-FF5E62?style=for-the-badge" alt="Minecraft Version">
</p>

---

AutoCraftPlus is an advanced, high-performance automatic crafting plugin for Spigot/Paper 1.19+ engineered with **Transactional In-Memory Safety** and strict NBT tracking. By utilizing a secure slot-state verification system, AutoCraftPlus ensures that administrators can define complex recipes (supporting custom names, lore, CustomModelData, and custom plugin attributes) and players can auto-craft them seamlessly. It also features a native, Fortune/Silk Touch-compliant **AutoPickup Engine** that coordinates with any third-party block drop plugins!

---

## 📌 Table of Contents

1. [📖 About the Project](#-about-the-project)
2. [✨ Key Features](#-key-features)
3. [💻 Commands & Permissions](#-commands--permissions)
4. [⚙️ Configuration Wiki](#%EF%B8%8F-configuration-wiki)
   - [Default config.yml](#default-configyml)
   - [Default recipe.yml](#default-recipeyml)
5. [📊 Telemetry & Analytics (bStats)](#-telemetry--analytics-bstats)
6. [🏗️ Architecture & Clean Code](#%EF%B8%8F-architecture--clean-code)
7. [🛠️ Compilation & Developer Guide](#%EF%B8%8F-compilation--developer-guide)
8. [⭐️ Rate & Reviews](#%EF%B8%8F-rate--reviews)

---

## 📖 About the Project

Are you tired of basic automatic crafting plugins that cause server lag, conflict with custom items, or worse—expose vulnerabilities that allow players to duplicate items? 

AutoCraftPlus is designed specifically to solve these issues. Every craft matches materials and modifies inventories using a transactional simulation, preventing half-finished crafts or item loss if the server lags or crashes. It also features a robust NBT checking system combined with deep state-tracking inside its custom menus, ensuring that players cannot exploit pre-loaded recipe items.

---

## ✨ Key Features

* **🛡️ Atomic Transaction Safety (Zero-Dupe):** Crafting simulations occur completely in-memory on cloned inventory arrays. State is committed in a single transaction, eliminating item duplication or loss risks.
* **🧪 Virtual-Item Security GUI:** A size-54 graphical recipe editor featuring slot-state tracking. Pre-loaded recipe icons are marked as virtual; closing the GUI or reloading the server safely returns only player-owned items.
* **📋 Interactive Recipes Menu (`/acp list`):** A fully paginated chest directory rendering recipes as their result icons. Ingredients and outputs are formatted dynamically in Title Case in the item Lore.
* **❌ In-GUI Admin Deletion:** Administrators can simply right-click any recipe icon inside the recipes list directory to instantly delete it, refresh the page, and update the server configuration.
* **🧲 Built-In Block Break AutoPickup:** Block broken drops go straight to player inventory, handling Fortune and Silk Touch naturally. Overflow items drop on the ground.
* **🔌 Universal 3rd-Party Compatibility:** Fully compatible with custom items (ItemsAdder, Oraxen, MythicMobs) and third-party AutoPickup plugins thanks to a smart 1-tick delayed inventory scan.
* **⏱️ Speed Permission Cooldowns:** Cooldown rates are parsed from `acp.speed.<ticks>` permissions. The algorithm evaluates effective permissions to apply the fastest allowed crafting speed.
* **🎨 HEX & Gradient Styling:** Fully supports advanced legacy, HEX (#AABBCC), and multi-color gradient messages.

---


## 💻 Commands & Permissions

All commands are grouped cleanly under `/autocraft` or `/acp`.

### 📌 Player & Administration Commands

| Command | Description | Permission | Default |
| :--- | :--- | :--- | :--- |
| `/acp info` | View plugin version, developer, and command lists. | `acp.use` | Everyone (`true`) |
| `/acp list` | Open active auto-crafting recipes menu GUI. | `acp.use` | Everyone (`true`) |
| `/acp reload` | Reload config.yml and recipe.yml database from disk. | `acp.admin` | Operator (`op`) |
| `/acp create <id>` | Open editor GUI to construct a new recipe. | `acp.admin` | Operator (`op`) |
| `/acp edit <id>` | Open editor GUI to modify an existing recipe. | `acp.admin` | Operator (`op`) |
| `/acp remove <id>` | Instantly delete a recipe by its ID. | `acp.admin` | Operator (`op`) |

### 📌 Telemetry & Cooldown Permissions

| Permission | Description | Default |
| :--- | :--- | :--- |
| `acp.recipe.<id>` | Grants permission to auto-craft the specified recipe. | Operator (`op`) |
| `acp.recipe.*` | Grants permission to auto-craft all recipes. | Operator (`op`) |
| `acp.speed.<ticks>` | Sets custom periodic-tick crafting intervals (e.g., `acp.speed.5` runs every 5 ticks). | `false` |
| `acp.speed.*` | Allows all speed rates. | `false` |

---

## ⚙️ Configuration Wiki

### Default config.yml
Customize messages with HEX codes, legacy styling, or gradients:
```yaml
# ==========================================
# AutoCraftPlus Configuration Wiki & Settings
# Author: GoldenGooner
# Supporting Minecraft 1.19+
# ==========================================

settings:
  # Trigger auto-crafting immediately when a player picks up an item
  trigger-on-pickup: true

  # Trigger auto-crafting periodically based on server ticks
  trigger-on-tick: true

  # Enable built-in AutoPickup of block drops (natively handles Fortune/Silk Touch and filters overflow)
  # When active, block broken drops go straight to the inventory and auto-crafting is evaluated immediately.
  # This integrates seamlessly with other third-party auto-pickup plugins.
  enable-auto-pickup: true

  # Default speed for periodic ticks (20 ticks = 1 second)
  # This speed is applied if a player does not have any specific 'acp.speed.<tick>' permission.
  # Note: Even if a player is OP (Operator), they will use this speed unless explicitly assigned a speed permission.
  default-tick-speed: 20

# Messaging Section
# Supports Legacy Color Codes (&a), Hex Colors (&#AABBCC), and Gradients (<gradient:#HEX1:#HEX2>Text</gradient>)
messages:
  prefix: "&#00D2FF&lAutoCraftPlus &7» "
  no-permission: "&cYou do not have permission to perform this action!"
  recipe-saved: "&aSuccessfully saved recipe: &e%id%"
  recipe-deleted: "&aSuccessfully deleted recipe: &e%id%"
  recipe-not-found: "&cRecipe with ID &e%id% &cnot found!"
  invalid-args: "&cInvalid arguments! Use: /acp info"
  only-players: "&cOnly players can perform this action!"
  reload-success: "&aSuccessfully reloaded config and recipes!"
  cannot-save-empty: "&cRecipe must contain at least 1 ingredient and 1 result!"
```

### Default recipe.yml
Recipes created via GUI are stored here using Spigot's standard serialization format:
```yaml
# ==========================================
# AutoCraftPlus Recipes Database
# Managed by /acp commands. Manually editing is possible,
# but it must follow Bukkit's standard ConfigurationSerializable syntax.
# ==========================================
recipes: {}
```

---

## 📊 Telemetry & Analytics (bStats)

AutoCraftPlus integrates official bStats metrics to track anonymous telemetry data (server count, player count, Java version, and system metrics) to help us improve the plugin.

* **bStats Plugin Page:** [https://bstats.org/plugin/bukkit/AutoCraftPlus/31724](https://bstats.org/plugin/bukkit/AutoCraftPlus/31724)
* **Plugin ID:** `31724`

*Note: All package shading and class relocations are handled under the hood to ensure zero conflicts with other plugins on your server.*

---

## 🏗️ Architecture & Clean Code

AutoCraftPlus is designed following clean, standard enterprise development patterns:
* **Managers (`/manager`):** Decoupled configurations (`ConfigManager`) and database registries (`RecipeManager`).
* **Listeners (`/listener`):** Event routers for GUI verification (`GuiListener`) and drop-collection (`BlockBreakListener`).
* **Commands (`/command`):** Single-endpoint command tree (`AutoCraftCommand`) featuring tab auto-completion.
* **GUI (`/gui`):** Custom interfaces leveraging Bukkit's `InventoryHolder` for unified object state binding (`RecipeGui`, `RecipeListGui`).
* **Storage & Models (`/model`):** Isolated abstract models representing configurations (`AutoRecipe`).
* **Utilities (`/util`):** Standalone formatters (`ColorUtil`).

---

## 🛠️ Compilation & Developer Guide

### Prerequisites
* Java JDK 17 or higher.
* Apache Maven installed.

### Build Instructions
Clone the repository, enter the directory, and run the Maven compile target:
```bash
git clone https://github.com/goldengooner/AutoCraftPlus.git
cd AutoCraftPlus
mvn clean package
```

The compiled and shaded jar file will be available in the `target/` directory:
`target/AutoCraftPlus-1.0.1.jar`

---

## ⭐️ Rate & Reviews

Enjoying AutoCraftPlus? Your ratings are what keeps this project alive! If you love the transactional safety mechanics, the NBT-compatible GUI, or the seamless auto-pickup integrations, please take a moment to **leave us a 5-star rating** on SpigotMC!
