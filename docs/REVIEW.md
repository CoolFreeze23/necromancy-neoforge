# Necromancy 1.7.1 → NeoForge 1.21.1: design review

Reviewed 2026-11-08 against `D:\Ports\necromancy-src` (110 Java files, lang, changelog) and vanilla 1.21.1 (`D:\Ports\mc1211-res`, `D:\Ports\mc1211-src`).
This is a design document. Every number is a proposal for tuning, not a measured value. Class names refer to the 1.7.10 source.

**Conventions**

- **Part stats** use the original `NecroEntityBase.addAttributeMods` format: `(health, followRange, knockbackResist, movementSpeed, attackDamage)`. Each value multiplies the base (20 HP, 16 follow, 0.1 KB, 0.1 speed, 2 damage). The game applies it as `ADD_MULTIPLIED_BASE` (old operation 1), and the values from all five slots are summed.
- **Extra attributes** are 1.21 attributes such as `generic.armor`, `generic.scale` and `generic.step_height`. Each one is a flat `ADD_VALUE` unless the text says otherwise.
- **Ticks:** t = ticks (20 t = 1 s).
- **Effort:** S = up to a day. M = 2 to 4 days. L = a week or more.
- **Art:** "none" means only vanilla textures are reused.

---

## 0. TL;DR

Today the mod is one loop: grind organs → sew → summon → a pet follows you. That loop has no middle and no end. The eight additions that make it feel complete are marked ★ in section 2:

1. ★ **Part Traits and Balance Pass.** Each part *does* something and has a trade-off.
2. ★ **Orders and Stances** (Bone Scepter). Minions guard, patrol and fight hostile mobs.
3. ★ **Minion Screen and Equipment.** You can see a minion's stats, and give it a weapon, a bow, a trident and a helmet.
4. ★ **Modern Parts, wave 1.** 10 post-1.7 mobs. Their textures are vanilla, so no entity art is needed.
5. ★ **Soul Potency.** The soul you reap decides how strong a body it can animate.
6. ★ **Shrine Tiers.** Candles, soul lights and skulls around the altar raise the minion cap, the same way bookshelves raise an enchanting table.
7. ★ **Remains and Phylactery.** A dead minion is no longer 100 kills of organs gone forever.
8. ★ **Harvest Economy.** Organs drop per mob, the scythe gets a Reaping enchantment, and cemeteries become grave-robbing sites.

Leave out the Scent Burner, Thaumcraft hooks, the specialFolk web fetch, boss-mob parts, flight, mining/building jobs and minion upkeep (section 4).

---

## 1. Critique of the original

### 1.1 The loop as shipped

```
kill anything ──1-4% RNG──> organs ─┐
leather ──1:8──> skin ──────────────┼─> Sewing Machine (4x4) ─> body part items ─┐
bone meal ─> Bone Needle (consumed per craft)                                    │
Bone Needle + bottle on any mob ─> Jar of Blood ────────────────────────────────┼─> Altar ─> Minion (follows/sits)
scythe kill + bottle ─> Soul in a Jar (generic) ────────────────────────────────┘
```

Nothing comes after the arrow on the right. A minion follows, sits, fights whatever you hit, and can be ridden if it has a spider torso. It has no job, never improves, gets no upgrades, and there is no reason to build a second, different one. After the first summon the mod has no goals left.

### 1.2 Economy

- **Organs are flat RNG, unrelated to the mob.** `ForgeEventHandler.LivingDropsEvent` fires on every `EntityLiving` death, including chickens, bats and your own minions. One exclusive `nextInt(100)` roll gives Brains 1%, Heart 1%, Muscle 4% and Lungs 1%. Looting and the kill source are ignored, and skin never drops. A full five-part minion needs 1 Brains, 1 Heart, 1 Lungs, 4 Muscle, 38 Skin (5 leather), 12 bones, 2 spider eyes and 7 essence items. That is roughly 100 kills of pure luck per minion, whether you killed chickens or Endermen.
- **Each mob's identity comes only from its "essence" item**, and most essences are trivial: rotten flesh, bone, string, beef, a book. Iron Golem needs 6 iron blocks, which is nothing next to an iron farm. Wolf Head needs a name tag.
- **Souls are generic.** `ItemScythe.hitEntity` gives the same "Soul in a Jar" for a chicken and for an Enderman, so there is no reason to fight anything dangerous. (It also sets `target.motionY = 10000` and throws the corpse into orbit.)
- **Blood is free.** Hitting any mob with a needle and a bottle gives blood, skeletons and iron golems included. The bucket ↔ 8 jars ratio is very generous. The blood fluid itself does nothing.
- **Cemeteries hand out free minions.** `ComponentVillageCemetery` puts 2 guaranteed chests in the house plus a 10% chest in each of 8 graves. Every chest holds 5 random body parts, so each cemetery gives 10 to 14 parts. These can include Iron Golem or Enderman parts, which skips the whole organ grind.

### 1.3 Stats and balance

The modifiers are summed with no trade-offs, so one build wins:

| Build | HP | Dmg | Speed | KB res | Cost |
|---|---|---|---|---|---|
| All Zombie (vanilla skull head) | 120 | 6 | 0.40 | 0.40 | rotten flesh |
| All Enderman | 180 | 9 | 0.50 | 0.70 | 7 ender pearls |
| All Iron Golem | 260 | 9 | 0.20 | 1.00 | pumpkin + 6 iron blocks |
| **Wolf head + IG torso + IG arms + Enderman legs** | **220** | **12** | **0.50** | **1.00** | 6 iron blocks, 2 pearls, 1 name tag |
| All Chicken | 41 | 2.5 | 0.15 | 0.10 | raw chicken |

For scale: a vanilla Iron Golem has 100 HP, Zombie speed is 0.23, Wolf 0.3 and Ravager 0.3. The +3 speed on humanoid and quadruped legs makes every minion faster than any vanilla land mob. **Wolf Head `(2,1,1,1,2)` beats every other head on every stat.** Health has no cap, so the build above has more than double a golem's HP and nothing gates it.

### 1.4 Minions don't do anything

