"""Builds src/main/lang-src/pt_br/main.json: Brazilian Portuguese for every key the English provider writes, except the
guide and feature fragments (translated by hand in their own pt_br files).

usage: python -I tools/gen_pt_br.py <project dir> <vanilla pt_br.json>

Creature names come from Minecraft's own pt_br (entity.minecraft.*) so they match the game; body parts follow the game's
pattern ("Cabeça de Zumbi"). Fails if any English key is left without a translation.
"""
import json
import os
import sys

root, vanilla_path = sys.argv[1], sys.argv[2]
vanilla = json.load(open(vanilla_path, encoding='utf-8'))
en = json.load(open(os.path.join(root, 'src/generated/resources/assets/necromancy/lang/en_us.json'), encoding='utf-8'))
frag_dir = os.path.join(root, 'src/main/lang-src')
done_elsewhere = set()
for name in os.listdir(os.path.join(frag_dir, 'en_us')):
    if name != 'main.json':
        done_elsewhere |= set(json.load(open(os.path.join(frag_dir, 'en_us', name), encoding='utf-8')))

MOD_MOBS = {'night_crawler': 'Rastejador Noturno', 'isaac': 'Isaac'}


def mob(name):
    if name in MOD_MOBS:
        return MOD_MOBS[name]
    return vanilla['entity.minecraft.' + name]


PART = {'head': 'Cabeça de %s', 'torso': 'Torso de %s', 'arm': 'Braço de %s', 'legs': 'Pernas de %s'}

