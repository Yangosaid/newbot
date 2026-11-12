"""يختار انتقالات متنوعة بدون تكرار لفيديو واحد، ويتذكر انتقالات الفيديو السابق.

الاستخدام:
    python pick_transitions.py --count 12 --has-person yes
"""
import argparse
import json
import random
from pathlib import Path

MAGIC_MASK = ["mm_reveal", "mm_text_behind", "mm_pop_out", "mm_glow_trail", "mm_split_bg"]
MOTION = [
    "zoom_punch", "whip_pan_l", "whip_pan_up", "spin_blur", "glitch_rgb", "light_leak",
    "shape_wipe", "slide_stack", "flash_cut", "film_burn", "ink_reveal", "grid_split",
]
# انتقالات عنيفة ما تناسب المشاهد الهادية (مطر، طبيعة، قهوة، سينمائي)
ENERGETIC = {"zoom_punch", "spin_blur", "glitch_rgb", "flash_cut", "grid_split", "whip_pan_l", "whip_pan_up"}
MAX_PER_VIDEO = 2
MM_EVERY = 3  # انتقال Magic Mask واحد على الأقل كل 3 انتقالات

HISTORY = Path(__file__).with_name("history.json")


def load_history():
    try:
        return json.loads(HISTORY.read_text(encoding="utf-8"))
    except (FileNotFoundError, json.JSONDecodeError):
        return []


def pick(count, has_person, mood="energetic"):
    previous = load_history()
    motion = [t for t in MOTION if mood != "calm" or t not in ENERGETIC]
    pool = motion + (MAGIC_MASK if has_person else [])
    used = {t: 0 for t in pool}
    result = []
    last = previous[-1] if previous else None
    since_mm = 0

    for _ in range(count):
        need_mm = has_person and since_mm >= MM_EVERY - 1
        candidates = [
            t for t in (MAGIC_MASK if need_mm else pool)
            if t != last and used[t] < MAX_PER_VIDEO
        ]
        if not candidates:  # فيديو طويل جداً: نسمح بإعادة الاستخدام بس بدون تكرار متتالي
            candidates = [t for t in pool if t != last]
        # الأولوية: اللي ما استُخدم بهالفيديو، ثم اللي ما استُخدم بالفيديو السابق
        unused = [t for t in candidates if used[t] == 0] or candidates
        fresh = [t for t in unused if t not in previous]
        choice = random.choice(fresh or unused)

        result.append(choice)
        used[choice] += 1
        last = choice
        since_mm = 0 if choice in MAGIC_MASK else since_mm + 1

    HISTORY.write_text(json.dumps(result, ensure_ascii=False), encoding="utf-8")
    return result


if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("--count", type=int, required=True)
    p.add_argument("--has-person", choices=["yes", "no"], default="yes")
    p.add_argument("--mood", choices=["energetic", "calm"], default="energetic",
                   help="calm للمشاهد السينمائية الهادية (مطر، طبيعة): يستبعد الجلتش والدوران والفلاش")
    a = p.parse_args()
    for i, t in enumerate(pick(a.count, a.has_person == "yes", a.mood), 1):
        print(f"{i}. {t}")
