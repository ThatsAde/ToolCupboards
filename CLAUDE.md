# ToolCupboards — Claude working agreement

Paper plugin (Java 25, paper-api 26.2) — Rust-style claims via Tool Cupboards.
Domain spec lives in `README.md`. **Do not re-derive the spec from code.**

---

## 1. Token protocol (read this first, every session)

The repo is meant to grow large and **the user writes a lot of the code himself**.
Never scan the tree to "get oriented". Follow this order:

1. Read `LOG.md` — it is the authoritative index of *what exists and where*.
2. Read **only** the files `LOG.md` points to for the current task.
3. If `LOG.md` is stale/missing an entry, fix `LOG.md` as part of the task.

Hard rules:

- **No `Glob **/*` / no full-tree `find` / no "let me explore the codebase"** unless `LOG.md`
  genuinely lacks the answer. Say so explicitly before doing it.
- Read files **partially** (`offset`/`limit`) when you only need one class or method.
- Do **not** re-read a file you edited this session to "verify" — the edit tool already failed
  loudly if it didn't apply.
- Do **not** re-read `README.md` if the relevant part is already summarized in §3 below.
- Prefer `Grep` with a precise symbol over reading whole files.
- Don't spawn subagents unless asked. A cold agent re-reads everything = the expensive path.
- Don't paste large code blocks back into chat. Point at `file.java:line`.

### Writing to LOG.md

After **every** change that adds/moves/removes a type or changes a contract, append one line:

```
- YYYY-MM-DD | <area> | <package or file> | <what changed> | <status>
```

and update the **Module map** table in `LOG.md` if a module's status moved.
One line. No prose. No duplicate entries — edit the existing line if the same area changed again
the same day.

---

## 2. Package hierarchy (the contract)

```
it.yteicos.toolcupboards
├── api/          PUBLIC. Interfaces, records, enums, events ONLY.
│                 No Bukkit-scheduler / no I/O / no static state / no impl.
│                 Breaking changes here are a versioned decision, not a refactor.
│   ├── block/    Custom build blocks: definitions + placed instances
│   ├── cupboard/ Tool cupboards + their claim ownership
│   ├── claim/    Areas and protection queries
│   ├── upkeep/   Fuel/maintenance requirements and decay
│   ├── item/     Cross-plugin item abstraction (vanilla / Nexo / MMOItems)
│   ├── crafting/ Recipe definitions
│   ├── shop/     Shop entries
│   └── event/    Bukkit events fired by the plugin
└── core/         INTERNAL. Everything else. Free to refactor.
    ├── config/   YAML loading (/blocks/*.yml + config.yml)
    ├── block/    Registry + placed-block runtime
    ├── cupboard/ Cupboard runtime
    ├── claim/    Spatial index + protection logic
    ├── upkeep/   Scheduled upkeep ticking
    ├── item/     ItemProvider implementations
    ├── storage/  Persistence backends
    ├── listener/ Bukkit listeners
    └── command/  Commands
```

Rules:

- `api` **never** imports from `core`. `core` imports `api` freely.
- Anything a third-party plugin could reasonably want → `api`. Everything else → `core`.
- Impl classes are named `<Interface>Impl` or `<Adjective><Interface>` (e.g. `YamlBlockDefinitionLoader`).
- Services are obtained through `ToolCupboardsAPI` (Bukkit `ServicesManager`), never via a static
  plugin singleton.

## 3. Domain cheat-sheet (so you don't re-read README)

- **Build blocks** are config-defined custom blocks (`plugins/ToolCupboards/blocks/*.yml`),
  two kinds: `FOUNDATION` and `STRUCTURE`. Structure blocks may only be placed above foundation blocks.
- Each definition carries: health, upkeep requirements, shop entry, item/texture keys, crafting recipe.
- **Tool cupboard**: must sit on a foundation block; projects a `SPHERE` or `CUBE` claim area;
  holds an authorized-player list and an internal inventory that feeds **upkeep**.
- **Upkeep**: every `interval`, N of item X are consumed from the cupboard. If missing, covered
  build blocks start losing health → bases decay. This is the core loop, treat it as load-bearing.
- **Protection covers build blocks only** — not chests. Raiders break through walls, then loot freely.
- Without a cupboard, build blocks are breakable by anyone.
- Item references are namespaced strings: `minecraft:stone`, `nexo:reinforced_plate`, `mmoitems:TYPE.ID`.
  Always go through `ItemProvider` — never hardcode a Nexo/MMOItems call outside `core/item/`.

## 4. Conventions

- Java 25. Records for value objects, sealed types where the set is closed, `Optional` for
  "may be absent" returns — **never** `null` in `api`.
- Javadoc in **English** on every `api` type (it is a published surface). Code comments sparse.
- No Bukkit API calls off the main thread; I/O goes async, mutation comes back via the scheduler.
- Config keys stay **kebab-case** and must match `README.md` exactly.
- `config-version` is checked on load; never silently migrate.

## 5. Before finishing a task

1. `./gradlew build` compiles.
2. `LOG.md` updated (see §1).
3. Say plainly what was left as a stub — stubs are expected here, hidden ones are not.
