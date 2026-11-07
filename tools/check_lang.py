"""Checks that every translation key the code uses exists in a language file.

usage: python -I tools/check_lang.py <project dir> [lang code ...]     (default: en_us pt_br)

Literal keys are read from Component.translatable("...") and friends; keys built at run time are listed in DYNAMIC below.
Exit code 1 if any key is missing; extra keys (in the file, not in the code) are reported but allowed.
"""
import glob
import json
import os
import re
import sys

root = sys.argv[1]
langs = sys.argv[2:] or ['en_us', 'pt_br']
src = os.path.join(root, 'src', 'main', 'java')
sys.path.insert(0, os.path.join(root, 'tools'))

literal = set()
for path in glob.glob(os.path.join(src, '**', '*.java'), recursive=True):
    text = open(path, encoding='utf-8').read()
    for m in re.finditer(r'(?:translatable|translatableEscape|displayClientMessage\(Component\.translatable)\(\s*"([a-z0-9_.]+)"', text):
        literal.add(m.group(1))

# keys built from enums / ids
mobs = []
table = open(os.path.join(src, 'com', 'sirolf2009', 'necromancy', 'registry', 'BodyPartTable.java'), encoding='utf-8').read()
parts = re.findall(r'add\("([a-z_]+)",((?:\s*PartType\.[A-Z]+,?)+)\)', table)
for mob, types in parts:
    mobs.append(mob)
mobs += ['isaac']
traits = re.findall(r'(?:flag|onHit|interact|new Simple)\("([a-z_]+)"', open(os.path.join(src, 'com', 'sirolf2009', 'necromancy', 'trait', 'Traits.java'), encoding='utf-8').read())
traits += re.findall(r'Traits\.flag\("([a-z_]+)"\)', open(os.path.join(src, 'com', 'sirolf2009', 'necromancy', 'mobs', 'VanillaNecroMobs.java'), encoding='utf-8').read())
dynamic = set()
for mob in mobs:
    dynamic.add('necromob.necromancy.' + mob)
for t in set(traits):
    dynamic.add('trait.necromancy.' + t)
    dynamic.add('trait.necromancy.' + t + '.desc')
for p in ['head', 'torso', 'arm', 'legs']:
    dynamic.add('tooltip.necromancy.part.' + p)
for loc in ['head', 'torso', 'arm_left', 'arm_right', 'legs']:
    dynamic.add('location.necromancy.' + loc)
for s in ['health', 'attack', 'speed', 'knockback', 'follow']:
    dynamic.add('tooltip.necromancy.stat.' + s)
for s in ['health', 'attack', 'armor', 'speed', 'follow']:
    dynamic.add('gui.necromancy.minion.' + s)
for o in ['follow', 'stay', 'guard', 'patrol']:
    dynamic.add('order.necromancy.' + o)
for s in ['passive', 'defensive', 'aggressive']:
    dynamic.add('stance.necromancy.' + s)
for r in range(6):
    dynamic.add('rank.necromancy.%d' % r)
for p in ['faint', 'lesser', 'common', 'potent', 'mighty', 'legendary']:
    dynamic.add('soul.necromancy.potency.' + p)
for t in [1, 2, 3]:
    dynamic.add('gui.necromancy.altar.tier.%d' % t)
for s in ['walking', 'defending', 'sitting']:
    dynamic.add('message.necromancy.teddy.' + s)
for k in ['friend', 'enemy', 'forget']:
    dynamic.add('commands.necromancy.minion.' + k)
for i in range(1, 13):
    dynamic.add('book.necromancy.guide.%d' % i)

needed = literal | dynamic
status = 0
for lang in langs:
    candidates = glob.glob(os.path.join(root, 'src', '*', 'resources', 'assets', 'necromancy', 'lang', lang + '.json'))
    if not candidates:
        print('%s: no file' % lang)
        status = 1
        continue
    data = {}
    for c in candidates:
        data.update(json.load(open(c, encoding='utf-8')))
    missing = sorted(k for k in needed if k not in data)
    print('%s: %d keys, %d needed by code, %d missing' % (lang, len(data), len(needed), len(missing)))
    for k in missing:
        print('  MISSING ' + k)
    if missing:
        status = 1
sys.exit(status)
