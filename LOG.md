# LOG

Authoritative index of *what exists and where*. Read this before touching the tree; then read only
the files it points at. Roadmap and open decisions live in `ROADMAP.md`, spec in `README.md`.

## Modules

| Module  | Source root                                         | Holds                                                 |
|---------|-----------------------------------------------------|-------------------------------------------------------|
| root    | —                                                   | no sources; aggregator + shared Gradle config only     |
| `:api`  | `api/src/main/java/it/yteicos/toolcupboards/api/`   | published surface: interfaces, records, enums, events  |
| `:core` | `core/src/main/java/it/yteicos/toolcupboards/core/` | everything internal                                    |

`:core` also holds the bootstrap (`it.yteicos.toolcupboards.ToolCupboardsPlugin`, at the root
package, outside `core.*`) and `core/src/main/resources/` — `config.yml`, `paper-plugin.yml`,
`blocks/example.yml`.

`:core` declares `api(project(":api"))`; nothing points back, so `:api` importing `:core` is a
compile error rather than a review comment. `:core:jar` bundles `:api` and emits
`core/build/libs/ToolCupboards-<version>.jar` — the server loads a single file.

## Module map

| Area           | Package                        | Status       | Key types |
|----------------|--------------------------------|--------------|-----------|
| api / block    | `api.block`                    | DONE         | `BuildBlockDefinition`, `BuildBlock`, `BuildBlockRegistry`, `BuildBlockService`, `BuildBlockType` |
| api / claim    | `api.claim`                    | DONE         | `ClaimArea`, `ClaimShape`, `ClaimAction`, `ProtectionService` |
| api / cupboard | `api.cupboard`                 | DONE         | `ToolCupboard`, `ToolCupboardService` (+ `PlacementRefusal`) |
| api / upkeep   | `api.upkeep`                   | DONE         | `UpkeepRequirement`, `UpkeepStatus`, `UpkeepService` |
| api / item     | `api.item`                     | DONE         | `ItemKey`, `ItemProvider`, `ItemProviderRegistry` |
| api / crafting | `api.crafting`                 | DONE         | `RecipeDefinition`, `CraftingService` |
| api / shop     | `api.shop`                     | DONE         | `ShopEntry`, `ShopService` (+ `Result`) |
| api / event    | `api.event`                    | PARTIAL      | `BuildBlockDamageEvent` only — destroy/place/cupboard/upkeep events missing |
| api / root     | `api`                          | DONE         | `ToolCupboardsAPI` |
| core / config  | `core.config`                  | INTERFACE    | `BlockDefinitionLoader` only — `PluginSettings`, `YamlBlockDefinitionLoader` to write |
| core / block   | `core.block`                   | PARTIAL      | `DefaultBuildBlockDefinition`, `DefaultBuildBlock`, `DefaultBuildBlockService` — registry impl to write |
| core / item    | `core.item`                    | PARTIAL      | `MMOItemsItemProvider` done; `NexoItemProvider` is a stub that does not compile; registry impl + vanilla provider to write |
| core / listener| `core.listener`                | PARTIAL      | `BuildProtectionListener` — no-ops until `gameplayReady()` |
| core / storage | `core.storage`                 | INTERFACE    | `StorageBackend` + `WorldSnapshot` — no backend implemented |
| core / claim   | `core.claim`                   | MISSING      | spatial index + `ProtectionService` impl |
| core / cupboard| `core.cupboard`                | MISSING      | `ToolCupboardService` impl |
| core / upkeep  | `core.upkeep`                  | MISSING      | scheduler + `UpkeepService` impl |
| core / crafting| `core.crafting`                | MISSING      | `CraftingService` impl |
| core / shop    | `core.shop`                    | MISSING      | `ShopService` impl (needs Vault) |
| core / command | `core.command`                 | MISSING      | `/tc` and subcommands |

Wiring state: `ToolCupboardsServices` accepts every service through setters and throws on access to
an unwired one. `ToolCupboardsPlugin#onEnable` wires only settings, item providers and the block
registry, so `gameplayReady()` is `false` and protection is inactive by design.

**`:core` does not compile.** `:api` does. The bootstrap and `ToolCupboardsServices` reference five
types that are deliberately left to be written by hand — the compile errors are the to-do list:

| Type | Package | Referenced by |
|------|---------|---------------|
| `PluginSettings`           | `core.config` | `ToolCupboardsServices`, `ToolCupboardsPlugin#loadSettings` |
| `YamlBlockDefinitionLoader`| `core.config` | `ToolCupboardsPlugin#reloadBlockDefinitions` — implements `BlockDefinitionLoader` |
| `SimpleBuildBlockRegistry` | `core.block`  | `ToolCupboardsPlugin` field — implements `BuildBlockRegistry`, plus a `clear()` for reloads |
| `SimpleItemProviderRegistry`| `core.item`  | `ToolCupboardsPlugin#onEnable` — implements `ItemProviderRegistry` |
| `VanillaItemProvider`      | `core.item`   | `ToolCupboardsPlugin#onEnable` — implements `ItemProvider` for `minecraft:` |

## Changelog

- 2026-08-06 | build | `settings.gradle.kts`, `build.gradle.kts`, `core/build.gradle.kts` | split into `:api` and `:core` Gradle modules; root has no sources, `:core` holds bootstrap + resources and emits `ToolCupboards-<version>.jar` with `:api` bundled | DONE
- 2026-08-06 | api | `it.yteicos.toolcupboards.api.**` | moved from the stray `it.yteicos.api.**` root; added `ToolCupboardsAPI`, `BuildBlock`, `BuildBlockDefinition`, `BuildBlockRegistry`, `BuildBlockService`, `ClaimArea`, `ClaimAction`, `ProtectionService`, `ToolCupboardService`, `UpkeepService`, `CraftingService`, `ShopService` — all were referenced but absent, the tree did not compile | DONE
- 2026-08-06 | api/cupboard | `ToolCupboard` | `area()` now returns `ClaimArea`, was `ClaimShape` | DONE
- 2026-08-06 | core | `it.yteicos.toolcupboards.core.**` | moved from the stray `it.yteicos.core.**` root; impls left to be written by hand, so `:core` does not compile (see the table above) | PARTIAL
- 2026-08-06 | config | `core/src/main/resources/config.yml` | added `config-version: 1.0`, now validated by `PluginSettings#from` | DONE
- 2026-08-07 | core/block | `core.block.DefaultBuildBlockService` | implemented `BuildBlockService`: position+id index, foundation-column placement rule, damage pipeline via `BuildBlockDamageEvent`, `blocksIn` linear scan; plus `load`/`unloadWorld`/`clear`/`byId` lifecycle hooks. World texture on place and storage calls left as TODO | PARTIAL
- 2026-08-07 | core/item | `core.item.MMOItemsItemProvider` | fixed key parsing (`TYPE.ID` split on the first dot, both halves upper-cased — MMOItems indexes types and templates by upper-case id with no normalisation), added `amount > 0` and `MMOItems.plugin != null` guards, `matches` now reads the id through `MMOItems.getID` instead of the raw NBT tag | DONE
- 2026-08-06 | deps | `build.gradle.kts`, `core/build.gradle.kts` | added soft-dep repos + `compileOnly` (non-transitive) for `com.nexomc:nexo:1.26.0`, `net.Indyuce:MMOItems-API:6.10.1-SNAPSHOT`, `io.lumine:MythicLib-dist:1.7.1-SNAPSHOT`, plus `kotlin-stdlib` which Nexo needs but neither ships nor declares; MMOItems moved host from the dead `repo.lumine.io` to `nexus.phoenixdevt.fr` | DONE
