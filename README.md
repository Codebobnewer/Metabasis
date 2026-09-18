# Metabasis

A Minecraft plugin for servers running **Folia** or **Paper**: per-world spawn points, warp points, permission-gated warp groups, and admin/player GUIs.

Create named warp points and per-world spawns and teleport back to them — safely, even under Folia's regionized multithreading model. Admins can restrict individual warps to a permission-bound group, temporarily disable warps or whole groups, attach a fade-title effect and warmup timer to teleports, and manage everything through in-game GUIs instead of chat commands. Every warp action is also logged to a local SQLite history and exposed through a public API and custom Bukkit events for other plugins to hook into.

## Features

- Per-world spawn points, settable by admins and falling back to the world's vanilla spawn when unset, each optionally gated behind its own permission
- Dying respawns you at your world's custom spawn instead of the vanilla world spawn (a player's own bed/respawn anchor still always takes priority)
- Warp points anyone can create, list, and teleport to
- Admin-managed groups, each bound to a permission node and an optional description
- Warps can be assigned to a group, restricting who can teleport to them
- Warps and groups can be temporarily enabled/disabled without deleting them
- Deleting a group cascades: every warp assigned to it is deleted too, not just unassigned
- Mass warp — teleport every online player to a warp at once, with an optional override to bypass the permission check
- Configurable title fade-in/stay/fade-out shown on teleport, and an optional warmup timer before the teleport fires (cancelled on movement or damage)
- SQLite-backed history of warp create/update/delete events and every teleport, queryable in-game
- Player-facing and admin-facing inventory GUIs (built on InvUI), including a location-adjustment screen with nudge buttons for warps and spawns, and Paper's Dialog API for text input
- A public `MetabasisAPI` service (via Bukkit's `ServicesManager`) and custom Bukkit events (`WarpCreateEvent`, `WarpUpdateEvent`, `WarpDeleteEvent`, `WarpTeleportEvent`, `GroupDeleteEvent`) so other plugins can integrate without touching internals
- All user-facing text lives in `message.yml`, extracted on first run — fully customizable without recompiling
- Fully Folia-safe teleportation, scheduling, and GUI handling via [UniversalScheduler](https://github.com/Anon8281/UniversalScheduler)
- Commands powered by [CommandAPI](https://commandapi.jorel.dev/)
- MiniMessage-formatted feedback via Adventure

## Commands

### Warps

| Command | Permission | Description |
|---|---|---|
| `/warp <name>` | — (or the warp's group permission) | Teleport to a warp |
| `/warp list [page]` | — | List all warps, sorted by group then name, 8 per page |
| `/warp list group <group\|public> [page]` | — | List only warps in a specific group (or ungrouped, via `public`) |
| `/warp gui` | — | Open the player warp browser GUI (paginated, searchable by warp or group name, shows only warps you can access) |
| `/warp create <name>` | `metabasis.admin` | Create a brand-new warp at your location; refuses if the name is already taken |
| `/warp move <name>` | `metabasis.admin` | Relocate an existing warp to your current location; refuses if the name doesn't exist |
| `/warp del <name>` | `metabasis.admin` | Delete a warp |
| `/warp group <name> <group\|none>` | `metabasis.admin` | Assign a warp to a group, or `none` to make it public |
| `/warp enable <name>` / `/warp disable <name>` | `metabasis.admin` | Temporarily allow/refuse teleports to a warp |
| `/warp fade <name> <fadeIn> <stay> <fadeOut>` | `metabasis.admin` | Set the title fade timing (in ticks) shown on teleport; all `0` disables it |
| `/warp warmup <name> <seconds>` | `metabasis.admin` | Set a delay before teleport fires (`0` = instant); cancelled by moving or taking damage |
| `/warp massport <name> [override]` | `metabasis.admin` | Teleport every online player to a warp; `override` bypasses each player's permission check |
| `/warp history <name> [count]` | `metabasis.admin` | Show the warp's recent create/update/delete/teleport history (default 10, max 50) |
| `/warp admin` | `metabasis.admin` | Open the admin GUI (manage warps, groups, and spawns) |

### Groups

| Command | Permission | Description |
|---|---|---|
| `/group create <name> [permission] [description]` | `metabasis.admin` | Create a group, optionally bound to a permission and a description |
| `/group delete <name>` | `metabasis.admin` | Delete a group **and every warp assigned to it** (cascading, destructive) |
| `/group permission <name> [permission]` | `metabasis.admin` | Rebind or clear a group's permission |
| `/group describe <name> [description]` | `metabasis.admin` | Set or clear a group's description |
| `/group enable <name>` / `/group disable <name>` | `metabasis.admin` | Temporarily allow/refuse access to every warp in a group |
| `/group list` | `metabasis.admin` | List all groups, their permissions, descriptions, and enabled state |

### Spawns

| Command | Permission | Description |
|---|---|---|
| `/spawn` | — (or the world's spawn permission) | Teleport to the spawn of the world you're in |
| `/spawn <world>` | — (or the world's spawn permission) | Teleport to a specific world's spawn |
| `/spawn set <world>` | `metabasis.admin` | Set `<world>`'s spawn to your current location |
| `/spawn permission <world> [permission]` | `metabasis.admin` | Rebind or clear a world's spawn permission |

A warp with no group assigned is public. Once assigned to a group, only players holding that group's permission (and with both the warp and its group enabled) can warp to it. Spawns work the same way, gated by their own optional permission.

### Group/spawn permissions

`[permission]` is optional:

- **Omitted** — public; anyone can use it.
- **`op`** — only server operators can use it. Works with or without a permissions plugin installed.
- **Any other value** — used as a raw Bukkit permission node (e.g. `metabasis.group.vip`). Grant it to players however your permission plugin of choice works; Metabasis just checks `player.hasPermission(node)` (and always lets ops through). Tab-complete suggests `op` plus any node you've already bound to another group/spawn, so you don't have to retype one.

## GUIs

- **`/warp gui`** — every player can browse the warps they currently have access to, sorted by group and paginated, with a "Search" button (Dialog-driven, matches what you type against either a warp's name or its group name — or type `public` for ungrouped warps), click to teleport.
- **`/warp admin`** (requires `metabasis.admin`) — a menu into three InvUI-driven management screens:
  - **Manage Warps** — same paginated, searchable list as the player GUI (now covering every warp, not just accessible ones), click a warp for its detail screen: enable/disable, assign a group, adjust location (per-axis nudge buttons, plus a teleport-preview), delete (with confirmation).
  - **Manage Groups** — create (via a Dialog prompting for name/permission/description), enable/disable, rebind permission, edit description, delete (with confirmation — cascades to the group's warps).
  - **Manage Spawns** — pick a world with a custom spawn and adjust its location the same way.
- Text input (permissions, descriptions) uses Paper's server-side Dialog API rather than chat prompts.

## Public API and events

Other plugins can depend on Metabasis and fetch the API instead of reaching into its internals:

```java
MetabasisAPI api = Bukkit.getServicesManager().load(MetabasisAPI.class);
```

It exposes warp/group lookups and a permission-aware `teleportToWarp(Player, String, boolean override)`. Metabasis also fires cancellable/observable Bukkit events — `WarpCreateEvent`, `WarpUpdateEvent`, `WarpDeleteEvent`, `WarpTeleportEvent` (cancellable), and `GroupDeleteEvent` — so other plugins can listen in rather than poll.

## Messages

All user-facing text is defined in `plugins/Metabasis/message.yml` (extracted from the plugin on first run, never overwritten after that) as MiniMessage strings keyed by id (e.g. `warp.teleport.success`, `group.not-found`). Edit it and restart (or your reload command of choice) to reword or retranslate anything without recompiling. A key that's missing or fails to parse falls back to a built-in default and logs a warning, rather than breaking the command.

## History

Every warp create/update/delete and every successful teleport is logged to a local SQLite database (`plugins/Metabasis/history.db`, via HikariCP). `/warp history <name>` shows the most recent entries for a warp, including who did what and when.

## Requirements

| Requirement | Version |
|---|---|
| Java | 21 |
| Server software | Paper or Folia |
| Minecraft API | 1.21 |
| Build tool | Maven |
| Dependency plugin | [CommandAPI](https://www.spigotmc.org/resources/commandapi.9718/) |

## Building

```bash
mvn clean package
```

The shaded jar is output to `target/Metabasis-<version>.jar`. All other third-party dependencies (UniversalScheduler, HikariCP, sqlite-jdbc, InvUI) are bundled and relocated, so nothing besides CommandAPI needs to be installed separately.

## Installation

1. Install the [CommandAPI](https://www.spigotmc.org/resources/commandapi.9718/) plugin on your server.
2. Drop `Metabasis-<version>.jar` into your server's `plugins/` folder.
3. Restart the server.

### Upgrading from FoliaWarps

The plugin's data folder is derived from its name, so existing warp data lived in `plugins/FoliaWarps/warps.yml`. After upgrading, copy that file into the new `plugins/Metabasis/` folder before starting the server with the new jar, or your existing warps won't be picked up automatically.

## Permissions

| Permission | Default | Description |
|---|---|---|
| `metabasis.admin` | `op` | Grants access to all Metabasis admin commands and the admin GUI (warp/group/spawn management, mass warp, history) |

Group-bound and spawn-bound permissions aren't declared here — they're either `op`, or a raw permission node you choose when creating them (see "Group/spawn permissions" above).

## Project Structure

```
src/main/java/xyz/goga221/metabasis/
├── MetabasisPlugin.java        # Plugin entry point
├── api/
│   ├── MetabasisAPI.java        # Public API interface
│   ├── MetabasisAPIImpl.java    # API implementation, registered via ServicesManager
│   └── event/                   # Custom Bukkit events (create/update/delete/teleport/group-delete)
├── command/
│   ├── WarpCommand.java         # /warp command tree
│   ├── SpawnCommand.java        # /spawn command tree
│   └── GroupCommand.java        # /group command tree
├── gui/
│   ├── AdminMenuGui.java        # /warp admin landing screen
│   ├── AdminWarpListGui.java / AdminWarpDetailGui.java   # Warp management
│   ├── AdminGroupListGui.java / AdminGroupDetailGui.java # Group management
│   ├── AdminSpawnListGui.java   # Spawn management
│   ├── PlayerWarpGui.java       # /warp gui player browser
│   ├── LocationAdjustGui.java   # Shared warp/spawn location-nudge screen
│   ├── GuiContext.java          # Shared services + Folia thread-hop helper for GUI screens
│   ├── GuiItems.java            # Adventure-aware ItemProvider helper
│   └── dialog/
│       └── TextInputDialogs.java # Paper Dialog-API text-input prompts
├── history/
│   ├── WarpHistoryRepository.java     # Storage interface
│   ├── SqliteWarpHistoryRepository.java # HikariCP + SQLite-backed implementation
│   └── WarpHistoryEntry.java    # A single logged event
├── listener/
│   ├── SpawnRespawnListener.java # Redirects death respawns to a world's custom spawn
│   ├── WarmupCancelListener.java # Cancels a pending warp warmup on move/damage
│   └── WarpHistoryListener.java  # Writes Metabasis's own events into the history DB
├── warp/
│   ├── Warp.java                # Warp data model (group, enabled, fade, warmup)
│   ├── WarpFilter.java          # Shared sort-by-group/filter-by-group logic (list command + GUIs)
│   ├── WarpRepository.java      # Storage interface
│   ├── YamlWarpRepository.java  # YAML-backed persistence
│   └── WarpService.java         # Warp business logic, group-permission checks, teleport/fade/warmup
├── spawn/
│   ├── Spawn.java               # Per-world spawn data model (with permission)
│   ├── SpawnRepository.java     # Storage interface
│   ├── YamlSpawnRepository.java # YAML-backed persistence
│   └── SpawnService.java        # Spawn business logic
├── group/
│   ├── Group.java                # Group data model (permission, description, enabled)
│   ├── GroupedWarp.java          # A warp's fields as stored inside its group's file
│   ├── GroupData.java            # A loaded group plus its embedded warps
│   ├── GroupRepository.java      # Storage interface
│   ├── YamlGroupRepository.java  # One-file-per-group YAML persistence (groups/<name>.yml)
│   └── GroupService.java         # Group CRUD logic, permission resolution, cascading delete
├── location/
│   ├── LocationSnapshot.java    # Shared immutable world/x/y/z/yaw/pitch value type
│   └── YamlLocationCodec.java   # Shared YAML read/write for LocationSnapshot
└── util/
    ├── MessageService.java      # message.yml-driven text lookup and sending
    ├── MessageDefaults.java     # Built-in fallback text for missing/invalid keys
    ├── Messages.java            # MiniMessage helper
    ├── Names.java                # Shared name normalization
    ├── PermissionResolver.java  # Shared op/blank/raw-permission-node resolution
    ├── Permissions.java         # op-aware permission checks
    └── SafeTeleport.java        # Shared Folia-safe teleport helper
```

## License

No license specified yet — all rights reserved by default.
