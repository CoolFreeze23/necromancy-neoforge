# OreSpawn creatures as Necromancy body parts: design review

Reviewed 2026-11-08 against the OreSpawn port at `C:\Homework\Projects\Orespawn` (commit d1597f31: `ModEntities.java`, 627 entity classes, 104 GeckoLib models, `sounds.json`, the item registry, loot tables and `audit_sections/`) and Necromancy as it stands (`VanillaNecroMobs.java`, `Traits.java`, `SoulPotency.java`, `docs/REVIEW.md`).
This is a design document. Every number is a proposal for tuning, not a measured value. The machine-readable twin is `docs/orespawn_parts_spec.json`. Both were generated from one design table and checked by a script: every id, bone pattern, balance band and essence pair below was verified. Treat the JSON as the source of truth for later edits.

**Conventions** (the same as `docs/REVIEW.md`)

- **Part stats** are `(health, followRange, knockbackResist, movementSpeed, attackDamage)`. Each value multiplies the base (20 HP, 16 follow, 0.1 KB, 0.065 speed, 2 damage). They are applied as `ADD_MULTIPLIED_BASE` and summed over all five slots; both arms count. Speed is capped at 0.35 and knockback resistance at 1.0.
- **Extra attributes** are flat `ADD_VALUE` modifiers with 1.21.1 ids (`minecraft:generic.armor`, `generic.scale`, `generic.step_height` and so on). Part armour totals at most 20.
- **Weight** is 1 to 4 per part. A soul's potency caps the sum: Faint 6, Lesser 9, Common 12, Potent 16, Mighty 20, Legendary 20 plus its bonuses.
- **Ticks:** t = ticks (20 t = 1 s). **W** = weight. **H T A L** = head, torso, arm, legs.
- ★ marks the 15 creatures I think are the most exciting to stitch (section 7).

---

## 0. TL;DR

- **97 creatures give parts** (99 entity ids, because Jeffery shares the Giant Robot's parts and the second Leonopteryx id shares the Leon's). They give 315 part items. **29 living registrations give no parts:** 9 bosses, 2 royal hatchlings, 3 boss hitbox sidecars, 9 objects and vehicles, 4 incorporeal things and 2 companion NPCs. The 17 projectile and item entities are not creatures at all.
- **Weights:** 35 at weight 1, 23 at weight 2, 23 at weight 3, 16 at weight 4. Nothing is heavier than a golem. No OreSpawn torso or legs beats the Iron Golem's, and the sturdiest possible hybrid (OreSpawn head and arms on golem legs) reaches 280 HP, below the 300 HP of the Warden-head golem that REVIEW #16 already allows.
- **32 trait types** cover every ability. Each one is a single Java class driven by parameters. 29 of the 31 existing hand-written traits become instances of these types, and 5 new trait flags are added.
- **One rule change is needed:** an OreSpawn creature's health counts half toward soul potency (config `souls.oreSpawnHealthScale = 0.5`). OreSpawn's health numbers run two to five times vanilla's, so without the rule a passive Whale would give a Mighty soul.
- **Essences** are unique for every creature and part type. Every id is checked against the vanilla 1.21.1 data and the OreSpawn item registry, and none clashes with the vanilla table.

---

## 1. Design philosophy

OreSpawn runs from a 1 HP ant to the 7000 HP King. Necromancy's spine is narrow: stats are multipliers of one fixed base, the four weights are 1 to 4, and the five soul caps run from 6 to 20. That spine survives OreSpawn because **raw size never transfers**. These five rules decide every number in this document.

1. **Weight carries size, and stats stay inside the vanilla band for that weight.** A weight-4 OreSpawn part is golem-class, never more. Hercules Beetle, Hammerhead and the Robo-Pounder all top out exactly at the Iron Golem torso `(5,0,2,0,0)` + 6 armour. The bands below are the largest value any part of that weight may have. Every part in this document was checked against them.

   | W | Head max | Torso max | Arm max (each) | Legs max | Armour (H/T/A/L) | Vanilla peers |
   |---|---|---|---|---|---|---|
   | 1 | (0.75,1.5,0.5,0.5,0.75) | (2,0,1,0,0) | (0.25,0,0,0,0.5) | (0.75,0,2,4,0.5) | 0/2/0/0 | chicken, cow, squid, cave spider |
   | 2 | (1,1.5,1,1,1) | (2.5,0,1,0,0) | (0.5,0,0.5,0,0.75) | (1.5,0,3,4,0.75) | 0/2/0/1 | zombie, skeleton, spider, witch |
   | 3 | (2,1.5,1,1,1) | (4,0,1.5,0,0) | (1,0,0.5,0,1.5) | (1.5,0,4,4,0) | 1/4/0/2 | enderman, blaze, wither skeleton |
   | 4 | (2,1.5,2,0.5,2) | (5,0,2,0,0) | (1,0,1,0,1.5) | (4,0,3,2.5,0) | 2/6/2/4 | iron golem, ravager |

2. **Power tracks weight, and heavy is slow.** Weight-4 legs give at most 2.5 speed (0.228 blocks/tick on their own). The fastest legs (speed 4, which hits the 0.35 cap with any head bonus) only exist at weights 1 and 2: Gazelle, Velocity Raptor and Ostrich, with the Burglar's 3.5 close behind. A Minion is either a tank or a sprinter, never both.

3. **OreSpawn's identity lives in the traits, not the numbers.** OreSpawn creatures are memorable for what they do: the Basilisk's gaze, the Cave Fisher's line, the Caterkiller's webs and butterflies, the robots' lasers, the Leonopteryx you can ride. Each of these becomes a parameterised trait (section 3). The weight bands then keep those traits from turning into raw stat inflation.

4. **Bosses are souls, not parts**, the same rule as `docs/REVIEW.md` #16. Mobzilla, the Kraken, the King and Queen, the Prince's adult and teen forms, the Nightmare, Mothra and the Alien Boss go into `necromancy:legendary_souls`. Their size, boss bars and multipart hitboxes don't fit a five-slot frame. Even their smallest part would be above weight 4.

5. **Soul potency is scaled for OreSpawn.** `SoulPotency.ofHealth` sees `max health × souls.oreSpawnHealthScale` (default 0.5) for any entity in the `orespawn` namespace. Under that rule:
   - The true heavies stay Mighty (raw 200 HP or more): Hercules Beetle, Emperor Scorpion, Caterkiller, Molenoid, Nastysaurus, Basilisk, Trooper Bug, Robo-Pounder, Giant Robot, Hammerhead, Leonopteryx, Cephadrome, Dragon, Spyro and the royal hatchlings.
   - The 80 to 199 HP band becomes Potent: T-Rex, Alien, Whale, Hydrolisc, Sea Viper and others.
   - A full weight-20 OreSpawn build therefore means killing one of OreSpawn's apex creatures with a scythe, the same "reap something strong" rule the vanilla golem body follows.
   - Two more rules: souls are never bottled from another player's tamed creature, and the three boss sidecars go into a new `necromancy:no_soul` tag. Those sidecars are 4000 to 6000 HP hitbox entities that would otherwise read as Mighty.

**What OreSpawn adds to Necromancy:**

- Real gliding wings or fins on 24 creatures.
- New mounts: the Camarasaurus (2 seats), Ostrich, Dragon, Leonopteryx and Cephadrome, the Whale on water, and pig-style saddles on the six orchard cows and the Spider Driver.
- Ranged heads and arms: fire, water, acid, laser and ink.
- Two new jobs: the Beaver's Lumberjack and the Molenoid's Digger.
- Producers that feed the economy: coal, rubies, golden apples and feathers.
- One deeply necromantic part: the Ender Reaper's arms, which **bottle souls with every kill**.

---

## 2. Exclusions

Every living OreSpawn registration is decided: either it gives parts, or it is in this table. The full registration list was checked against both. The 17 non-living entities are projectiles and item entities (`acid`, `laser_ball`, `thunder_bolt`, `ultimate_arrow` and so on) and are skipped.

| Entity | Kind | Soul | Decision |
|---|---|---|---|
| **Godzilla** `godzilla` | boss | Legendary | Mobzilla: 4000 HP, 25 blocks tall. Its soul is the prize; no part fits a Minion frame. |
| **Kraken** `kraken` | boss | Legendary | 1000+ HP flying squid boss with grab-and-carry. Legendary soul only. |
| **The King** `the_king` | boss | Legendary | 7000 HP three-headed final boss. Legendary soul only. |
| **The Queen** `the_queen` | boss | Legendary | 6000 HP three-headed boss. Legendary soul only. |
| **The Prince** `the_prince` | royal | Mighty | Royal hatchling (500 HP) that appears when the King dies and hatches from Prince eggs. Soul only, and only Mighty (by health, not the Legendary tag), so one boss kill cannot pay out two Legendary souls. |
| **The Princess** `the_princess` | royal | Mighty | Royal hatchling (400 HP) that appears when the Queen dies. Soul only, Mighty by health. |
| **The Prince** `the_prince_teen` | boss | Legendary | 1500 HP royal mount. Legendary soul only. |
| **The Prince** `the_prince_adult` | boss | Legendary | 3000 HP royal mount that grows into The King. Legendary soul only. |
| **Alien Boss** `alien_boss` | boss | Legendary | The WTF-dungeon's single capped summoner (320 HP). A dungeon boss: Legendary soul; the plain Alien gives the parts. |
| **Pitch Black** `pitch_black` | boss | Legendary | The Nightmare: Danger Dimension apex with a boss bar, 125-1000 HP by size tier. Legendary soul only. |
| **Mothra** `mothra` | boss | Legendary | Boss bar, multipart hitbox and a Nether Star drop. Legendary soul; the Brutalfly gives moth wings. |
| **King Head** `king_head` | sidecar | none | Invisible hitbox sidecar of The King. Not a creature: it must never yield a soul (6000 HP would read as Mighty). |
| **Queen Head** `queen_head` | sidecar | none | Hitbox sidecar of The Queen. No soul, no parts. |
| **Godzilla Head** `godzilla_head` | sidecar | none | Hitbox sidecar of Mobzilla. No soul, no parts. |
| **Coin** `coin` | object | none | A spinning loot pinata, not a creature. |
| **Island** `island` | object | none | A floating island block-entity with a health bar. |
| **Island Too** `island_too` | object | none | The second floating island. |
| **Hoverboard** `elevator` | object | none | The Hoverboard vehicle. |
| **Rock** `rock_base` | object | none | A rock you pick up. |
| **T-Shirt** `tshirt` | object | none | A T-shirt statue that drops leather. |
| **Purple Power** `purple_power` | object | none | An energy bomb the King and Queen throw; it has health only so it can be shot down. |
| **Ant Robot** `ant_robot` | object | none | A rideable mech built from a kit; a vehicle, not a body. |
| **Spider Robot** `spider_robot` | object | none | A rideable mech built from a kit; a vehicle, not a body. (The Spider Driver gives parts.) |
| **Ghost** `ghost` | incorporeal | Faint | A spirit with no flesh to stitch. It still yields a Faint soul. |
| **Ghost Skeleton** `ghost_skelly` | incorporeal | Faint | A ghost in chains; incorporeal, soul only. |
| **Vortex** `vortex` | incorporeal | Potent | A living whirlwind: no head, no torso, one bone in its model. Soul only. |
| **Rotator** `rotator` | incorporeal | Common | A crystal gyroscope of spinning blades; nothing that could be sewn. Soul only. |
| **Girlfriend** `girlfriend` | companion | Potent | A named companion NPC. The Villager already covers human parts, and stitching your partner is past the tone. Soul only. |
| **Boyfriend** `boyfriend` | companion | Potent | As the Girlfriend. Soul only. |

