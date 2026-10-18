# Builds planning_doc/diagrams/screen_flow.svg: Stitch screens + dialog mockups + labeled arrows.
import base64, pathlib, sys
from xml.sax.saxutils import escape

ROOT = pathlib.Path(sys.argv[1])           # planning_doc folder
SCREENS = ROOT / "screens"
OUT = ROOT / "diagrams" / "screen_flow.svg"

W, H = 1100, 772
PW, PH = 130, 281                           # phone size (390x844 scaled)
COLS = [950, 720, 490, 260, 30]              # right-to-left
R1, R2 = 66, 458                             # phone top per row
FONT = "Segoe UI, Arial, sans-serif"
BLUE, DARK, INK, MUTED = "#0037b0", "#1e3a8a", "#111827", "#64748b"

def img_uri(name):
    return "data:image/jpeg;base64," + base64.b64encode((SCREENS / name).read_bytes()).decode()

parts = []
add = parts.append
uid = [0]

def t(x, y, s, size=9, weight=400, fill=INK, anchor="middle", rtl=True, family=FONT, extra=""):
    d = ' direction="rtl"' if rtl else ""
    add(f'<text x="{x}" y="{y}" font-family="{family}" font-size="{size}" font-weight="{weight}" '
        f'fill="{fill}" text-anchor="{anchor}"{d} {extra}>{escape(s)}</text>')

def phone(col, top, image=None, dim=False, dashed=False):
    x = COLS[col]; uid[0] += 1; cid = f"c{uid[0]}"
    add(f'<clipPath id="{cid}"><rect x="{x}" y="{top}" width="{PW}" height="{PH}" rx="14"/></clipPath>')
    if dashed:
        add(f'<rect x="{x}" y="{top}" width="{PW}" height="{PH}" rx="14" fill="#f8fafc" stroke="#94a3b8" stroke-width="2" stroke-dasharray="6 5"/>')
        return x
    add(f'<g clip-path="url(#{cid})">')
    add(f'<image href="{img_uri(image)}" x="{x}" y="{top}" width="{PW}" height="{PH}" preserveAspectRatio="xMidYMin slice"/>')
    if dim:
        add(f'<rect x="{x}" y="{top}" width="{PW}" height="{PH}" fill="#0f172a" opacity="0.55"/>')
    add('</g>')
    add(f'<rect x="{x}" y="{top}" width="{PW}" height="{PH}" rx="14" fill="none" stroke="#1f2937" stroke-width="2.5"/>')
    return x

def caption(col, top, num, title, sub, sub_ltr=True, gray=False):
    cx = COLS[col] + PW / 2
    c = MUTED if gray else INK
    # number badge on the right, title next to it
    t(cx, top - 10, f"{num} · {title}", size=11.5, weight=700, fill=c)
    t(cx, top + PH + 15, sub, size=9.5, fill=MUTED if gray else "#334155",
      rtl=not sub_ltr, family=("Consolas, monospace" if sub_ltr else FONT))