T = {
    # advancements
    'advancements.necromancy.root.title': 'Conhecimento Proibido',
    'advancements.necromancy.root.description': 'Una couro, sangue e tinta num Necronomicon',
    'advancements.necromancy.bound_in_blood.title': 'Selado com Sangue',
    'advancements.necromancy.bound_in_blood.description': 'Use o Necronomicon em tábuas com dois pedregulhos em fila ao lado',
    'advancements.necromancy.its_alive.title': 'Está Vivo!',
    'advancements.necromancy.its_alive.description': 'Erga um Lacaio no Altar de Invocação',
    'advancements.necromancy.stitches.title': 'Pontos e Costuras',
    'advancements.necromancy.stitches.description': 'Construa uma Máquina de Costura',
    'advancements.necromancy.soul_reaper.title': 'Ceifador de Almas',
    'advancements.necromancy.soul_reaper.description': 'Prenda uma alma num Frasco de Vidro com o golpe final de uma foice',
    'advancements.necromancy.basement_dweller.title': 'Morador do Porão',
    'advancements.necromancy.basement_dweller.description': 'Fique com a Cabeça de Isaac',
    'advancements.necromancy.blood_bank.title': 'Banco de Sangue',
    'advancements.necromancy.blood_bank.description': 'Encha um balde de sangue',
    'advancements.necromancy.night_terror.title': 'Terror Noturno',
    'advancements.necromancy.night_terror.description': 'Mate um Rastejador Noturno',
    'advancements.necromancy.crying_shame.title': 'Chega de Choro',
    'advancements.necromancy.crying_shame.description': 'Ponha fim às lágrimas do Isaac Sangrento',
    # blocks, containers, fluid
    'block.necromancy.altar_side': 'Lateral do Altar de Invocação',
    'block.necromancy.blood': 'Sangue',
    'block.necromancy.sewing_machine': 'Máquina de Costura',
    'block.necromancy.summoning_altar': 'Altar de Invocação',
    'container.necromancy.sewing_machine': 'Máquina de Costura',
    'container.necromancy.summoning_altar': 'Altar de Invocação',
    'fluid_type.necromancy.blood': 'Sangue',
    # commands
    'commands.necromancy.minion.aggressive': 'Seus Lacaios agora atacam todo jogador que não for seu amigo',
    'commands.necromancy.minion.passive': 'Seus Lacaios agora deixam os outros jogadores em paz, a não ser os seus inimigos',
    'commands.necromancy.minion.enemy': 'Seus Lacaios vão caçar %s assim que o virem',
    'commands.necromancy.minion.friend': 'Seus Lacaios nunca vão atacar %s',
    'commands.necromancy.minion.forget': 'Seus Lacaios esqueceram %s',
    'commands.necromancy.minion.follow': '%s Lacaios seguem você',
    'commands.necromancy.minion.sit': '%s Lacaios se sentam e esperam',
    'commands.necromancy.minion.list.header': 'Você tem %s Lacaios (%s)',
    'commands.necromancy.minion.list.friend': 'Amigo: %s',
    'commands.necromancy.minion.list.enemy': 'Inimigo: %s',
    'commands.necromancy.minion.mode.aggressive': 'agressivos',
    'commands.necromancy.minion.mode.passive': 'passivos',
    # enchantment
    'enchantment.necromancy.reaping': 'Ceifa',
    'enchantment.necromancy.reaping.desc': 'Criaturas mortas com a foice entregam os órgãos com mais frequência.',
    # entities
    'entity.minecraft.villager.necromancy.necromancer': 'Necromante',
    'entity.necromancy.blood_isaac': 'Isaac Sangrento',
    'entity.necromancy.blood_tear': 'Lágrima de Sangue',
    'entity.necromancy.isaac': 'Isaac',
    'entity.necromancy.isaac_body': 'Corpo de Isaac',
    'entity.necromancy.isaac_head': 'Cabeça de Isaac',
    'entity.necromancy.minion': 'Lacaio',
    'entity.necromancy.night_crawler': 'Rastejador Noturno',
    'entity.necromancy.tear': 'Lágrima',
    'entity.necromancy.teddy': 'Ursinho Necromântico',
    # gui
    'gui.necromancy.altar.blood': 'Sangue',
    'gui.necromancy.altar.soul': 'Alma',
    'gui.necromancy.altar.minions': 'Lacaios %s / %s',
    'gui.necromancy.altar.raise': 'Erguer',
    'gui.necromancy.altar.raise.tooltip': 'Prenda a alma ao corpo e erga o seu Lacaio. O corpo, o sangue e a alma são consumidos.',
    'gui.necromancy.altar.shrine': 'Santuário %s (%s pts)',
    'gui.necromancy.altar.tier.1': 'I',
    'gui.necromancy.altar.tier.2': 'II',
    'gui.necromancy.altar.tier.3': 'III',
    'gui.necromancy.altar.weight': 'Peso %s / %s',
    'gui.necromancy.sewing.needle': 'Agulha',
    'gui.necromancy.sewing.thread': 'Linha',
    'gui.necromancy.sewing.not_ready': 'Falta agulha e linha',
    # items
    'item.necromancy.blood_bucket': 'Balde de Sangue',
    'item.necromancy.blood_isaac_spawn_egg': 'Ovo Gerador de Isaac Sangrento',
    'item.necromancy.blood_scythe': 'Foice de Sangue',
    'item.necromancy.blood_tear': 'Lágrima de Sangue',
    'item.necromancy.bone_needle': 'Agulha de Osso',
    'item.necromancy.bone_scythe': 'Foice de Osso',
    'item.necromancy.brain_on_a_stick': 'Vara com Cérebro',
    'item.necromancy.brains': 'Cérebro',
    'item.necromancy.heart': 'Coração',
    'item.necromancy.isaac_body_spawn_egg': 'Ovo Gerador de Corpo de Isaac',
    'item.necromancy.isaac_head_spawn_egg': 'Ovo Gerador de Cabeça de Isaac',
    'item.necromancy.isaac_spawn_egg': 'Ovo Gerador de Isaac',
    'item.necromancy.isaacs_head': 'Cabeça de Isaac',
    'item.necromancy.jar_of_blood': 'Frasco de Sangue',
    'item.necromancy.lungs': 'Pulmões',
    'item.necromancy.muscle': 'Músculo',
    'item.necromancy.necronomicon': 'Necronomicon',
    'item.necromancy.night_crawler_spawn_egg': 'Ovo Gerador de Rastejador Noturno',
    'item.necromancy.skin': 'Pele',
    'item.necromancy.soul_heart': 'Coração de Alma',
    'item.necromancy.soul_in_a_jar': 'Alma num Frasco',
    'item.necromancy.tear': 'Lágrima',
    'item.necromancy.teddy_spawn_egg': 'Ovo Gerador de Ursinho Necromântico',
    'itemGroup.necromancy': 'Necromancia',
    'itemGroup.necromancy.body_parts': 'Partes do Corpo',
    # JEI
    'jei.necromancy.category.sewing': 'Costura',
    'jei.necromancy.info.altar': 'Para montar um Altar de Invocação, coloque tábuas de qualquer madeira com dois pedregulhos em fila ao lado e use o Necronomicon nas tábuas. O livro se gasta e os três blocos viram o altar.\n\nDeite nele uma cabeça, um torso, braços e pernas, junte um Frasco de Sangue e uma Alma num Frasco e erga o seu Lacaio. Velas, luzes das almas e cabeças de criaturas em volta do altar deixam você manter mais Lacaios.',
    'jei.necromancy.info.jar_of_blood': 'Use uma Agulha de Osso numa criatura viva levando um Frasco de Vidro vazio para tirar um Frasco de Sangue.',
    'jei.necromancy.info.organs': 'Órgãos vêm de criaturas mortas por você ou pelos seus Lacaios: cérebros, corações, músculos e pulmões. Esqueletos, golens, slimes e espíritos não têm nenhum. Algumas criaturas têm mais de um órgão que de outros: aldeões e illagers têm mais cérebro, hoglins mais coração, ghasts mais pulmão.',
    'jei.necromancy.info.skin': 'A Pele é cortada do couro na Máquina de Costura: um couro dá oito pedaços.',
    'jei.necromancy.info.soul_in_a_jar': 'Leve um Frasco de Vidro vazio e dê o golpe final com uma foice: a alma da criatura fica presa no frasco. Criaturas mais fortes dão almas mais fortes, que erguem corpos mais pesados.',
    'jei.necromancy.sewing.needle': 'Precisa de uma Agulha de Osso no espaço da agulha; cada ponto a desgasta',
    'jei.necromancy.sewing.thread': 'Gasta uma Linha por ponto',
    # keys
    'key.categories.necromancy': 'Necromancia',
    'key.necromancy.shoot_tear': 'Chorar uma Lágrima (Cabeça de Isaac)',
    'key.necromancy.shoot_blood_tear': 'Chorar uma Lágrima de Sangue (Cabeça de Isaac)',
    # messages
    'message.necromancy.altar.needs_blood_and_soul': 'O altar tem sede: precisa de um Frasco de Sangue e de uma Alma num Frasco',
    'message.necromancy.altar.needs_body': 'Deite no altar ao menos uma cabeça, um torso e pernas',
    'message.necromancy.altar.too_heavy': 'Este corpo pesa %s; a alma só aguenta %s',
    'message.necromancy.altar.too_many': 'Os mortos recusam você: você já tem todos os Lacaios que o seu santuário permite',
    'message.necromancy.minion.bidding': '%s: Às suas ordens, mestre?',
    'message.necromancy.minion.following': '%s segue você',
    'message.necromancy.minion.obey_only': '%s só obedece a %s',
    'message.necromancy.minion.staying': '%s fica aqui',
    'message.necromancy.need_bottle': 'Você precisa de um Frasco de Vidro vazio para recolher o sangue',
    'message.necromancy.teddy.defending': 'O Ursinho monta guarda',
    'message.necromancy.teddy.sitting': 'O Ursinho fica parado',
    'message.necromancy.teddy.walking': 'O Ursinho segue você',
    # config
    'necromancy.configuration.title': 'Configurações do Necromancy',
    'necromancy.configuration.balance': 'Equilíbrio',
    'necromancy.configuration.classicStats': 'Atributos Clássicos da 1.7.1',
    'necromancy.configuration.creeperTorsoBreaksBlocks': 'Torso de Creeper Quebra Blocos',
    'necromancy.configuration.drops': 'Drops',
    'necromancy.configuration.maxMinionsPerPlayer': 'Máximo de Lacaios por Jogador',
    'necromancy.configuration.minionChat': 'Falas dos Lacaios',
    'necromancy.configuration.minions': 'Lacaios',
    'necromancy.configuration.organDropMultiplier': 'Multiplicador de Órgãos',
    'necromancy.configuration.rarityIsaacs': 'Raridade do Isaac',
    'necromancy.configuration.rarityNightCrawlers': 'Raridade do Rastejador Noturno',
    'necromancy.configuration.shrineTierCaps': 'Lacaios por Nível de Santuário',
    'necromancy.configuration.soulPotency': 'Força das Almas',
    'necromancy.configuration.spawning': 'Aparecimento',
    'necromancy.configuration.useShrineTiers': 'Níveis de Santuário',
    # souls
    'soul.necromancy.nameless': 'Uma alma sem nome (%s)',
    'soul.necromancy.of': 'Alma de %s (%s)',
    'soul.necromancy.potency.faint': 'Débil',
    'soul.necromancy.potency.lesser': 'Menor',
    'soul.necromancy.potency.common': 'Comum',
    'soul.necromancy.potency.potent': 'Potente',
    'soul.necromancy.potency.mighty': 'Poderosa',
    'soul.necromancy.potency.legendary': 'Lendária',
    # subtitles
    'subtitles.necromancy.nightcrawler.howl': 'Rastejador Noturno uiva',
    'subtitles.necromancy.nightcrawler.scream': 'Rastejador Noturno grita',
    'subtitles.necromancy.spawn': 'Lacaio se ergue',
    'subtitles.necromancy.tear': 'Lágrima respinga',
    # tags
    'tag.item.necromancy.body_parts': 'Partes do Corpo',
    'tag.item.necromancy.organs': 'Órgãos',
    'tag.item.necromancy.scythes': 'Foices',
    'tag.item.necromancy.sewing_thread': 'Linha de Costura',
    # tooltips
    'tooltip.necromancy.isaacs_head': 'Ainda está quente. Vista-a e chore lágrimas nos seus inimigos.',
    'tooltip.necromancy.part.head': 'Cabeça: %s',
    'tooltip.necromancy.part.torso': 'Torso: %s',
    'tooltip.necromancy.part.arm': 'Braço: %s',
    'tooltip.necromancy.part.legs': 'Pernas: %s',
    'tooltip.necromancy.per_arm': '%s por braço',
    'tooltip.necromancy.saddleable': 'Pode ser selado e montado',
    'tooltip.necromancy.scythe': 'O golpe final prende a alma da vítima num Frasco de Vidro vazio',
    'tooltip.necromancy.soul.cap': 'Ergue um corpo de peso %s ou menos',
    'tooltip.necromancy.soul.legendary': 'A alma de uma lenda: forte o bastante para qualquer corpo',
    'tooltip.necromancy.soul_heart': 'Coloque-o no chão e algo vai rastejar para fora dele',
    'tooltip.necromancy.stat.attack': '%s de Dano de Ataque',
    'tooltip.necromancy.stat.follow': '%s de Alcance de Visão',
    'tooltip.necromancy.stat.health': '%s de Vida',
    'tooltip.necromancy.stat.knockback': '%s de Resistência a Repulsão',
    'tooltip.necromancy.stat.speed': '%s de Velocidade',
    'tooltip.necromancy.weight': 'Peso %s',
    # traits
    'trait.necromancy.archer': 'Arqueiro',
    'trait.necromancy.archer.desc': 'Dispara flechas com o arco que segura.',
    'trait.necromancy.blink': 'Salto Sombrio',
    'trait.necromancy.blink.desc': 'Teleporta-se para perto de um alvo distante.',
    'trait.necromancy.bouncy': 'Elástico',
    'trait.necromancy.bouncy.desc': 'Quando atingido, pode arremessar o atacante para longe.',
    'trait.necromancy.climber': 'Escalador',
    'trait.necromancy.climber.desc': 'Escala paredes como uma aranha.',
    'trait.necromancy.dark_brew': 'Poção Sombria',
    'trait.necromancy.dark_brew.desc': 'Abaixo da metade da vida, bebe uma poção de Dano, que cura os mortos-vivos.',
    'trait.necromancy.fire_proof': 'À Prova de Fogo',
    'trait.necromancy.fire_proof.desc': 'Com torso e pernas à prova de fogo, o Lacaio não queima.',
    'trait.necromancy.fireballs': 'Bolas de Fogo',
    'trait.necromancy.fireballs.desc': 'Arremessa bolas de fogo em alvos a 4 a 16 blocos de distância.',
    'trait.necromancy.gold_fighter': 'Lutador Dourado',
    'trait.necromancy.gold_fighter.desc': '+2 de dano empunhando uma espada ou machado de ouro.',
    'trait.necromancy.golem_toss': 'Arremesso de Golem',
    'trait.necromancy.golem_toss.desc': 'Os golpes jogam o alvo para o alto.',
    'trait.necromancy.heavy': 'Pesado',
    'trait.necromancy.heavy.desc': 'Pesado demais para nadar; afunda feito pedra.',
    'trait.necromancy.hunger_bite': 'Mordida Podre',
    'trait.necromancy.hunger_bite.desc': 'Os golpes podem deixar o alvo com Fome.',
    'trait.necromancy.husk_bite': 'Mordida Seca',
    'trait.necromancy.husk_bite.desc': 'Todo golpe deixa o alvo com Fome.',
    'trait.necromancy.ink_cloud': 'Nuvem de Tinta',
    'trait.necromancy.ink_cloud.desc': 'Quando atingido, pode cegar o atacante com tinta.',
    'trait.necromancy.lays_eggs': 'Bota Ovos',
    'trait.necromancy.lays_eggs.desc': 'Bota um ovo a cada cinco a dez minutos.',
    'trait.necromancy.melts': 'Derrete',
    'trait.necromancy.melts.desc': 'Derrete em biomas quentes, e a água o machuca.',
    'trait.necromancy.milkable': 'Leiteiro',
    'trait.necromancy.milkable.desc': 'Ordenhe-o com um balde, uma vez por minuto.',
    'trait.necromancy.pack_hunter': 'Caçador em Bando',
    'trait.necromancy.pack_hunter.desc': '+1 de dano para cada outro Lacaio seu com cabeça de lobo por perto, até +3.',
    'trait.necromancy.poison_archer': 'Arqueiro Venenoso',
    'trait.necromancy.poison_archer.desc': 'As flechas do arco que segura levam veneno.',
    'trait.necromancy.potion_thrower': 'Atirador de Poções',
    'trait.necromancy.potion_thrower.desc': 'Arremessa poções de Lentidão, Fraqueza ou Veneno. Nunca de Dano.',
    'trait.necromancy.ram': 'Investida',
    'trait.necromancy.ram.desc': 'Investe: o primeiro golpe depois de descansar causa metade a mais de dano e joga o alvo longe.',
    'trait.necromancy.roll_up': 'Enrolar',
    'trait.necromancy.roll_up.desc': 'Perto da morte, se enrola e aguenta os golpes por alguns segundos. Uma vez por minuto.',
    'trait.necromancy.shadow_touch': 'Toque Sombrio',
    'trait.necromancy.shadow_touch.desc': 'Todo golpe aplica Decomposição no alvo por um instante.',
    'trait.necromancy.snowballs': 'Bolas de Neve',
    'trait.necromancy.snowballs.desc': 'Atira bolas de neve nos alvos próximos.',
    'trait.necromancy.stew': 'Ensopado',
    'trait.necromancy.stew.desc': 'Uma tigela dá ensopado de cogumelos, uma vez a cada cinco minutos.',
    'trait.necromancy.sun_proof': 'Imune ao Sol',
    'trait.necromancy.sun_proof.desc': 'Nunca queima à luz do dia, do que quer que o resto dele tenha sido costurado.',
    'trait.necromancy.sunburn': 'Queimadura Solar',
    'trait.necromancy.sunburn.desc': 'Queima à luz do dia, a menos que use um capacete.',
    'trait.necromancy.tears': 'Lágrimas',
    'trait.necromancy.tears.desc': 'Chora lágrimas no alvo; uma em cada cinco é de sangue.',
    'trait.necromancy.thinker': 'Pensante',
    'trait.necromancy.thinker.desc': 'Uma cabeça que ainda pensa: o Lacaio com ela pode aprender um ofício.',
    'trait.necromancy.trident_thrower': 'Lançador de Tridente',
    'trait.necromancy.trident_thrower.desc': 'Arremessa nos inimigos o tridente que segura.',
    'trait.necromancy.venom': 'Peçonha',
    'trait.necromancy.venom.desc': 'Todo golpe envenena o alvo.',
    'trait.necromancy.volatile': 'Volátil',
    'trait.necromancy.volatile.desc': 'Explode quando o Lacaio morre.',
    'trait.necromancy.water_hurts': 'Hidrofóbico',
    'trait.necromancy.water_hurts.desc': 'Água e chuva queimam a carne dele.',
    'trait.necromancy.wings': 'Asas',
    'trait.necromancy.wings.desc': 'Asas batendo: cai devagar e sofre menos dano de queda.',
    'trait.necromancy.withering': 'Decompositor',
    'trait.necromancy.withering.desc': 'Todo golpe aplica Decomposição no alvo.',
    'trait.necromancy.woolly': 'Lanoso',
    'trait.necromancy.woolly.desc': 'Tosquie-o para tirar lã; ela cresce de novo em dois minutos.',
}

out = {}
missing = []
for key in en:
    if key in done_elsewhere:
        continue
    if key.startswith('necromob.necromancy.'):
        out[key] = mob(key.split('.')[-1])
        continue
    if key.startswith('item.necromancy.'):
        name = key.split('.')[-1]
        mob_name, _, part = name.rpartition('_')
        if part in PART and mob_name and key not in T:
            out[key] = PART[part] % mob(mob_name)
            continue
    if key in T:
        out[key] = T[key]
    else:
        missing.append(key)
if missing:
    raise SystemExit('untranslated: %s' % missing)
extra = [k for k in T if k not in en]
if extra:
    print('note: table has keys the English file does not:', extra)
path = os.path.join(frag_dir, 'pt_br', 'main.json')
json.dump(out, open(path, 'w', encoding='utf-8', newline='\n'), ensure_ascii=False, indent=2, sort_keys=True)
print('wrote %d keys to %s' % (len(out), path))
