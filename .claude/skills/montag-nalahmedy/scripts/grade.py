"""تلوين الفيديو بهوية نايف، مع حماية لون البشرة (البشرة تاخذ إضاءة التلوين فقط بدون ألوانه).

الستايلات المعتمدة:
  neon      : نيون الهوية (بنفسجي + أزرق). الافتراضي.
  warm_cine : سينمائي دافي (تيل بالظلال، ذهبي بالهايلايت، أسود عميق).

الاستخدام:
  python grade.py in.mp4 out.mp4 --look neon [--strength 1.0] [--skin 0.85]
  python grade.py in.mp4 out.mp4 --lut "path.cube"   # لوت خارجي بدل الستايلات
  أضف --preview 3 لتلوين أول 3 ثواني فقط وتطليع صورة مقارنة قبل/بعد.
"""
import argparse
import subprocess

import cv2
import imageio_ffmpeg
import numpy as np

FF = imageio_ffmpeg.get_ffmpeg_exe()

# الستايلات المعتمدة من نايف
LOOKS = {
    # سينمائي دافي: تيل بالظلال وذهبي بالهايلايت، أسود عميق
    "warm_cine": "eq=contrast=1.15:saturation=0.95:gamma=0.95,"
                 "colorbalance=rs=-0.08:gs=0.0:bs=0.10:rm=0.03:bm=-0.03:rh=0.08:gh=0.03:bh=-0.08,"
                 "curves=all='0/0.02 0.25/0.2 0.5/0.5 0.8/0.84 1/0.97',vignette=PI/6",
    # نيون الهوية: يبرز البنفسجي والأزرق اللي في هوية نايف
    "neon":"eq=contrast=1.12:saturation=1.2:gamma=0.94,"
            "colorbalance=rs=0.04:bs=0.12:rm=0.02:bm=0.08:rh=-0.02:bh=0.04,"
            "huesaturation=hue=0:saturation=0.25:colors=b+m,vignette=PI/5",
}


def probe(path):
    r = imageio_ffmpeg.read_frames(path)
    meta = next(r)
    r.close()
    return meta["size"], meta["fps"]


def skin_mask(rgb):
    """ماسك البشرة (0..1) من نطاق YCrCb، يُحسب على نسخة مصغرة ثم يُكبّر بنعومة."""
    h, w = rgb.shape[:2]
    small = cv2.resize(rgb, (w // 4, h // 4))
    ycc = cv2.cvtColor(small, cv2.COLOR_RGB2YCrCb)
    y, cr, cb = ycc[..., 0], ycc[..., 1].astype(np.int16), ycc[..., 2].astype(np.int16)
    m = ((cr > 135) & (cr < 175) & (cb > 80) & (cb < 128) & (y > 50)).astype(np.float32)
    m = cv2.morphologyEx(m, cv2.MORPH_OPEN, np.ones((3, 3), np.uint8))
    m = cv2.GaussianBlur(m, (0, 0), 3)
    return cv2.resize(m, (w, h))[..., None]


def run(a):
    (w, h), fps = probe(a.inp)
    limit = ["-t", str(a.preview)] if a.preview else []

    if a.look:
        vf = LOOKS[a.look]
    else:
        lut = a.lut.replace("\\", "/").replace(":", "\\:").replace("'", "\\'")
        vf = f"lut3d=file='{lut}':interp=tetrahedral"

    def reader(filters):
        cmd = [FF, "-loglevel", "error", "-i", a.inp, *limit]
        if filters:
            cmd += ["-vf", filters]
        cmd += ["-f", "rawvideo", "-pix_fmt", "rgb24", "-"]
        return subprocess.Popen(cmd, stdout=subprocess.PIPE)

    orig, graded = reader(None), reader(vf)
    enc = subprocess.Popen([
        FF, "-y", "-loglevel", "error",
        "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", f"{w}x{h}", "-r", str(fps), "-i", "-",
        "-i", a.inp, *limit, "-map", "0:v", "-map", "1:a?",
        "-c:v", "libx264", "-crf", "16", "-pix_fmt", "yuv420p", "-c:a", "copy", a.out,
    ], stdin=subprocess.PIPE)

    n = w * h * 3
    i = 0
    preview_pair = None
    while True:
        o, g = orig.stdout.read(n), graded.stdout.read(n)
        if len(o) < n or len(g) < n:
            break
        o = np.frombuffer(o, np.uint8).reshape(h, w, 3).astype(np.float32)
        g = np.frombuffer(g, np.uint8).reshape(h, w, 3).astype(np.float32)
        out = o + (g - o) * a.strength
        if a.skin > 0:
            m = skin_mask(o.astype(np.uint8)) * a.skin
            # البشرة: لونها الأصلي، مع أخذ إضاءة اللوت فقط عشان تندمج مع الصورة
            lum_o = o.mean(axis=2, keepdims=True)
            lum_g = out.mean(axis=2, keepdims=True)
            skin = o * (lum_g + 1) / (lum_o + 1)
            out = out * (1 - m) + skin * m
        out = np.clip(out, 0, 255).astype(np.uint8)
        enc.stdin.write(out.tobytes())
        if a.preview and i == int(fps):
            preview_pair = (o.astype(np.uint8), out)
        i += 1

    enc.stdin.close()
    enc.wait()
    if preview_pair:
        cmp = np.hstack(preview_pair)
        cv2.imwrite(a.out.rsplit(".", 1)[0] + "_compare.jpg", cv2.cvtColor(cmp, cv2.COLOR_RGB2BGR))
    print("done", i, "frames")


if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("inp")
    p.add_argument("out")
    p.add_argument("--lut")
    p.add_argument("--look", choices=list(LOOKS))
    p.add_argument("--strength", type=float, default=1.0)
    p.add_argument("--skin", type=float, default=0.85)
    p.add_argument("--preview", type=float, default=0)
    a = p.parse_args()
    if not (a.lut or a.look):
        a.look = "neon"
    run(a)
