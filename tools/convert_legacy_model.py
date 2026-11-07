"""Turns a 1.7.10 ModelBase/ModelBiped class (ModelRenderer boxes) into a 1.21.1 LayerDefinition method.

usage: python -I tools/convert_legacy_model.py <ModelX.java> [method name]

Every ModelRenderer field becomes a root child named after the field (snake_case), unless it was attached with addChild,
in which case it becomes a child of its parent. Box offsets, rotation points, rotations, mirror and texture offsets are
copied as they are, so the result renders like the old model (ground at y = 24, y pointing down).
"""
import re
import sys

src = open(sys.argv[1], encoding='utf-8').read()
method = sys.argv[2] if len(sys.argv) > 2 else 'createBodyLayer'

num = r'(-?[\d.]+)[Ff]?'
tex_w = re.search(r'textureWidth\s*=\s*(\d+)', src)
tex_h = re.search(r'textureHeight\s*=\s*(\d+)', src)
tw = int(tex_w.group(1)) if tex_w else 64
th = int(tex_h.group(1)) if tex_h else 32

parts = {}
order = []

def snake(name):
    s = re.sub(r'(?<!^)(?=[A-Z])', '_', name).lower()
    return s.replace('__', '_')

for m in re.finditer(r'(\w+)\s*=\s*new\s+(?:ModelRenderer|BodyPart)\(\s*this\s*(?:,\s*(\d+)\s*,\s*(\d+))?\s*\)', src):
    name = m.group(1)
    parts[name] = {'u': int(m.group(2) or 0), 'v': int(m.group(3) or 0), 'boxes': [], 'pivot': (0, 0, 0), 'rot': (0, 0, 0),
                   'mirror': False, 'parent': None}
    order.append(name)

stmts = re.findall(r'(\w+)\.(addBox|setRotationPoint|setTextureOffset|setTextureSize)\(([^;]*)\);|(\w+)\.mirror\s*=\s*(true|false)|'
                   r'setRotation\(\s*(\w+)\s*,([^;]*)\);|(\w+)\.rotateAngle([XYZ])\s*=\s*([^;]*);|(\w+)\.addChild\(\s*(\w+)\s*\)', src)
for st in stmts:
    if st[0] and st[0] in parts:
        p = parts[st[0]]
        args = [a.strip() for a in st[2].split(',')]
        vals = [float(re.sub(r'[Ff]$', '', a)) if re.match(r'^-?[\d.]+[Ff]?$', a) else a for a in args]
        if st[1] == 'addBox':
            p['boxes'].append((p['u'], p['v'], vals[:6], vals[6] if len(vals) > 6 else 0.0, p['mirror']))
        elif st[1] == 'setRotationPoint':
            p['pivot'] = tuple(vals[:3])
        elif st[1] == 'setTextureOffset':
            p['u'], p['v'] = int(vals[0]), int(vals[1])
    elif st[3] and st[3] in parts:
        parts[st[3]]['mirror'] = st[4] == 'true'
    elif st[5] and st[5] in parts:
        vals = [float(re.sub(r'[Ff]$', '', a.strip())) for a in st[6].split(',')]
        parts[st[5]]['rot'] = tuple(vals[:3])
    elif st[7] and st[7] in parts:
        expr = st[9].strip()
        try:
            v = float(re.sub(r'[Ff]$', '', expr))
        except ValueError:
            continue
        r = list(parts[st[7]]['rot'])
        r['XYZ'.index(st[8])] = v
        parts[st[7]]['rot'] = tuple(r)
    elif st[10] and st[10] in parts and st[11] in parts:
        parts[st[11]]['parent'] = st[10]

def f(x):
    s = ('%.7f' % x).rstrip('0').rstrip('.')
    if s in ('-0', ''):
        s = '0'
    return s + 'F'

out = []
out.append('    public static LayerDefinition %s() {' % method)
out.append('        MeshDefinition mesh = new MeshDefinition();')
out.append('        PartDefinition root = mesh.getRoot();')
for name in order:
    p = parts[name]
    if not p['boxes']:
        continue
    parent = p['parent']
    holder = 'root' if parent is None else snake(parent)
    builder = 'CubeListBuilder.create()'
    for (u, v, b, grow, mirror) in p['boxes']:
        builder += '.texOffs(%d, %d)%s.addBox(%s, %s, %s, %s, %s, %s%s)' % (u, v, '.mirror()' if mirror else '', f(b[0]), f(b[1]), f(b[2]),
                                                                          f(b[3]), f(b[4]), f(b[5]), ', new CubeDeformation(%s)' % f(grow) if grow else '')
        if mirror:
            builder += '.mirror(false)'
    pv, rt = p['pivot'], p['rot']
    pose = 'PartPose.offsetAndRotation(%s, %s, %s, %s, %s, %s)' % (f(pv[0]), f(pv[1]), f(pv[2]), f(rt[0]), f(rt[1]), f(rt[2])) \
        if any(rt) else 'PartPose.offset(%s, %s, %s)' % (f(pv[0]), f(pv[1]), f(pv[2]))
    has_children = any(parts[c]['parent'] == name for c in order)
    prefix = 'PartDefinition %s = ' % snake(name) if has_children else ''
    out.append('        %s%s.addOrReplaceChild("%s", %s, %s);' % (prefix, holder, snake(name), builder, pose))
out.append('        return LayerDefinition.create(mesh, %d, %d);' % (tw, th))
out.append('    }')
print('\n'.join(out))
print('    // parts: ' + ', '.join(snake(n) for n in order if parts[n]['boxes'] and parts[n]['parent'] is None))
