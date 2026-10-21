# Renders diagram SVGs to PNG (2x) with headless Chrome, for the project book.
# Usage: python svg_to_png.py <diagrams folder> name1 [name2 ...]
import re, subprocess, sys, pathlib
from PIL import Image

CHROME = r"C:\Program Files\Google\Chrome\Application\chrome.exe"
d = pathlib.Path(sys.argv[1]).resolve()
for name in sys.argv[2:]:
    svg = (d / f"{name}.svg").read_text(encoding="utf-8")
    w, h = [float(x) for x in re.search(r'viewBox="0 0 ([\d.]+) ([\d.]+)"', svg).groups()]
    W, H = int(w) + 40, int(h) + 40
    wrap = d / f"_wrap_{name}.html"
    wrap.write_text(f'<!doctype html><body style="margin:0;background:#fff"><img src="{name}.svg" '
                    f'style="display:block;margin:20px;width:{w}px;height:{h}px"></body>', encoding="utf-8")
    raw = d / f"_{name}_raw.png"
    subprocess.run([CHROME, "--headless=new", "--disable-gpu", "--hide-scrollbars", f"--user-data-dir={d / '_prof'}",
                    "--force-device-scale-factor=2", f"--window-size={max(W, 600)},{H}", "--virtual-time-budget=5000",
                    f"--screenshot={raw}", wrap.as_uri()], capture_output=True, timeout=120)
    im = Image.open(raw).convert("RGB").crop((0, 0, W * 2, H * 2))
    im.save(d / f"{name}.png", optimize=True)
    raw.unlink()
    wrap.unlink()
    print(name, im.size)
