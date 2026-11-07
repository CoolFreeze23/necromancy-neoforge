# Necromancy for NeoForge 1.21.1

A port of **Necromancy 1.7.1** (Minecraft 1.7.10) by sirolf2009, maintained by AtomicStryker, with art by Mr_boness,
Fabiulu and TheBlackPixel. Source: the AtomicStryker Google Code archive, `hg/Necromancy`.

**Private build.** sirolf2009's NecroAPI and the original Necromancy are LGPL-3.0, but AtomicStryker's 1.7.10
changes fall under the AtomicStryker license, which forbids publishing compiled builds outside his own pages. Keep this
jar for personal use and private packs; do not upload it.

## What carried over

Everything that worked in 1.7.1:

- **Necronomicon.** Use it on planks with two cobblestone in a row to bind the 3-block **Summoning Altar**. Breaking
  any of the three blocks unbinds it and gives the original blocks back.
- **Sewing Machine.** A 4x4 grid, a Bone Needle and String. It sews the original head, torso, arm and leg patterns from
  organs and each mob's "essence". Recipes are data-driven now (`necromancy:sewing_shaped` and
  `necromancy:sewing_shapeless`), and JEI shows them.
- **Organs.** Brains, heart, muscle, lungs and skin, all edible. Leather becomes skin in the Sewing Machine.
- **Blood and souls.** The Bone Needle plus a bottle draws a Jar of Blood. A killing blow with a scythe plus a bottle
  bottles a soul. Blood is a real fluid with its own bucket. Jars and buckets convert into each other.
- **The Altar.** Blood, soul and five body-part slots, the original layout, with the Minion lying on the table as it
  takes shape. Sneak-use the altar, or press **Raise**, to bring the Minion to life.
- **Minions.** The 17 original creatures (Cave Spider, Chicken, Cow, Creeper, Enderman, Iron Golem, Isaac, Pig, Zombie
  Pigman as the Zombified Piglin, Sheep, Skeleton, Spider, Squid, Villager, Witch, Wolf head, Zombie), with their
  original per-part stats (operation-1 attribute modifiers summed).
- **Minion behaviour.** Follow, sit, fight, saddle and ride, Brain on a Stick steering (it never worked in 1.7.1),
  Isaac-head tears, and `/minion set aggressive|passive|friend|enemy`.
- **Creatures.** Night Crawlers and the Isaac chain (Isaac → Blood Isaac → Head + Body) replace rare natural zombie and
  skeleton spawns. Isaac's Head is a helmet that weeps tears on keys (R and G now, because F is the off-hand key). The
  Necromantic Teddy, sewn from leather and wool, scares monsters away. The Soul Heart spawner.
- **Village cemetery** with graves, a tomb and a necromancer's house. The **Nether Chalice**.
- **Necromancer villager.** Its job site is the Sewing Machine. Five trade levels.
- Achievements are now **advancements**.

Fixed on the way: a Bone Needle passing as blood, wrong-slot parts building the wrong body, headless summons eating
the items, a null-pointer crash when a Minion died with its master offline, the counter that reset on respawn, sounds
that never resolved, the Teddy's memory leak and owner hijack, world generation outside the chunk, and client-only
tear cooldowns.

## Modernised

1.21 data components, attributes and tags; data-driven recipes, loot tables, enchantment and advancements; a global
loot modifier for organs; a NeoForge fluid; the Sewing Machine as a POI job site; jigsaw village pieces; ModConfigSpec
(`config/necromancy-common.toml`); attachments and SavedData instead of player NBT; a Brigadier command; custom network
payloads with server-side checks; and JEI support. Minions take their geometry from the vanilla 1.21 models, so every
part matches the current vanilla textures.

## Added to make it feel complete

These follow the design review in `docs/REVIEW.md`:

- **Part traits and a balance pass (#1).** Every part *does* something: venom, blinks, golem tosses, potion throws,
  explosions on death, egg laying, milking and shearing, wall climbing, sunburn, water damage, fire-proofing, armour.
  Base speed is 0.065, capped at 0.35. The Wolf head is toned down. `classicStats` restores 1.7.1 exactly.
- **Orders and stances (#2).** The Bone Scepter selects Minions and gives Follow, Stay, Guard, Patrol or Work, plus
  Passive, Defensive or Aggressive stances. Also `/minion list|sit|follow|forget`.
- **Minion screen and equipment (#3).** Sneak with an empty hand to open it. It has armour slots, both hands and a
  saddle, and shows stats, rank and order. Bows and tridents work with the right arms.
- **Ten modern creatures as parts (#4):** Husk, Drowned, Bogged, Wither Skeleton, Blaze, Slime, Mooshroom, Snow Golem,
  Armadillo and Goat, plus the Night Crawler (#13).
- **Soul potency (#5).** A soul remembers what it was reaped from. Its strength caps how heavy a body it can lift.
  Legendary souls give +20% health and Undying.
- **Shrine tiers (#6).** Candles, soul lights, blood and mob heads around the altar allow 3, 5 or 8 Minions.
- **Phylactery and Remains (#7).** A bound Minion leaves Remains that raise it again, name and kills included. The
  Phylactery also recalls it.
- **Harvest economy (#8).** Organs drop from the right creatures (killed by you or your Minions). There is a Reaping
  scythe enchantment, and cemetery graves to brush.
- **Jobs (#10).** Farmer, Collector and Reaper under the Work order.
- **Blood heals (#11).** Minions mend in blood and from Jars of Blood. Players in blood go hungry.
- **Veterancy (#12).** Five ranks from kills, up to Revenant.
- **The mod's own creatures pay out (#13)**, and the scythes are rebalanced with a Netherite Scythe that has Soul
  Harvest (#14).
- **The Necronomicon is a guide book (#15)** with an advancement tree.
- **The Rite of the Abomination (#17).** A repeatable boss raised at a tier III shrine with an Ominous Bottle.

## Left out

The Scent Burner and its shaders (the original author had already removed them as broken), the special-scythe web
fetch to a dead Dropbox link, Thaumcraft hooks, the unfinished apprentice armour and Skull Wall, boss creatures as
body parts, and true flight.

## Languages

English and Brazilian Portuguese. pt-BR uses Minecraft's own names for every vanilla creature and item. Sources are
in `src/main/lang-src/` (hand-written texts) and in `tools/gen_pt_br.py`. `tools/check_lang.py` proves that every key
the code uses exists in both languages.

## Building and testing

- Build: `GRADLE_USER_HOME=D:/Ports/gradle-home ./gradlew build`.
- Data: `./gradlew runData`. It reads `src/main/lang-src`. Commit `src/generated`.
- GameTests run on the hub: `bash tools/hubcheck.sh`.
- Hidden client: `GAME_LOCK=... bash tools/smoke.sh tools/<script>.txt`.