- **No part has an on-hit ability.** `NecroEntityBase.attackEntityAsMob` is a hook that none of the 17 mobs override. The creeper torso doesn't explode, the witch doesn't throw potions, spider legs don't climb, and the enderman doesn't teleport. The one ability in the mod (Isaac's tears) is hardcoded in `EntityMinion.attackEntityWithRangedAttack` by comparing the head's name string to `"Isaac"`.
- **Targeting:** "aggressive" mode only attacks *players*. Every tick, `onUpdate` scans all players within 10 blocks and reads the owner's NBT for each of them. Minions never fight hostile mobs on their own.
- **No orders beyond sit/follow**, no equipment and no inventory. `printMinionInfo` prints the stats to `System.out`, so the player never sees them.
- **Mounting is half-built.** Only `NecroEntitySpider` implements `ISaddleAble`, but `cowsaddle.png` and `squidsaddle.png` ship unused. Brain on a Stick steering **never worked**: `canBeSteered` compares `ItemStack`s with `==` against a freshly built stack, so it is always false.

### 1.5 Progression has no middle or end

The full tech tree is the Necronomicon (one nether wart is the only gate), the Sewing Machine (iron), the Blood Scythe (obsidian, iron hoe, jar) and the Bone Scythe (+1 diamond, then 8 damage, which is netherite-sword level). Everything after that is the same activity again.

- Nothing ties into the Nether, the End, trial chambers, raids or ancient cities.
- The Nether Chalice is a decorative blood pool with no loot.
- The mod's own mobs pay nothing back into the loop. Night Crawler drops rotten flesh and an `ender_pearl` with damage value 1. The Isaac chain (20 → 75 → 40 + 20 HP) drops only Isaac's Head: 0 armour, unlimited durability, and free tears at 3 per second with a client-side cooldown only.

### 1.6 Bugs in 1.7.1 that should *not* be ported faithfully

| Where | Bug | Port fix |
|---|---|---|
| `TileEntityAltar.canSpawn/soulCheck` | Checks `getItem()` only. All four `ItemGeneric` subtypes share one item, so a Bone Needle passes as blood and a Brain on a Stick passes as a soul | Goes away with separate items |
| `TileEntityAltar.spawn` | Never calls `isLegalCombo`. `getBodyPart` returns whichever slot type the item matches first, so a Legs item in the Head slot becomes leg geometry at the head. The preview hides this but the summon builds it | Validate slots at summon time, using the same code as the preview |
| `EntityMinion.onUpdate` | `head == null → setDead()`. A headless summon eats the items and then vanishes | Refuse to summon without a head, or allow headless minions |
| `EntityMinion.onDeath` | Doesn't call `super`. Calls `getOwner()`, which is null when the owner is offline: NPE | Count bound minions in owner SavedData by UUID |
| Minion counter | Kept in `player.getEntityData()`, which is not persisted, so it resets when the player respawns. `maxSpawn` comes only from `server.properties`, so single player has no limit | Player attachment plus a config value (see #6) |
| `EntityMinion.canBeSteered` | `==` comparison of stacks, always false | Use vanilla `ItemSteerable` (#9) |
| `EntityMinion.interact` (riding) | `highestPoint = cube.posY1` gives the wrong rider height | Compute the height from leg and torso parts |
| Sounds | `"mob." + headName + ".say"`, for example `mob.Pig Zombie.say`, which never resolved even in 1.7 | Map head mob → `SoundEvent` |
| `EntityAIScareEntities` | The `targets` list is never cleared and grows every tick (leak). `EntityTeddy.interact` hands ownership to *any* player who right-clicks | Clear it per run. Only claim ownership when the Teddy has no owner |
| `WorldGenerator` | Uses `random.nextInt(16) * chunkX` instead of `chunkX*16 + rand`, so it generates far outside the chunk being decorated | Already becoming a proper structure |
| `VillageCreationHandler` trades | `new Random().nextInt(3)` emeralds can be 0. `nextInt(size-1)` never sells the last part (Wolf Head) | New trade table (#18) |
| Isaac's Head tears | 333 ms cooldown enforced only on the client. The server trusts the packet | Cooldown on the server (#13) |
| Minion hitbox | Fixed at 0.6×1.8 whatever the legs are | Derive it from the parts, combined with `generic.scale` |

### 1.7 Abandoned content in the source (it shows the intended scope)

The original author was clearly building a bigger mod:

- **Scent Burner:** lang, `ModelScentBurner` and GLSL shaders. Changelog 1.6.0 says "removed some broken and/or unfinished things such as scents".
- **Apprentice armour:** four fields declared but never registered.
- **Skull Wall:** block, item, `TileEntitySkullWall` and craft-event NBT code (`Base`, `Skull1-3`), but no recipe.
- **Mooshroom:** parts present in `en_US.lang` but no `NecroEntity`.
- **"Small Slime":** commented out in `ItemBodyPart`.
- **Saddle textures** for cow and squid: present but unused.
- **Thaumcraft aspects:** commented out.

Several items below finish these.

### 1.8 Port notes found while reading

- **Modifier ids must be unique in 1.21.** `AttributeInstance.addModifier` throws `"Modifier is already applied on this attribute!"`. Cow, Pig, Sheep and Villager register their *right* arm as `"ArmL"`. Use ids like `necromancy:part/<slot>/<mob>/<stat>`.
- **Vanilla textures changed layout** since 1.7. Zombie, husk, drowned and villager are 64×64 with outer layers. The iron golem is 128×128 and the witch 64×128. Zombified Piglin is now the *piglin* model (64×64, with ears), not the old biped pigman. Re-check every UV.
- **New parts:** lift geometry by baking the vanilla `XModel.createBodyLayer()` `LayerDefinition`s directly and calling `getChild("head")` and so on. Don't pull them from `EntityModelSet`, because entity-model resource-pack mods swap those layers.
- **Minion kills already count as player kills.** 1.21 `LivingEntity.hurt` sets `lastHurtByPlayer` to the owner when a tame `TamableAnimal` deals damage. So minion kills satisfy `killed_by_player` loot conditions. #8 and #10 rely on this.

---

## 2. Ranked additions

★ = essential for "feels complete" (section 3).

### #1 ★ Part Traits and Balance Pass. Effort M. Art: none

**What.** Each part gets one signature trait. Some get a real downside. The stat formula is tuned so that no single build wins on every stat.

**Why.** Combining parts is the whole fantasy of the mod, and it means nothing while the parts are reskins of a stat sum.

**Mechanics: global rules.**

- **Base movement speed 0.1 → 0.065.** This one number brings speeds close to vanilla without touching any part's numbers:
  - Zombie legs: 0.26
  - Enderman head + legs: 0.325
  - Spider: 0.195
  - Iron Golem: 0.13 (heavy)
  - Cap: 0.35
- **Knockback resistance** is capped at 1.0, the vanilla clamp. Health, follow range and damage stay summed as in the original; that is the mod's identity. Soul Potency (#5) gates the heavy builds.
- **Wolf Head** goes from `(2,1,1,1,2)` to `(1,1,0.5,1,1)` and gains **Pack Hunter**: +1 damage for each other wolf-headed minion of the same owner within 8 blocks, up to +3. This matches how wolves play.
- **New armour channel** (`generic.armor`):
  - Zombie torso: +2 (vanilla zombies have 2 armour)
  - Sheep torso: +2
  - Iron Golem: torso +6, legs +4, each arm +1
  - Armour total capped at 20 before worn armour
- **Undead entity tags.** Put `necromancy:minion` in `#minecraft:undead`, `#sensitive_to_smite`, `#inverted_healing_and_harm` and `#ignores_poison_and_regen`. Then Smite hurts minions, Harming potions heal them, and Regeneration does nothing. **Not** in `#wither_friends`: the Wither would ignore minions, and minions would farm it.
- **Sunlight.** Minions with a Zombie, Skeleton, Drowned, Bogged or Phantom part burn in daylight unless wearing a helmet, the vanilla undead rule. This gives #3 an immediate purpose.
- **Stats live in JSON** under `data/<ns>/necromancy/body_part/<mob>.json`: stats, weight, essence and trait ids, loaded by a reload listener. Models stay in code. Balance changes then need no rebuild, and packs can tune them. Keep a `balance.classicStats` config that restores the 1.7.1 numbers exactly.
- **Trait hooks** on the part class: `onAttack(minion, target)`, `onHurt(minion, source)`, `tick(minion)` with a per-part cooldown, `onDeath(minion)`, and `extraAttributes()`.

After the pass, the old best build (Wolf + IG torso/arms + Ender legs) has 200 HP, 10 damage, 0.325 speed, 0.95 KB and 8 armour, at weight 17. That needs a Mighty soul (#5), so it is a late-game reward.

**Traits for the 17 originals.** The stat columns stay as in the source unless a change is noted.

| Mob | Signature trait | Downside |
|---|---|---|
| Cave Spider | Head: hits apply Poison I for 140 t. Legs: climb walls, `generic.scale` −0.15 | — |
| Chicken | Torso: lays an egg every 6000–12000 t (vanilla timing). Arms (wings): `generic.gravity` −0.02 each, `fall_damage_multiplier` −0.5 each (both wings: no fall damage) | Weak |
| Cow | Torso: milk with a bucket (cooldown 1200 t). Saddleable (#9, uses the shipped `cowsaddle.png`) | — |
| Creeper | Torso: explodes on death with power 2.0. Breaks blocks only if `mobGriefing` **and** config `creeperTorso.breaksBlocks` (default false). A charged minion (struck by lightning) has power 4 and uses vanilla `creeper_armor.png`. Legs: `explosion_knockback_resistance` +0.5 | Avoids cats and ocelots within 6 blocks (vanilla creeper rule) |
| Enderman | Head: teleports next to its target when the target is more than 6 blocks away, cooldown 100 t | Any Enderman part takes 1 damage/s in water or rain |
| Iron Golem | Arms: hits throw the target upward (+0.4 Y, the vanilla golem toss). Armour as above | Weight 4. Sinks in water (no float goal) |
| Isaac | Head: tears (existing): 3 damage every 60 t, 20% chance of a blood tear for 6 | — |
| Pig | Torso: saddleable, steered with **Brain on a Stick**. It's the item's obvious purpose, a carrot-on-a-stick parody | — |
| Pig Zombie (Zombified Piglin) | Torso and legs: `burning_time` −0.5 each. Torso + legs together: fire immune. Arms: +2 damage holding a golden sword or axe | — |
| Sheep | Torso: armour +2. Shear for 1–3 wool, regrows after 2400 t | — |
| Skeleton | Arms: shoots a held bow every 20–40 t (vanilla skeleton). Other arms use a bow every 60 t | Head burns in sun |
| Spider | Legs: climb walls, also while ridden. Torso: saddleable (existing). Head: +8 follow range when light < 7 | — |
| Squid | Legs: `water_movement_efficiency` +1.0, `oxygen_bonus` +4. Torso: 25% chance when hurt to give the attacker Blindness for 40 t. Saddleable in water (shipped `squidsaddle.png`) | Slow on land |
| Villager | Head: required for Jobs (#10). It is the only "thinking" head | No combat bonus |
| Witch | Arms: every 120 t throws a splash potion: Slowness if the target is more than 8 blocks away, otherwise 50/50 Weakness or Poison. Never Harming, because that would heal undead. Head: below 50% HP, drinks Harming and heals (inverted). Cooldown 600 t | — |
| Wolf | Head: nerf plus Pack Hunter (above) | — |
| Zombie | Head: 15% chance on hit of Hunger for 140 t. Torso: armour +2 | Head burns in sun |

**Vanilla systems used:** attributes (`armor`, `scale`, `gravity`, `fall_damage_multiplier`, `burning_time`, `explosion_knockback_resistance`, `water_movement_efficiency`, `oxygen_bonus`), entity type tags, `MobEffectInstance`, `Explosion` with `mobGriefing`, wall climbing (the Spider's `onClimbable` pattern), and a datapack reload listener.

---

### #2 ★ Orders and Stances (Bone Scepter). Effort M. Art: 1 item icon

**What.** A command item and a small set of orders, which turn minions from pets into an army you can use.

**Why.** There is currently nothing to *do* with minions. Guarding a base or farm and fighting hostile mobs is the minimum people expect from a 2026 summoner mod.

**Mechanics.**

- **Bone Scepter.** Shaped recipe `"  E" / " B " / "B  "`, with E = spider eye and B = bone. It is cheap on purpose.
  - **Use on your own minion:** select it (soul particles over its head).
  - **Sneak-use on your own minion:** select all your minions within 24 blocks.
  - **Use on air:** cycle the order (shown in the action bar).
  - **Use on a block:** apply the order at that position.
- **Orders:**
  - **Follow.** The default: vanilla `FollowOwnerGoal`, teleporting when more than 12 blocks away.
  - **Stay.** The existing sit.
  - **Guard.** Uses `PathfinderMob.restrictTo(pos, 12)`. Attacks hostiles within 16 blocks and returns to the post.
  - **Patrol.** Up to 4 waypoints, set by sneak-using blocks. They are stored in a data component on the scepter and copied to the minions when the order is given.
  - **Work.** Runs the minion's job (#10).
- **Stances** (per minion, set by sneak-using air or from the screen in #3):
  - **Passive:** never attacks; flees when hurt.
  - **Defensive** (default): the current behaviour, attacking what attacks the owner and what the owner attacks.
  - **Aggressive:** `NearestAttackableTargetGoal<Mob>` against `Enemy` mobs within follow range, plus players on the enemy list.
- **Ignore list:** creepers (unless the minion is ranged or has a Creeper torso), endermen and neutral piglins are ignored unless they provoke the minion. This lives in an entity tag, `necromancy:minion_ignores`.
- **Faithful parts kept:** `/minion set aggressive|passive` becomes the owner's *default* stance for new minions. `friend`/`enemy` lists stay as they are.
- **New commands:** `/minion list` (name, parts, dimension, position) and `/minion release <name>` (frees the minion's binding slot; it dies and drops one random part).

**Vanilla systems used:** goals (`FollowOwnerGoal`, `SitWhenOrderedToGoal`, `MoveTowardsRestrictionGoal`, `NearestAttackableTargetGoal`, `OwnerHurtByTargetGoal`, `OwnerHurtTargetGoal`), `restrictTo`, data components, entity tags, Brigadier.

---

### #3 ★ Minion Screen and Equipment. Effort M–L. Art: 1 GUI texture (vanilla inventory style)

**What.** Sneak + empty hand on your own minion opens a screen. Plain right-click stays as sit, as in the original.

- **Readout:** the 5 parts, the soul (type and potency), HP x/y, damage, armour, speed in blocks/s, follow range, rank and kills (#12), and the current order and stance.
- **Slots:**
  - Main hand and offhand: need at least one arm.
  - Head: needs a biped head.
  - Chest, legs and feet: need a biped torso and legs.
  - Saddle: only if the torso is saddleable.
  - A 9-slot pack: Jobs only (#10).

**Why.** The original shows players nothing (`printMinionInfo` goes to `System.out`). Equipment gives every vanilla weapon and enchantment meaning for minions, at almost no design cost.

**Mechanics.**

- **Attack through vanilla.** Run the attack through `Mob.doHurtTarget` (`super`) and *then* the part hooks. That way 1.21's data-driven enchantments (Sharpness, Smite, Fire Aspect, Knockback, Breach) and weapon attribute modifiers work for free.
- **Ranged items:**
  - **Bow:** any arms can use it; Skeleton-family arms shoot twice as fast (#1).
  - **Crossbow:** any arms.
  - **Trident:** Drowned arms throw it. Any other arms use it as a melee weapon.
- **No durability loss and a guaranteed drop**, following the vanilla convention for mob-held items.
- **Helmets** block sunburn, and **Isaac's Head worn by a minion gives it tears** (synergy with #13).
- **Rendering:** held items plus a helmet on biped heads first. Chest, leg and foot armour can render later; their attributes apply immediately either way.

**Vanilla systems used:** `EquipmentSlot`, `Mob` equipment and drop chances, `RangedBowAttackGoal`, `RangedCrossbowAttackGoal`, a trident throw like `Drowned.performRangedAttack`, `MenuType` with entity id, `ItemInHandLayer`-style rendering.

---

### #4 ★ Modern Parts, wave 1. Effort M (L with all icons by hand). Art: none for entities (vanilla textures). Part icons: see the note below

**What.** Ten post-1.7 mobs whose parts reuse vanilla textures and vanilla model geometry.

**Why.** A 1.21.1 port without husks, drowned, bogged, blazes and wither skeletons in the body-part system feels like a museum piece. Each one below also fills a gap: a sun-proof head, water legs, poison arrows, wither hits, fire, bounce, armour, early ranged support and a ram attack.

**Icon cost note.** Don't draw roughly 40 part icons. Render body-part items with a `BlockEntityWithoutLevelRenderer` that draws the actual part `ModelPart`s, the way vanilla renders skulls in the inventory. That is one M-sized job, and after it every future mob costs zero icons.

Stats use the original format. *Weight* is used by #5.

| Mob | Parts | Essence (shared 4x4 shapes) | Stats per part | Trait | Downside | Weight |
|---|---|---|---|---|---|---|
| **Husk** | H T A L | sand | Same as Zombie: H (1,1,0,0,1), T (2,0,0,0,0), A (0.5,0,0,0,0.5), L (1,0,3,3,0) | Head: hits give Hunger for 140 t. **Immune to sunlight** (the early sun-proof head) | — | 2 |
| **Drowned** | H T A L | copper_ingot | H (1,1,0,0,0.75), T (2,0,0,0,0), A (0.5,0,0,0,0.5), L (1,0,2,2,0) | Torso: `oxygen_bonus` +4. Legs: `water_movement_efficiency` +1.0. Arms: throw a held trident every 40 t (a no-pickup copy, so nothing is lost) | Head burns in sun | 2 |
| **Bogged** | H T A L | brown_mushroom | H (0.75,1,0,0,0), T (1.25,0,0,0,0), A (0.4,0,0,0,0.5), L (0.75,0,3,3,0) | Arms + bow: arrows give Poison for 80 t, firing every 50 t (vanilla Bogged) | Head burns in sun | 2 |
| **Wither Skeleton** | H (vanilla `wither_skeleton_skull` as the head item, like the original skull heads) T A L | coal | H (1,1,0,0,1), T (2,0,1,0,0), A (0.5,0,0,0,1), L (1,0,3,3,0) | Arms: hits give Wither I for 200 t. Torso and legs: `scale` +0.1 each. Head + torso + legs: fire immune | The head costs a skull you also want for beacons and the Wither: a real choice | 3 |
| **Blaze** | H T L (rods as legs) | blaze_rod | H (0.5,1,0,0,0.5), T (1,0,0,0,0), L (0.5,0,0,1,0.5) | Head: volley of 3 small fireballs every 100 t at targets 4–16 blocks away. Torso: fire immune. Legs: `gravity` −0.04 (floaty), `safe_fall_distance` +100 | Any Blaze part: 1 damage/s in water or rain | 3 |
| **Slime** (finishes the commented-out "Small Slime") | H T L | slime_ball | H (0.5,1,0,0,0.25), T (1.5,0,0,0,0), L (0.5,0,1,1,0) | Torso: 30% chance to knock an attacker back by 1.0. Legs: `fall_damage_multiplier` −1.0, `jump_strength` +0.15; landing from 4+ blocks deals 4 damage within 2 blocks | Low damage | 1 |
| **Mooshroom** (finishes the lang entry) | H T A L (cow geometry) | red_mushroom | Same as Cow | Torso: bowl → mushroom stew, cooldown 6000 t. Renders the back mushrooms (vanilla `MushroomCowMushroomLayer`) | — | 1 |
| **Snow Golem** | H T A (sticks) L | H carved_pumpkin, T snow_block, A stick, L snow_block (4-item form) | H (0.25,1,0,0,0), T (0.5,0,0,0,0), A (0.1,0,0,0,0), L (0.25,0,0,1,0) | Head: throws a snowball every 20 t at targets within 10 blocks (0 damage, 3 to blazes, knockback). An early aggro-puller. Legs: snow trail (`mobGriefing`) | Torso and legs: 1 damage/s in hot biomes (temperature > 1.0) or when wet | 1 |
| **Armadillo** | H T L | armadillo_scute | H (0.25,1,0,0,0), T (1,0,1,0,0) + armour 4 + toughness 1, L (0.25,0,0.5,1,0) | Torso: below 30% HP, rolls up for 100 t (half damage, no moving or attacking). Once per 1200 t | Can't attack while rolled up | 2 |
| **Goat** (head only, like Wolf and Isaac) | H | goat_horn | H (1,1,0.5,0.5,1) + `attack_knockback` +1.0 | **Ram:** after moving 4+ blocks toward the target within 40 t, the next hit does ×1.5 damage and 2.5 knockback. Cooldown 100 t | — | 2 |

Models come from: `ZombieModel`, `DrownedModel`, `SkeletonModel`, `BlazeModel`, `SlimeModel` (outer layer translucent), `CowModel`, `SnowGolemModel`, `ArmadilloModel`, `GoatModel`. **Vanilla systems used:** loot-dropped essences, attributes, `SmallFireball`, `Snowball`, `ThrownTrident`, `MobEffects`.

---

### #5 ★ Soul Potency. Effort M. Art: none (tooltip only)

**What.** A Soul in a Jar remembers what it was reaped from, and how strong that mob was. A soul can only animate a body up to a matching total *weight*.

**Why.** This is the progression spine. Building an Iron Golem or Ravager body means reaping something strong. It gives the scythe a reason to be used on dangerous mobs, and it gates the heavy builds that #1 leaves strong. The default early game still works, because a zombie soul animates a zombie body.

**Mechanics.**

- **Data component** `necromancy:soul` = `{entity: <type id>, potency}`. Tooltip example: "Soul of an Enderman (Potent)". Old generic souls and creative souls count as Common.
- **Potency comes from the victim's max HP at death:**

| Potency | Victim max HP | Weight cap | Examples |
|---|---|---|---|
| Faint | < 10 | 6 | chicken, rabbit, sheep (8), silverfish |
| Lesser | 10–19 | 9 | pig, cow, spider (16), cave spider, bogged |
| Common | 20–39 | 12 | zombie, skeleton, creeper, villager, witch, blaze, wither skeleton, breeze |
| Potent | 40–99 | 16 | enderman, hoglin, piglin brute, Blood Isaac (75) |
| Mighty | ≥ 100 | 20 | iron golem, ravager |
| Legendary | `#c:bosses`, warden, elder guardian | 20, plus the minion gets +20% max health and **Undying**: the first lethal hit every 12000 t leaves it at 1 HP | wither, dragon, warden |

- **Ominous** trial-spawner mobs and raid captains count as one tier higher. This ties the scythe to trial chambers and raids.
- **Part weights:**
  - 1: Chicken, Pig, Sheep, Cow, Mooshroom, Squid, Glow Squid, Villager, Snow Golem, Cave Spider, Slime
  - 2: Zombie, Husk, Drowned, Skeleton, Bogged, Spider, Creeper, Witch, Pig Zombie, Armadillo, Strider, Goat/Wolf heads, Phantom wings
  - 3: Enderman, Blaze, Wither Skeleton, Hoglin, Camel, Breeze, Piglin Brute, Night Crawler, Isaac/Evoker heads
  - 4: Iron Golem, Ravager, Warden head

  Examples:
  - All Zombie = 10 (needs Common)
  - All Enderman = 15 (needs Potent)
  - All Iron Golem = 20 (needs Mighty)
  - Golem torso + zombie everything else = 12 (Common): the interesting hybrids stay reachable
- **Altar GUI** shows "Weight 14 / 16". If the build is over the cap, the summon is refused with a red line. No items are eaten.
- **Kindred bonus:** if the soul's entity matches the head's mob, follow range +25% and head-ability cooldowns −25%. Flavour, not required.

**Vanilla systems used:** data components and tooltips, `LivingDeathEvent` (or the existing scythe path), the `#c:bosses` entity tag, `Mob`'s trial-spawner ominous flag.

---

### #6 ★ Shrine Tiers (altar focus). Effort M. Art: none

**What.** Dark décor around the altar raises how many minions you can keep bound. It counts like enchanting-table bookshelves.

**Why.** It replaces the `server.properties` `max_minion_spawn` hack with a vanilla-feeling progression. It gives you a home base worth building, a reason to collect mob heads, and a mid-to-late game goal. The tone fits: a candle-lit, skull-lined crypt.

**Mechanics.**

- **Scan area:** blocks within ±4 horizontally and −1..+3 vertically of the Summoning Altar, checked on summon and shown in the altar GUI.
- **Points**, from block tags `necromancy:altar_focus/1|2|3|6` so packs can edit them:

| Points | Block | Cap |
|---|---|---|
| 1 per candle in a lit candle block | `#minecraft:candles` | 8 points total |
| 1 | lit soul torch, soul lantern, soul campfire | 6 total |
| 1 | blood source block | 4 total |
| 2 | any mob head or skull (skeleton, zombie, creeper, piglin, player) | uncapped |
| 3 | wither skeleton skull, Skull Wall (#19), sculk catalyst | uncapped |
| 6 | dragon head | uncapped |

- **Tiers:**
  - **I** (altar alone): 3 bound minions.
  - **II** (≥ 10 points): 5 minions, and the Reaper job unlocks (#10).
  - **III** (≥ 24 points): 8 minions. Resurrecting from Remains needs no Jar of Blood (#7), and the Rite becomes possible (#17).
  - The capped categories give at most 18 points, so Tier III *needs* heads: three mob heads (charged creepers) or two wither skulls.
- **Config:** `minions.perTier = [3, 5, 8]`, a hard cap, and `-1` for unlimited (classic).
- **Necronomicon:** it is consumed when the altar is formed (changelog 1.6.1). Keep that, but form the altar on *any* `#minecraft:planks` + 2 `#c:cobblestones`, not just oak.

**Vanilla systems used:** block tags, lit/candle block-state properties, and a player attachment that lists bound minion UUIDs (already planned for the /minion lists).

---

### #7 ★ Remains and Phylactery. Effort M. Art: 2 item icons (Phylactery, Minion Remains)

**What.** Dying stops being a total loss, and a minion you care about can be recalled and resurrected.

**Why.** At about 100 kills per body, losing a minion to a creeper is the moment most players quit a summoner mod. A recovery loop also makes long-lived minions possible, which #12 builds on.

**Mechanics.**

- **Unbound minion death:** each part has a 25% chance to drop as its item. Equipment always drops (#3). The soul escapes (soul particles).
- **Phylactery.** Shaped recipe `" G " / "TLT" / " J "`, with G = gold ingot, T = ghast tear, L = soul lantern, J = Soul in a Jar. Durability 3. It is a Nether-gated mid-game item.
  - **Bind:** sneak-use it on your own minion. One minion per phylactery.
  - **Recall:** use it to teleport the bound minion to you. Same dimension, loaded, within 128 blocks. Cooldown 1200 t.
  - **Bound minion death:** it drops **Minion Remains**, an item with the `necromancy:minion_build` component (5 part ids, soul potency, name, rank and kills). The phylactery loses 1 durability.
- **Resurrection:** put the Remains in the altar's soul slot plus a Jar of Blood (no blood needed at Tier III), then sneak-use. The same minion returns, with its name and rank.
- **Bookkeeping:** owner bookkeeping is UUID-based in SavedData, so minions dying while the owner is offline are counted correctly (fixes the NPE in 1.6).

**Vanilla systems used:** data components, `SavedData`, the death drop path (`dropCustomDeathLoot`), and a teleport like `FollowOwnerGoal.teleportToOwner`.

---

### #8 ★ Harvest Economy (organ loot, Reaping, grave robbing). Effort S–M (mostly JSON). Art: none (vanilla sherd and gravel)

**What.** Organs come from the *right* mobs. A scythe enchantment improves reaping. Village cemeteries become grave-robbing sites that use 1.20 archaeology.

**Why.** It replaces the flat 1% grind with knowledge ("hoglins are all heart, ghasts are all lung"). It gives the early game a non-combat organ source, and it ends the free-parts cemetery chests.

**Mechanics.**

- **Organ loot modifier** (already planned). Each organ gets its own **independent** roll instead of one exclusive switch.
  - **Conditions:** `minecraft:killed_by_player` (minion kills count, see 1.8), not a baby, and the victim not in `#necromancy:no_organs`.
  - **Base chances:** Brains 2%, Heart 2%, Lungs 2%, Muscle 5%, Skin 1–2 at 15% (for fleshy mobs). Each gets +1% per level (`random_chance_with_enchanted_bonus`) of Looting **or** Reaping.
- **Affinity tags** (`necromancy:organ_rich/<organ>`) set the chance to:
  - Brains 6%: villagers, witches, illagers, piglins, endermen
  - Heart 5%: hoglins, ravagers, cows, pigs
  - Muscle 12%: ravagers, hoglins, goats (iron golems are in `#no_organs`)
  - Lungs 25% for ghasts (a ghast is basically a lung), 10% for blazes, breezes and phantoms
- **`#no_organs`:** all skeletons (they drop extra bones instead), slimes, magma cubes, iron and snow golems, vex, allay, minions and Teddy. You can't harvest a heart from a skeleton.
- **Reaping enchantment** (`data/necromancy/enchantment/reaping.json`):
  - Supported items `#necromancy:scythes`, levels I–III, weight 2, anvil cost 4, main hand.
  - Exclusive with Looting through the `#necromancy:exclusive_set/harvest` tag, so you choose vanilla drops or organs.
  - It is pure data: the organ loot conditions reference it. Also add the scythes to `#minecraft:enchantable/sword`, `sharp_weapon`, `weapon`, `fire_aspect` and `durability`.
- **Grave robbing:**
  - In the cemetery jigsaw piece, each of the 8 graves gets one **suspicious gravel**, using the `rule` processor with an `append_loot` block-entity modifier (the trail-ruins pattern). Loot table: `necromancy:archaeology/cemetery_grave`.
  - Loot weights: bone ×2–4 (30), rotten flesh (20), Skin ×2–4 (15), Muscle (8), Brains (4), Heart (4), Lungs (4), Bone Needle (5), Jar of Blood (5), one weight-≤2 body part (6), vanilla `skull_pottery_sherd` (2).
  - Skip a custom sherd: `DecoratedPotPatterns.ITEM_TO_POT_TEXTURE` is an immutable map and would need a mixin.
- **Cemetery chests:** the house chests become `necromancy:chests/cemetery`: organs, skin, at most 1 body part of weight ≤2, 1–3 Jars of Blood. The 10%-per-grave free-part chests are removed.

**Vanilla systems used:** global loot modifiers, loot conditions (`killed_by_player`, `random_chance_with_enchanted_bonus`, `entity_properties`), data-driven enchantments and exclusive sets, entity tags, archaeology (`BrushableBlockEntity`), jigsaw processors.

---

### #9 Mounts Done Right. Effort M. Art: hoglin and camel saddle overlays (2 textures). Pig and strider reuse vanilla saddle textures

**What.** Brain on a Stick works, and several torsos can be saddled.

**Why.** The original clearly meant to have mounts: it has a Brain on a Stick and three saddle textures. It is a fun, cheap payoff for torso choices.

**Mechanics.**

- **Brain on a Stick:** becomes `FoodOnAStickItem<EntityMinion>` with durability 25 and a 7-durability boost, matching the carrot on a stick. The minion implements `ItemSteerable` + `Saddleable` and uses `ItemBasedSteering`, exactly like `Pig`.
- **Saddleable torsos:**
  - Pig, Cow, Spider: the Spider climbs walls while ridden if it also has spider legs.
  - Squid: water mount, `water_movement_efficiency` 1.0.
  - Wave 2: Hoglin, Strider (lava; also accepts a vanilla Warped Fungus on a Stick), Camel (2 seats), Ravager.
- **Jump bar:** legs with `jump_strength` (Slime, Breeze) give the horse jump bar through `PlayerRideableJumping`.
- **Rider offset** is computed from the leg and torso model heights. The saddle drops on death.

**Vanilla systems used:** `FoodOnAStickItem`, `ItemSteerable`, `ItemBasedSteering`, `Saddleable`, `PlayerRideableJumping`, `canStandOnFluid` for the strider.

---

### #10 Minion Jobs. Effort L. Art: none

**What.** Under the Work order (#2), with a post and a container linked by sneak-using the scepter on a chest and then on the minion, a minion does one job decided by its parts.

**Why.** Jobs are a reason to keep minions at home, and they *feed the mod's own loop*: the Reaper produces organs and souls.

**Mechanics.** Work radius is 8 blocks (`restrictTo`). Each minion has a 9-slot pack and empties it into the linked container through the NeoForge `Capabilities.ItemHandler.BLOCK` capability when it is full or every 600 t.

| Job | Requires | Behaviour |
|---|---|---|
| **Farmer** | Villager head, at least 1 arm, hoe in the main hand | Harvests `CropBlock`s at max age, replants from the seeds it collected. One action per 40 t (logic like the vanilla `HarvestFarmland` behaviour) |
| **Collector** | Both arms Enderman | Picks up `ItemEntity`s within 12 blocks every 20 t. If the offhand holds an item, only matching items are taken. Turns any mob farm into an organ collector |
| **Reaper** | Blood, Bone or Netherite Scythe in the main hand, Shrine Tier II | Keeps at least `jobs.reaper.minAdults` (default 2) adults of each animal species within 8 blocks and culls one surplus adult every 600 t. Skips babies, named, leashed and tamed mobs. Bottles souls with glass bottles from its pack. Kills count as player kills, so organs drop |

**Vanilla systems used:** goals, `SimpleContainer`, the item handler capability, `CropBlock`, `restrictTo`. Leave out mining and building (section 4).

---

### #11 Blood That Matters. Effort S–M. Art: none (blood cauldron uses the blood fluid texture)

**What.** Blood heals minions, and the Bone Needle stops working on bloodless mobs.

**Why.** The blood fluid exists and does nothing. Healing is also missing from the original entirely.

**Mechanics.**

- **Minions** standing in blood heal 1 HP every 40 t. Players in blood get Hunger I while inside.
- **Feeding:** feed a minion a Jar of Blood to heal 8 HP and get the glass bottle back. Rotten flesh heals 2 HP. Harming potions heal through the `#inverted_healing_and_harm` tag (#1).
- **Bone Needle:** durability 32 instead of being consumed by blood-drawing. Drawing blood deals 1 damage, and it fails on `#necromancy:bloodless` mobs (skeletons, golems, slimes, blazes, breezes, vex, allay). The Sewing Machine can keep consuming one needle per craft (faithful), or take 1 durability per craft (recommended).
- **Blood cauldron:** a `CauldronInteraction` map with a 3-level `LayeredCauldronBlock`.
  - **Exploit warning:** with the original 8 jars per bucket, cauldrons create blood from nothing (3 jars → cauldron → bucket → 8 jars).
  - Either switch every ratio to the vanilla 1 bucket ↔ 3 jars, or skip the cauldron.

**Vanilla systems used:** fluid tick and entity-inside hooks, `CauldronInteraction`, entity tags, item durability.

---

### #12 Veterancy. Effort S. Art: none

**What.** Minions rank up from kills.

**Why.** It creates attachment to one minion, makes the Phylactery (#7) matter, and is a reason to keep playing.

**Mechanics.**

- **Counted kills:** the minion must land the killing blow on a mob with max HP ≥ 10.
- **Ranks** at 10, 30, 75, 150 and 300 kills. Each rank gives +5% max health and +5% attack damage (`ADD_MULTIPLIED_TOTAL`, id `necromancy:rank`).
- **Rank 5, "Revenant":** +1 armour toughness and a dark red name.
- Rank and kills are shown in the screen (#3) and stored in Remains (#7).

**Vanilla systems used:** attribute modifiers, entity data.

---

### #13 The Mod's Own Mobs Pay Out. Effort S. Art: none (models and textures already exist)

**What.** Night Crawler and Isaac feed the necromancy loop, and Teddy gets fixed.

**Why.** Right now they are hazards with no payoff.

**Mechanics.**

- **Night Crawler becomes a necro mob** (H T A L, split from the existing `ModelNightCrawler`). Essence: ender_pearl. Weight 3.
  - Stats: H (1,1.5,0,0,0.75), T (2,0,0,0,0), A (0.5,0,0,0,0.75), L (1,0,2,3.5,0).
  - Trait: Head hits give Wither I for 60 t (its own attack).
  - Downside, "photophobic": in daylight with sky light ≥ 12, −30% damage and speed.
  - Drop fix: a 25% chance of a real ender pearl instead of `ender_pearl` damage value 1.
- **Isaac:**
  - Blood Isaac drops 2 guaranteed Jars of Blood. Isaac's Body drops 1 guaranteed Heart and 1 Lungs, plus a 5% Soul Heart. The three-stage fight now pays in organs.
  - Spawn the Isaac replacement only below Y 40 in darkness ("the basement").
  - **Isaac's Head (helmet):** armour 2, durability 275, repaired with Skin.
  - **Tears** on a server cooldown: normal tears 10 t / 3 damage / free; blood tears 10 t / 6 damage / cost 1 HP (the original took 0.5).
- **Soul Heart:** becomes a deliberate Isaac boss summon instead of a random Isaac/Teddy/Night Crawler. It is a repeatable source of Isaac's Head.
- **Teddy:** fix the leak and the owner hijack (1.6). 12 HP, heals with wool.

**Vanilla systems used:** loot tables, spawn placement predicates, armour materials (1.21 `ArmorMaterial` registry).

---

### #14 Scythe Rebalance and Netherite Scythe. Effort S. Art: 1 retexture for the OBJ scythe (NeoForge `neoforge:obj` loader)

**What.** The scythes become real crowd-reaping weapons with a late-game tier.

**Why.** Today the Bone Scythe gives 8 damage for one diamond, which beats a diamond sword. Scythes also have no identity beyond bottling souls.

**Mechanics** (item attribute modifiers):

| Scythe | Damage | Attack speed | Durability | `player.sweeping_damage_ratio` | `player.entity_interaction_range` | Other |
|---|---|---|---|---|---|---|
| Blood | 6 | 1.1 | 666 | +0.5 (about a free Sweeping Edge I) | +0.5 | — |
| Bone | 7 | 1.1 | 1332 | +0.5 | +0.5 | — |
| **Netherite** | 8 | 1.1 | 2031 | +0.6 | +0.75 | Made at a smithing table from a netherite upgrade template + Bone Scythe + netherite ingot. Fire resistant. **Soul Harvest:** while it is held in either hand, mobs killed by your bound minions within 16 blocks give you their soul if you carry a glass bottle |

**Vanilla systems used:** item attribute modifiers, `SmithingTransformRecipe`, the `DataComponents.FIRE_RESISTANT` item property, enchantable tags (#8).

---

### #15 Necronomicon as a Guide, plus an Advancement Tree. Effort S–M. Art: none

**What.**

- **Book:** right-clicking air opens the vanilla `BookViewScreen` from a generated `WRITTEN_BOOK_CONTENT` component. The pages are translatable lang keys:
  1. Altar
  2. Where organs come from
  3. Sewing
  4. Souls and potency
  5. Shrine tiers
  6. Orders
  7. Phylactery
  8. The Rite
- **Lectern:** add the book to `#minecraft:lectern_books` so it can sit on a lectern. `LecternBlockEntity` reads `WRITTEN_BOOK_CONTENT`; check that it renders.
- **Advancements** (the conversion is already planned; this is the tree):
  - Forbidden Knowledge (Necronomicon) → Stitches (Sewing Machine) → It's Alive! (first minion)
  - → Mix and Match (parts from 5 different mobs) → Heavy Soul (Mighty soul) → Shrine Keeper (Tier III) → Legion (8 minions)
  - Side branches: Grave Robber (brush a grave), Back from the Grave (resurrect from Remains), Basement Dweller (Isaac's Head), Abomination (#17).

**Why.** JEI covers recipes but cannot explain potency, shrine points or orders. Advancements show the player the progression path.

---

### #16 Modern Parts, wave 2 (mounts, heavy and apex). Effort L. Art: none for entities. Saddles: see #9

| Mob | Parts | Essence | Stats | Trait | Downside | Weight |
|---|---|---|---|---|---|---|
| **Hoglin** | H T L | crimson_fungus | H (1,1,1,0,1) + `attack_knockback` 1.0, T (3,0,1,0,0), L (1,0,2,2.5,0) + `step_height` 0.4 | Head: hits toss the target (+0.5 Y), cooldown 40 t. Torso: saddleable | Won't path within 7 blocks of warped fungus | 3 |
| **Strider** | T L | warped_fungus | T (1,0,0,0,0), L (0.5,0,1,2,0) | Legs: walk on lava (`canStandOnFluid`). Torso + legs: fire immune. Torso: saddleable, Warped Fungus on a Stick works | Out of lava outside the Nether: −35% speed ("shivering") | 2 |
| **Camel** | T L | cactus | T (2,0,0.5,0,0), L (1,0,1,2,0) + `step_height` 0.9 (1.5 total) | Torso: saddleable, 2 seats | Slow to turn | 3 |
| **Ravager** | H T L | H ominous_bottle, T saddle, L saddle | H (1.5,1,1,0,1.5), T (5,0,2,0,0), L (2,0,3,1.5,0) + `step_height` 0.4 | Head: **Roar** every 200 t when 2+ enemies are within 4 blocks (6 damage plus knockback, vanilla). Torso: saddleable. Legs: break leaves (`mobGriefing`) | Weight 4 | 4 |
| **Breeze** | H T L | breeze_rod | H (0.75,1,0,0,0.5), T (1,0,0,0,0), L (0.5,0,0.5,3,0) + `jump_strength` 0.3, `safe_fall_distance` +20 | Head: wind charge every 60 t (crowd control). Torso: 30% chance to deflect projectiles | Low HP | 3 |
| **Piglin Brute** | H T A L (piglin geometry) | gold_ingot | H (1.5,1,0,0,1), T (3,0,1,0,0), A (0.75,0,0,0,1.25), L (1.5,0,3,3,0) | Arms: +50% damage with axes; can use crossbows | Head: walks off to pick up gold ingots within 8 blocks | 3 |
| **Glow Squid** | H T L | glow_ink_sac | Same as Squid | Head: hits give Glowing for 200 t (target marking). Legs: as Squid. Rendered emissive | Slow on land | 1 |
| **Phantom** (arms only, as wings) | A | phantom_membrane | A (0.25,0,0,0,0.25) each + `safe_fall_distance` +8, `gravity` −0.015 each | Gliding descent, no fall damage up to 16 blocks | Burns in sun unless helmeted | 2 |
| **Evoker** (head only) | H | totem_of_undying (deliberately sacrifices the evoker's own drop) | H (1,1,0,0,0.5) | Summons a line of 8 evoker fangs at targets within 12 blocks every 100 t | Costs a totem | 3 |
| **Warden** (head only, apex) | H | sculk_catalyst | H (3,1,1,0,2) | **Sonic Boom:** 10 damage of the `sonic_boom` type (ignores armour), range 15, cooldown 200 t | "Blind": follow range −50% (`ADD_MULTIPLIED_TOTAL`). Can't wear a helmet | 4 |

**Not chosen, and why:**

- Polar Bear: overlaps the Hoglin.
- Stray: Bogged already covers the debuff-archer role; Stray is a cheap add with Slowness arrows if wanted.
- Pillager and Vindicator: equipment (#3) already gives crossbow and axe play.
- Zombie Villager: Villager + Zombie already exist.
- Zoglin: duplicates the Hoglin.
- Fish, bat, rabbit, parrot, allay, frog, tadpole, sniffer, fox, cat, panda, bee, llama: weak identity, or cute mobs that clash with the tone.
- Horses: already rideable in vanilla.
- Vex, Ghast, Guardian, Shulker: their bodies don't fit a biped/quadruped frame, and they need flight or lasers.
- Wither, Ender Dragon, Elder Guardian: bosses are *souls* (Legendary), not parts.

---

### #17 Rite of the Abomination (repeatable late-game boss). Effort L. Art: 1 item icon (Abomination Heart)

**What.** A boss summoned at the altar that reuses the composite renderer, so it needs almost no art.

**Why.** It gives the mod an endgame, is a repeatable source of heavy parts and Legendary souls, and fits the theme: the necromancer's failed experiment.

**Mechanics.**

- **Summoning:**
  - Conditions: Shrine Tier III, at night or during a thunderstorm.
  - Altar contents: Jar of Blood plus an **Ominous Bottle** in the soul slot, empty part slots. Sneak-use.
  - A 100 t ritual follows, with visual-only lightning. Then the **Abomination** spawns.
- **The boss:**
  - It is a hostile, ownerless composite with one random weight-≥3 part per slot. The head always has an ability.
  - 300 HP (fixed), armour 10, part damage ×1.5, KB resistance 1.0, `scale` 1.4, purple `ServerBossEvent`. It uses every trait of its parts.
  - At 75%, 50% and 25% HP it **sheds** a random non-head part: the part drops as an item, its trait is lost, and 3 zombies or husks spawn (scaled by difficulty).
- **Drops:** the remaining parts, an **Abomination Heart** (a Legendary-potency soul), 3–5 mixed Brains, Hearts and Lungs, and 50 XP.
- **Cooldown:** each altar can run the rite once per in-game day.

**Vanilla systems used:** `ServerBossEvent`, `LightningBolt.setVisualOnly`, the ominous bottle item, the existing minion model and renderer.

---

### #18 Necromancer Trade Table. Effort S. Art: none

**What.** Replaces the 1.7 trade handler with a full 5-level profession table. The profession itself is already planned.

**Why.** It is a reliable emerald sink and source for the economy, and a way to get the Necronomicon without visiting the Nether.

**Trades** (cost → result, max uses, villager XP):

| Level | Trades |
|---|---|
| Novice | 16 rotten flesh → 1 emerald (16, 2). 1 emerald → 6 Skin (12, 1) |
| Apprentice | 8 bones → 1 emerald (16, 10). 3 emeralds → 2 Jars of Blood (12, 5) |
| Journeyman | 1 Soul in a Jar → 2 emeralds (12, 20). 6 emeralds → 2 Muscle (8, 10) |
| Expert | 10 emeralds → one of Brains, Heart or Lungs (fixed per villager) (6, 15). 6 emeralds + book → Necronomicon (4, 15) |
| Master | 18 emeralds → a body part of weight ≤ 2 (3, 30). 24 emeralds + book → Reaping I book (3, 30) |

---

### #19 Finish the Skull Wall. Effort S–M. Art: none (renders vanilla skull models)

**What.** The orphaned `BlockSkullWall` becomes a wall trophy holding 3 heads.

- **Recipe:** 3 `#minecraft:skulls` + 1 `#minecraft:stone_bricks`. The heads are stored in a data component; the original NBT used `Base`, `Skull1` to `Skull3`.
- **Rendering:** each slot uses vanilla `SkullBlockRenderer` models.
- **Shrine value:** worth 3 points (#6).

**Why.** It's free décor that is already half-written and feeds the shrine. Low priority.

---

## 3. The essential eight, and a build order

| Rank | Item | Closes this gap |
|---|---|---|
| ★1 | Part Traits and Balance | Part choice means something |
| ★2 | Orders and Stances | Minions have something to *do* |
| ★3 | Minion Screen and Equipment | You can see and outfit your minion. Vanilla weapons and enchantments carry over |
| ★4 | Modern Parts, wave 1 | It feels like 1.21, not 1.7 |
| ★5 | Soul Potency | A progression spine from early to late game |
| ★6 | Shrine Tiers | A home base, mid/late goals, a sane minion cap |
| ★7 | Remains and Phylactery | Death isn't a quit moment |
| ★8 | Harvest Economy | The organ grind becomes knowledge, and graves give an early source |

**Suggested order** (by dependency):

1. **Engine:** #1 trait hooks and JSON stats → #3 screen → #2 orders. Everything else displays through these or hooks into them.
2. **Economy:** #8 loot → #5 potency (needs part weights from #1) → #6 shrine → #7 remains.
3. **Content:** #4 wave 1 (do the BEWLR part icons first) → #13 → #14 → #12.
4. **Polish:** #9 mounts → #11 blood → #15 guide and advancements → #18 trades.
5. **Stretch:** #10 jobs → #16 wave 2 → #17 Rite → #19 Skull Wall.

**Total new art** for the top 8: about 1 GUI texture and 3 item icons (Bone Scepter, Phylactery, Minion Remains), plus the one-time BEWLR part-icon renderer. Across all 19 items, add 2 saddle overlays, 1 scythe retexture and 1 Abomination Heart icon. No new entity textures are needed.

---

## 4. What to leave out

- **Scent Burner and its GLSL shaders.** The original author removed it as broken in 1.6.0, and 1.7-era shaders don't map onto 1.21 core shaders.
- **The `specialFolk` Dropbox fetch** in `Necromancy.preInit`. The URL is dead, and it phones home at startup. Drop it, and the "special scythes" gate with it.
- **Thaumcraft aspects**, and any other-mod hooks (you asked for none).
- **Apprentice armour.** It was never designed and has no textures or stats. A robe set would be new scope with no gameplay hook.
- **Boss parts** (Wither, Dragon, Elder Guardian). They make the late game trivial. Bosses give Legendary *souls* only (#5).
- **A full Warden body.** The head only, if anything. The Warden's vanilla design is "don't fight it".
- **True flight** (phantom or ghast). Mob flight pathfinding and control is an L-sized rabbit hole. Gliding (Phantom wings, Blaze legs) captures most of the fun.
- **Mining and building jobs.** Pathing, block-break permissions and griefing make them very costly for what they add. Farmer, Collector and Reaper cover the "minions work" fantasy.
- **Player souls or PvP soul theft.** A griefing vector.
- **Minion breeding, children, hunger, upkeep or decay.** Tedious, and against the "army you earned" feel.
- **Per-mob organ items** (Creeper Heart, Ghast Lung...). The item count explodes. Affinity tags (#8) give the same knowledge with 5 items.
- **Cemetery "restless dead"** (graves spawning zombies at night). It kills villages. Grave robbing (#8) gets the theme without the harm. If you want it anyway, make it config-off and limited to when the player brushes a grave.
- **A custom pottery sherd.** `DecoratedPotPatterns.ITEM_TO_POT_TEXTURE` is immutable in 1.21.1 and would need a mixin. Use the vanilla skull sherd.

---

## Appendix: numbers cheat-sheet

| Key | Value |
|---|---|
| Base minion stats | 20 HP, 16 follow, 0.1 KB, **0.065 speed** (was 0.1), 2 damage. Speed cap 0.35, KB cap 1.0, part-armour cap 20 |
| Part weights | 1 / 2 / 3 / 4 (see #5) |
| Soul caps | Faint 6, Lesser 9, Common 12, Potent 16, Mighty 20, Legendary 20 + bonuses |
| Shrine | Tier II ≥ 10 points, Tier III ≥ 24. Minions [3, 5, 8]. Category caps: candles 8, soul lights 6, blood 4 |
| Organs (base / affinity) | Brains 2/6%, Heart 2/5%, Lungs 2/10–25%, Muscle 5/12%, Skin 15% ×1–2. +1% per Looting or Reaping level |
| Phylactery | 3 uses, recall cooldown 1200 t, range 128 |
| Ranks | 10 / 30 / 75 / 150 / 300 kills, +5% HP and damage each |
| Brain on a Stick | Durability 25, boost cost 7 |
| Suggested config keys | `balance.classicStats`, `minions.baseSpeed`, `minions.perTier`, `souls.potencyGate`, `creeperTorso.breaksBlocks`, `jobs.reaper.minAdults`, `organs.chanceScale`, `abomination.enabled` |
