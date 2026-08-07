# ROADMAP

Checklist di implementazione. Le fasi sono in **ordine di dipendenza**: ogni fase produce qualcosa
di giocabile e sblocca la successiva. Lo stato di ciò che esiste è in `LOG.md`, la spec in `README.md`.

Legenda: `[ ]` da fare · `[~]` parziale · `[x]` fatto.

---

## Fase 0 — Struttura (fatta)

- [x] Split in moduli Gradle `:api` / `:core`; root senza sorgenti, `:core` produce il jar del plugin
- [x] Package unificati sotto `it.yteicos.toolcupboards.*`
- [x] Superficie `:api` completa e compilante
- [x] `config.yml` e `blocks/example.yml` con `config-version`
- [x] `LOG.md`

---

## Fase 0.5 — Far ricompilare `:core`

`:api` compila, `:core` no: il bootstrap referenzia cinque classi che vanno scritte. I contratti
sono già in `:api`, qui servono solo le implementazioni.

- [ ] `core.config.PluginSettings` — vista tipizzata di `config.yml`, validata una volta al load.
      Deve controllare `config-version` e **non** migrare in silenzio (`CLAUDE.md` §4).
      Serve a `ToolCupboardsServices#settings()` e a `ToolCupboardsPlugin#loadSettings`
- [ ] `core.config.YamlBlockDefinitionLoader` — implementa `BlockDefinitionLoader`. Un file rotto
      si salta e si logga, non deve fermare gli altri blocchi del server.
      ⚠️ Le chiavi tipo `mmoitems:MATERIAL.STEEL` contengono il `.` che Bukkit tratta come
      separatore di path: vanno lette con `getValues(false)`, non con `getConfigurationSection(path)`
- [ ] `core.block.SimpleBuildBlockRegistry` — implementa `BuildBlockRegistry`, più un `clear()`
      (il bootstrap lo chiama a ogni reload per non lasciare definizioni stale)
- [ ] `core.item.SimpleItemProviderRegistry` — implementa `ItemProviderRegistry`
- [ ] `core.item.VanillaItemProvider` — implementa `ItemProvider` per il namespace `minecraft`

**Fatto quando:** `./gradlew build` è verde e il plugin si avvia loggando le definizioni caricate.

---

## Fase 1 — I blocchi custom esistono nel mondo

Senza questa fase non c'è niente da proteggere. È il prerequisito di tutto.

