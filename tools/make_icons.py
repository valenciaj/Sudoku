"""Genera el icono de la app (vector Android) y los gráficos de Google Play (PNG).

Uso: python3 tools/make_icons.py   (necesita Pillow)
"""
from PIL import Image, ImageDraw, ImageFont

BG = "#3F51B5"
LINE = "#3F51B5"
AMBER = "#FFC107"
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"

# Tablero dentro de un lienzo de 108 (unidades del icono adaptativo).
X0, SIZE = 27.0, 54.0
CELL = SIZE / 9
HIGHLIGHT = [(1, 1), (4, 4), (7, 7), (1, 7), (7, 1)]
DIGITS = {(1, 1): "5", (4, 4): "9", (7, 7): "1", (1, 7): "3", (7, 1): "7", (0, 4): "2", (4, 0): "8",
          (4, 8): "4", (8, 4): "6", (2, 5): "1", (6, 3): "3"}


def vector_foreground():
    paths = []
    r = 4
    x1 = X0 + SIZE
    paths.append(f'<path android:fillColor="#FFFFFF" android:pathData="M{X0 + r},{X0} H{x1 - r} '
                 f'Q{x1},{X0} {x1},{X0 + r} V{x1 - r} Q{x1},{x1} {x1 - r},{x1} H{X0 + r} '
                 f'Q{X0},{x1} {X0},{x1 - r} V{X0 + r} Q{X0},{X0} {X0 + r},{X0} Z" />')
    cells = "".join(
        f"M{X0 + c * CELL:.2f},{X0 + rr * CELL:.2f} h{CELL:.2f} v{CELL:.2f} h-{CELL:.2f} Z " for rr, c in HIGHLIGHT
    )
    paths.append(f'<path android:fillColor="{AMBER}" android:pathData="{cells.strip()}" />')
    thin = " ".join(
        f"M{X0 + i * CELL:.2f},{X0} V{x1} M{X0},{X0 + i * CELL:.2f} H{x1}" for i in range(1, 9) if i % 3
    )
    thick = " ".join(f"M{X0 + i * CELL * 3:.2f},{X0} V{x1} M{X0},{X0 + i * CELL * 3:.2f} H{x1}" for i in (1, 2))
    paths.append(f'<path android:strokeColor="{LINE}" android:strokeAlpha="0.45" android:strokeWidth="0.6" '
                 f'android:pathData="{thin}" />')
    paths.append(f'<path android:strokeColor="{LINE}" android:strokeWidth="1.6" android:pathData="{thick}" />')
    body = "\n    ".join(paths)
    return f'''<?xml version="1.0" encoding="utf-8"?>
<!-- Generado por tools/make_icons.py -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    {body}
</vector>
'''


def draw_board(d, x, y, size, digits=True):
    cell = size / 9
    d.rounded_rectangle([x, y, x + size, y + size], radius=size * 0.07, fill="white")
    for rr, c in HIGHLIGHT:
        d.rectangle([x + c * cell, y + rr * cell, x + (c + 1) * cell, y + (rr + 1) * cell], fill=AMBER)
    for i in range(1, 9):
        w = max(2, int(size * 0.03)) if i % 3 == 0 else max(1, int(size * 0.008))
        p = i * cell
        d.line([x + p, y, x + p, y + size], fill=LINE, width=w)
        d.line([x, y + p, x + size, y + p], fill=LINE, width=w)
    if digits:
        font = ImageFont.truetype(FONT, int(cell * 0.7))
        for (rr, c), t in DIGITS.items():
            d.text((x + (c + 0.5) * cell, y + (rr + 0.5) * cell), t, fill="#1A237E", font=font, anchor="mm")


def play_icon(path):
    img = Image.new("RGB", (512, 512), BG)
    d = ImageDraw.Draw(img)
    draw_board(d, 76, 76, 360)
    img.save(path)


def feature_graphic(path):
    img = Image.new("RGB", (1024, 500), BG)
    d = ImageDraw.Draw(img)
    draw_board(d, 70, 60, 380)
    big = ImageFont.truetype(FONT, 96)
    small = ImageFont.truetype(FONT, 34)
    d.text((510, 150), "Sudoku", fill="white", font=big)
    d.text((514, 280), "4 niveles: de Fácil", fill="white", font=small)
    d.text((514, 325), "a Demencial", fill="white", font=small)
    d.text((514, 390), "Sin anuncios", fill=AMBER, font=small)
    img.save(path)


if __name__ == "__main__":
    with open("app/src/main/res/drawable/ic_launcher_foreground.xml", "w") as f:
        f.write(vector_foreground())
    with open("app/src/main/res/drawable/ic_launcher_background.xml", "w") as f:
        f.write('<?xml version="1.0" encoding="utf-8"?>\n'
                '<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">\n'
                f'    <solid android:color="{BG}" />\n</shape>\n')
    play_icon("play/icon-512.png")
    feature_graphic("play/feature-graphic-1024x500.png")
    print("ok")
