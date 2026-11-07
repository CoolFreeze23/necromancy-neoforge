"""Builds the OreSpawn creature data from docs/orespawn_parts_spec.json.

usage: python -I tools/gen_orespawn.py [repo root] [OreSpawn en_us.json] [CrazyCraft pt_br.json for orespawn]
writes:
  src/main/resources/data/necromancy/compat/orespawn.json        the creatures, read at start-up when OreSpawn is loaded
  src/main/resources/data/necromancy/recipe/sewing/orespawn/*.json  one Sewing Machine recipe per part (only with OreSpawn)
  src/main/lang-src/{en_us,pt_br}/orespawn_mobs.json             the creature names, when the two language files are given
                                                                  (OreSpawn's own English, the CrazyCraft pack's Portuguese)
"""
import json
import os
import shutil
import sys

root = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(__file__), '..')
spec = json.load(open(os.path.join(root, 'docs', 'orespawn_parts_spec.json'), encoding='utf-8'))

PARTS = ['head', 'torso', 'arm', 'legs']
mobs = []
for c in spec['creatures']:
    parts = {}
    for p in PARTS:
        if p not in c['parts']:
            continue
        pd = c['parts'][p]
        parts[p] = {'stats': pd['stats'], 'attributes': pd.get('attributes', []), 'traits': pd.get('traits', [])}
    mobs.append({
        'mob': c['mob'],
        'entity': c['entity'],
        'weight': c['weight'],
        'shape': c['shape'],
        'saddleable': c['saddleable'],
        'rider_offset': c['rider_offset'],
        'sounds': c['sounds'],
        'essence': c['essence'],
        'parts': parts,
    })

tags = dict(spec['tag_additions'])
out = {'requires': 'orespawn', 'tags': tags, 'mobs': mobs}
compat = os.path.join(root, 'src', 'main', 'resources', 'data', 'necromancy', 'compat')
os.makedirs(compat, exist_ok=True)
with open(os.path.join(compat, 'orespawn.json'), 'w', encoding='utf-8', newline='\n') as f:
    json.dump(out, f, ensure_ascii=False, indent=1)
    f.write('\n')


def ingredient(e):
    return {'tag': e[1:]} if e.startswith('#') else {'item': e}


SKIN, BRAINS, HEART, LUNGS, MUSCLE = 'necromancy:skin', 'necromancy:brains', 'necromancy:heart', 'necromancy:lungs', 'necromancy:muscle'
PATTERNS = {
    'head': (["SSSS", "SBFS", "SEES"], {'S': SKIN, 'B': BRAINS, 'E': 'minecraft:spider_eye'}),
    'torso': ([" LL ", "BHUB", "LEEL", "BLLB"], {'L': SKIN, 'H': HEART, 'U': LUNGS, 'B': 'minecraft:bone'}),
    'arm': (["LLLL", "BMEB", "LLLL"], {'L': SKIN, 'M': MUSCLE, 'B': 'minecraft:bone'}),
    'legs': (["LBBL", "LMML", "LEEL", "LBBL"], {'L': SKIN, 'M': MUSCLE, 'B': 'minecraft:bone'}),
}
recipes = os.path.join(root, 'src', 'main', 'resources', 'data', 'necromancy', 'recipe', 'sewing', 'orespawn')
shutil.rmtree(recipes, ignore_errors=True)
os.makedirs(recipes)
seen = {}
count = 0
for m in mobs:
    for p in m['parts']:
        essence = m['essence'].get(p, m['essence']['default'])
        clash = seen.setdefault((p, essence), m['mob'])
        if clash != m['mob']:
            raise SystemExit('recipe clash: %s %s and %s both use %s' % (p, m['mob'], clash, essence))
        pattern, key = PATTERNS[p]
        keys = {k: {'item': v} for k, v in key.items()}
        essence_key = 'F' if p == 'head' else 'E'
        keys[essence_key] = ingredient(essence)
        recipe = {
            'neoforge:conditions': [{'type': 'neoforge:mod_loaded', 'modid': 'orespawn'}],
            'type': 'necromancy:sewing_shaped',
            'key': dict(sorted(keys.items())),
            'pattern': pattern,
            'result': {'count': 1, 'id': 'necromancy:creature_part',
                       'components': {'necromancy:creature_part': {'mob': 'orespawn:' + m['mob'], 'type': p}}},
        }
        with open(os.path.join(recipes, '%s_%s.json' % (m['mob'], p)), 'w', encoding='utf-8', newline='\n') as f:
            json.dump(recipe, f, indent=2)
            f.write('\n')
        count += 1
print('%d creatures, %d recipes' % (len(mobs), count))

if len(sys.argv) > 3:
    for lang, path in (('en_us', sys.argv[2]), ('pt_br', sys.argv[3])):
        source = json.load(open(path, encoding='utf-8'))
        names = {}
        for m in mobs:
            key = 'entity.orespawn.' + m['mob']
            if key not in source:
                raise SystemExit('%s has no %s' % (path, key))
            names['necromob.orespawn.' + m['mob']] = source[key]
        out_path = os.path.join(root, 'src', 'main', 'lang-src', lang, 'orespawn_mobs.json')
        with open(out_path, 'w', encoding='utf-8', newline='\n') as f:
            json.dump(dict(sorted(names.items())), f, ensure_ascii=False, indent=2)
            f.write('\n')
    print('creature names written')
