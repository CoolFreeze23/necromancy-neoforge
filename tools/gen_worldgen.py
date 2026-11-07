#!/usr/bin/env python3
"""Generates the Necromancy worldgen data: the village cemetery templates (one per village type), their processor lists,
and the cemetery / grave / Nether Chalice loot tables.

The cemetery is sirolf2009's ComponentVillageCemetery (1.7.10): a walled yard entered through an arched gate, four graves
either side of a gravel path, a stone tomb with two chests on the left and the necromancer's house on the right, both
at the back with their doors on the path. The 1.7 piece was 18 x 19. Village streets leave room for houses about 17 wide
along the street but only 10 to 11 deep (village houses in the snowy and desert pools are at most that deep), so this
one is laid out 19 x 10: the graves stand in a row across the front of the yard instead of in two files beside the path.
The gate is in the front wall (z = 0); the jigsaw block sits in it facing north, one block above the ground layer like
the vanilla houses' building_entrance jigsaws.

usage: python tools/gen_worldgen.py      (needs nbtlib: pip install nbtlib)
"""
import json
import os

import nbtlib
from nbtlib.tag import Byte, Compound, Double, Int, List, String

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "data", "necromancy")
DATA_VERSION = 3955  # 1.21.1

SIZE_X, SIZE_Y, SIZE_Z = 13, 6, 10
JIGSAW = (6, 1, 0)
VILLAGER = (10, 1, 7)
GRAVE_TABLE = "necromancy:archaeology/cemetery_grave"
CHEST_TABLE = "necromancy:chests/cemetery"

# ---------------------------------------------------------------- palettes, one per village type
BIOMES = {
    "plains": dict(wall="cobblestone_wall", stone="cobblestone", headstone="cobblestone", planks="oak_planks", log="oak_log",
                   door="oak_door", stairs="stone_brick_stairs", ground="grass_block", path="gravel", grave="suspicious_gravel",
                   villager="minecraft:plains", mossy=True),
    "taiga": dict(wall="cobblestone_wall", stone="cobblestone", headstone="mossy_cobblestone", planks="spruce_planks", log="spruce_log",
                  door="spruce_door", stairs="cobblestone_stairs", ground="grass_block", path="gravel", grave="suspicious_gravel",
                  villager="minecraft:taiga", mossy=True),
    "snowy": dict(wall="cobblestone_wall", stone="cobblestone", headstone="stone_bricks", planks="spruce_planks", log="stripped_spruce_log",
                  door="spruce_door", stairs="stone_brick_stairs", ground="grass_block", ground_props={"snowy": "true"}, snow=True,
                  path="gravel", grave="suspicious_gravel", villager="minecraft:snow"),
    "savanna": dict(wall="cobblestone_wall", stone="cobblestone", headstone="cobblestone", planks="acacia_planks", log="acacia_log",
                    door="acacia_door", stairs="stone_brick_stairs", ground="grass_block", path="gravel", grave="suspicious_gravel",
                    villager="minecraft:savanna"),
    "desert": dict(wall="sandstone_wall", stone="sandstone", headstone="chiseled_sandstone", planks="cut_sandstone", log="smooth_sandstone",
                   door="jungle_door", stairs="sandstone_stairs", ground="sand", path="smooth_sandstone", grave="suspicious_sand",
                   villager="minecraft:desert"),
}

SOLID = {"cobblestone", "mossy_cobblestone", "stone_bricks", "sandstone", "chiseled_sandstone", "cut_sandstone", "smooth_sandstone",
         "oak_planks", "spruce_planks", "acacia_planks", "oak_log", "spruce_log", "stripped_spruce_log", "acacia_log"}
WALLS = {"cobblestone_wall", "sandstone_wall"}


class Grid:
    """Blocks by (x, y, z): (name, properties dict, block entity nbt or None)."""

    def __init__(self):
        self.b = {}

    def set(self, x, y, z, name, props=None, nbt=None):
        if 0 <= x < SIZE_X and 0 <= y < SIZE_Y and 0 <= z < SIZE_Z:
            self.b[(x, y, z)] = (name, dict(props or {}), nbt)

    def fill(self, x0, y0, z0, x1, y1, z1, name, props=None):
        for x in range(x0, x1 + 1):
            for y in range(y0, y1 + 1):
                for z in range(z0, z1 + 1):
                    self.set(x, y, z, name, props)

    def name(self, x, y, z):
        e = self.b.get((x, y, z))
        return e[0] if e else "air"


