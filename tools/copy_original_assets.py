"""Copies the 1.7.10 Necromancy art and sounds into the 1.21.1 resource layout. Usage: python -I tools/copy_original_assets.py <original Necromancy dir> <project dir>"""
import os, shutil, sys

src = os.path.join(sys.argv[1], 'src', 'main', 'resources', 'assets', 'necromancy')
dst = os.path.join(sys.argv[2], 'src', 'main', 'resources', 'assets', 'necromancy')

ITEMS = {
    'boneneedle': 'bone_needle', 'brainonastick': 'brain_on_a_stick', 'brains': 'brains', 'bucketblood': 'blood_bucket',
    'heart': 'heart', 'isaacsseveredhead': 'isaacs_head', 'jarofblood': 'jar_of_blood', 'lungs': 'lungs', 'muscle': 'muscle',
    'necronomicon': 'necronomicon', 'skin': 'skin', 'soulheart': 'soul_heart', 'soulinajar': 'soul_in_a_jar', 'tear': 'tear',
    'tearblood': 'blood_tear',
}
MOBS = {'CaveSpider': 'cave_spider', 'Chicken': 'chicken', 'Cow': 'cow', 'Creeper': 'creeper', 'Enderman': 'enderman',
        'IronGolem': 'iron_golem', 'Pig': 'pig', 'Pigzombie': 'zombified_piglin', 'Sheep': 'sheep', 'Skeleton': 'skeleton',
        'Spider': 'spider', 'Squid': 'squid', 'Villager': 'villager', 'Witch': 'witch', 'Wolf': 'wolf', 'Zombie': 'zombie'}
PARTS = {'Head': 'head', 'Torso': 'torso', 'Arm': 'arm', 'Legs': 'legs'}
ENTITIES = {'cowsaddle': 'saddle/cow', 'spidersaddle': 'saddle/spider', 'squidsaddle': 'saddle/squid', 'isaac': 'isaac/isaac',
            'isaacblood': 'isaac/blood_isaac', 'nightcrawler': 'night_crawler', 'teddy': 'teddy', 'villagernecro': 'villager/profession/necromancer'}
MODELS = {'altartexture': 'block/summoning_altar', 'sewingtexture': 'block/sewing_machine', 'scythe': 'item/blood_scythe_model',
          'scythebone': 'item/bone_scythe_model', 'necronomicon': 'item/necronomicon_model'}

def copy(a, b):
    os.makedirs(os.path.dirname(b), exist_ok=True)
    shutil.copyfile(a, b)
    print(os.path.relpath(b, dst))

for old, new in ITEMS.items():
    copy(os.path.join(src, 'textures', 'items', old + '.png'), os.path.join(dst, 'textures', 'item', new + '.png'))
for old, new in MOBS.items():
    for op, np_ in PARTS.items():
        f = os.path.join(src, 'textures', 'items', 'bodyparts', old, op + '.png')
        if os.path.exists(f):
            copy(f, os.path.join(dst, 'textures', 'item', 'bodypart', new + '_' + np_ + '.png'))
for old, new in ENTITIES.items():
    copy(os.path.join(src, 'textures', 'entities', old + '.png'), os.path.join(dst, 'textures', 'entity', new + '.png'))
for old, new in MODELS.items():
    copy(os.path.join(src, 'textures', 'models', old + '.png'), os.path.join(dst, 'textures', new + '.png'))
copy(os.path.join(src, 'textures', 'models', 'armor', 'isaacarmor.png'), os.path.join(dst, 'textures', 'models', 'armor', 'isaac_layer_1.png'))
copy(os.path.join(src, 'textures', 'guis', 'altargui.png'), os.path.join(dst, 'textures', 'gui', 'container', 'summoning_altar.png'))
copy(os.path.join(src, 'textures', 'guis', 'sewinggui.png'), os.path.join(dst, 'textures', 'gui', 'container', 'sewing_machine.png'))
copy(os.path.join(src, 'textures', 'logo.png'), os.path.join(dst, 'icon.png'))
copy(os.path.join(sys.argv[1], 'src', 'main', 'resources', 'assets', 'minecraft', 'textures', 'gui', 'container', 'creative_inventory', 'tab_necro_gui.png'),
     os.path.join(dst, 'textures', 'gui', 'container', 'creative_inventory', 'tab_necromancy.png'))
for snd in ['spawn.ogg', 'tear.ogg', 'nightcrawler/howl.ogg', 'nightcrawler/scream.ogg']:
    copy(os.path.join(src, 'sounds', snd), os.path.join(dst, 'sounds', snd))
