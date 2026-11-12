"""يقص السكتات من فيديو كلام، اعتماداً على توقيت الكلمات (أدق من قياس مستوى الصوت).

- يفرّغ الكلام بـ faster-whisper بتوقيت كل كلمة.
- أي فراغ بين كلمتين أطول من --gap يُقص، ويترك --pad حول الكلام عشان ما ينقطع نفَس أو آخر حرف.
- يحذف عبارات التصوير مثل "3 2 1 أكشن" من البداية.
- يطلع: الفيديو المقصوص + words.json بتوقيتات الفيديو الجديد (جاهز لخطة المونتاج).

الاستخدام:
  python cut_silence.py in.mp4 out.mp4 [--gap 0.4] [--pad 0.12]
"""
import argparse
import json
import re
import subprocess

import imageio_ffmpeg
from faster_whisper import WhisperModel

FF = imageio_ffmpeg.get_ffmpeg_exe()
SLATE = re.compile(r"^(\d|واحد|اثنين|ثلاث|ثلاثة|اكشن|أكشن|action)[.,،!]*$", re.I)

p = argparse.ArgumentParser()
p.add_argument("inp")
p.add_argument("out")
p.add_argument("--gap", type=float, default=0.4)
p.add_argument("--pad", type=float, default=0.12)
p.add_argument("--fps", type=float, default=0, help="0 = نفس فريم ريت المصدر (الافتراضي)")
a = p.parse_args()

model = WhisperModel("medium", device="cpu", compute_type="int8")
segs, info = model.transcribe(a.inp, language="ar", word_timestamps=True, vad_filter=True)
words = [dict(w=w.word.strip(), s=w.start, e=w.end) for s in segs for w in s.words]

# شيل عبارات العد والأكشن من البداية
while words and SLATE.match(words[0]["w"]):
    words.pop(0)
if not words:
    raise SystemExit("ما لقيت كلام في الفيديو")

# ادمج الكلمات المتقاربة في مقاطع
keep = []
start, end = words[0]["s"], words[0]["e"]
for w in words[1:]:
    if w["s"] - end > a.gap:
        keep.append((start, end))
        start = w["s"]
    end = w["e"]
keep.append((start, end))
keep = [(max(0, s - a.pad), e + a.pad) for s, e in keep]

# بناء الفلتر
parts, labels = [], ""
for i, (s, e) in enumerate(keep):
    parts.append(f"[0:v]trim={s:.3f}:{e:.3f},setpts=PTS-STARTPTS[v{i}];"
                 f"[0:a]atrim={s:.3f}:{e:.3f},asetpts=PTS-STARTPTS,afade=t=in:d=0.01,afade=t=out:st={e - s - 0.01:.3f}:d=0.01[a{i}];")
    labels += f"[v{i}][a{i}]"
fc = "".join(parts) + f"{labels}concat=n={len(keep)}:v=1:a=1[v][a]"
# يحافظ على الفريم ريت والدقة الأصلية (24fps يبقى 24)، وصوت المستخدم بدون ضغط إضافي
rate = ["-r", str(a.fps)] if a.fps else []
subprocess.run([FF, "-y", "-loglevel", "error", "-i", a.inp, "-filter_complex", fc,
                "-map", "[v]", "-map", "[a]", *rate, "-c:v", "libx264", "-preset", "slow", "-crf", "14",
                "-pix_fmt", "yuv420p", "-color_primaries", "bt709", "-color_trc", "bt709", "-colorspace", "bt709",
                "-c:a", "pcm_s24le" if a.out.lower().endswith(".mov") else "aac", *([] if a.out.lower().endswith(".mov") else ["-b:a", "320k"]),
                a.out], check=True)

# توقيت الكلمات على الفيديو الجديد
offset, out_words = 0.0, []
for s, e in keep:
    for w in words:
        if s <= w["s"] < e:
            out_words.append(dict(w=w["w"], s=round(w["s"] - s + offset, 2), e=round(w["e"] - s + offset, 2)))
    offset += e - s
json.dump(out_words, open(a.out.rsplit(".", 1)[0] + "_words.json", "w", encoding="utf-8"), ensure_ascii=False, indent=0)

removed = info.duration - offset
print(f"قبل {info.duration:.1f}ث ← بعد {offset:.1f}ث (انقص {removed:.1f}ث في {len(keep) - 1} قصة)")