def cemetery(p):
    g = Grid()
    gp = p.get("ground_props")
    s = p["stone"]
    # ground and the wall round the yard (the tomb and the house close its back corners)
    g.fill(0, 0, 0, 12, 0, 9, p["ground"], gp)
    g.fill(0, 1, 0, 12, 1, 0, p["wall"])
    g.fill(0, 1, 9, 12, 1, 9, p["wall"])
    g.fill(0, 1, 0, 0, 1, 9, p["wall"])
    g.fill(12, 1, 0, 12, 1, 9, p["wall"])
    # four graves, two either side of the path: a headstone at the wall, then two suspicious blocks to brush
    # (REVIEW #8 grave robbing: eight blocks to rob, as the 1.7 cemetery had eight graves)
    for i, (z, head, dig) in enumerate(((1, 1, (2, 3)), (3, 1, (2, 3)), (1, 11, (10, 9)), (3, 11, (10, 9)))):
        g.set(head, 1, z, p["headstone"])
        for x in dig:
            g.set(x, 0, z, p["grave"], {"dusted": "0"})
        # candles left by mourners: a little light against night spawns
        g.set(head, 2, z, "candle", {"candles": str(1 + i % 3), "lit": "true", "waterlogged": "false"})
    # the gate: a stepped arch over a three-wide opening, a soul lantern hanging in it
    g.fill(5, 1, 0, 7, 1, 0, "air")
    g.fill(4, 1, 0, 4, 3, 0, s)
    g.fill(5, 3, 0, 5, 4, 0, s)
    g.fill(6, 4, 0, 6, 5, 0, s)
    g.fill(7, 3, 0, 7, 4, 0, s)
    g.fill(8, 1, 0, 8, 3, 0, s)
    g.set(6, 3, 0, "soul_lantern", {"hanging": "true", "waterlogged": "false"})
    # the path from the gate, between the graves and on between the tomb and the house
    g.fill(5, 0, 0, 7, 0, 8, p["path"])
    # the tomb (left): a cross of stone with a stair-rimmed roof and two chests
    g.fill(1, 1, 5, 3, 3, 5, s)
    g.fill(1, 1, 9, 3, 3, 9, s)
    g.fill(0, 1, 6, 0, 3, 8, s)
    g.fill(4, 1, 6, 4, 3, 8, s)
    g.fill(1, 0, 6, 3, 0, 8, s)
    g.fill(1, 4, 6, 3, 4, 8, s)
    g.fill(1, 1, 6, 3, 3, 8, "air")
    g.set(1, 1, 7, s)
    st = {"half": "bottom", "shape": "straight", "waterlogged": "false"}
    for i in range(3):
        g.set(1 + i, 4, 5, p["stairs"], dict(st, facing="south"))
        g.set(1 + i, 4, 9, p["stairs"], dict(st, facing="north"))
        g.set(0, 4, 6 + i, p["stairs"], dict(st, facing="east"))
        g.set(4, 4, 6 + i, p["stairs"], dict(st, facing="west"))
    g.set(4, 0, 7, s)
    door(g, 4, 1, 7, p["door"], "west")
    chest(g, 1, 1, 6, "east")
    chest(g, 1, 1, 8, "east")
    # the necromancer's house (right): plank walls on stone corners, a log-rimmed roof, his Sewing Machine and a chest
    g.fill(8, 0, 5, 12, 0, 9, s)
    g.fill(8, 1, 5, 12, 3, 5, p["planks"])
    g.fill(8, 1, 9, 12, 3, 9, p["planks"])
    g.fill(8, 1, 5, 8, 3, 9, p["planks"])
    g.fill(12, 1, 5, 12, 3, 9, p["planks"])
    log = p["log"]
    axis = log != "smooth_sandstone"
    for x in range(8, 13):
        for z in range(5, 10):
            edge_x = x in (8, 12)
            edge_z = z in (5, 9)
            if edge_x or edge_z:
                props = {"axis": "y" if edge_x and edge_z else ("z" if edge_x else "x")} if axis else None
                g.set(x, 4, z, log, props)
    g.fill(9, 4, 6, 11, 4, 8, p["planks"])
    for cx, cz in ((8, 5), (12, 5), (8, 9), (12, 9)):
        g.fill(cx, 1, cz, cx, 3, cz, s)
    g.fill(9, 1, 6, 11, 3, 8, "air")
    door(g, 8, 1, 7, p["door"], "east")
    g.set(10, 2, 5, "glass_pane")
    g.set(12, 2, 7, "glass_pane")
    g.set(11, 1, 8, "necromancy:sewing_machine", {"facing": "west"})
    chest(g, 11, 1, 6, "west")
    g.set(10, 2, 8, "soul_wall_torch", {"facing": "north"})
    # snow over open ground in snowy villages
    if p.get("snow"):
        for x in range(SIZE_X):
            for z in range(SIZE_Z):
                if g.name(x, 0, z) == p["ground"] and g.name(x, 1, z) == "air" and (x, z) != (JIGSAW[0], JIGSAW[2]):
                    g.set(x, 1, z, "snow", {"layers": "1"})
    # the jigsaw block in the gate, facing the street (the vanilla houses' building_entrance jigsaw)
    g.set(*JIGSAW, "jigsaw", {"orientation": "north_up"}, nbt=Compound({
        "final_state": String("minecraft:air"), "name": String("minecraft:building_entrance"), "pool": String("minecraft:empty"),
        "joint": String("aligned"), "id": String("minecraft:jigsaw"), "target": String("minecraft:building_entrance")}))
    connect(g)
    return g


