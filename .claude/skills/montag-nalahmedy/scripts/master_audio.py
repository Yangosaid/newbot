"""معايرة صوت الفيديو النهائي للمنصات.

- يوحّد العلو (LUFS) بقياس مرتين (loudnorm two-pass) — أدق من المرة الوحدة.
- اختياري: موسيقى/صوت خلفية ينخفض تلقائياً وقت الكلام (sidechain ducking) ويرجع بالسكتات.
- الصورة تُنسخ كما هي بلا إعادة ترميز.

الاستخدام:
  python master_audio.py in.mp4 out.mp4 [--target -14] [--bg music.mp3 --bg-level -22]
  --target: -14 للريلز/تيك توك/شورتس، -16 ليوتيوب الطويل.
"""
import argparse
import json
import re
import subprocess

import imageio_ffmpeg

FF = imageio_ffmpeg.get_ffmpeg_exe()

p = argparse.ArgumentParser()
p.add_argument("inp")
p.add_argument("out")
p.add_argument("--target", type=float, default=-14.0)
p.add_argument("--tp", type=float, default=-1.5, help="أعلى ذروة حقيقية dBTP")
p.add_argument("--bg", help="ملف صوت خلفية (موسيقى أو أجواء)")
p.add_argument("--bg-level", type=float, default=-22.0, help="علو الخلفية قبل الخفض dB")
a = p.parse_args()

# 1) ابنِ سلسلة الصوت (مع أو بدون خلفية)
if a.bg:
    inputs = ["-i", a.inp, "-stream_loop", "-1", "-i", a.bg]
    mix = (f"[1:a]volume={a.bg_level}dB,aresample=48000[bg];"
           f"[0:a]asplit=2[v][sc];"
           f"[bg][sc]sidechaincompress=threshold=0.03:ratio=8:attack=20:release=400[duck];"
           f"[v][duck]amix=inputs=2:duration=first:normalize=0")
else:
    inputs = ["-i", a.inp]
    mix = "[0:a]anull"

# 2) القياس الأول
probe = subprocess.run([FF, "-hide_banner", *inputs, "-filter_complex",
                        f"{mix},loudnorm=I={a.target}:TP={a.tp}:LRA=11:print_format=json",
                        "-f", "null", "-"], capture_output=True, text=True).stderr
m = json.loads(re.findall(r"\{[^{}]*\"input_i\"[^{}]*\}", probe)[-1])

# 3) التطبيع الفعلي بالقيم المقاسة
ln = (f"loudnorm=I={a.target}:TP={a.tp - 0.5}:LRA=11:measured_I={m['input_i']}:measured_TP={m['input_tp']}:"
      f"measured_LRA={m['input_lra']}:measured_thresh={m['input_thresh']}:offset={m['target_offset']}:linear=true,"
      f"alimiter=limit={10 ** ((a.tp - 0.3) / 20):.4f}:level=disabled")  # ليميتر يضمن الذروة تحت الحد
subprocess.run([FF, "-y", "-loglevel", "error", *inputs, "-filter_complex", f"{mix},{ln}[a]",
                "-map", "0:v", "-map", "[a]", "-c:v", "copy", "-c:a", "aac", "-b:a", "320k", "-ar", "48000",
                a.out], check=True)

# 4) تحقق
chk = subprocess.run([FF, "-hide_banner", "-i", a.out, "-af", "loudnorm=print_format=json", "-f", "null", "-"],
                     capture_output=True, text=True).stderr
r = json.loads(re.findall(r"\{[^{}]*\"input_i\"[^{}]*\}", chk)[-1])
print(f"قبل: {m['input_i']} LUFS ← بعد: {r['input_i']} LUFS · ذروة {r['input_tp']} dBTP")
