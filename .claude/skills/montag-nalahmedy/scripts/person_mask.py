"""يفصل الشخص عن الخلفية لـ Magic Mask بدون رجفة.

1) BiRefNet-portrait لحواف دقيقة (الشعر والنظارة والكتف).
2) تنعيم زمني: كل ماسك = متوسط موزون للفريمات المجاورة (±2) فتختفي الرجفة.
3) Feather خفيف للحواف.

الاستخدام:
    python person_mask.py <مجلد_الفريمات> <مجلد_الإخراج> [--model birefnet-portrait|u2net_human_seg]
الفريمات بأسماء f_0001.png ... والإخراج p_0001.png ... (RGBA).
"""
import argparse
import glob
import os

import numpy as np
from PIL import Image, ImageFilter
from rembg import new_session, remove

WEIGHTS = [1, 2, 4, 2, 1]  # ±2 فريم

p = argparse.ArgumentParser()
p.add_argument("src")
p.add_argument("dst")
p.add_argument("--model", default="birefnet-portrait")
a = p.parse_args()

os.makedirs(a.dst, exist_ok=True)
session = new_session(a.model)
files = sorted(glob.glob(os.path.join(a.src, "f_*.png")))

masks = []
for i, f in enumerate(files, 1):
    m = remove(Image.open(f), session=session, only_mask=True)
    masks.append(np.asarray(m, dtype=np.float32))
    print(f"mask {i}/{len(files)}", flush=True)

half = len(WEIGHTS) // 2
for i, f in enumerate(files):
    acc, wsum = 0.0, 0.0
    for k, w in enumerate(WEIGHTS):
        j = i + k - half
        if 0 <= j < len(masks):
            acc = acc + masks[j] * w
            wsum += w
    m = Image.fromarray(np.clip(acc / wsum, 0, 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(1.2))
    im = Image.open(f).convert("RGB")
    im.putalpha(m)
    im.save(os.path.join(a.dst, os.path.basename(f).replace("f_", "p_")))
print("done", len(files))
