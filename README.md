# FoliaWarps

A lightweight warp point plugin for Minecraft servers running **Folia** or **Paper**.

Create named warp points and teleport back to them — safely, even under Folia's regionized multithreading model.

## Features

- `/warp set <name>` — create a warp at your current location
- `/warp del <name>` — delete a warp
- `/warp list` — list all warps
- `/warp <name>` — teleport to a warp
- Fully Folia-safe teleportation and scheduling via [UniversalScheduler](https://github.com/Anon8281/UniversalScheduler)
- Commands powered by [CommandAPI](https://commandapi.jorel.dev/)
- MiniMessage-formatted chat feedback via Adventure

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

The shaded jar is output to `target/FoliaWarps-<version>.jar`.

## Installation

1. Install the [CommandAPI](https://www.spigotmc.org/resources/commandapi.9718/) plugin on your server.
2. Drop `FoliaWarps-<version>.jar` into your server's `plugins/` folder.
3. Restart the server.

## Permissions

| Permission | Default | Description |
|---|---|---|
| `foliawarps.admin` | `op` | Allows creating and deleting warp points |

## Project Structure

```
src/main/java/com/goga221/foliawarps/
├── FoliaWarpsPlugin.java     # Plugin entry point
├── command/
│   └── WarpCommand.java      # /warp command tree
├── warp/
│   ├── Warp.java             # Warp data model
│   ├── WarpRepository.java   # Storage interface
│   ├── YamlWarpRepository.java # YAML-backed persistence
│   └── WarpService.java      # Warp business logic
└── util/
    └── Messages.java         # MiniMessage helper
```

## License

No license specified yet — all rights reserved by default.
