"""Builds the README images and GIFs from a smoke run (tools/smoke-media.txt).

usage: python -I tools/make_media.py <screenshots dir> <out dir>
Scenes are scaled to 960 px wide; screens are cropped to their window; frame bursts become looping GIFs (~10 fps).
Pictures missing from the run are skipped, so one script can redo a few of them.
"""
import glob
import os
import sys

from PIL import Image

src, out = sys.argv[1], sys.argv[2]
os.makedirs(out, exist_ok=True)

SCENES = ['hero', 'lineup', 'cemetery', 'chalice', 'orespawn_pairs', 'orespawn_chimeras']
SCREENS = {'altar_screen': 230, 'sewing_screen': 0, 'minion_screen': 0}  # extra width to the right (altar side panel)
TOOLTIPS = ['tooltip_part', 'tooltip_soul', 'tooltip_orespawn']
GIFS = {'raise': 640, 'battle': 640, 'abomination': 640, 'orespawn_battle': 640}


def save(img, name):
    path = os.path.join(out, name)
    img.save(path, optimize=True)
    print('%-22s %4dx%-4d %6.1f KB' % (name, img.width, img.height, os.path.getsize(path) / 1024))


def window_box(img, extra_right):
    """The vanilla grey window: the bounding box of its (198, 198, 198) panel colour."""
    px = img.load()
    xs, ys = [], []
    for y in range(0, img.height, 2):
        for x in range(0, img.width, 2):
            if px[x, y][:3] == (198, 198, 198):
                xs.append(x)
                ys.append(y)
    pad = 10
    return (max(0, min(xs) - pad), max(0, min(ys) - pad), min(img.width, max(xs) + pad + extra_right), min(img.height, max(ys) + pad))


def present(name):
    return os.path.exists(os.path.join(src, name + '.png'))


CROPS = {'orespawn_pairs': (40, 150, 1240, 450), 'orespawn_chimeras': (40, 140, 1240, 440), 'tooltip_orespawn': (90, 40, 1105, 290)}

for name in [n for n in SCENES if present(n)]:
    img = Image.open(os.path.join(src, name + '.png')).convert('RGB')
    if name in CROPS:
        img = img.crop(CROPS[name])
    save(img.resize((960, int(img.height * 960 / img.width)), Image.LANCZOS), name + '.png')

for name, extra in [(n, e) for n, e in SCREENS.items() if present(n)]:
    img = Image.open(os.path.join(src, name + '.png')).convert('RGB')
    save(img.crop(window_box(img, extra)), name + '.png')

if present('guide'):
    guide = Image.open(os.path.join(src, 'guide.png')).convert('RGB')
    w, h = guide.size
    save(guide.crop((w // 2 - 170, 0, w // 2 + 170, 380)), 'guide.png')

for name in [n for n in TOOLTIPS if present(n)]:
    img = Image.open(os.path.join(src, name + '.png')).convert('RGB')
    save(img.crop(CROPS.get(name, (60, 30, 700, 260))), name + '.png')

for name, width in GIFS.items():
    files = sorted(glob.glob(os.path.join(src, name + '_*.png')))
    if not files:
        continue
    frames = []
    for f in files:
        img = Image.open(f).convert('RGB')
        img = img.resize((width, int(img.height * width / img.width)), Image.LANCZOS)
        frames.append(img)
    # one shared palette keeps the colours steady between frames
    palette = frames[len(frames) // 2].quantize(colors=200, method=Image.Quantize.MEDIANCUT)
    quantized = [f.quantize(palette=palette, dither=Image.Dither.NONE) for f in frames]
    path = os.path.join(out, name + '.gif')
    quantized[0].save(path, save_all=True, append_images=quantized[1:], duration=100, loop=0, optimize=True, disposal=1)
    print('%-22s %4dx%-4d %6.1f KB (%d frames)' % (name + '.gif', width, frames[0].height, os.path.getsize(path) / 1024, len(frames)))