- [ ] **Decidere il meccanismo di rendering** dei blocchi custom → vedi *Decisioni aperte #1*.
      Tutto il resto di questa fase dipende da questa scelta. (Appena deciso, si inserirà l'item type, se viene omesso nexo: o mmoitems: verrà preso un item normale di minecraft, altrimenti il nexo o il mmoitems)
- [ ] `core.item.NexoItemProvider` — `ItemProvider` per il namespace `nexo`, degrada a
      `isAvailable() == false` se il plugin non c'è
- [ ] `core.item.MMOItemsItemProvider` — idem per `mmoitems:TYPE.ID`
- [ ] `core.block.DefaultBuildBlock` — implementazione di `BuildBlock` (health mutabile, clamp a
      0 e a `maxHealth`)
- [ ] `core.block.DefaultBuildBlockService` — implementazione di `BuildBlockService`:
  - [ ] indice `Location → BuildBlock` (chiave posizionale packed, non `Location` che è mutabile)
  - [ ] `canPlace`: regola fondamenta — `STRUCTURE` solo sopra un `FOUNDATION`, direttamente o
        transitivamente
  - [ ] `damage`: pipeline completa (evento → amount modificato → applica → distruggi a 0)
  - [ ] `destroy`: rimuove dal mondo, dall'indice e dalla persistenza
  - [ ] `blocksIn(ClaimArea)`
- [ ] `core.listener.BuildBlockPlaceListener` — riconosce l'item custom in mano e piazza un
      `BuildBlock` invece del blocco vanilla
- [ ] Marcare i blocchi piazzati con un `PersistentDataContainer` sul chunk o su block-entity, così
      un riavvio senza storage non li perde silenziosamente
- [ ] Wiring in `ToolCupboardsPlugin#onEnable` + `services.setBlocks(...)`

**Fatto quando:** piazzo un blocco custom, lo colpisco, perde vita e a 0 sparisce.

---

## Fase 2 — Tool cupboard e claim

- [ ] `core.cupboard.DefaultToolCupboard` — implementazione di `ToolCupboard` (lista autorizzati,
      `Inventory` di upkeep, area proiettata)
- [ ] `core.cupboard.DefaultToolCupboardService`:
  - [ ] `refusePlacement`: fondamenta sotto, overlap, cap `claim.max-per-player`, world abilitato
  - [ ] `place` / `remove`
- [ ] `core.claim.ClaimIndex` — indice spaziale. Non iterare tutti i cupboard a ogni evento:
      chunk-key → lista cupboard, poi test esatto con `ClaimArea.contains`
      (`ClaimArea.boundingRadius()` esiste apposta per il pre-filtro)
- [ ] `core.claim.DefaultProtectionService` — regole di `ClaimAction`:
  - [ ] posizione non coperta da nessun cupboard → permetti tutto (è ciò che rende i blocchi
        senza cupboard rompibili da chiunque)
  - [ ] `BREAK_BUILD_BLOCK` / `PLACE_BUILD_BLOCK` / `BREAK_CUPBOARD` / `OPEN_CUPBOARD` /
        `MANAGE_AUTHORIZATIONS`
  - [ ] **non** proteggere chest, mob, blocchi vanilla — è deliberato (README)
- [ ] `core.listener.CupboardListener` — piazzamento, rottura, apertura inventario
- [ ] Estendere `BuildProtectionListener` alle esplosioni (`EntityExplodeEvent`,
      `BlockExplodeEvent`) → devono passare da `BuildBlockService.damage`, non cancellare
- [ ] Sostituire il `TODO: raiding` in `BuildProtectionListener#onBreak`: una rottura non
      autorizzata deve entrare nella pipeline di danno, non essere cancellata
- [ ] Wiring: `setCupboards(...)`, `setProtection(...)` → `gameplayReady()` diventa `true`

**Fatto quando:** un cupboard protegge la base, un non autorizzato deve rompere il muro a colpi.

---

## Fase 3 — Upkeep e decay (il loop centrale)

- [ ] `core.upkeep.DefaultUpkeepService`:
  - [ ] `requirementsOf`: aggrega i requisiti di tutti i blocchi coperti, sommando per `ItemKey`
  - [ ] `charge`: consuma dall'inventario del cupboard via `ItemProviderRegistry.matches`
  - [ ] pagamento parziale → decay proporzionale, non tutto-o-niente
  - [ ] `statusOf` / `remainingMillis` per la UI
- [ ] `core.upkeep.UpkeepScheduler` — task ogni `upkeep.tick-interval-seconds`; ogni cupboard ha il
      proprio `nextUpkeepAt`, il tick globale è solo la sveglia
- [ ] Decay: `decay-per-missed-tick` di danno ai blocchi coperti via `BuildBlockService.damage`
      con `Cause.DECAY`
- [ ] Recuperare i tick persi mentre il server era spento (altrimenti basta riavviare per non
      pagare mai) → vedi *Decisioni aperte #4*
- [ ] Wiring: `setUpkeep(...)`

**Fatto quando:** una base senza materiali nel cupboard crolla da sola.

---

## Fase 4 — Persistenza

Prima di qui, tutto si perde al riavvio.

- [ ] `core.storage.SqliteStorageBackend` — default
- [ ] `core.storage.YamlStorageBackend` — server piccoli (opzionale)
- [ ] `core.storage.MySqlStorageBackend` — network (opzionale)
- [ ] Schema + migrazioni versionate
- [ ] Load su `WorldLoadEvent`, save asincrono con write batching
- [ ] Salvare: posizione, definizione, health, `placedBy`, `placedAt`, lista autorizzati,
      contenuto inventario cupboard, `nextUpkeepAt`
- [ ] Nessuna chiamata Bukkit fuori dal main thread — I/O async, mutazione via scheduler

**Fatto quando:** riavvio il server e la base è ancora lì con la vita giusta.

---

## Fase 5 — Crafting

- [ ] `core.crafting.BukkitCraftingService` — mappa `RecipeDefinition` su `ShapedRecipe`
- [ ] `unregisterAll()` prima di ogni reload, altrimenti le ricette si duplicano
- [ ] Ingredienti custom (Nexo/MMOItems): `RecipeChoice.ExactChoice` sugli stack risolti
- [ ] Wiring: `setCrafting(...)`

---

## Fase 6 — Shop

- [ ] Integrazione Vault (già `soft-depend` in `paper-plugin.yml`)
- [ ] `core.shop.VaultShopService` — implementa `ShopService`, tutti i rami di `Result`
- [ ] Acquisto atomico: o scala i soldi *e* consegna, o non fa nulla
- [ ] Persistenza di `amount-available` (lo stock globale cala e deve sopravvivere al riavvio)
- [ ] GUI dello shop (opzionale, dopo il comando)

---

## Fase 7 — Comandi e UX

- [ ] `core.command.ToolCupboardsCommand` — `/tc`
  - [ ] `reload` (già c'è `ToolCupboardsPlugin#reloadEverything`, manca solo il comando)
  - [ ] `auth <player>` / `unauth <player>` / `authlist`
  - [ ] `info` — stato upkeep, tempo rimanente, blocchi coperti
  - [ ] `give <block> [amount]` (admin)
  - [ ] `shop`
- [ ] Permessi in `paper-plugin.yml`
- [ ] Messaggi configurabili (`messages.yml`) — non hardcodare stringhe nei service
- [ ] Feedback visivo del claim (particelle sul bordo di `ClaimArea`)
- [ ] Indicatore di danno sui blocchi (BossBar o action bar con `healthRatio()`)

---

## Fase 8 — API pubblica: completare gli eventi

`api.event` ha solo `BuildBlockDamageEvent`. Mancano:

- [ ] `BuildBlockPlaceEvent` (cancellabile)
- [ ] `BuildBlockDestroyEvent`
- [ ] `ToolCupboardPlaceEvent` (cancellabile)
- [ ] `ToolCupboardRemoveEvent`
- [ ] `CupboardAuthorizationEvent` (cancellabile)
- [ ] `UpkeepChargeEvent` — con lo stato prima/dopo
- [ ] Javadoc in inglese su tutti (superficie pubblica)

---

## Fase 9 — Test

- [ ] `ClaimArea.contains` — sfera vs cubo, bordi, mondi diversi
- [ ] `YamlBlockDefinitionLoader` — file validi, file rotti (uno rotto non deve fermare gli altri),
      `config-version` sbagliata, chiavi con `.` tipo `mmoitems:MATERIAL.STEEL`
- [ ] `PluginSettings.from` — default e valori fuori range
- [ ] `RecipeDefinition` / `UpkeepRequirement` / `ShopEntry` — validazione nei compact constructor
- [ ] Aggregazione upkeep e decay proporzionale (logica pura, testabile senza server)
- [ ] Regola fondamenta di `canPlace`

I service sono dietro interfacce apposta: la logica va testata senza avviare un server.

---

## Decisioni aperte

Queste bloccano o cambiano il lavoro a valle. Vanno decise, non scoperte a metà implementazione.

1. **Rendering dei blocchi custom.** `item-settings.block-texture` esiste nel config ma non è ancora
   deciso *come* il blocco appare nel mondo: blocco custom di Nexo, `NOTE_BLOCK`/`MUSHROOM_STEM` con
   blockstate, o item display. Determina l'intera Fase 1 e se Nexo è di fatto obbligatorio.
2. **Claim sovrapposti.** `ToolCupboardService.PlacementRefusal.OVERLAPS_FOREIGN_CLAIM` presume che
   siano vietati, e `ProtectionService.cupboardCovering` risolve "il più vicino vince". Da
   confermare: si vietano del tutto o si permettono con OR delle autorizzazioni?
3. **`amount-available` dello shop** è per-server o per-giocatore? Ora è modellato come stock globale.
4. **Upkeep offline.** Il decay va recuperato al riavvio (tempo reale trascorso) o si conta solo il
   tempo con server acceso? Il primo è più Rust, il secondo è più clemente. Cambia lo schema di
   persistenza, quindi va deciso prima della Fase 4.
5. **Blocchi coperti da più cupboard** — chi paga l'upkeep? Uno solo, tutti, il più vicino?

## Discrepanze README ↔ implementazione

Da sanare in `README.md` (il codice segue `blocks/example.yml`, che è più curato):

- `time_unit` nel README, `time-unit` in `example.yml` e nel loader. La regola in `CLAUDE.md` è
  kebab-case → vince `time-unit`, il README va corretto.
- L'esempio nel README non ha `settings.block-type`, ma senza non si distingue fondamenta da
  struttura. Il loader lo **richiede**.
- `crafting-recipe.result-amount` è implementato (default 1) ma non compare nel README.
- `config.yml` non aveva `config-version`: aggiunto `1.0` e ora validato.
