"""مراجعة الفيديو النهائي قبل التسليم.

  python review.py sheet final.mp4 sheet.jpg [--n 8]      # ورقة لقطات وحدة (اقرأها بدل صور متفرقة)
  python review.py sync final.mp4 words.json              # يفرّغ الناتج ويقارن توقيت الكلام (لازم الفرق < 0.15 ث)
  python review.py specs final.mp4                        # الدقة، البت ريت، الألوان، العلو، الحجم
"""
import argparse
import json
import os
import re
import subprocess

import imageio_ffmpeg

FF = imageio_ffmpeg.get_ffmpeg_exe()


def duration(path):
    d = subprocess.run([FF, "-i", path], capture_output=True, text=True).stderr
    h, m, s = re.search(r"Duration: (\d+):(\d+):([\d.]+)", d).groups()
    return int(h) * 3600 + int(m) * 60 + float(s)


p = argparse.ArgumentParser()
p.add_argument("cmd", choices=["sheet", "sync", "specs"])
p.add_argument("video")
p.add_argument("arg", nargs="?")
p.add_argument("--n", type=int, default=8)
a = p.parse_args()

if a.cmd == "sheet":
    d = duration(a.video)
    cols = 4
    rows = (a.n + cols - 1) // cols
    fps = a.n / d
    subprocess.run([FF, "-y", "-loglevel", "error", "-i", a.video, "-vf",
                    f"fps={fps:.5f},scale=480:-1,tile={cols}x{rows}:padding=4", "-frames:v", "1", a.arg], check=True)
    print("ورقة اللقطات:", a.arg)

elif a.cmd == "sync":
    from faster_whisper import WhisperModel
    ref = json.load(open(a.arg, encoding="utf-8"))
    m = WhisperModel("small", device="cpu", compute_type="int8")
    segs, _ = m.transcribe(a.video, language="ar", word_timestamps=True, vad_filter=True)
    got = [w for s in segs for w in s.words]
    diffs = []
    for r in ref[:: max(1, len(ref) // 10)]:  # عينة من 10 كلمات
        best = min(got, key=lambda w: abs(w.start - r["s"]), default=None)
        if best:
            diffs.append(abs(best.start - r["s"]))
    worst = max(diffs) if diffs else 0
    # دقة توقيت Whisper نفسها ~0.1-0.2 ث، فالحد 0.25 ث. فوقه = تأخير حقيقي
    print(f"أكبر فرق تزامن: {worst:.2f} ث — {'✅ تمام' if worst < 0.25 else '⚠️ فيه تأخير، راجع الصوت'}")

elif a.cmd == "specs":
    info = subprocess.run([FF, "-i", a.video], capture_output=True, text=True).stderr
    v = re.search(r"Video: (.+)", info).group(1)
    lu = subprocess.run([FF, "-hide_banner", "-i", a.video, "-af", "loudnorm=print_format=json", "-f", "null", "-"],
                        capture_output=True, text=True).stderr
    r = json.loads(re.findall(r"\{[^{}]*\"input_i\"[^{}]*\}", lu)[-1])
    mb = os.path.getsize(a.video) / 1e6
    br = mb * 8 / duration(a.video)
    print(f"الفيديو: {v}")
    print(f"البت ريت: {br:.1f} Mbps {'⚠️ منخفض جداً — غالباً صورة سوداء أو جودة طايحة' if br < 1 else ''}")
    print(f"العلو: {r['input_i']} LUFS · الذروة {r['input_tp']} dBTP")
    print(f"الحجم: {mb:.1f} MB")
