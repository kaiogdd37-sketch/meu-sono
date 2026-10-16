"""Roda depois de `npx cap add android`: instala o plugin nativo, a permissão e o ícone."""
import glob, os, re, shutil, sys
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.abspath(__file__))
ANDROID = os.path.join(ROOT, "android")
JAVA_DIR = os.path.join(ANDROID, "app", "src", "main", "java", "com", "meusono", "app")
RES = os.path.join(ANDROID, "app", "src", "main", "res")
MANIFEST = os.path.join(ANDROID, "app", "src", "main", "AndroidManifest.xml")

# 1) código nativo
os.makedirs(JAVA_DIR, exist_ok=True)
for name in ("SleepModePlugin.java", "MainActivity.java"):
    shutil.copy(os.path.join(ROOT, name), os.path.join(JAVA_DIR, name))

# 2) permissão do Não Perturbe
m = open(MANIFEST, encoding="utf-8").read()
perm = '<uses-permission android:name="android.permission.ACCESS_NOTIFICATION_POLICY" />'
if perm not in m:
    m = m.replace("</manifest>", "    " + perm + "\n</manifest>")
    open(MANIFEST, "w", encoding="utf-8").write(m)

# 3) ícone: usa a foto em todas as densidades (quadrado e redondo)
src = Image.open(os.path.join(ROOT, "icon.jpg")).convert("RGBA")
sizes = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
for dens, px in sizes.items():
    d = os.path.join(RES, "mipmap-" + dens)
    os.makedirs(d, exist_ok=True)
    ic = src.resize((px, px), Image.LANCZOS)
    ic.save(os.path.join(d, "ic_launcher.png"))
    ic.save(os.path.join(d, "ic_launcher_foreground.png"))
    mask = Image.new("L", (px * 4, px * 4), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, px * 4 - 1, px * 4 - 1), fill=255)
    mask = mask.resize((px, px), Image.LANCZOS)
    rd = ic.copy(); rd.putalpha(mask)
    rd.save(os.path.join(d, "ic_launcher_round.png"))
# remove o ícone adaptativo padrão para o Android usar os PNGs acima
for p in glob.glob(os.path.join(RES, "mipmap-anydpi*")):
    shutil.rmtree(p, ignore_errors=True)
print("Android configurado.")
