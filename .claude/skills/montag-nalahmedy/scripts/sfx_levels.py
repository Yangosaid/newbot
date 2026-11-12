"""يحسب مستوى (volume) كل مؤثر صوتي عشان يكون أوطى من الكلام بمقدار ثابت.

ملفات المؤثرات مو بنفس العلو: ضربة بيس ممكن تكون أعلى من whoosh بـ 15 dB.
لو حطيت نفس الـ volume لكلها، بعضها يغطي على الكلام. هالسكربت يقيس أعلى لحظة
(نافذة 400ms) بكل مؤثر ويقارنها بعلو الكلام، ويطلع volume جاهز لكل ملف.

الاستخدام:
  python sfx_levels.py speech.mp4 sfx1.mp3 sfx2.mp3 ... [--below 10]
  --below: كم dB المؤثر أوطى من الكلام (10 افتراضي؛ 8 للريلز الحماسية، 12 للشرح الهادي)

الناتج: JSON {اسم_الملف: volume} — استخدمه مباشرة في Remotion أو حوّله dB لـ DaVinci.
"""
import argparse
import json
import math
import os
import subprocess

import imageio_ffmpeg
import numpy as np

FF = imageio_ffmpeg.get_ffmpeg_exe()
SR = 16000


def load(path):
    raw = subprocess.run([FF, "-loglevel", "error", "-i", path, "-ac", "1", "-ar", str(SR), "-f", "f32le", "-"],
                         capture_output=True).stdout
    return np.frombuffer(raw, np.float32)


def windows_db(x, win=0.4):
    n = int(SR * win)
    if len(x) < n:
        x = np.pad(x, (0, n - len(x)))
    hop = n // 4
    out = [20 * math.log10(np.sqrt(np.mean(x[i:i + n] ** 2)) + 1e-9) for i in range(0, len(x) - n + 1, hop)]
    return np.array(out)


p = argparse.ArgumentParser()
p.add_argument("speech")
p.add_argument("sfx", nargs="+")
p.add_argument("--below", type=float, default=10.0)
a = p.parse_args()

sp = windows_db(load(a.speech))
speech_level = float(np.percentile(sp[sp > -50], 75))  # علو الكلام وقت الكلام (مو السكتات)
print(f"علو الكلام: {speech_level:.1f} dB")
result = {}
for f in a.sfx:
    peak = float(windows_db(load(f)).max())
    target = speech_level - a.below
    vol = min(1.0, 10 ** ((target - peak) / 20))
    result[os.path.splitext(os.path.basename(f))[0]] = round(vol, 3)
    print(f"{os.path.basename(f):28s} أعلى لحظة {peak:6.1f} dB → volume {vol:.3f} ({20 * math.log10(vol):+.1f} dB)")
print(json.dumps(result, ensure_ascii=False))