def door(g, x, y, z, name, facing):
    base = {"facing": facing, "hinge": "left", "open": "false", "powered": "false"}
    g.set(x, y, z, name, dict(base, half="lower"))
    g.set(x, y + 1, z, name, dict(base, half="upper"))


def chest(g, x, y, z, facing):
    g.set(x, y, z, "chest", {"facing": facing, "type": "single", "waterlogged": "false"},
          Compound({"id": String("minecraft:chest"), "LootTable": String(CHEST_TABLE)}))


DIRS = {"north": (0, -1), "south": (0, 1), "west": (-1, 0), "east": (1, 0)}


def connect(g):
    """Jigsaw pieces place states as written (known shape), so walls and panes get their connections here."""
    for (x, y, z), (name, props, nbt) in list(g.b.items()):
        if name in WALLS:
            c = {}
            for d, (dx, dz) in DIRS.items():
                n = g.name(x + dx, y, z + dz)
                c[d] = n in WALLS or n in SOLID
            straight = (c["north"] and c["south"] and not c["east"] and not c["west"]) or \
                       (c["east"] and c["west"] and not c["north"] and not c["south"])
            props = {d: ("low" if v else "none") for d, v in c.items()}
            props["up"] = "false" if straight else "true"
            props["waterlogged"] = "false"
            g.b[(x, y, z)] = (name, props, nbt)
        elif name == "glass_pane":
            props = {"waterlogged": "false"}
            for d, (dx, dz) in DIRS.items():
                n = g.name(x + dx, y, z + dz)
                props[d] = "true" if (n in SOLID or n == "glass_pane") else "false"
            g.b[(x, y, z)] = (name, props, nbt)


def villager(p):
    x, y, z = VILLAGER
    pos = List[Double]([Double(x + 0.5), Double(y), Double(z + 0.5)])
    return Compound({
        "blockPos": List[Int]([Int(x), Int(y), Int(z)]),
        "pos": pos,
        "nbt": Compound({
            "id": String("minecraft:villager"),
            "Pos": pos,
            "Rotation": List[nbtlib.tag.Float]([nbtlib.tag.Float(90.0), nbtlib.tag.Float(0.0)]),
            "OnGround": Byte(1),
            "VillagerData": Compound({"profession": String("necromancy:necromancer"), "level": Int(1), "type": String(p["villager"])}),
            # experience keeps the profession while the villager walks to its Sewing Machine (ResetProfession only
            # strips level-1 villagers with no experience and no job site)
            "Xp": Int(1),
        }),
    })


