# ROADMAP

Cosa manca, in ordine. Chi legge questo file deve capire **cosa fare, in quale file, e a cosa stare
attento** senza aprire il codice.

Le fasi sono in ordine di dipendenza: ogni fase produce qualcosa di giocabile e sblocca la successiva.
Lo stato di ciò che esiste è in `LOG.md`, le regole del gioco in `README.md`.

Legenda: `[ ]` da fare · `[~]` parziale · `[x]` fatto.

> Aggiornato al 2026-08-08.

---

## In due righe: dove siamo

**Tutto compila** (`./gradlew build` verde dall'8 agosto): la Fase 0.5 è chiusa, il plugin si avvia,
legge `config.yml` e carica i `blocks/*.yml`. Il prossimo passo non è codice ma una scelta: come
appare un blocco custom una volta piazzato nel mondo (*Decisioni aperte #1*). Da lì parte la Fase 1.

---

## Fase 0 — Struttura (fatta)

- [x] Split in moduli Gradle `:api` / `:core`; la root non ha sorgenti, `:core` produce il jar
- [x] Package unificati sotto `it.yteicos.toolcupboards.*`
- [x] Superficie `:api` completa e compilante
- [x] `config.yml` e `blocks/example.yml` con `config-version`
- [x] `LOG.md`

---

## Fase 0.5 — Far ricompilare `:core` (fatta)

Sei file, tutti scritti l'8 agosto. Le note qui sotto restano come documentazione delle scelte fatte.

### 1. `core/config/PluginSettings.java` — da creare

Legge `config.yml` una volta sola e lo trasforma in un oggetto tipizzato.

- Serve a `ToolCupboardsServices#settings()` e a `ToolCupboardsPlugin#loadSettings`
  (`ToolCupboardsPlugin.java:97`).
- Il bootstrap cattura `RuntimeException`: se un valore è sbagliato **lancia e basta**, non
  correggere in silenzio (`CLAUDE.md` §4).
- Forma consigliata:

  ```java
  public record PluginSettings(ClaimShape claimShape, int claimRadius, int maxCupboardsPerPlayer,
                               long tickIntervalSeconds, double decayPerMissedTick,
                               boolean shopEnabled, Set<String> worlds) {
      public static PluginSettings from(FileConfiguration config) { ... }
      public boolean enabledIn(String worldName) { return worlds.isEmpty() || worlds.contains(worldName); }
  }
  ```

- ⚠️ `config-version` va letto come **stringa** (`"1.0"`). Con `getDouble` una chiave mancante
  restituisce `0.0` e non distingui "assente" da "versione sbagliata".

- [x] scritto e validato

### 2. `core/block/SimpleBuildBlockRegistry.java` — da creare

Implementa `BuildBlockRegistry`.

- `LinkedHashMap<String, BuildBlockDefinition>` con chiave id **lowercase**: l'ordine di
  registrazione fa parte del contratto (`BuildBlockRegistry.java:28`).
- `register` lancia `IllegalStateException` se l'id è già preso — il bootstrap la cattura e logga
  (`ToolCupboardsPlugin.java:113`).
- Serve anche un `clear()`, che non è nell'interfaccia: lo chiama il reload per non lasciare
  definizioni vecchie in giro (`ToolCupboardsPlugin.java:110`).

- [x] scritto

### 3. `core/item/SimpleItemProviderRegistry.java` — da creare

Implementa `ItemProviderRegistry`. Stessa struttura del registry sopra, ma la chiave è il
`namespace()` del provider.

- ⚠️ I due metodi di comodo `createItem` e `matches` (`ItemProviderRegistry.java:40,49`) devono
  **controllare `isAvailable()`** e restituire `Optional.empty()` / `false` se il plugin di supporto
  non è installato. È questo che permette al server di girare senza Nexo o MMOItems.

- [x] scritto

### 4. `core/item/VanillaItemProvider.java` — da creare

Implementa `ItemProvider` per il namespace `minecraft`.

- `namespace()` → `"minecraft"`, `isAvailable()` → sempre `true`.
- `createItem` → `Material.matchMaterial(key.id())`, con guardia `amount > 0`.
- `matches` → confronto su `stack.getType()`.
- ⚠️ Limite noto e accettato: un item Nexo costruito sopra `PAPER` risulta "match" per
  `minecraft:paper`. Va bene finché l'upkeep confronta chiavi esatte, ma ricordarselo.

- [x] scritto

### 5. `core/item/NexoItemProvider.java` — da finire (**non compila**)

Non è uno stub innocuo: `createItem` (riga 24) non ha `return`, quindi rompe il build.

- I metodi Nexo che servono sono già annotati nel commento dentro il file.
- `matches` → `key.id().equals(NexoItems.idFromItem(stack))`.
- Se vuoi rimandare: fai almeno tornare `Optional.empty()`, così il build passa.

- [x] finito

### 6. `core/config/YamlBlockDefinitionLoader.java` — da creare (il più delicato)

Implementa `BlockDefinitionLoader`. Costruttore `YamlBlockDefinitionLoader(java.util.logging.Logger)`
(`ToolCupboardsPlugin.java:107`).

`loadAll(Path)`: crea la cartella se manca, prende i `*.yml` **ordinati per nome**, e per ognuno fa
il parse dentro un `try/catch`. **Un file rotto si logga e si salta**, non ferma gli altri
(`BlockDefinitionLoader.java:16`).

Da `blocks/example.yml` al record `DefaultBuildBlockDefinition` (già pronto):

| YAML                              | campo del record                                              |
|-----------------------------------|---------------------------------------------------------------|
| nome file senza estensione, lowercase | `id`                                                      |
| `settings.block-name`             | `blockName`                                                   |
| `settings.block-type`             | `BuildBlockType.valueOf(...toUpperCase())`                    |
| `settings.block-health`           | `maxHealth`                                                   |
| `settings.required-materials.*`   | `List<UpkeepRequirement>`                                     |
| `shop-settings`                   | `new ShopEntry(enabled, price, amountAvailable)` — assente → `ShopEntry.DISABLED` |
| `model-settings.item-type`        | `itemModel` **e** `blockTexture` (vedi nota sotto)             |
| `crafting-recipe`                 | `RecipeDefinition(pattern, ingredients, resultAmount)` — assente → `null` |

L'intervallo di upkeep si costruisce così:
`ChronoUnit.valueOf(timeUnit).getDuration().multipliedBy(frequencyCheck)`.

⚠️ **La trappola dei punti.** Una chiave come `mmoitems:MATERIAL.STEEL` viene spezzata da Bukkit,
che tratta il `.` come separatore di path: al load te la ritrovi come sezione annidata
`mmoitems:MATERIAL` → `STEEL`. Si risolve cambiando il separatore **prima** di caricare:

```java
YamlConfiguration yaml = new YamlConfiguration();
yaml.options().pathSeparator('/');   // deve venire prima di load()
yaml.load(file.toFile());
yaml.getConfigurationSection("settings/required-materials");
```

Da lì `getKeys(false)` restituisce le chiavi intere.

⚠️ **Decisione da prendere qui, non dopo.** `BuildBlockDefinition` ha due campi separati,
`itemModel()` e `blockTexture()`, ma `example.yml` ha una chiave sola, `model-settings.item-type`
(e il README parla ancora di `item-settings.item-modeldata` / `block-texture`, che non esistono più).
Due strade:

- **(a)** il loader legge `item-type` e lo passa a entrambi i campi → zero modifiche all'API, si fa
  subito. **Consigliata ora.**
- **(b)** si collassano i due metodi in un `model()` solo dentro `:api` → più pulito, ma tocca la
  superficie pubblica. Da fare quando si decide il rendering (Decisione aperta #1).

- [x] scritto

### Cosa è stato deciso strada facendo

- È nato un settimo file, `core/config/ConfigVersion.java`: la lettura di `config-version` serviva
  in due posti (config generale e file dei blocchi) e YAML tipizza il valore da solo (`1` diventa
  Integer, `1.0` Double, `"1.0"` String). Normalizzare in un posto solo evita che le due letture
  divergano.
- Scelta **(a)** sul modello: `model-settings.item-type` viene letto una volta e passato sia a
  `itemModel` sia a `blockTexture`. La chiave è **obbligatoria**: senza, non si saprebbe cosa mettere
  in mano al giocatore.
- I tre item provider sono registrati sempre, anche se Nexo o MMOItems non sono installati: ognuno
  risponde `isAvailable() == false` e il registry non gli delega niente.
- `time-unit` accetta solo `SECONDS`, `MINUTES`, `HOURS`, `DAYS`.

**Fase finita:** `./gradlew build` è verde.

---

## Fase 1 — I blocchi custom esistono nel mondo

Senza questa fase non c'è niente da proteggere. **Buona parte è già scritta** (7 agosto): mancano
solo gli ultimi pezzi.

- [x] `core.block.DefaultBuildBlock` — `BuildBlock` con health mutabile, clampata tra 0 e `maxHealth`
- [x] `core.item.MMOItemsItemProvider` — provider per `mmoitems:TYPE.ID`
- [~] `core.block.DefaultBuildBlockService` — fatto: indice posizionale, regola fondamenta in
      `canPlace`, pipeline di danno, `blocksIn`. **Restano due TODO dentro il file**: mettere la
      texture nel mondo al piazzamento e le chiamate alla persistenza
- [ ] **Decidere il meccanismo di rendering** dei blocchi custom → *Decisioni aperte #1*.
      Blocca i due TODO qui sopra e tutto il resto della fase.
      (Idea già presa: si indica l'item type nel config; se non ha prefisso `nexo:` o `mmoitems:`
      si usa un normale item di Minecraft)
- [ ] `core.listener.BuildBlockPlaceListener` — riconosce l'item custom in mano e piazza un
      `BuildBlock` invece del blocco vanilla
- [ ] Marcare i blocchi piazzati con un `PersistentDataContainer` (sul chunk o su block-entity), così
      un riavvio senza storage non li perde in silenzio
- [ ] Wiring in `ToolCupboardsPlugin#onEnable` + `services.setBlocks(...)` — il punto esatto è già
      segnato da un TODO a `ToolCupboardsPlugin.java:55`

**Fase finita quando:** piazzo un blocco custom, lo colpisco, perde vita e a 0 sparisce.

---

## Fase 2 — Tool cupboard e claim

È la fase che accende `gameplayReady()` e rende il plugin davvero giocabile.

- [ ] `core.cupboard.DefaultToolCupboard` — lista autorizzati, `Inventory` di upkeep, area proiettata
- [ ] `core.cupboard.DefaultToolCupboardService`:
  - [ ] `refusePlacement`: fondamenta sotto, overlap, cap `claim.max-per-player`, mondo abilitato
  - [ ] `place` / `remove`
- [ ] `core.claim.ClaimIndex` — indice spaziale. Non iterare tutti i cupboard a ogni evento:
      chunk-key → lista cupboard, poi test esatto con `ClaimArea.contains`
      (`ClaimArea.boundingRadius()` esiste apposta per il pre-filtro)
- [ ] `core.claim.DefaultProtectionService` — regole di `ClaimAction`:
  - [ ] posizione non coperta da nessun cupboard → permetti tutto (è ciò che rende i blocchi senza
        cupboard rompibili da chiunque)
  - [ ] `BREAK_BUILD_BLOCK` / `PLACE_BUILD_BLOCK` / `BREAK_CUPBOARD` / `OPEN_CUPBOARD` /
        `MANAGE_AUTHORIZATIONS`
  - [ ] **non** proteggere chest, mob, blocchi vanilla — è deliberato (README)
- [ ] `core.listener.CupboardListener` — piazzamento, rottura, apertura inventario
- [ ] Estendere `BuildProtectionListener` alle esplosioni (`EntityExplodeEvent`, `BlockExplodeEvent`)
      → devono passare da `BuildBlockService.damage`, non essere cancellate
- [ ] Sostituire il `TODO: raiding` in `BuildProtectionListener#onBreak`: una rottura non autorizzata
      entra nella pipeline di danno, non viene cancellata
- [ ] Wiring: `setCupboards(...)`, `setProtection(...)` → `gameplayReady()` diventa `true`

**Fase finita quando:** un cupboard protegge la base e un non autorizzato deve rompere il muro a colpi.

---

## Fase 3 — Upkeep e decay (il loop centrale)

- [ ] `core.upkeep.DefaultUpkeepService`:
  - [ ] `requirementsOf`: somma i requisiti di tutti i blocchi coperti, aggregando per `ItemKey`
  - [ ] `charge`: consuma dall'inventario del cupboard via `ItemProviderRegistry.matches`
  - [ ] pagamento parziale → decay proporzionale, non tutto-o-niente
  - [ ] `statusOf` / `remainingMillis` per la UI
- [ ] `core.upkeep.UpkeepScheduler` — task ogni `upkeep.tick-interval-seconds`; ogni cupboard ha il
      suo `nextUpkeepAt`, il tick globale è solo la sveglia
- [ ] Decay: `decay-per-missed-tick` di danno ai blocchi coperti via `BuildBlockService.damage` con
      `Cause.DECAY`
- [ ] Recuperare i tick persi mentre il server era spento (altrimenti basta riavviare per non pagare
      mai) → *Decisioni aperte #4*
- [ ] Wiring: `setUpkeep(...)`

**Fase finita quando:** una base senza materiali nel cupboard crolla da sola.

---

## Fase 4 — Persistenza

Prima di qui, tutto si perde al riavvio.

- [ ] `core.storage.SqliteStorageBackend` — default
- [ ] `core.storage.YamlStorageBackend` — server piccoli (opzionale)
- [ ] `core.storage.MySqlStorageBackend` — network (opzionale)
- [ ] Schema + migrazioni versionate
- [ ] Load su `WorldLoadEvent`, save asincrono con write batching
- [ ] Salvare: posizione, definizione, health, `placedBy`, `placedAt`, lista autorizzati, contenuto
      dell'inventario del cupboard, `nextUpkeepAt`
- [ ] Nessuna chiamata Bukkit fuori dal main thread — I/O async, mutazione via scheduler

**Fase finita quando:** riavvio il server e la base è ancora lì con la vita giusta.

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
  - [ ] `reload` (la logica c'è già in `ToolCupboardsPlugin#reloadEverything`, manca il comando)
  - [ ] `auth <player>` / `unauth <player>` / `authlist`
  - [ ] `info` — stato upkeep, tempo rimanente, blocchi coperti
  - [ ] `give <block> [amount]` (admin)
  - [ ] `shop`
- [ ] Permessi in `paper-plugin.yml`
- [ ] Messaggi configurabili (`messages.yml`) — niente stringhe hardcodate nei service
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
- [ ] Javadoc in inglese su tutti (è superficie pubblica)

---

## Fase 9 — Test

I service stanno dietro interfacce apposta: la logica si testa senza avviare un server.

- [ ] `ClaimArea.contains` — sfera vs cubo, bordi, mondi diversi
- [ ] `YamlBlockDefinitionLoader` — file validi, file rotti (uno rotto non ferma gli altri),
      `config-version` sbagliata, chiavi con il punto tipo `mmoitems:MATERIAL.STEEL`
- [ ] `PluginSettings.from` — default e valori fuori range
- [ ] `RecipeDefinition` / `UpkeepRequirement` / `ShopEntry` — validazione nei compact constructor
- [ ] Aggregazione upkeep e decay proporzionale (logica pura)
- [ ] Regola fondamenta di `canPlace`

---

## Decisioni aperte

Vanno decise a tavolino, non scoperte a metà implementazione.

1. **Rendering dei blocchi custom.** Come appare il blocco nel mondo: blocco custom di Nexo,
   `NOTE_BLOCK`/`MUSHROOM_STEM` con blockstate, o item display? Determina tutta la Fase 1, i due TODO
   rimasti in `DefaultBuildBlockService` e se Nexo diventa di fatto obbligatorio.
2. **Claim sovrapposti.** `PlacementRefusal.OVERLAPS_FOREIGN_CLAIM` presume che siano vietati, e
   `ProtectionService.cupboardCovering` risolve "il più vicino vince". Da confermare: vietati del
   tutto, o permessi con OR delle autorizzazioni?
3. **`amount-available` dello shop**: per-server o per-giocatore? Ora è modellato come stock globale.
4. **Upkeep offline.** Il decay si recupera al riavvio (tempo reale trascorso) o conta solo il tempo
   a server acceso? Il primo è più Rust, il secondo più clemente. Cambia lo schema di persistenza,
   quindi va deciso **prima** della Fase 4.
5. **Blocchi coperti da più cupboard**: chi paga l'upkeep? Uno solo, tutti, il più vicino?

---

## Discrepanze README ↔ implementazione

Da sanare in `README.md` (il codice segue `blocks/example.yml`, che è più curato):

- `time_unit` nel README, `time-unit` in `example.yml` e nel loader. La regola in `CLAUDE.md` §4 è
  kebab-case → vince `time-unit`, il README va corretto.
- L'esempio nel README non ha `settings.block-type`, ma senza non si distingue fondamenta da
  struttura. Il loader lo **richiede**.
- `crafting-recipe.result-amount` è implementato (default 1) ma non compare nel README.
- Il README parla di `item-settings.item-modeldata` e `item-settings.block-texture`; `example.yml`
  ha invece `model-settings.item-type`, una chiave sola. Vedi la nota nella Fase 0.5 §6.
- `config.yml` non aveva `config-version`: aggiunto `1.0`, ora validato.