def card(x, y, w, h):
    add(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="8" fill="#ffffff" stroke="#cbd5e1"/>')

def button(x, y, w, label, primary=True, h=15):
    if primary:
        add(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="5" fill="{BLUE}"/>')
        t(x + w / 2, y + h - 4.5, label, size=7.5, weight=700, fill="#ffffff")
    else:
        add(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="5" fill="#ffffff" stroke="{BLUE}"/>')
        t(x + w / 2, y + h - 4.5, label, size=7.5, weight=600, fill=BLUE)

def field(x, y, w, hint, h=15):
    add(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="4" fill="#f8fafc" stroke="#cbd5e1"/>')
    t(x + w - 5, y + h - 4.5, hint, size=7, fill="#94a3b8", anchor="start")

def arrow(points, label=None, lx=None, ly=None, dashed=False):
    d = "M " + " L ".join(f"{px},{py}" for px, py in points)
    dash = ' stroke-dasharray="5 4"' if dashed else ""
    col = "#94a3b8" if dashed else "#334155"
    mk = "ahg" if dashed else "ah"
    add(f'<path d="{d}" fill="none" stroke="{col}" stroke-width="2"{dash} marker-end="url(#{mk})"/>')
    if label:
        lines = label.split("\n")
        tw = max(len(s) for s in lines) * 6.2 + 14
        th = 15 * len(lines) + 3
        add(f'<rect x="{lx - tw / 2}" y="{ly - th / 2}" width="{tw}" height="{th}" rx="8" fill="#ffffff" '
            f'stroke="{"#94a3b8" if dashed else "#3b5bdb"}"{dash}/>')
        for i, s in enumerate(lines):
            t(lx, ly - th / 2 + 13 + i * 15, s, size=10.5, weight=600, fill=MUTED if dashed else DARK)

add(f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {W} {H}" width="{W}" height="{H}">')
add('<defs>'
    '<marker id="ah" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">'
    '<path d="M0,0 L10,5 L0,10 z" fill="#334155"/></marker>'
    '<marker id="ahg" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">'
    '<path d="M0,0 L10,5 L0,10 z" fill="#94a3b8"/></marker></defs>')
add(f'<rect width="{W}" height="{H}" fill="#ffffff"/>')

# ---- row 1: main path -------------------------------------------------------
phone(0, R1, "01_login.jpg");      caption(0, R1, "1", "כניסה והרשמה", "LoginActivity")
phone(1, R1, "02_main.jpg");       caption(1, R1, "2", "לוח בקרה", "MainActivity")
phone(2, R1, "03_scanner.jpg");    caption(2, R1, "3", "סורק מסמכים", "ScannerActivity")
x = phone(3, R1, "03_scanner.jpg", dim=True)
caption(3, R1, "3א", "יצירת כרטיסיות ב-AI", "דיאלוג בתוך ScannerActivity", sub_ltr=False)
# progress dialog
card(x + 9, R1 + 34, PW - 18, 76)
t(x + PW / 2, R1 + 50, "סריקה חכמה", size=9, weight=700)
add(f'<circle cx="{x + PW / 2}" cy="{R1 + 68}" r="9" fill="none" stroke="#dbe4ff" stroke-width="3"/>'
    f'<path d="M {x + PW / 2} {R1 + 59} A 9 9 0 0 1 {x + PW / 2 + 9} {R1 + 68}" fill="none" stroke="{BLUE}" stroke-width="3"/>')
t(x + PW / 2, R1 + 90, "המערכת מנתחת את המסמך", size=7.3, fill="#334155")
t(x + PW / 2, R1 + 101, "ומחלצת כרטיסיות לימוד...", size=7.3, fill="#334155")
t(x + PW / 2, R1 + 126, "התשובה מגיעה מ-Gemini", size=7.3, fill="#e2e8f0")
add(f'<path d="M {x + PW / 2} {R1 + 131} L {x + PW / 2} {R1 + 143}" stroke="#e2e8f0" stroke-width="1.5" marker-end="url(#ahg)"/>')
# success dialog
card(x + 9, R1 + 150, PW - 18, 96)
t(x + PW / 2, R1 + 167, "הערכה נוצרה בהצלחה!", size=9, weight=700)
t(x + PW / 2, R1 + 181, "חולצו 12 כרטיסיות זיכרון", size=7.3, fill="#334155")
button(x + 18, R1 + 192, PW - 36, "תרגל כעת")
button(x + 18, R1 + 212, PW - 36, "חזרה למסך הראשי", primary=False)
phone(4, R1, "04_flashcards.jpg"); caption(4, R1, "4", "תרגול כרטיסיות", "FlashcardsActivity")

# ---- row 2 ------------------------------------------------------------------
x = phone(0, R2, "01_login.jpg", dim=True)
caption(0, R2, "1א", "איפוס סיסמה", "דיאלוג בתוך LoginActivity", sub_ltr=False)
card(x + 9, R2 + 84, PW - 18, 104)
t(x + PW / 2, R2 + 101, "איפוס סיסמה", size=9, weight=700)
t(x + PW / 2, R2 + 115, "הזן את האימייל שאיתו נרשמת,", size=7.2, fill="#334155")
t(x + PW / 2, R2 + 126, "ונשלח קישור לאיפוס הסיסמה", size=7.2, fill="#334155")
field(x + 17, R2 + 135, PW - 34, "אימייל")
button(x + PW / 2 + 2, R2 + 160, PW / 2 - 19, "שלח קישור")
button(x + 17, R2 + 160, PW / 2 - 19, "ביטול", primary=False)

phone(1, R2, "05_settings.jpg");   caption(1, R2, "5", "הגדרות ופרופיל", "SettingsActivity")

x = phone(2, R2, "02_main.jpg", dim=True)
caption(2, R2, "2א", "ערכה חדשה (ידנית)", "דיאלוג בתוך MainActivity", sub_ltr=False)
card(x + 9, R2 + 62, PW - 18, 150)
t(x + PW / 2, R2 + 79, "יצירת ערכת לימוד חדשה", size=8.8, weight=700)
for i, hint in enumerate(["שם ערכת הלימוד", "נושא / קורס", "שאלה ראשונה לכרטיסייה", "תשובה לכרטיסייה"]):
    field(x + 17, R2 + 88 + i * 21, PW - 34, hint)
button(x + PW / 2 + 2, R2 + 180, PW / 2 - 19, "צור ערכה")
button(x + 17, R2 + 180, PW / 2 - 19, "ביטול", primary=False)

x = phone(3, R2, dashed=True)
caption(3, R2, "6", "עריכת כרטיסייה", "אחרי 80% · Issue #28", sub_ltr=False, gray=True)
add(f'<rect x="{x}" y="{R2}" width="{PW}" height="34" rx="14" fill="#e2e8f0"/>'
    f'<rect x="{x}" y="{R2 + 20}" width="{PW}" height="14" fill="#e2e8f0"/>')
t(x + PW / 2, R2 + 22, "עריכת כרטיסייה", size=9, weight=700, fill=MUTED)
for i, (lab, hh) in enumerate([("שאלה", 52), ("תשובה", 52)]):
    yy = R2 + 50 + i * 80
    t(x + PW - 14, yy, lab, size=8, weight=600, fill=MUTED, anchor="start")
    add(f'<rect x="{x + 12}" y="{yy + 6}" width="{PW - 24}" height="{hh}" rx="5" fill="#ffffff" stroke="#cbd5e1" stroke-dasharray="3 3"/>')
add(f'<rect x="{x + 12}" y="{R2 + 216}" width="{PW - 24}" height="17" rx="5" fill="#cbd5e1"/>')
t(x + PW / 2, R2 + 228, "שמירה", size=8, weight=700, fill="#475569")
add(f'<rect x="{x + 12}" y="{R2 + 240}" width="{PW - 24}" height="17" rx="5" fill="#ffffff" stroke="#cbd5e1"/>')
t(x + PW / 2, R2 + 252, "מחיקת כרטיסייה", size=8, fill=MUTED)

x = phone(4, R2, "04_flashcards.jpg", dim=True)
caption(4, R2, "4א", "סיום הערכה", "דיאלוג בתוך FlashcardsActivity", sub_ltr=False)
card(x + 9, R2 + 84, PW - 18, 104)
t(x + PW / 2, R2 + 102, "כל הכבוד!", size=10, weight=700)
t(x + PW / 2, R2 + 117, "השלמת את כל 48 הכרטיסיות.", size=7.2, fill="#334155")
t(x + PW / 2, R2 + 128, "הרצף והיעד היומי עודכנו.", size=7.2, fill="#334155")
button(x + 18, R2 + 139, PW - 36, "חזרה ללוח הבקרה")
button(x + 18, R2 + 160, PW - 36, "תרגל שוב", primary=False)

# ---- arrows -----------------------------------------------------------------
mid = R1 + PH / 2
def hgap(c_from, c_to, label, y=mid):
    x1, x2 = COLS[c_from], COLS[c_to] + PW
    arrow([(x1 - 2, y), (x2 + 4, y)], label, (x1 + x2) / 2, y - 30)

hgap(0, 1, "התחברות\nהצליחה")
hgap(1, 2, "כפתור\nסריקה")
hgap(2, 3, "צילום או\nבחירה מהגלריה")
hgap(3, 4, "תרגל כעת")

# Main -> Flashcards over the top
cx_main, cx_cards = COLS[1] + PW / 2, COLS[4] + PW / 2
arrow([(cx_main, R1 - 28), (cx_main, 14), (cx_cards, 14), (cx_cards, R1 - 26)],
      "לחיצה על ערכה", (COLS[2] + PW / 2 + COLS[3] + PW / 2) / 2, 14)

top1, top2 = R1 + PH + 24, R2 - 30          # vertical band between the rows
ym = (top1 + top2) / 2
def vdown(col, label):
    cx = COLS[col] + PW / 2
    arrow([(cx, top1), (cx, top2)], label, cx, ym)

vdown(0, "שכחת סיסמה?")
vdown(1, "תמונת פרופיל\nאו לשונית הגדרות")
vdown(4, "סיום הערכה")
# Main -> create dialog (down-left), Settings -> Login (up-right), Flashcards -> edit (dashed)
arrow([(COLS[1] + 18, top1), (COLS[2] + PW - 18, top2)], "ערכה חדשה", (COLS[1] + COLS[2] + PW) / 2 - 2, ym + 2)
arrow([(COLS[1] + PW - 14, top2), (COLS[0] + 16, top1)], "התנתקות", (COLS[0] + COLS[1] + PW) / 2 + 4, ym + 2)
arrow([(COLS[4] + PW - 16, top1), (COLS[3] + 18, top2)], "עריכה", (COLS[3] + COLS[4] + PW) / 2, ym, dashed=True)

add('</svg>')
OUT.write_text("\n".join(parts), encoding="utf-8")
print("wrote", OUT, OUT.stat().st_size)
