# Metabasis

A Minecraft plugin for servers running **Folia** or **Paper**: per-world spawn points, warp points, and permission-gated warp groups.

Create named warp points and per-world spawns and teleport back to them — safely, even under Folia's regionized multithreading model. Admins can restrict individual warps to a permission-bound group so only certain players can use them.

## Features

- Per-world spawn points, settable by admins and falling back to the world's vanilla spawn when unset
- Dying respawns you at your world's custom spawn instead of the vanilla world spawn (a player's own bed/respawn anchor still always takes priority)
- Warp points anyone can create, list, and teleport to
- Admin-managed groups, each bound to a permission node
- Warps can be assigned to a group, restricting who can teleport to them
- Fully Folia-safe teleportation and scheduling via [UniversalScheduler](https://github.com/Anon8281/UniversalScheduler)
- Commands powered by [CommandAPI](https://commandapi.jorel.dev/)
- MiniMessage-formatted chat feedback via Adventure

## Commands

| Command | Permission | Description |
|---|---|---|
| `/spawn` | — | Teleport to the spawn of the world you're in |
| `/spawn <world>` | — | Teleport to a specific world's spawn |
| `/spawn set <world>` | `metabasis.admin` | Set `<world>`'s spawn to your current location |
| `/warp set <name>` | `metabasis.admin` | Create a warp, or move an existing one to your current location |
| `/warp del <name>` | `metabasis.admin` | Delete a warp |
| `/warp group <name> <group\|none>` | `metabasis.admin` | Assign a warp to a group, or `none` to make it public |
| `/warp list` | — | List all warps (and their group, if assigned) |
| `/warp <name>` | — (or the warp's group permission) | Teleport to a warp |
| `/group create <name> [permission]` | `metabasis.admin` | Create a group, optionally bound to a permission |
| `/group delete <name>` | `metabasis.admin` | Delete a group (unassigns any warps using it) |
| `/group permission <name> [permission]` | `metabasis.admin` | Rebind or clear a group's permission |
| `/group list` | `metabasis.admin` | List all groups and their permissions |

A warp with no group assigned is public. Once assigned to a group, only players holding that group's permission can warp to it.

### Group permissions and LuckPerms

`[permission]` is optional:

- **Omitted** — the group is public; anyone can use its warps.
- **`op`** — only server operators can use its warps. Works with or without a permissions plugin installed.
- **The name of an existing [LuckPerms](https://luckperms.net/) group** (e.g. `vip`) — only requires LuckPerms to be installed and that group to already exist there (create/manage it with LuckPerms as usual, e.g. `/lp creategroup vip`). Metabasis checks the name against LuckPerms's actual group list and tab-completes it for you; anyone LuckPerms considers a member of that group (directly or via inheritance) can use the group's warps.

Without LuckPerms installed, `op` or blank are the only accepted values — a custom permission nobody could ever be granted would otherwise silently lock a warp behind an unreachable check.

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

The shaded jar is output to `target/Metabasis-<version>.jar`.

## Installation

1. Install the [CommandAPI](https://www.spigotmc.org/resources/commandapi.9718/) plugin on your server.
2. Drop `Metabasis-<version>.jar` into your server's `plugins/` folder.
3. Restart the server.

### Upgrading from FoliaWarps

The plugin's data folder is derived from its name, so existing warp data lived in `plugins/FoliaWarps/warps.yml`. After upgrading, copy that file into the new `plugins/Metabasis/` folder before starting the server with the new jar, or your existing warps won't be picked up automatically.

## Permissions

| Permission | Default | Description |
|---|---|---|
| `metabasis.admin` | `op` | Grants access to all Metabasis admin commands (warp set/delete/group-assign, group create/delete/permission/list, spawn set) |

Group-bound permissions aren't declared here — they're either `op`, or derived from a LuckPerms group you name when creating a group (see "Group permissions and LuckPerms" above).

## Project Structure

```
src/main/java/com/goga221/metabasis/
├── MetabasisPlugin.java       # Plugin entry point
├── command/
│   ├── WarpCommand.java       # /warp command tree
│   ├── SpawnCommand.java      # /spawn command tree
│   └── GroupCommand.java      # /group command tree
├── listener/
│   └── SpawnRespawnListener.java # Redirects death respawns to a world's custom spawn
├── warp/
│   ├── Warp.java              # Warp data model
│   ├── WarpRepository.java    # Storage interface
│   ├── YamlWarpRepository.java # YAML-backed persistence
│   └── WarpService.java       # Warp business logic, group-permission checks
├── spawn/
│   ├── Spawn.java             # Per-world spawn data model
│   ├── SpawnRepository.java   # Storage interface
│   ├── YamlSpawnRepository.java # YAML-backed persistence
│   └── SpawnService.java      # Spawn business logic
├── group/
│   ├── Group.java             # Group data model (name + bound permission)
│   ├── GroupedWarp.java       # A warp's fields as stored inside its group's file
│   ├── GroupData.java         # A loaded group plus its embedded warps
│   ├── GroupRepository.java   # Storage interface
│   ├── YamlGroupRepository.java # One-file-per-group YAML persistence (groups/<name>.yml)
│   └── GroupService.java      # Group CRUD logic, LuckPerms permission validation
├── location/
│   ├── LocationSnapshot.java  # Shared immutable world/x/y/z/yaw/pitch value type
│   └── YamlLocationCodec.java # Shared YAML read/write for LocationSnapshot
└── util/
    ├── Messages.java          # MiniMessage helper
    ├── Names.java              # Shared name normalization
    ├── Permissions.java       # op-aware permission checks, LuckPerms group queries
    └── SafeTeleport.java      # Shared Folia-safe teleport helper
```

## License

No license specified yet — all rights reserved by default.
