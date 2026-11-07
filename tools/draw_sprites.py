"""Draws the 16x16 item sprites that the 1.7 mod never had (Bone Scepter, Phylactery, Minion Remains).

usage: python -I tools/draw_sprites.py <project dir> [preview.png]
"""
import os
import sys

from PIL import Image

PALETTE = {
    '.': (0, 0, 0, 0),
    'o': (46, 34, 24, 255),      # outline
    'b': (237, 227, 200, 255),   # bone light
    'B': (207, 195, 160, 255),   # bone mid
    'd': (168, 154, 118, 255),   # bone dark
    'r': (230, 40, 40, 255),     # glowing eye
    'R': (130, 14, 14, 255),     # dark red
    'k': (26, 26, 26, 255),      # socket
    'g': (250, 214, 92, 255),    # gold light
    'G': (184, 134, 11, 255),    # gold dark
    'w': (214, 240, 240, 255),   # glass edge
    'c': (95, 224, 232, 255),    # soul
    'C': (30, 159, 176, 255),    # deep soul
    's': (194, 122, 90, 255),    # skin
    'S': (150, 84, 60, 255),     # skin dark
    'x': (120, 120, 140, 255),   # stitches
}

SPRITES = {
    'bone_scepter': [
        "...........ooo..",
        "..........obbbo.",
        ".........obrbrbo",
        ".........obbbbbo",
        ".........oBbkbBo",
        "..........oBBBo.",
        ".........obdo...",
        "........obdo....",
        ".......obdo.....",
        "......obdo......",
        ".....obdo.......",
        "....obdo........",
        "..oobdo.........",
        ".obbbo..........",
        ".obBo...........",
        "..oo............",
    ],
    'phylactery': [
        "......oooo......",
        ".....ogggGo.....",
        "......oGGo......",
        ".....oowwoo.....",
        "....owwccwwo....",
        "...owcccccCwo...",
        "...owccCccccwo..",
        "...owcccccCcwo..",
        "...owcCccccCwo..",
        "...owwcccCcwwo..",
        "....owwwwwwwo...",
        "...oggggggggGo..",
        "....oGGGGGGGo...",
        ".....ooooooo....",
        "................",
        "................",
    ],
    'minion_remains': [
        "................",
        "................",
        "................",
        "................",
        "......oooo......",
        ".....obbbbo.....",
        "....obkbbkbo....",
        "....obbbbbbo....",
        ".....obBBbo.....",
        "..oo..oooo..oo..",
        ".obbo.osxso.obbo",
        "..obbbossssobbo.",
        "...obbbsSsbbbo..",
        "..obbo.oooo.obbo",
        "..oo........oo..",
        "................",
    ],
}


def main():
    root = sys.argv[1]
    out_dir = os.path.join(root, 'src', 'main', 'resources', 'assets', 'necromancy', 'textures', 'item')
    previews = []
    for name, rows in SPRITES.items():
        assert len(rows) == 16 and all(len(r) == 16 for r in rows), name
        img = Image.new('RGBA', (16, 16))
        for y, row in enumerate(rows):
            for x, ch in enumerate(row):
                img.putpixel((x, y), PALETTE[ch])
        img.save(os.path.join(out_dir, name + '.png'))
        previews.append(img)
    if len(sys.argv) > 2:
        sheet = Image.new('RGBA', (len(previews) * 18, 16), (70, 70, 90, 255))
        for i, img in enumerate(previews):
            sheet.alpha_composite(img, (i * 18, 0))
        sheet.resize((sheet.width * 10, sheet.height * 10), Image.NEAREST).save(sys.argv[2])


if __name__ == '__main__':
    main()