Notes on the harder calls:

- **Royal hatchlings** (the Prince and Princess) are soul-only. They are Mighty by health, not Legendary. Both appear free when the King or Queen dies, so if they were Legendary, one boss kill would pay out two Legendary souls.
- **The Alien Boss** is excluded, but the plain Alien gives parts. **Mothra** is excluded, but the Brutalfly gives moth wings. **The Spider Robot and Ant Robot** are vehicles built from kits; the Spider Driver that rides them gives parts. Every excluded creature has a part-giving cousin, so no theme is lost.
- **The Girlfriend and Boyfriend** are a tone call: they are named companions with voices and outfits. Necromancy already has human parts (the Villager), and sewing a player's partner onto a corpse is past "dark but vanilla". If you'd rather include them, they would be weight 2 bipeds with Ultimate-Bow arms.
- **The Vortex, Rotator and ghosts** have no flesh. The Vortex model is a single bone. They still give souls. The ghosts also join `necromancy:bloodless`, so the Bone Needle fails on them.

---

## 3. Trait vocabulary

### 3.1 Shared rules

Shared rules for every type. Server side only. 'Combat gate' = the Minion has a live target, is not sitting, its stance is not Passive, and (unless needs_los is false) it can see the target. Cooldowns use MinionEntity.cooldown with the key <mob>/<type>/<part>, and the Kindred bonus (soul matches the head's mob) shortens head cooldowns by 25%. 'Friendly' = the owner, the owner's Minions and spawnlings, the owner's tamed animals, and players on the owner's friend list: no trait ever harms, debuffs, pulls, teleports or disarms a friendly. Extra damage is dealt as mobAttack(minion) with invulnerableTime reset (the PACK_HUNTER pattern), so kills count as the owner's. Arm traits run once per Minion (right arm first) unless stated. Effects never use Instant Damage or Instant Health (both invert on undead).

### 3.2 Flags

The existing `TraitFlag` enum gains five values. Each row below is one flag the `flag` type can set.

| Flag | Enum | New? | Rule |
|---|---|---|---|
| `climbs` | `CLIMBS` | no | Climbs walls like a spider (legs). |
| `burns_in_sun` | `BURNS_IN_SUN` | no | Undead sunburn unless helmeted. |
| `hurt_by_water` | `HURT_BY_WATER` | no | Any part with it makes the Minion sensitive to water (vanilla isSensitiveToWater: 1 damage per second when wet). One part or four, the damage is the same. |
| `fire_proof` | `FIRE_PROOF` | no | Fire immunity when the torso and legs both have it (existing rule). Change: when the legs slot is empty, the torso alone counts, so a snake or fish torso can be fire-proof. |
| `sun_proof` | `SUN_PROOF` | no | Never sunburns. |
| `thinks` | `THINKS` | no | May take a job (head). |
| `sinks` | `SINKS` | no | No float goal. |
| `breathes_water` | `BREATHES_WATER` | **new** | New. The Minion never loses air underwater when its head or torso has it. |
| `cactus_proof` | `CACTUS_PROOF` | **new** | New, OreSpawn's signature immunity: ignores minecraft:cactus and minecraft:sweet_berry_bush damage. |
| `in_wall_proof` | `IN_WALL_PROOF` | **new** | New. Ignores minecraft:in_wall (suffocation) damage; burrowers. |
| `reaps` | `REAPS` | **new** | New. A killing blow from a Minion with this arm counts as a scythe kill: if the Minion's pack or its owner within 16 blocks holds a glass bottle, a Soul in a Jar is bottled for the owner. |
| `floats` | `FLOATS` | **new** | New. Bobs on the water surface instead of swimming down; overrides SINKS. |

### 3.3 The 32 types

Each row is one Java implementation: a `PartTrait` built from the JSON parameters. Parameters with a default are optional. The *Covers* column lists the existing hand-written traits that become instances of the type. Their ids stay the same, so saved Minions and lang keys are unaffected. Only `GOLD_FIGHTER` and `POTION_THROWER` stay bespoke.

| Type and parameters | Hook | Exact behaviour | Covers |
|---|---|---|---|
| `flag{flags}` | has(TraitFlag) | Passive properties the Minion checks. Each flag's rule is in the top-level 'flags' table. | CLIMBER, SUNBURN, SUN_PROOF, WATER_HURTS, FIRE_PROOF, THINKER, HEAVY |
| `on_hit_effect{effect, duration, amplifier=0, chance=1.0, cooldown=0}` | onAttack | After a successful melee hit, with probability chance (and off cooldown), target.addEffect(effect, duration, amplifier) sourced from the Minion. Vanilla rules decide stacking; undead targets ignore poison. | VENOM, HUNGER_BITE, HUSK_BITE, WITHERING, SHADOW_TOUCH |
| `on_hit_ignite{seconds, chance=1.0}` | onAttack | After a hit, with probability chance, target.igniteForSeconds(seconds). Does nothing to fire-immune targets or targets in water or rain. | - |
| `on_hit_toss{horizontal, vertical, chance=1.0, cooldown=0}` | onAttack | After a hit, target.push(dir.x*h, v, dir.z*h) with dir the normalised horizontal Minion->target vector, all scaled by (1 - target knockback resistance). Vertical is doubled against players (the OreSpawn rule). Plays the head mob's hurt sound at pitch 0.8. | GOLEM_TOSS (= horizontal 0, vertical 0.4) |
| `on_hit_disarm{slots, chance, to_pack=False}` | onAttack | Only against a Mob (never a player, never a named, tamed or owned mob). With probability chance, takes the first non-empty listed slot, damages it by max durability / 20 and drops it at the target's feet with no pickup delay for the Minion's owner (or into the pack when to_pack and the Minion has one; if the pack is full it drops). The mob's drop chance for that slot becomes 0 so the item is not duplicated. | - |
| `on_hit_block{block, chance, lifetime, cooldown}` | onAttack | Needs mobGriefing. With probability chance, places the block at the target's feet if that block is air or replaceable plants, and records it in a SavedData list; it is removed after lifetime ticks (also on chunk load if overdue). Never replaces a block entity or a block another trait placed. | - |
| `lifesteal{fraction, max_per_hit}` | onAttack | After a hit, minion.heal(min(max_per_hit, fraction * damage dealt)). heal() is direct, so it works although Minions ignore Regeneration. Arm copies stack per arm (each arm heals for its own hit share). | - |
| `bane{target, multiplier}` | onAttack | If the target matches, deal (multiplier - 1) * the Minion's attack damage as an extra hit. airborne = not on ground and not in water; small = bounding box width < 0.6; large = width * height > 4; wounded = below 50% health. | - |
| `charge{min_run, window, multiplier, knockback, cooldown}` | onAttack + tick | The tick hook records distance closed on the target. If the Minion closed min_run blocks within window ticks (min_run 0: the Minion has not hit this target for cooldown ticks, an ambush), the next hit deals an extra (multiplier - 1) * attack damage and pushes the target back by knockback (0.35 up). Then cooldown. | RAM (= min_run 4, window 40, multiplier 1.5, knockback 1.2, cooldown 100) |
| `pack{per_ally, max, radius=8}` | onAttack | Extra damage = min(max, per_ally * other Minions of the same owner within radius whose same part slot holds this trait). | PACK_HUNTER (= per_ally 1, max 3) |
| `ranged{projectile, cooldown, range, min_range=0, count=1, spread=0.0, velocity=1.6, damage=None, item=None, effect=None, effect_duration=0, effect_amplifier=0, ignite=0, explosion=0, needs_los=True}` | tick | Under the combat gate and range checks, every cooldown ticks fires count projectiles at the target's eye height minus 0.3, each with spread. Head parts fire from the eyes, arms from shoulder height (once per Minion; with two arms the volley alternates hands), torsos from the centre. necromancy:part_bolt is one generic ThrowableItemProjectile (gravity 0.01, life 100 t) that renders 'item' and carries damage/effect/ignite/explosion, so OreSpawn's own laser, water, acid and ice balls supply the look without their hard-coded 16-100 damage. Fireballs never set blocks alight unless mobGriefing. | TEARS, FIREBALLS, SNOWBALLS (POTION_THROWER stays bespoke) |
| `leap{min_range, max_range, horizontal, vertical, cooldown}` | tick | Under the combat gate, when on the ground and the target is between min_range and max_range: setDeltaMovement(dir * horizontal, vertical). Fall damage from the leap is cancelled (the landing within 40 t takes none). | - |
| `teleport{mode, cooldown, chance=1.0, min_distance=0, radius=8}` | tick, onHurt, onAttack | to_target: the BLINK rule (16 tries near the target, ender sounds, 40 t retry on failure). away_when_hurt: when hit by a projectile or indirect magic, with chance, randomTeleport within radius and cancel that hit (the enderman dodge). victim: after a melee hit, with chance, randomTeleport the target (a Mob or an enemy-listed player, never a boss or #c:bosses) within radius. | BLINK (= to_target, min_distance 6, cooldown 100) |
| `on_hurt_effect{effect, duration, amplifier=0, chance=1.0, melee_only=True}` | onHurt | When hurt by a living, non-friendly attacker, with chance apply the effect to that attacker. Plays a cue particle (squid ink for blindness, sneeze cloud for nausea). | INK_CLOUD |
| `on_hurt_reflect{fraction=0.0, flat=0.0, max, ignite_seconds=0, melee_only=True}` | onHurt | When hurt in melee by a living non-friendly attacker, deal min(max, flat + fraction * amount) as thorns(minion) damage to it, and ignite it if ignite_seconds > 0. Never reflects reflected damage. | - |
| `on_hurt_knockback{strength, chance}` | onHurt | When hurt by a melee attacker, with chance, attacker.knockback(strength) away from the Minion, with the slime-jump sound. | BOUNCY |
| `damage_filter{types, multiplier, condition=always}` | onHurt | Multiplies incoming damage whose type matches while the condition holds. Never applies to #minecraft:bypasses_invulnerability. Multiple filters multiply together; the total floor is 0.1 unless one filter is an immunity (0). | - |
| `last_stand{threshold, cooldown, heal_fraction=0.0, effects=[], damage_multiplier=1.0}` | onHurt | When a hit would leave the Minion below threshold * max health and the cooldown is clear: apply the effects to itself, heal heal_fraction * max health, multiply that hit by damage_multiplier. | ROLL_UP, DARK_BREW |
| `regen{amount, interval, condition}` | tick | Every interval ticks while the condition holds, minion.heal(amount). in_dark = block light and sky light both below 8. | - |
| `on_death_explode{power, fire=False, breaks_blocks=False}` | onDeath | The VOLATILE rule with this power. Charged (lightning-struck) Minions double it, up to 4. | VOLATILE |
| `on_death_cloud{effect, duration, amplifier, radius, cloud_duration}` | onDeath | Spawns an AreaEffectCloud owned by the Minion at its death position; the cloud skips friendlies (custom predicate). Undead Minions nearby are unaffected by poison anyway. | - |
| `summon{entity, count, cooldown, max_alive, lifetime, trigger=combat, scale=-0.5}` | tick, onHurt, onDeath | spawnling:<mob> creates a temporary Minion built from every part that mob has (missing slots stay empty), owned by the same master, stance Aggressive, sharing the summoner's target. Spawnlings never drop items, souls, organs or Remains, gain no veterancy, cannot summon, do not count toward the shrine cap, and turn to soul particles after lifetime or when the summoner dies. An ambient entity id (orespawn:butterfly) is spawned as-is and is cosmetic. combat = under the combat gate every cooldown; hurt = when damaged; death = once on death. | - |
| `aura{effect, amplifier, duration, radius, interval, targets, needs_los=False}` | tick | Every interval ticks applies the effect to each target in radius. enemies = hostile Mobs (Enemy), the Minion's target, and enemy-listed players, never friendlies, villagers or bosses; allies = the friendly set; owner = the owner only. For enemies the aura is suppressed while the Minion's stance is Passive. | - |
| `pull{radius, strength, interval, targets=target}` | tick | Under the combat gate, every interval ticks adds normalize(minion - victim) * strength (vertical * 0.5) to each victim in radius that is not a boss and weighs less than the Minion (bounding box volume). Plays a fishing-reel sound. | - |
| `produce{items, interval, count=[1, 1], sound=None}` | tick | The LAYS_EGGS rule: on a random timer between interval min and max, drops one weighted pick (count items) at the Minion's feet. Pauses while the Minion is sitting in a minecart or boat, and while it is a spawnling. | LAYS_EGGS |
| `interact_yield{use_item, result, cooldown, count=[1, 1], consume=replace, sound=None}` | onInteract | The owner uses use_item on the Minion: if off cooldown, gives count results (replace: ItemUtils.createFilledResult; damage: drops the result and damages the tool). Otherwise PASS. | MILKABLE, STEW, WOOLLY |
| `forager{blocks, radius, interval, action, heal=0, needs_order=work, replace_with=None}` | tick (a goal) | A low-priority goal. Paths to the nearest matching block within radius of its post, breaks one block per interval (needs mobGriefing). harvest: the block's loot goes into the pack (Work order) like the Farmer job; eat: heal and replace. Never breaks blocks in claimed areas (FakePlayer break event) or within 3 blocks of a bed, chest or the altar. | new jobs: Lumberjack (beaver), Digger (molenoid) |
| `mount{seats=1, medium=land, steering=saddle_direct, jump=0.0, speed_bonus=0.0}` | Saddleable + ItemSteerable | Makes the torso saddleable (NecroMob.saddleable). Water mounts move at full speed only in water and dismount nobody underwater (#minecraft:dismounts_underwater excluded). No true flight: a mount with gliding wings glides its rider down instead. | the existing saddleable flag |
| `glide{fall_speed, needs=2, horizontal_boost=0.0}` | tick | When the Minion holds at least 'needs' glide parts, is falling faster than fall_speed, is not in a fluid and not on a ladder: clamp deltaMovement.y to -fall_speed, add horizontal_boost along its body yaw (capped at 0.4 horizontal speed), and reset fall distance. The slowest fall_speed among the parts wins. Ridden Minions glide with their rider. | - |
| `environment{condition, damage=0, interval=0, attribute_multipliers={}}` | tick | While the condition holds: hurt(generic) by damage every interval, and/or apply ADD_MULTIPLIED_TOTAL modifiers (factor - 1) with id necromancy:env/<mob>/<part>, removed when it stops. dry = not in water, rain or a bubble column; daylight = day and sky light >= 12; sunlight = the undead sunburn test. | MELTS, the WATER_HURTS damage tick, Night Crawler 'photophobic' |
| `emits_light{level, condition=dark}` | tick | Keeps one minecraft:light block (with this level) in the air block at the Minion's eye position, moved every 10 ticks and removed when the Minion moves away, dies, unloads or sits. Never replaces anything but air; no mobGriefing check needed because it never changes a real block. | - |
| `transfuse{amount, interval, owner_threshold, self_floor}` | tick | When the owner is within 8 blocks, below owner_threshold of max health, and the Minion is above self_floor: every interval the Minion takes amount magic damage and the owner heals amount. Red heart particles flow from Minion to owner. | - |

### 3.4 New tags and data

- `necromancy:diggable` (block tag) holds the blocks the Molenoid's Digger arms may take: `dirt`, `coarse_dirt`, `rooted_dirt`, `grass_block`, `podzol`, `mycelium`, `sand`, `red_sand`, `gravel`, `clay`, `mud`, `soul_sand`, `soul_soil`, `mole_dirt`. No stone, no ores, no farmland or paths.
- `necromancy:no_soul` (entity tag) lists entities that never yield a soul: the King, Queen and Mobzilla head sidecars, and the nine objects.
- The spec's `tag_additions` block lists the OreSpawn entries for `legendary_souls`, `bloodless` and every `organ_rich/*` tag. All of these are optional datapack entries (`"required": false`), so Necromancy still loads without OreSpawn.
- **Spawnlings** (`summon` with `spawnling:<mob>`) are ordinary Minions built from that creature's own parts. They need no new entity type, renderer or AI.

---

## 4. Anatomy: which parts each creature gives

The part list follows each creature's GeckoLib model, the model players see. The rules:

| Body plan | Parts | Examples |
|---|---|---|
| Biped | H T A L | Alien, Ender Knight, Burglar, Fairy, robots, Leaf Monster |
| Quadruped | H T A L, front legs as arms (the vanilla cow precedent), or H T L when the front legs are not distinct | Dungeon Beast, Kyuubi, cows / Pointysaurus, Hydrolisc, Gazelle |
| Theropod | H T L, plus arms only where the model has real forelimbs | T-Rex, Alosaurus, Cryolophosaurus (H T L) / Nastysaurus, Baryonyx, Velocity Raptor (H T A L) |
| Snake, serpent, worm, caterpillar | **H T only** | Basilisk, Sea Viper, Sea Monster, the three Worms, Caterkiller |
| Fish and cetacean | **H T only** | Whale, Skate, Flounder, Gold Fish, Cloud Shark |
| Jelly or spiny ball | **T only** | Irukandji, Urchin |
| Insect and arachnid | H T L. Raptorial or pincer front legs count as arms (Mantis, scorpions, Cave Fisher, Creeping Horror). Otherwise the **wings are the arms**, with `glide` (Bee, butterflies, Dragonfly, Mosquito) | |
| Flier with arms and wings | arms stay arms, and the wings ride on the torso as a one-part `glide` (`needs: 1`) | Fairy, Ender Reaper, Mantis |
| Bird, bat-winged reptile, dragon | wings are the arms, with `glide` | Cockateil, Cliff Racer, Dragon, Leonopteryx, Cephadrome, Stinky, Terrible Terror |
| Wheeled or bomb robot | T L | Robo-Sniper (turret and wheels), Bomb-Omb robot (bomb and feet) |
| Giant | the parts that fit a Minion | Giant Robot: laser head and fists only |

**Rendering.** Each part lists the model bones it draws (`bones` in the spec). A plain name matches that bone and its numbered copies (`leg` matches `leg1`, `leg_2`). A name with `*` is a glob (`wing_*_l`). Every pattern is checked against the real `.geo.json`, and any bone in no list is hidden. A `GeoPartModel` bakes those bones once from OreSpawn's geo file into `ModelPart`s. This follows the vanilla rule in REVIEW 1.8: bake the definition, don't borrow a live renderer. Each part is scaled so its height matches the `shape` value, so a T-Rex head is about one block, not two. The six orchard cows and the Spider Driver use the vanilla cow and spider models with OreSpawn's textures. Rendering is the largest single cost of this design: effort L, no new art.

---

## 5. Balance

Sample Minions, computed from the stats in section 6. Speed is in blocks per tick (zombie 0.23, player walk 0.1). These figures leave out the traits.

| Build | HP | Damage | Speed | KB res | Part armour | Weight | Soul needed |
|---|---|---|---|---|---|---|---|
| All Iron Golem (vanilla ceiling) | 260 | 9 | 0.130 | 1.00 | 12 | 20 | Mighty |
| All Zombie (vanilla baseline) | 120 | 6 | 0.260 | 0.40 | 2 | 10 | Common |
| T-Rex head, torso, legs + Nastysaurus arms | 200 | 11 | 0.228 | 0.70 | 4 | 20 | Mighty |
| Hammerhead head, torso, legs + Robo-Pounder arms | 250 | 12 | 0.163 | 0.95 | 10 | 20 | Mighty |
| Leonopteryx (all four) + Basilisk head | 240 | 10 | 0.195 | 0.70 | 4 | 20 | Mighty |
| Dragon (all four parts) | 230 | 7 | 0.195 | 0.70 | 4 | 20 | Mighty |
| Emperor Scorpion (all four) | 230 | 10 | 0.195 | 0.90 | 5 | 20 | Mighty |
| Ender Reaper (all four) | 150 | 9 | 0.325 | 0.55 | 0 | 15 | Potent |
| Early game: Baby Dragon head, torso, wings + Velocity Raptor legs | 85 | 4 | 0.325 | 0.20 | 0 | 10 | Common |
| Faint-soul critter: Butterfly head, torso, wings + Ant legs | 34 | 2 | 0.228 | 0.20 | 0 | 5 | Faint |

Reading the table:

- **No OreSpawn build passes the golem on health or armour.** The heavy OreSpawn builds trade some armour for damage or utility: 7 to 12 damage, against the golem's 9. The highest is Hammerhead plus Robo-Pounder arms at 12 damage, with 250 HP and 10 armour. It is worth noting that this build still needs a Mighty soul. REVIEW's late-game Wolf/Golem/Ender build has 10 damage.
- The **early game** gains a lot from OreSpawn, and that is intended. A Common soul lifts a fire-proof, fire-spitting, gliding Baby Dragon on Raptor legs at 0.325 speed, but with only 85 HP.
- **Glass cannons are real.** The butterfly-and-ant build has 34 HP but glides from any height and climbs any wall: a scout.
- **Traits are where the risk is**, so the strongest ones carry cooldowns and caps:
  - The Basilisk gaze: Slowness III for 60 t out of every 100 t, line of sight only.
  - Spawnlings: at most 1 to 4 alive, each lasting 20 to 30 s.
  - Thorns are capped at 2 to 4 damage.
  - Lifesteal is capped at 1 to 4 HP per hit.
  - Ranged damage is 1 to 6 per bolt, against OreSpawn's own 16 to 100.

**Economy gates.** The essences make the heavy parts cost what they are worth. Weight-4 essences are boss-adjacent drops or rare items: T-Rex Tooth, Emperor Scorpion Scale, Caterkiller Jaw, Dragon's Breath, a diamond for the Leonopteryx, obsidian, a redstone block. Weight-1 essences are things you already carry. Torso and legs recipes use the essence twice.

---

## 6. Per-creature table

Columns: weight; the parts given with stats per part (and extra attributes); traits per part; the essence (one id for every part type unless the spec says otherwise); organ affinity (`necromancy:organ_rich/*`); blood (`yes`, or bloodless, meaning the Bone Needle fails); whether the torso is saddleable; and the flavour reason. Every number here is also in the JSON.

#### Dinosaurs and reptiles

| Creature | W | Parts and stats per part | Traits per part | Essence | Organ | Blood | Saddle | Why |
|---|---|---|---|---|---|---|---|---|
| **T-Rex** ★ `trex` | 4 | H (1.5,1,1,0,2) attack knockback +0.5<br>T (4,0,2,0,0) armour +4, scale +0.1<br>L (2,0,3,2.5,0) step height +0.5 | H: toss 1.2/0.1; aura weakness I r6 every 200t (enemies)<br>T: cactus_proof | `orespawn:trex_tooth` | heart | yes | - | The tyrant's skull still roars: everything near it fights a little weaker. |
| **Nastysaurus** `nastysaurus` | 4 | H (1.5,1,1,0,1.5)<br>T (4,0,2,0,0) armour +4<br>A (0.75,0,0,0,1.25)<br>L (2,0,3,2.5,0) step height +0.5 | H: toss 1.2/0.1<br>T: thorns 20% | `minecraft:flint` | heart | yes | - | A back full of spikes: whatever bites the torso bleeds for it. |
| **Alosaurus** `alosaurus` | 3 | H (1,1,0.5,0,1)<br>T (3,0,1,0,0) armour +2<br>L (1.5,0,3,3,0) step height +0.5 | H: toss 1.2/0.1 | `minecraft:bone_block` | heart | yes | - | The night hunter: a cheaper T-Rex for a Potent soul, sewn with fossil bone. |
| **Cryolophosaurus** `cryolophosaurus` | 1 | H (0.5,1,0,0,0.5)<br>T (0.75,0,0,0,0)<br>L (0.5,0,1,3.5,0) | H: pack +1/ally (max 2) | `minecraft:snowball` | - | yes | - | A frost-crested scavenger that hunts in twos and threes. |
| **Baryonyx** `baryonyx` | 3 | H (1,1,0.5,0,1)<br>T (2.5,0,1,0,0) oxygen bonus +4<br>A (0.5,0,0,0,1)<br>L (1,0,2,2.5,0) water movement efficiency +0.5 | H: bane aquatic x1.5 | `minecraft:salmon` | muscle | yes | - | The river fisher: hooked claws and a snout made for things that swim. |
| **Velocity Raptor** `velocity_raptor` | 2 | H (0.75,1.5,0,0.5,0.75)<br>T (1,0,0,0,0)<br>A (0.25,0,0,0,0.5)<br>L (0.5,0,1,4,0) jump strength +0.1, fall damage multiplier -0.5 | H: pack +1/ally (max 3)<br>L: leap 3-8 | `minecraft:sugar` | muscle | yes | - | The fastest legs in the book, and they pounce; better in a pack. |
| **Basilisk** ★ `basilisk` | 4 | H (2,1.5,1,0,2)<br>T (5,0,2,0,0) armour +4 | H: hit: poison I 200t 33%; aura slowness III r8 every 100t (enemies)<br>T: fire_proof; regen 1/75t always | `orespawn:basilisk_scale` | heart | yes | - | A serpent king: its venom bites, and its gaze nails the crowd to the floor. |
| **Kyuubi** ★ `kyuubi` | 3 | H (1,1,0,0,0.5)<br>T (2.5,0,0,0,0) burning time -0.5<br>A (0.5,0,0,0,0.75)<br>L (1,0,1,3,0) burning time -0.5 | H: ranged small_fireball every 40t, 8 blocks; hurt_by_water<br>T: fire_proof, hurt_by_water<br>A: ignite 3s; hurt_by_water<br>L: fire_proof, hurt_by_water | `minecraft:fire_charge` | lungs | yes | - | The nine-tailed Nether fox: a fire-proof body that throws fire and hates rain. |
| **Pointysaurus** `pointysaurus` | 3 | H (1,1,1,0,1) attack knockback +0.5<br>T (3,0,1.5,0,0) armour +4<br>L (1,0,3,2,0) | H: charge x1.5 | `minecraft:pointed_dripstone` | muscle | yes | - | Three horns and a plated back: it charges, and it doesn't stop to think. |
| **Camarasaurus** `camarasaurus` | 2 | H (0.5,1.5,0,0,0)<br>T (2.5,0,1,0,0)<br>L (1.5,0,2,2,0) step height +0.5, fall damage multiplier -0.3 | H: grazes leaves<br>T: mount 2 seats | `minecraft:fern` | heart | yes | yes | A gentle long-neck: two riders on its back, and it browses the hedges to heal. |
| **Hammerhead** `hammerhead` | 4 | H (2,1,1.5,0,2) attack knockback +1<br>T (5,0,2,0,0) armour +6<br>L (2.5,0,3,1.5,0) step height +0.5, armour +2 | H: charge x1.5; toss 0.4/0.85<br>T: cactus_proof | `minecraft:experience_bottle` | heart | yes | - | An armoured battering ram whose horns throw men over the treetops. |
| **Lizard** `lizard` | 2 | H (0.75,1,0,0,0.75)<br>T (1.5,0,0,0,0) burning time -0.3<br>L (0.75,0,1,3,0) | H: bane arthropod x1.5<br>T: cactus_proof<br>L: climbs | `minecraft:dead_bush` | - | yes | - | A desert climber that eats spiders, sun-warmed and cactus-proof. |

#### Beasts and livestock

| Creature | W | Parts and stats per part | Traits per part | Essence | Organ | Blood | Saddle | Why |
|---|---|---|---|---|---|---|---|---|
| **Beaver** `beaver` | 1 | H (0.5,1,0,0,0.5)<br>T (1,0,0,0,0) oxygen bonus +4<br>L (0.25,0,1,2,0) water movement efficiency +0.5 | H: harvests logs | `#minecraft:logs` | - | yes | - | The Lumberjack: give it the Work order and it fells trees into its pack. |
| **Chipmunk** `chipmunk` | 1 | H (0.25,1,0,0,0.25)<br>T (0.5,0,0,0,0)<br>L (0.25,0,0.5,3,0) scale -0.15 | T: produces wheat_seeds/pumpkin_seeds/melon_seeds 6000-12000t | `minecraft:sweet_berries` | - | yes | - | Its cheek pouches still cough up the seeds it hoarded. |
| **Gazelle** `gazelle` | 1 | H (0.5,1.5,0,0,0)<br>T (1,0,0,0,0)<br>L (0.25,0,1,4,0) jump strength +0.1 | - | `minecraft:wheat` | muscle | yes | - | Long ears and longer legs: the cheapest fast legs there are. |
| **Rat** `rat` | 1 | H (0.25,1,0,0,0.5)<br>T (0.5,0,0,0,0)<br>L (0.25,0,0.5,3,0) scale -0.15 | H: hit: hunger I 140t 25%<br>L: climbs | `orespawn:cheese` | - | yes | - | Plague teeth on a small body that climbs the walls. |
| **Easter Bunny** `easter_bunny` | 1 | H (0.25,1,0,0,0)<br>T (0.5,0,0,0,0)<br>A (0.1,0,0,0,0.25)<br>L (0.25,0,0.5,3.5,0) jump strength +0.2, fall damage multiplier -0.5 | T: produces egg/rabbit_foot/cocoa_beans 6000-12000t | `minecraft:carrot` | - | yes | - | Still lays eggs, now and then a lucky foot. Nobody asks where from. |
| **Whale** `whale` | 3 | H (1.5,1,1,0,0.5) attack knockback +0.5<br>T (4,0,1.5,0,0) oxygen bonus +10, water movement efficiency +1 | H: toss 0.8/0.3<br>T: mount 2 seats, water; dry: movement speed x0.7 | `minecraft:nautilus_shell` | lungs_plenty | yes | yes | A sea-going raft of a torso with lungs to spare; slow and sad on land. |
| **Peacock** `peacock` | 1 | H (0.5,1.5,0,0,0.25)<br>T (0.75,0,0,0,0)<br>L (0.25,0,1,3,0) | H: bane arthropod x1.5<br>T: produces peacock_feather 12000-24000t | `orespawn:peacock_feather` | - | yes | - | It moults armour-grade feathers and still pecks at termites. |
| **Ostrich** `ostrich` | 2 | H (0.5,1,0,0,0.5)<br>T (1.5,0,0.5,0,0)<br>L (1,0,2,4,0.5) jump strength +0.15, step height +0.4 | T: mount 1 seat<br>L: toss 0.6/0.2 | `minecraft:hay_block` | muscle | yes | yes | The racing saddle: a kick that clears the road, and a back made for riding. |
| **Cassowary** `cassowary` | 2 | H (0.5,1,0.5,0,0.5)<br>T (1,0,0,0,0)<br>L (0.75,0,2,3,0.75) | L: hit: slowness II 60t 30% | `minecraft:egg` | muscle | yes | - | The most dangerous bird alive: dagger-clawed legs that hobble what they kick. |
| **Dungeon Beast** `dungeon_beast` | 3 | H (1,1,1,0,1)<br>T (3,0,1,0,0) armour +2<br>A (0.75,0,0,0,1)<br>L (1,0,3,2.5,0) | H: charge x1.5<br>T: in_wall_proof, cactus_proof | `minecraft:mossy_cobblestone` | heart | yes | - | Bred in stone corridors: it walks through cave-ins that would bury anything else. |
| **Cannon Fodder** `cannon_fodder` | 1 | H (0.5,1,0,0,0.5)<br>T (1,0,0,0,0)<br>L (0.25,0,1,3,0) | H: pack +0.5/ally (max 3) | `orespawn:corn_cob` | - | yes | - | Born to fight in coloured hats; a horde of them hits harder together. |
| **Molenoid** `molenoid` | 4 | H (1.5,1.5,1,0,1)<br>T (4,0,2,0,0) armour +4<br>A (1,0,0.5,0,1.5)<br>L (2,0,3,1.5,0) | H: in_wall_proof<br>T: in_wall_proof<br>A: harvests diggable | `orespawn:molenoid_nose` | muscle | yes | - | The Digger: spade claws that clear dirt, sand and gravel under the Work order. |
| **Stinky** ★ `stinky` | 3 | H (1,1,0,0,0.75)<br>T (3,0,1,0,0)<br>A (0.5,0,0,0,0.25) gravity -0.015, safe fall distance +6<br>L (1,0,2,2.5,0) | H: produces coal 6000-12000t<br>T: when hurt: attacker nausea 100t 25%; produces bone/string/wheat/... 9000-18000t<br>A: glide 0.12 | `minecraft:charcoal` | lungs | yes | - | Burps coal from the front, leaves treasure out the back, and gasses whoever hits it. |
| **Hydrolisc** ★ `hydrolisc` | 3 | H (1,1,0,0,0.25)<br>T (3,0,1,0,0) armour +4, oxygen bonus +6<br>L (1,0,2,2.5,0) water movement efficiency +0.5 | H: transfuses 2 HP to master<br>T: regen 1/60t in water | `orespawn:blue_fish` | heart | yes | - | Bleeds itself into its master: a living blood bag stitched to your side. |
| **Gamma Metroid** `gamma_metroid` | 3 | H (1,1,0,0,1)<br>T (3,0,1,0,0) armour +3<br>L (1,0,2,2,0) | H: lifesteal 25%<br>T: last stand | `minecraft:iron_ingot` | brains | yes | - | A shelled drain-beak that drinks the life out of whatever it bites. |

#### Dragons and great fliers

| Creature | W | Parts and stats per part | Traits per part | Essence | Organ | Blood | Saddle | Why |
|---|---|---|---|---|---|---|---|---|
| **Dragon** ★ `dragon` | 4 | H (2,1.5,1,0,1.5)<br>T (5,0,2,0,0) armour +4<br>A (0.75,0,0,0,0.5) gravity -0.02, safe fall distance +10<br>L (2,0,3,2,0) burning time -0.5 | H: ranged small_fireball x3 every 100t, 16 blocks<br>T: fire_proof; mount 1 seat<br>A: glide 0.1<br>L: fire_proof | `minecraft:dragon_breath` | lungs | yes | yes | Fire-proof, saddled and fire-breathing, with wings to glide the drop; sewn with the breath that made it. |
| **Baby Dragon** `baby_dragon` | 2 | H (0.75,1,0,0,0.5)<br>T (1.5,0,0,0,0)<br>A (0.25,0,0,0,0.25) gravity -0.01, safe fall distance +4<br>L (0.75,0,1,3,0) | H: ranged small_fireball every 80t, 10 blocks<br>T: fire_proof<br>A: glide 0.16<br>L: fire_proof | `minecraft:magma_cream` | lungs | yes | - | A hatchling's spark: the early fire-proof body, sewn long before you can afford its mother. |
| **Spyro** `spyro` | 3 | H (1,1,0,0,0.5)<br>T (2.5,0,0.5,0,0)<br>A (0.5,0,0,0,0.5) gravity -0.015, safe fall distance +6<br>L (1,0,1,3,0) | H: ranged small_fireball every 60t, 12 blocks<br>T: fire_proof<br>A: glide 0.13<br>L: fire_proof | `minecraft:flint_and_steel` | lungs | yes | - | The young purple dragon: lit with flint and steel, quick to spit fire. |
| **Water Dragon** `water_dragon` | 4 | H (1.5,1,1,0,1)<br>T (4,0,1.5,0,0) armour +3, oxygen bonus +10<br>L (1.5,0,2,2,0) water movement efficiency +1 | H: ranged water_ball bolt every 60t, 14 blocks, 4 dmg; breathes_water<br>T: breathes_water; regen 1/80t in water | `orespawn:water_dragon_scale` | lungs | yes | - | A river wyrm that spits water hard enough to slow a charge, and never drowns. |
| **Leonopteryx** ★ `leon` (+leonopteryx) | 4 | H (2,1.5,1,0,2) attack knockback +0.5<br>T (5,0,2,0,0) armour +4<br>A (1,0,0,0,1) gravity -0.02, safe fall distance +12<br>L (2,0,3,2,0) step height +0.5 | H: toss 1.5/0.3<br>T: mount 1 seat<br>A: glide 0.08 | `minecraft:diamond` | lungs | yes | yes | The Leonopteryx: the best gliding wings in the mod on a saddled, man-tossing body. |
| **Cephadrome** `cephadrome` | 4 | H (2,1.5,1,0,2)<br>T (5,0,2,0,0) armour +4<br>A (1,0,0,0,0.5) gravity -0.02, safe fall distance +10<br>L (2,0,2.5,2,0) | H: bane large x1.5<br>T: mount 1 seat; cactus_proof<br>A: glide 0.1 | `minecraft:prismarine_shard` | lungs | yes | yes | A sky-shark mount bred to hunt Krakens: its bite is worst on the biggest prey. |
| **Terrible Terror** `terrible_terror` | 2 | H (0.5,1,0,0,0.75)<br>T (1,0,0,0,0)<br>A (0.25,0,0,0,0.25) gravity -0.01, safe fall distance +4<br>L (0.5,0,1,3,0) | H: ignite 2s 30%<br>T: daylight: attack damage x0.7, movement speed x0.7<br>A: glide 0.15 | `minecraft:blaze_powder` | lungs | yes | - | A night-pest dragonet with a hot bite, sluggish under the sun. |

#### Giant arthropods

| Creature | W | Parts and stats per part | Traits per part | Essence | Organ | Blood | Saddle | Why |
|---|---|---|---|---|---|---|---|---|
| **Emperor Scorpion** ★ `emperor_scorpion` | 4 | H (1.5,1,1,0,1)<br>T (5,0,2,0,0) armour +5<br>A (1,0,1,0,1.5)<br>L (2,0,3,2,0) step height +0.5 | T: hit: poison II 90t 33%; summon scorpion x2 / 600t<br>A: hit: slowness II 40t 50%<br>L: cactus_proof | `orespawn:emperor_scorpion_scale` | - | yes | - | Pincers that pin, a stinger that poisons, and a brood that crawls out to help. |
| **Scorpion** `scorpion` | 2 | H (0.5,1,0,0,0.25)<br>T (1.5,0,0.5,0,0) armour +2<br>A (0.25,0,0,0,0.5)<br>L (0.5,0,1,2,0) | T: hit: poison I 60t 25%<br>L: cactus_proof | `minecraft:sandstone` | - | yes | - | The desert's small change: a poison tail on an armoured back. |
| **Hercules Beetle** ★ `hercules_beetle` | 4 | H (1.5,1,2,0,2) attack knockback +0.5<br>T (5,0,2,0,0) armour +6<br>A (0.75,0,0.5,0,1)<br>L (2,0,3,1.5,0) burning time -0.5 | H: toss 0.45/1.25<br>T: fire_proof<br>L: fire_proof | `minecraft:obsidian` | muscle | yes | - | A horn that hurls things straight up, on a shell that shrugs off fire. |
| **Mantis** `mantis` | 3 | H (1,1.5,0,0,0.5)<br>T (3,0,0.5,0,0) gravity -0.01, safe fall distance +4<br>A (0.75,0,0,0,1.5)<br>L (1,0,2,3,0) | H: hurt_by_water<br>T: glide 0.18 (alone); hurt_by_water<br>A: lifesteal 15% | `orespawn:mantis_claw` | - | yes | - | Scythe-arms that drink a little of every cut; the thorax still carries wings. |
| **Spit Bug** `spit_bug` | 3 | H (1,1,0,0,0.5)<br>T (3,0,1,0,0) armour +2<br>A (0.5,0,0,0,0.75)<br>L (1,0,2,3,0) fall damage multiplier -1 | H: ranged acid bolt x4 every 140t, 16 blocks, 2 dmg<br>L: leap 4-10 | `orespawn:amethyst_gem` | - | yes | - | A swamp artillery bug: a spray of acid, then a pounce. |
| **Trooper Bug** `trooper_bug` | 4 | H (1.5,1,1,0,1.5)<br>T (5,0,2,0,0) armour +4<br>A (1,0,0,0,1.25)<br>L (2,0,3,2.5,0) fall damage multiplier -1 | T: summon spit_bug x1 / 1200t<br>L: leap 4-8; cactus_proof | `orespawn:jumpy_bug_scale` | muscle | yes | - | The heavy jumper: leaps the gap, lands hard, and hatches a spit bug to cover it. |
| **Bee** `bee` | 3 | H (1,1,0,0,0.75)<br>T (3,0,0.5,0,0)<br>A (0.25,0,0,0,0.25) gravity -0.02, safe fall distance +8<br>L (0.75,0,1,2.5,0) | T: hit: poison I 60t 33%; water_or_rain: 1 dmg/20t<br>A: glide 0.12 | `minecraft:honeycomb` | lungs | yes | - | A house-sized stinger: poison from the abdomen, gliding wings, and a terror of rain. |
| **Brutalfly** `brutalfly` | 3 | H (1,1,0,0,0.5)<br>T (3,0,0.5,0,0) burning time -0.5<br>A (0.25,0,0,0,0.25) gravity -0.02, safe fall distance +10 | H: ranged small_fireball every 80t, 14 blocks<br>T: fire_proof; regen 1/100t always<br>A: glide 0.1 | `orespawn:moth_scale` | lungs | yes | - | The Caterkiller's grown-up form: a fire-spitting moth that glides on great wings. |
| **Caterkiller** ★ `cater_killer` | 4 | H (2,1,1,0,2)<br>T (5,0,2,0,0) armour +4 | H: web the target 25%<br>T: thorns 15%; summon butterfly x6 on death; grazes leaves | `orespawn:caterkiller_jaw` | muscle | yes | - | Webs its prey to the floor; when it finally dies, its body breaks open into butterflies. |
| **Creeping Horror** `creeping_horror` | 2 | H (0.5,1.5,0,0,0.5)<br>T (1.5,0,0,0,0)<br>A (0.25,0,0,0,0.5)<br>L (0.5,0,1,3,0) | T: daylight: attack damage x0.7, movement speed x0.7<br>A: hit: weakness I 60t 25%<br>L: climbs | `minecraft:cobweb` | - | yes | - | A night crawler of the ceilings; it wilts when the sun comes up. |
| **Lurking Terror** `lurking_terror` | 2 | H (1,1,0,0,1)<br>T (1.5,0,0,0,0)<br>A (0.25,0,0,0,0.25) gravity -0.01, safe fall distance +4<br>L (0.5,0,1,3,0) | H: first strike x1.5<br>A: glide 0.15 | `minecraft:feather` | - | yes | - | Four jaws that open on a first strike from nowhere. |
| **Cave Fisher** ★ `cave_fisher` | 2 | H (0.5,1.5,0,0,0.5)<br>T (1.5,0,0.5,0,0) armour +2<br>A (0.25,0,0,0,0.5)<br>L (0.5,0,1,2.5,0) | T: cactus_proof<br>A: pull r8<br>L: climbs | `minecraft:fishing_rod` | - | yes | - | It casts and reels: claws that drag the target in from eight blocks away. |
| **Irukandji** `irukandji` | 2 | T (0.25,0,0,0,0) oxygen bonus +6 | T: hit: poison II 100t; hit: nausea I 160t; thorns 4; breathes_water; dry: 1 dmg/40t | `orespawn:dead_irukandji` | - | bloodless | - | A thumbnail of jelly with the worst sting in the sea; it must stay wet. |
| **Urchin** `urchin` | 2 | T (1.5,0,1,0,0) armour +2 | T: thorns 2 + ignite; fire_proof | `orespawn:sunspot_urchin` | - | bloodless | - | A burning pincushion of a torso: punch it and catch fire. |

#### Small insects and critters

| Creature | W | Parts and stats per part | Traits per part | Essence | Organ | Blood | Saddle | Why |
|---|---|---|---|---|---|---|---|---|
| **Ant** `ant` | 1 | H (0.25,1,0,0,0.25)<br>T (0.5,0,0.5,0,0) scale -0.1<br>L (0.25,0,1,2.5,0) scale -0.15 | L: climbs | `orespawn:ant_block` | - | yes | - | A black ant: the cheapest wall-climbing legs, and a tiny sturdy body. |
| **Red Ant** `red_ant` | 1 | H (0.25,1,0,0,0.25)<br>T (0.5,0,0.5,0,0) scale -0.1<br>L (0.25,0,1,2.5,0) scale -0.15 | H: ignite 1s 30%<br>L: climbs | `orespawn:red_ant_block` | - | yes | - | A fire ant whose bite lights the wound. |
| **Rainbow Ant** `rainbow_ant` | 1 | H (0.25,1,0,0,0.25)<br>T (0.5,0,0.5,0,0) scale -0.1<br>L (0.25,0,1,2.5,0) scale -0.15 | H: hit: glowing I 200t<br>L: climbs | `orespawn:rainbow_ant_block` | - | yes | - | Its bite paints the target in rainbow light for everyone to see. |
| **Unstable Ant** `unstable_ant` | 1 | H (0.25,1,0,0,0.25)<br>T (0.5,0,0.5,0,0) scale -0.1<br>L (0.25,0,1,2.5,0) scale -0.15 | T: explodes 1 on death<br>L: climbs | `orespawn:unstable_ant_block` | - | yes | - | It was always going to pop. |
| **Termite** `termite` | 1 | H (0.25,1,0,0,0.5)<br>T (0.5,0,0,0,0) scale -0.1<br>L (0.25,0,1,2.5,0) scale -0.15 | H: grazes planks<br>L: climbs | `orespawn:termite_block` | - | yes | - | Heals by chewing planks; keep it out of the house. |
| **Cricket** `cricket` | 1 | H (0.25,1,0,0,0)<br>T (0.5,0,0,0,0)<br>L (0.25,0,0.5,2.5,0) jump strength +0.25, fall damage multiplier -0.75, scale -0.1 | - | `minecraft:short_grass` | - | yes | - | Springs for legs: the best jump in the book, and barely any body. |
| **Stink Bug** ★ `stink_bug` | 1 | H (0.25,1,0,0,0.25)<br>T (0.75,0,0,0,0)<br>L (0.25,0,0.5,2,0) | T: death cloud poison r4; when hurt: attacker nausea 60t 20% | `orespawn:dead_stink_bug` | - | yes | - | Dies in a poison cloud that your undead Minions breathe like perfume. |
| **Dragonfly** `dragonfly` | 1 | H (0.25,1.5,0,0,0.5)<br>T (0.5,0,0,0,0)<br>A (0.1,0,0,0,0) gravity -0.02, safe fall distance +8 | H: bane small x1.5<br>A: glide 0.1 | `minecraft:sugar_cane` | - | yes | - | Four wings that glide far, on a body that hunts anything smaller than itself. |
| **Mosquito** `mosquito` | 1 | H (0.1,1,0,0,0.25)<br>T (0.25,0,0,0,0)<br>A (0.05,0,0,0,0) gravity -0.01, safe fall distance +4 | H: lifesteal 50%<br>A: glide 0.15 | `orespawn:mosquito_seed` | - | yes | - | A proboscis that sips back a little blood with every bite. |
| **Butterfly** `butterfly` | 1 | H (0.1,1,0,0,0)<br>T (0.25,0,0,0,0)<br>A (0.05,0,0,0,0) gravity -0.03, safe fall distance +16 | A: glide 0.06 | `orespawn:butterfly_seed` | - | yes | - | Paper-thin wings with the slowest fall in the book: pure gliding, no fight. |
| **Luna Moth** `luna_moth` | 1 | H (0.1,1,0,0,0)<br>T (0.25,0,0,0,0)<br>A (0.05,0,0,0,0) gravity -0.025, safe fall distance +12 | H: night: follow range x1.5<br>A: glide 0.08 | `orespawn:moth_seed` | - | yes | - | Pale night wings and feathered antennae that find prey in the dark. |
| **Vampire Butterfly** `vampire_butterfly` | 1 | H (0.25,1,0,0,0.25)<br>T (0.25,0,0,0,0)<br>A (0.05,0,0,0,0) gravity -0.02, safe fall distance +8 | H: lifesteal 50%; hit: weakness I 100t 50%; burns_in_sun<br>A: glide 0.1 | `minecraft:fermented_spider_eye` | - | yes | - | A blood-drinking head that weakens its prey and burns in the sun. |
| **Firefly** `firefly` | 1 | H (0.1,1,0,0,0)<br>T (0.25,0,0,0,0)<br>A (0.05,0,0,0,0) gravity -0.015, safe fall distance +6<br>L (0.1,0,0.5,2,0) | T: light 12<br>A: glide 0.12 | `orespawn:firefly_seed` | - | yes | - | A walking lantern: its torso lights the cave around it. |
| **Cliff Racer** `cliff_racer` | 1 | H (0.25,1.5,0,0,0.25)<br>T (0.5,0,0,0,0)<br>A (0.1,0,0,0,0.1) gravity -0.02, safe fall distance +8 | A: glide 0.1 | `minecraft:calcite` | lungs | yes | - | The pest of every cliff road; its wings at least carry you down from one. |
| **Cockateil** `cockateil` | 1 | H (0.25,1,0,0,0.25)<br>T (0.25,0,0,0,0)<br>A (0.1,0,0,0,0) gravity -0.02, safe fall distance +8, fall damage multiplier -0.5<br>L (0.1,0,0,1,0) | A: glide 0.1 | `minecraft:wheat_seeds` | - | yes | - | A songbird's wings: a better chicken, sewn from bird seed. |
| **Ruby Bird** `ruby_bird` | 1 | H (0.25,1,0,0,0.25)<br>T (0.25,0,0,0,0)<br>A (0.1,0,0,0,0) gravity -0.02, safe fall distance +8, fall damage multiplier -0.5<br>L (0.1,0,0,1,0) | T: produces ruby 48000-72000t<br>A: glide 0.1 | `orespawn:ruby` | - | yes | - | It still lays a ruby every few days, as it did in life. |
| **Rubber Ducky** `rubber_ducky` | 1 | H (0.25,1,0,0,0.5)<br>T (0.75,0,0,0,0) oxygen bonus +4<br>A (0.1,0,0,0,0) | T: floats; drown x0; bounce 0.8 | `minecraft:yellow_dye` | - | bloodless | - | Rubber all the way through: no blood, no drowning, and it floats. |
| **Frog** `frog` | 1 | H (0.25,1,0,0,0.25)<br>T (0.5,0,0,0,0) oxygen bonus +4<br>L (0.25,0,0.5,2,0) jump strength +0.2, fall damage multiplier -0.5, water movement efficiency +0.5 | H: bane arthropod x1.5; pull r4 | `minecraft:lily_pad` | - | yes | - | A sticky tongue that reels in bugs, on legs that swim and spring. |
| **Fairy** `fairy` | 2 | H (0.5,1.5,0,0,0.25)<br>T (1,0,0,0,0) gravity -0.02, safe fall distance +10<br>A (0.25,0,0,0,0.5)<br>L (0.25,0,0.5,3,0) | T: glide 0.1 (alone); light 8 | `minecraft:glow_berries` | - | yes | - | Pinned wings that still shed light and let the body drift down slowly. |

#### Water creatures

| Creature | W | Parts and stats per part | Traits per part | Essence | Organ | Blood | Saddle | Why |
|---|---|---|---|---|---|---|---|---|
| **Sea Monster** `sea_monster` | 3 | H (1.5,1,0.5,0,1)<br>T (3.5,0,1.5,0,0) oxygen bonus +10, water movement efficiency +0.75 | H: breathes_water<br>T: breathes_water; regen 1/60t in water; dry: movement speed x0.7 | `orespawn:sea_monster_scale` | lungs | yes | - | A finned sea serpent that heals in water and flounders on land. |
| **Sea Viper** `sea_viper` | 4 | H (2,1,1,0,2)<br>T (5,0,2,0,0) armour +3, oxygen bonus +10, water movement efficiency +1 | H: hit: poison II 100t 50%; breathes_water<br>T: breathes_water; regen 1/100t in water; dry: movement speed x0.6 | `orespawn:sea_viper_tongue` | heart | yes | - | The sea's own basilisk: fangs dripping Poison II, deadly in water and slow out of it. |
| **Attack Squid** `attack_squid` | 2 | H (0.5,1,0,0,0.5)<br>T (1.5,0,0,0,0) oxygen bonus +4<br>L (0.5,0,1,2,0.5) water movement efficiency +1, oxygen bonus +4 | H: ranged water_ball bolt every 60t, 12 blocks, 2 dmg; ranged ink_sac bolt every 180t, 12 blocks, 1 dmg<br>T: when hurt: attacker blindness 40t 25%; dry: 1 dmg/40t | `orespawn:water_ball` | - | yes | - | A squid that learned to shoot back: water balls, and now and then a blinding ink shot. |
| **Crab** `crab` | 3 | H (0.5,1,0,0,0.5)<br>T (3,0,1.5,0,0) armour +4<br>A (0.75,0,0.5,0,1.25)<br>L (1,0,3,2,0) water movement efficiency +0.5 | T: regen 1/120t in water; cactus_proof<br>A: toss 1.15/0.48 | `orespawn:raw_crab_meat` | - | yes | - | Shell plates and two claws that fling whatever they pinch. |
| **Skate** `skate` | 1 | H (0.25,1,0,0,0.5)<br>T (1,0,0,0,0) water movement efficiency +1, oxygen bonus +4 | H: breathes_water<br>T: breathes_water; dry: 1 dmg/40t | `minecraft:kelp` | - | yes | - | A ray's flat body: the best swimmer for its price, gasping out of water. |
| **Flounder** `flounder` | 1 | H (0.25,1,0,0,0)<br>T (0.75,0,0,0,0) water movement efficiency +0.5, oxygen bonus +4 | H: breathes_water<br>T: breathes_water; is_projectile x0.75; dry: 1 dmg/40t | `minecraft:cod` | - | yes | - | Flat enough that arrows slide off it, as long as it stays wet. |
| **Gold Fish** `gold_fish` | 1 | H (0.25,1,0,0,0.25)<br>T (0.5,0,0,0,0) gravity -0.03, safe fall distance +12 | T: glide 0.12 (alone) | `minecraft:raw_gold` | - | yes | - | A fish that swims in the sky; its body still won't fall properly. |
| **Cloud Shark** `cloud_shark` | 2 | H (0.75,1.5,0,0,0.75)<br>T (1.5,0,0,0,0) gravity -0.02, safe fall distance +10 | H: bane airborne x1.5<br>T: glide 0.12 (alone); daylight: attack damage x0.8 | `minecraft:paper` | - | yes | - | A shark that swims the clouds: it bites fliers out of the air. |

#### Worms

| Creature | W | Parts and stats per part | Traits per part | Essence | Organ | Blood | Saddle | Why |
|---|---|---|---|---|---|---|---|---|
| **Small Worm** `worm_small` | 1 | H (0.25,1,0,0,0.5)<br>T (0.75,0,0.5,0,0) | H: disarm feet 15%<br>T: in_wall_proof | `minecraft:rooted_dirt` | - | yes | - | It rips the boots off whatever it bites. |
| **Medium Worm** `worm_medium` | 2 | H (0.75,1,0.5,0,1)<br>T (2,0,1,0,0) armour +2 | H: disarm feet/legs 15%<br>T: in_wall_proof | `minecraft:coarse_dirt` | - | yes | - | A bigger mouth that strips greaves as well as boots. |
| **Large Worm** `worm_large` | 3 | H (1.5,1,1,0,1)<br>T (4,0,1.5,0,0) armour +4 | H: disarm head/chest/mainhand 25%<br>T: in_wall_proof; summon worm_small x2 / 600t | `orespawn:worm_tooth` | muscle | yes | - | The great worm disarms knights and births little worms as it goes. |

#### Plants

| Creature | W | Parts and stats per part | Traits per part | Essence | Organ | Blood | Saddle | Why |
|---|---|---|---|---|---|---|---|---|
| **Leaf Monster** `leaf_monster` | 1 | H (0.25,1,0,0,0.25)<br>T (1,0,0.5,0,0)<br>A (0.1,0,0,0,0.25)<br>L (0.25,0,1,2,0) | H: first strike x1.5 | `#minecraft:leaves` | - | bloodless | - | A hedge that waits: its first blow from cover lands hard. |
| **Triffid** ★ `triffid` | 3 | H (1,1.5,0,0,1)<br>T (3,0,1.5,0,0) armour +2<br>L (1,0,4,0.5,0) | H: hit: poison I 60t 30%<br>T: all damage x0.25 when idle; cactus_proof | `orespawn:green_goo` | - | bloodless | - | A shell that clams shut when idle, a snapping venomous head, and roots that will not be moved. |

#### Ender folk, aliens and people

| Creature | W | Parts and stats per part | Traits per part | Essence | Organ | Blood | Saddle | Why |
|---|---|---|---|---|---|---|---|---|
| **Alien** `alien` | 3 | H (1,1.5,0,0,0.75)<br>T (3,0,1,0,0) armour +2<br>A (0.75,0,0,0,1.25)<br>L (1,0,3,3,0) jump strength +0.1, step height +0.5 | H: hit: poison I 100t 20%<br>T: regen 1/40t always<br>L: leap 4-10 | `minecraft:clock` | brains | yes | - | Something from the WTF dungeon: a self-mending torso and legs that leap. |
| **Ender Knight** `ender_knight` | 3 | H (1,1,0.5,1,0.5)<br>T (3,0,1,0,0) armour +3<br>A (0.75,0,0,0,1.5)<br>L (1,0,3,3,0) | H: blink to target; hurt_by_water<br>T: dodge-teleport 50%; hurt_by_water<br>A: hit teleports target 15%; hurt_by_water<br>L: hurt_by_water | `minecraft:ender_eye` | brains | yes | - | An armoured enderman: it blinks in, dodges arrows, and its blade throws you somewhere else. |
| **Ender Reaper** ★ `ender_reaper` | 3 | H (1,1.5,0.5,1,0.5)<br>T (3,0,1,0,0) gravity -0.01, safe fall distance +6<br>A (0.75,0,0,0,1.5)<br>L (1,0,3,3,0) step height +0.5 | H: blink to target; hurt_by_water<br>T: glide 0.15 (alone); hurt_by_water<br>A: hit: wither I 60t 50%; reaps, hurt_by_water<br>L: hurt_by_water | `minecraft:chorus_fruit` | brains | yes | - | A reaper's arms that reap: its kills bottle souls for you, as a scythe's would. |
| **B and P** `band_p` | 2 | H (0.75,1.5,0,0,0)<br>T (2,0,0.5,0,0) armour +2<br>A (0.5,0,0,0,0.25)<br>L (1,0,2,3.5,0) | A: disarm mainhand/offhand 25% | `minecraft:emerald` | brains | yes | - | The Burglar's light fingers: they lift a mob's weapon straight into the Minion's pack. |
| **Spider Driver** `spider_driver` | 2 | H (0.5,1,0,0,0.5)<br>T (2,0,0.5,0,0) armour +2<br>L (0.5,0,1,2,0.5) | H: hit: poison I 60t 50%<br>T: mount 1 seat<br>L: climbs | `minecraft:redstone` | - | yes | yes | A spider that wears armour and is used to being in the driver's seat. |

#### Robots

| Creature | W | Parts and stats per part | Traits per part | Essence | Organ | Blood | Saddle | Why |
|---|---|---|---|---|---|---|---|---|
| **Robot 1** `robot_1` | 1 | T (0.5,0,0,0,0)<br>L (0.25,0,0.5,3,0) | T: explodes 2 on death | `minecraft:tnt` | - | bloodless | - | The wind-up Bomb-Omb: a cheap bomb torso that goes off when the Minion falls. |
| **Robot 2** `robot_2` | 4 | H (1,1,1,0,0.5) armour +2<br>T (5,0,2,0,0) armour +6, explosion knockback resistance +0.5<br>A (1,0,1,0,1.5) armour +1<br>L (3,0,3,1,0) armour +3, step height +0.5 | T: sinks<br>A: toss 0.6/0.6<br>L: sinks | `minecraft:piston` | - | bloodless | - | The Robo-Pounder: golem-grade plating and fists that slam targets into the air. |
| **Robot 3** `robot_3` | 3 | H (1,1,0.5,0,0)<br>T (3,0,1.5,0,0) armour +4<br>A (0.5,0,0,0,0.5)<br>L (1.5,0,3,2,0) armour +2 | T: sinks<br>A: ranged laser_ball bolt every 70t, 16 blocks, 4 dmg | `minecraft:dispenser` | - | bloodless | - | The Robo-Gunner: arm cannons that fire laser balls every few seconds. |
| **Robot 4** `robot_4` | 3 | H (1,1,0.5,0,0.5)<br>T (3.5,0,1.5,0,0) armour +4<br>A (0.75,0,0.5,0,1.25)<br>L (1.5,0,3,2,0) armour +2 | T: is_projectile x0.5; sinks<br>A: toss 2/0.2 | `minecraft:repeater` | - | bloodless | - | The Robo-Warrior: a shielded chest that halves arrows, and a swat that sends foes flying. |
| **Robot 5** `robot_5` | 2 | T (2,0,1,0,0) armour +2<br>L (0.5,0,2,3.5,0) jump strength -0.3, step height -0.4 | T: ranged laser_ball bolt every 60t, 30 blocks, 5 dmg | `minecraft:observer` | - | bloodless | - | The Robo-Sniper's turret and wheels: fast on the flat, can't climb a step, shoots from thirty blocks. |
| **Giant Robot** ★ `giant_robot` (+jeffery) | 4 | H (2,1.5,1,0,1) armour +2<br>A (1,0,1,0,1.5) armour +2 | H: ranged laser_ball bolt every 120t, 16 blocks, 6 dmg<br>A: toss 2.2/0.25 | `minecraft:redstone_block` | - | bloodless | - | Jeffery's laser-eye head and wrecking-ball fists, salvaged from a ten-block robot. |

#### Orchard cows

| Creature | W | Parts and stats per part | Traits per part | Essence | Organ | Blood | Saddle | Why |
|---|---|---|---|---|---|---|---|---|
| **Apple Cow** `red_cow` | 1 | H (0.5,1,0,0,0)<br>T (1,0,0,0,0)<br>A (0.25,0,0,0,0.25)<br>L (0.25,0,1,3,0) | T: bucket -> milk_bucket; produces apple 6000-12000t | `minecraft:apple` | heart | yes | yes | The Apple Cow: milk from the udder, an apple now and then from somewhere else. |
| **Orchard Cow** `apple_cow` | 1 | H (0.5,1,0,0,0)<br>T (1,0,0,0,0)<br>A (0.25,0,0,0,0.25)<br>L (0.25,0,1,3,0) | T: bucket -> milk_bucket; shears -> apple | `orespawn:apple_tree_seed` | heart | yes | yes | The Orchard Cow: shear the blossom off its back for apples. |
| **Crystal Cow** `crystal_cow` | 1 | H (0.5,1,0,0,0)<br>T (1,0,0,0,0)<br>A (0.25,0,0,0,0.25)<br>L (0.25,0,1,3,0) | T: bucket -> milk_bucket; produces crystal_apple 12000-24000t | `orespawn:crystal_apple` | heart | yes | yes | From the Crystal dimension: it grows crystal apples. |
| **Golden Apple Cow** `gold_cow` | 2 | H (0.5,1,0,0,0)<br>T (1,0,0,0,0)<br>A (0.25,0,0,0,0.25)<br>L (0.25,0,1,3,0) | T: bucket -> milk_bucket; produces golden_apple 48000-72000t | `minecraft:gold_block` | heart | yes | yes | The original Golden Apple Cow: one golden apple every two or three days. |
| **Golden Orchard Cow** `golden_apple_cow` | 2 | H (0.5,1,0,0,0)<br>T (1,0,0,0,0)<br>A (0.25,0,0,0,0.25)<br>L (0.25,0,1,3,0) | T: bucket -> milk_bucket; shears -> golden_apple | `minecraft:golden_apple` | heart | yes | yes | The Golden Orchard Cow: shear a golden apple off it once every two days. |
| **Enchanted Golden Apple Cow** `enchanted_apple_cow` | 2 | H (0.5,1,0,0,0)<br>T (1,0,0,0,0)<br>A (0.25,0,0,0,0.25)<br>L (0.25,0,1,3,0) | T: bucket -> milk_bucket; aura absorption I r8 every 6000t (owner) | `minecraft:lapis_lazuli` | heart | yes | yes | The glinting cow: every five minutes its master gains a shell of Absorption. |

---

## 7. The 15 most exciting

Ranked by how much each changes what a Minion can do:

1. **Ender Reaper arms**: kills bottle souls, as a scythe kill does. The Minion becomes a reaper, and the necromancy loop feeds itself.
2. **Leonopteryx**: the best gliding wings on a saddled, rider-tossing body. Jump off a cliff together.
3. **Dragon**: saddled, fire-proof and breathing fire, sewn with the Dragon's Breath that made it.
4. **Basilisk head**: Poison bite and a gaze that slows the crowd. The weight-4 support head.
5. **T-Rex head**: a roar that weakens everything around it, and a bite that throws.
6. **Hydrolisc head**: bleeds its own health into its master. A blood bag on legs.
7. **Giant Robot head**: an explosive laser eye that never breaks blocks.
8. **Cave Fisher arms**: reel the target in from 8 blocks. Pairs with any melee body.
9. **Emperor Scorpion**: pincers that pin, a poison stinger, and a brood of spawnling scorpions.
10. **Stink Bug torso**: dies in a poison cloud that undead Minions don't even notice. Cheap, weight 1, nasty.
11. **Stinky**: burps coal, drops treasure out the back, and gasses whoever hits it.
12. **Triffid**: a shell that takes a quarter damage while idle, rooted legs and a venom snap. The garden guard.
13. **Hercules Beetle head**: hurls targets straight up, fall damage included.
14. **Kyuubi**: a fire-proof fox that throws fireballs and burns what it claws, but hates the rain.
15. **Caterkiller**: webs its prey to the floor, then bursts into butterflies when it dies.

---

## 8. Integration notes

- **Ids and loading.** Each NecroMob id is `necromancy:orespawn/<mob>`, and each part item is `necromancy:orespawn_<mob>_<part>`. Both are registered only when `ModList.get().isLoaded("orespawn")`. Recipes, loot and tags use NeoForge `mod_loaded` conditions. OreSpawn stays a soft dependency, and nothing compiles against its classes: the projectiles are Necromancy's own `part_bolt`, rendered with OreSpawn's items.
- **Sewing recipes** use the four fixed shapes. No two parts share both a part type and an essence (checked), across both the OreSpawn and the vanilla tables. Tag essences (`#minecraft:logs` for the beaver, `#minecraft:leaves` for the leaf monster) overlap no item essence.
- **Sounds.** Every OreSpawn id exists in `sounds.json`. Some of them (the ids listed under `sounds_unregistered`, such as `robot_hurt` and `fart`) are pooled events that OreSpawn plays by id but never put in its `SOUND_EVENT` registry. Resolve those with `SoundEvent.createVariableRangeEvent(id)` instead of a registry lookup. Two OreSpawn ids are broken in the port itself: the Frog's ambient `orespawn:frog` and the Fairy's hurt `orespawn:rat_hit` match no `sounds.json` entry. The spec uses `frog1` and `rathit`.
- **Kindred bonus** works unchanged. A T-Rex soul (Potent under the 0.5 rule) lifts T-Rex head, torso and legs (12) plus two weight-2 arms, and shortens the roar's cooldown by 25%.
- **Jobs.** `forager` with `needs_order: work` hooks into the existing Work order and pack (REVIEW #10). This adds a Lumberjack (Beaver head) and a Digger (Molenoid arms) next to Farmer, Collector and Reaper.
- **Part icons** use the existing BEWLR part renderer (REVIEW #4 note), so OreSpawn parts need no hand-drawn icons.
- **Tests.**
  - One GameTest per trait type, covering its hooks and its friendly filter.
  - One spec-load test: every creature registers, and every recipe resolves with OreSpawn present and absent.
  - A data test that repeats the checks behind this document: bands, ids, bones and essence uniqueness.

---

## 9. Open questions

- **The soul-health rule** (`oreSpawnHealthScale = 0.5`) is the one change to Necromancy's own rules. Without it, the Whale, Hydrolisc, Stinky and Gamma Metroid (100 HP, two of them passive) give Mighty souls. Owner ruling needed. The alternative is to leave potency alone and accept that OreSpawn worlds give out Mighty souls easily.
- **Companions** (Girlfriend, Boyfriend) are excluded on tone (section 2). They are easy to add as weight-2 bipeds if wanted.
- **Mothra and the Nightmare** are treated as bosses because they have boss bars. Both also spawn naturally. If they become part-givers, they would be weight-4 wings and head respectively.
- **True flight** is still left out (REVIEW #4). The Dragon, Leonopteryx and Cephadrome are the obvious place to break that rule later. Their `mount` trait already has the field (`steering`), and gliding is the stopgap.
- **Rendering scale.** `shape` gives the hitbox, but each geo part also needs a render scale picked by eye in the hidden client, because OreSpawn's models range from 0.1 to 25 blocks tall. That is a tuning pass that has not been done.
- **Spawnling load.** A Minion army with Emperor Scorpion torsos can field 4 spawnlings each. The shrine cap (8 Minions) bounds this at 32, but it should be measured on the hub before it ships.
