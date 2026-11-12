"""الكابشن العربي: تصحيح التفريغ، كشف الجمل المكررة، وتصدير SRT.

يشتغل على ملف الكلمات اللي يطلعه cut_silence.py (`<name>_words.json`: [{w,s,e}, ...]).

الأوامر:
  python captions.py words.json show                 # يطبع الجمل مرقّمة بالتوقيت (لعرضها على المستخدم)
  python captions.py words.json dupes                # يكشف الجمل المعادة (الأولى غالباً هي الغلط)
  python captions.py words.json fix fixes.json       # يطبّق تصحيحات: {"نايفالأحمد": "نايف الأحمدي", ...}
  python captions.py words.json srt out.srt [--max 42]   # ترجمة: سطرين كحد أقصى، ≤42 حرف للسطر
  python captions.py words.json caps out.json        # جمل قصيرة للكابشن على الشاشة (3-5 كلمات) بتوقيتها
"""
import argparse
import difflib
import json

GAP = 0.45  # فراغ أطول من هذا = نهاية جملة


def load(p):
    return json.load(open(p, encoding="utf-8"))


def sentences(words):
    out, cur = [], []
    for w in words:
        if cur and (w["s"] - cur[-1]["e"] > GAP or cur[-1]["w"].endswith((".", "؟", "!", "،"))):
            out.append(cur)
            cur = []
        cur.append(w)
    if cur:
        out.append(cur)
    return out


def text(ws):
    return " ".join(w["w"] for w in ws)


def ts(t, sep=","):
    h, r = divmod(t, 3600)
    m, s = divmod(r, 60)
    return f"{int(h):02d}:{int(m):02d}:{int(s):02d}{sep}{int(round((s % 1) * 1000)):03d}"


PHRASE_GAP = 0.18  # وقفة قصيرة = مكان طبيعي لكسر الترجمة
MAX_DUR = 4.5      # أطول مدة لشريحة ترجمة
STARTERS = ("و", "اذا", "إذا", "فانت", "فأنت", "لكن", "بس", "عشان", "لأن", "يعني")


def phrases(ws, maxc):
    """قسّم الجملة لعبارات طبيعية: عند الوقفات، أو قبل كلمات الربط، وبدون تجاوز طول/مدة الشريحة."""
    out, cur = [], []
    for i, w in enumerate(ws):
        if cur:
            gap = w["s"] - cur[-1]["e"]
            too_long = len(text(cur + [w])) > maxc * 2 or w["e"] - cur[0]["s"] > MAX_DUR
            natural = gap >= PHRASE_GAP or (w["w"] in STARTERS and len(text(cur)) > maxc * 0.6)
            if too_long or natural:
                out.append(cur)
                cur = []
        cur.append(w)
    if cur:
        out.append(cur)
    return out


def wrap(ws, maxc):
    """كل عبارة = شريحة ترجمة، مقسومة لسطرين متوازنين ≤ maxc حرف."""
    blocks = []
    for ph in phrases(ws, maxc):
        if len(text(ph)) <= maxc:
            blocks.append([ph])
            continue
        best = min(range(1, len(ph)), key=lambda k: abs(len(text(ph[:k])) - len(text(ph[k:]))))
        blocks.append([ph[:best], ph[best:]])
    return blocks


p = argparse.ArgumentParser()
p.add_argument("words")
p.add_argument("cmd", choices=["show", "dupes", "fix", "srt", "caps"])
p.add_argument("arg", nargs="?")
p.add_argument("--max", type=int, default=42)
a = p.parse_args()
words = load(a.words)
sents = sentences(words)

if a.cmd == "show":
    for i, s in enumerate(sents, 1):
        print(f"{i:>3}. [{s[0]['s']:6.2f}–{s[-1]['e']:6.2f}] {text(s)}")

elif a.cmd == "dupes":
    found = False
    for i in range(len(sents)):
        for j in range(i + 1, min(i + 3, len(sents))):
            r = difflib.SequenceMatcher(None, text(sents[i]), text(sents[j])).ratio()
            if r > 0.6:
                found = True
                print(f"مكررة ({r:.0%}): #{i+1} «{text(sents[i])}»  ↔  #{j+1} «{text(sents[j])}»  → اقترح حذف #{i+1}")
    if not found:
        print("ما فيه جمل مكررة")

elif a.cmd == "fix":
    fixes = load(a.arg)
    n = 0
    for w in words:
        if w["w"] in fixes:
            w["w"] = fixes[w["w"]]
            n += 1
    json.dump(words, open(a.words, "w", encoding="utf-8"), ensure_ascii=False, indent=0)
    print("صحّحت", n, "كلمة")

elif a.cmd == "srt":
    blocks = [b for s in sents for b in wrap(s, a.max)]
    out = []
    for k, lines in enumerate(blocks):
        ws = [w for l in lines for w in l]
        start, end = ws[0]["s"], max(ws[-1]["e"], ws[0]["s"] + 1.2)
        if k + 1 < len(blocks):  # لا تتداخل مع الترجمة اللي بعدها
            end = min(end, blocks[k + 1][0][0]["s"] - 0.02)
        out.append(f"{k + 1}\n{ts(start)} --> {ts(end)}\n" + "\n".join(text(l) for l in lines) + "\n")
    open(a.arg, "w", encoding="utf-8-sig").write("\n".join(out))
    print("كتبت", len(blocks), "ترجمة في", a.arg)

elif a.cmd == "caps":
    caps = []
    for s in sents:
        for i in range(0, len(s), 4):
            chunk = s[i:i + 4]
            caps.append({"text": text(chunk), "s": chunk[0]["s"], "e": chunk[-1]["e"],
                         "words": [{"w": w["w"], "s": w["s"], "e": w["e"]} for w in chunk]})
    json.dump(caps, open(a.arg, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print("كتبت", len(caps), "كابشن في", a.arg)
