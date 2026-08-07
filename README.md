Sto realizzando un plugin che mi permette di creare in minecraft java server dei claim, ma claim diversi, tipo quelli di rust, con la tool cupboard (un blocco in minecraft) che piazzandolo crea un'area, sferica o quadratica che mi permette di  proteggere i blocchi entro un determinato range, la particolarità è che ogni blocco per buildare è un blocco custom definibile da config, craftabile con item normali di minecraft o provenienti 
da texturepack manager (come nexo) o mmoitems per esempio.

# Blocchi da costruzione
I blocchi da costruzione possono essere definiti andando nel folder del plugin
/blocks/example.yml
all'interno del file .yml la struttura per definire un blocco deve essere circa questa:

```yaml
config-version: 1.0 # Do not touch it

settings:
  block-name: REINFORCED_CONCRETE
  block-health: 4000
  required-materials:
    block-name: #for nexo one nexo:block etc...
      time_unit: HOURS
      frequency-check: 1
      amount: 40 # Ciò significa che ogni ora serviranno 40 blocchi di [block_name] altrimenti
      # i blocchi da costruzione e da fondamenta di quel tipo inizieranno a perdere vita

shop-settings:
  enabled: true
  price: 10000
  amount-available: 10000

item-settings:
  item-modeldata: "nexo:/" # Non ricordo bene la sintassi della definizione
  block-texture: "nexo:/"

   
```

## Tipologia
I blocchi hanno diverse tipologie in base all'uso

- Blocchi da fondamenta
- Blocchi da costruzione

I blocchi da costruzione possono essere inseriti solo al di sopra di blocchi da fondamenta

## Lo shop
I blocchi possono essere acquistati dallo shop ma quest'ultimo è disattivabile nel config generale
altrimenti si possono semplicemente escludere alcuni item facendo shop-settings.enabled su false
che risulta più semplice per blocchi di tier troppo alto da poter essere acquisiti così. 

## Crafting
Poi viene
la parte del crafting, quest'ultima, la più importante, ci serve per permettere ai player di craftare i blocchi con la quale verrà costruita
la base, questo si configura aggiungendo una sezione al config del singolo blocco

```yaml
...

crafting-recipe:
  pattern:
    - "AAA"
    - "BBB"
    - "CCC"
  A: "blocco_custom"
  B: "blocco_custom"
  C: "blocco_custom"
```

In una V2 sarà previsto anche un sistema di crafting custom per questi blocchi, e comunque verrà fornita
un'api pubblica nel caso si volesse fare per conto proprio prima dell'uscita di quella feature

# Funzionamento delle TOOL CUPBOARDS

Un blocco standard il quale piazzato entro un determinato range da protezione a tutti i blocchi piazzati, ha bisogno di essere piazzato su 
un blocco di fondamenta per forza, la tool cupboard avrà una lista interna in cui salverà tutti i player
che possono spaccare blocchi, piazzarne etc... il claim in se non protegge la chest ma bensì solo l'esterno
così che i raiders possano fare breccia nelle mura e poi appropiarsi di ciò che c'è dentro, infatti questi ultimi i blocchi menzionati
sopra sono esattamente quelli che, all'occorenza, vengono distrutti da altri giocatori, in quanto solo questi ultimi contano come claim, dunque protetti e non spaccabili di conseguenza.
Ovviamente, finché non è presente una tool cupboard sono spaccabili da tutti i giocatori.

I blocchi usati per costruire hanno una particoalrità hanno bisogno di fuel per essere tenuti su
altrimenti c'è il rischio che la base dei player crolli, questo obbliga i player a loggare periodicamente
per farmare ed è una cosa che ai server dovrebbe piacere e questi blocchi da mantenimento vanno inseriti
nella tool cupboard come su rust ovviamente