def write_nbt(g, p, path):
    palette, index, blocks = [], {}, []
    for x in range(SIZE_X):
        for y in range(SIZE_Y):
            for z in range(SIZE_Z):
                name, props, nbt = g.b.get((x, y, z), ("air", {}, None))
                full = name if ":" in name else "minecraft:" + name
                key = (full, tuple(sorted(props.items())))
                if key not in index:
                    index[key] = len(palette)
                    entry = {"Name": String(full)}
                    if props:
                        entry["Properties"] = Compound({k: String(v) for k, v in sorted(props.items())})
                    palette.append(Compound(entry))
                block = {"pos": List[Int]([Int(x), Int(y), Int(z)]), "state": Int(index[key])}
                if nbt is not None:
                    block["nbt"] = nbt
                blocks.append(Compound(block))
    root = Compound({
        "DataVersion": Int(DATA_VERSION),
        "size": List[Int]([Int(SIZE_X), Int(SIZE_Y), Int(SIZE_Z)]),
        "palette": List[Compound](palette),
        "blocks": List[Compound](blocks),
        "entities": List[Compound]([villager(p)]),
    })
    os.makedirs(os.path.dirname(path), exist_ok=True)
    nbtlib.File(root).save(path, gzipped=True)


def processor_list(p):
    rules = [{
        "block_entity_modifier": {"type": "minecraft:append_loot", "loot_table": GRAVE_TABLE},
        "input_predicate": {"predicate_type": "minecraft:block_match", "block": "minecraft:" + p["grave"]},
        "location_predicate": {"predicate_type": "minecraft:always_true"},
        "output_state": {"Name": "minecraft:" + p["grave"], "Properties": {"dusted": "0"}},
    }]
    processors = [{"processor_type": "minecraft:rule", "rules": rules}]
    if p.get("mossy"):
        processors.append({"processor_type": "minecraft:rule", "rules": [{
            "input_predicate": {"predicate_type": "minecraft:random_block_match", "block": "minecraft:cobblestone", "probability": 0.1},
            "location_predicate": {"predicate_type": "minecraft:always_true"},
            "output_state": {"Name": "minecraft:mossy_cobblestone"},
        }]})
    return {"processors": processors}


# ---------------------------------------------------------------- loot
# Sewn parts of mobs with weight 1 or 2 (mobs/VanillaNecroMobs.java, registry/BodyPartTable.java)
LIGHT_PARTS = {
    # weight 1
    "cave_spider": "head torso legs", "chicken": "head torso arm legs", "cow": "head torso arm legs", "pig": "head torso arm legs",
    "sheep": "head torso arm legs", "squid": "head torso legs", "villager": "head torso arm legs", "slime": "head torso legs",
    "mooshroom": "head torso arm legs", "snow_golem": "head torso arm legs",
    # weight 2
    "creeper": "torso legs", "zombified_piglin": "head torso arm legs", "skeleton": "torso arm legs", "spider": "head torso legs",
    "witch": "head torso arm legs", "wolf": "head", "zombie": "torso arm legs", "husk": "head torso arm legs",
    "drowned": "head torso arm legs", "bogged": "head torso arm legs", "armadillo": "head torso legs", "goat": "head",
}


def light_part_ids():
    return ["necromancy:%s_%s" % (mob, t) for mob, types in LIGHT_PARTS.items() for t in types.split()]


def item(name, weight=1, count=None, functions=None):
    e = {"type": "minecraft:item", "name": name if ":" in name else "minecraft:" + name}
    if weight != 1:
        e["weight"] = weight
    fs = list(functions or [])
    if count:
        lo, hi = count
        fs.insert(0, {"function": "minecraft:set_count", "count": lo if lo == hi else {"type": "minecraft:uniform", "min": lo, "max": hi}})
    if fs:
        e["functions"] = fs
    return e


def light_part(weight):
    """One body part of weight 1 or 2, every part as likely as the next."""
    return {"type": "minecraft:loot_table", "weight": weight, "value": {
        "pools": [{"rolls": 1, "entries": [item(i) for i in light_part_ids()]}]}}


