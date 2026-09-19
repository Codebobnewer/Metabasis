# CLAUDE.md

Guidance for Claude Code when working in this repository.

## Project

A Minecraft plugin/server project targeting **Folia + Paper compatibility**.

## Requirements

- Java 21
- Maven (build tool)
- Must remain compatible with both Folia and Paper (avoid APIs that break under Folia's regionized threading model — e.g. no global `BukkitScheduler` assumptions; use the scheduler abstraction below)

## Dependencies

- InvUI — inventory GUIs
- CommandAPI — command registration/handling
- UniversalScheduler — Folia/Paper-safe task scheduling (always use this instead of raw Bukkit scheduler calls)
- Lombok — reduce boilerplate (getters/setters/builders/etc.)
- Adventure text-minimessage — all user-facing text/formatting should use MiniMessage via Adventure, not legacy `§` color codes

### If a database is needed

- HikariCP — connection pooling
- sqlite-jdbc — SQLite driver

## Architecture

- Strict OOP: favor clear class hierarchies, encapsulation, and single-responsibility classes over procedural/utility-dump style code.
- Package by feature/domain, not by layer: `warp/`, `group/`, `spawn/`, `gui/`, `api/`, `history/`, `listener/`, `util/`, `location/`, `command/`. Each domain package owns its full vertical slice (model, repository, service).
- Per domain: a `Repository` interface (storage contract) + a `Yaml<Domain>Repository` implementation, plus a `<Domain>Service` that owns business logic, validation, and caching and is the only thing commands/GUIs call — never reach past the service into the repository or raw file I/O.
- Data model classes (`Warp`, `Group`, `Spawn`, `GroupedWarp`) are immutable Lombok value types: `@Getter @With @AllArgsConstructor`. Change state by producing a new instance (`.withX(...)`), never by adding setters.
- Each subcommand is its own class (`command/warp/`, `command/group/`, `command/spawn/`), extending `command.BaseCommand` (`CommandAPICommand register()`), built with `withArguments`/`withOptionalArguments`/`withSubcommand(s)` and named after the literal it implements (`CreateCommand`, `PermissionCommand`, ...). Two nearly-identical leaves (`enable`/`disable`) share an abstract base in their package rather than duplicating the branch-building code. Root command classes (`WarpCommand`, `GroupCommand`, `SpawnCommand`, in `command/` directly) are thin: they explicitly construct each leaf and attach it via `.withSubcommands(new CreateCommand().getCommand(), ...)`, adding only their own default/fallback behavior (e.g. `/warp <name>` teleport) directly. Leaf commands use a no-arg constructor and read services from the static `Services` accessor (populated once in `MetabasisPlugin.onEnable()`) rather than constructor injection, keeping their shape uniform with the root classes' explicit-construction call sites. Shared suggestion providers and completion handlers used by more than one leaf in a domain go in a `<Domain>CommandSupport` class in that domain's command package (e.g. `command.warp.WarpCommandSupport`).
- GUI classes (`gui/`) are one class per screen, each taking a shared `GuiContext` (bundles every service plus the Folia thread-hop helper) via constructor injection rather than individual services. A screen navigates to another by directly constructing and opening it (`new AdminGroupListGui(context).open(player)`), not through an event bus or navigation stack.
- Shared logic used by more than one command/GUI gets pulled into its own small class in the owning domain package rather than duplicated (e.g. `WarpFilter` for sort/search, `PermissionResolver` for op/blank/raw-permission-node resolution) — reach for this the moment a second call site needs the same logic, not preemptively.
- Validation throws `IllegalArgumentException`/`IllegalStateException` from the service layer with a human-readable message; the command layer catches it and shows it via the shared `error.generic` message key rather than duplicating validation client-side.
- Folia safety: any code that touches a `Player`/inventory after an async callback (a YAML save, a SQL query) must hop back with `scheduler.runTask(player, ...)` (`GuiContext.onPlayerThread(...)` in GUI code) before doing so. Sending a chat message is safe off-thread; opening/refreshing an inventory window is not.
- No hardcoded user-facing strings: every message a player/admin sees goes through `MessageService`, keyed in `message.yml` with a fallback in `MessageDefaults`. Add both together, never one without the other.
- Comments are rare and terse: a one-line `/**...*/` on a class stating its responsibility when the name alone doesn't make it obvious, and inline comments only for a non-obvious *why* (a workaround, a threading constraint) — never restating what the code already says.

## Author preferences

- Refer to the user as goga221.