def soul(entity, potency, weight=1):
    return item("necromancy:soul_in_a_jar", weight, functions=[{
        "function": "minecraft:set_components", "components": {"necromancy:soul": {"entity": entity, "potency": potency}}}])


def chance(c):
    return [{"condition": "minecraft:random_chance", "chance": c}]


def grave_table():
    return {"type": "minecraft:archaeology", "random_sequence": GRAVE_TABLE, "pools": [{"rolls": 1, "bonus_rolls": 0, "entries": [
        item("bone", 30, (2, 4)),
        item("rotten_flesh", 20),
        item("necromancy:skin", 15, (2, 4)),
        item("necromancy:muscle", 8),
        item("necromancy:brains", 4),
        item("necromancy:heart", 4),
        item("necromancy:lungs", 4),
        item("necromancy:bone_needle", 5),
        item("necromancy:jar_of_blood", 5),
        light_part(6),
        item("skull_pottery_sherd", 2),
    ]}]}


def cemetery_chest():
    return {"type": "minecraft:chest", "random_sequence": CHEST_TABLE, "pools": [
        {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "bonus_rolls": 0, "entries": [
            item("necromancy:skin", 8, (2, 5)), item("necromancy:muscle", 6), item("necromancy:brains", 3),
            item("necromancy:heart", 3), item("necromancy:lungs", 3)]},
        {"rolls": 1, "bonus_rolls": 0, "entries": [item("necromancy:jar_of_blood", 1, (1, 3))]},
        {"rolls": 1, "bonus_rolls": 0, "entries": [item("necromancy:bone_needle")]},
        {"rolls": 1, "bonus_rolls": 0, "conditions": chance(0.3), "entries": [light_part(1)]},
        {"rolls": 1, "bonus_rolls": 0, "conditions": chance(0.06), "entries": [item("necromancy:necronomicon")]},
        {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "bonus_rolls": 0, "entries": [
            item("bone", 10, (2, 6)), item("rotten_flesh", 10, (2, 6)), item("candle", 5, (1, 3)),
            item("black_candle", 2, (1, 2)), item("soul_torch", 5, (1, 4)), item("cobweb", 2, (1, 2))]},
    ]}


def chalice_chest():
    table = "necromancy:chests/nether_chalice"
    return {"type": "minecraft:chest", "random_sequence": table, "pools": [
        {"rolls": 1, "bonus_rolls": 0, "entries": [item("necromancy:jar_of_blood", 1, (2, 5))]},
        {"rolls": 1, "bonus_rolls": 0, "conditions": chance(0.75), "entries": [
            soul("minecraft:wither_skeleton", "common", 3), soul("minecraft:blaze", "common", 3), soul("minecraft:ghast", "lesser", 2),
            soul("minecraft:hoglin", "potent", 1)]},
        {"rolls": {"type": "minecraft:uniform", "min": 2, "max": 4}, "bonus_rolls": 0, "entries": [
            item("ghast_tear", 4, (1, 2)), item("gold_ingot", 6, (1, 4)), item("gold_nugget", 8, (3, 9)),
            item("blaze_powder", 3, (1, 3)), item("nether_wart", 3, (2, 5)), item("soul_torch", 2, (2, 4))]},
        {"rolls": 1, "bonus_rolls": 0, "conditions": chance(0.04), "entries": [item("necromancy:soul_heart")]},
    ]}


def write_json(rel, data):
    path = os.path.join(ROOT, rel)
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        json.dump(data, f, indent=2)
        f.write("\n")


def main():
    for biome, p in BIOMES.items():
        write_nbt(cemetery(p), p, os.path.join(ROOT, "structure", "village", biome, "cemetery.nbt"))
        write_json("worldgen/processor_list/cemetery_%s.json" % biome, processor_list(p))
    write_json("loot_table/archaeology/cemetery_grave.json", grave_table())
    write_json("loot_table/chests/cemetery.json", cemetery_chest())
    write_json("loot_table/chests/nether_chalice.json", chalice_chest())
    print("light parts (%d): %s" % (len(light_part_ids()), " ".join(light_part_ids())))


if __name__ == "__main__":
    main()
