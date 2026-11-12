"""يولّد مكتبة مؤثرات صوتية أساسية بالكود — للمستخدم اللي ما عنده مؤثرات.

بدون تحميل وبدون حقوق: كل الأصوات مركّبة رياضياً (ضوضاء مفلترة + نغمات).
يطلع نفس تصنيفات sfx.md، فالسكيل يشتغل عليها مباشرة.

الاستخدام:
  python make_sfx.py <مجلد_الإخراج>           # مثال: python make_sfx.py sfx
  python make_sfx.py sfx --ambient 60          # + صوت خلفية هادي 60 ثانية (مطر خفيف / جو)
"""
import argparse
import os
import wave

import numpy as np

SR = 48000
rng = np.random.default_rng(7)


def t(sec):
    return np.linspace(0, sec, int(SR * sec), endpoint=False)


def env(n, attack, release):
    """غلاف صوت: صعود سريع ثم نزول أُسّي."""
    a = int(SR * attack)
    e = np.ones(n)
    e[:a] = np.linspace(0, 1, a) if a else 1
    e[a:] = np.exp(-np.linspace(0, release, n - a))
    return e


def lowpass(x, cutoff):
    """فلتر تمرير منخفض بسيط (بالتردد عبر FFT)."""
    X = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / SR)
    X[f > cutoff] *= np.exp(-(f[f > cutoff] - cutoff) / (cutoff * 0.3))
    return np.fft.irfft(X, len(x))


def sweep_noise(sec, f0, f1):
    """ضوضاء يتحرك ترددها (أساس الـ whoosh)."""
    n = int(SR * sec)
    noise = rng.standard_normal(n)
    out = np.zeros(n)
    chunks = 24
    for i in range(chunks):
        s, e = i * n // chunks, (i + 1) * n // chunks
        fc = f0 + (f1 - f0) * (i / chunks)
        out[s:e] = lowpass(noise, fc)[s:e]
    return out


def norm(x, peak=0.8):
    m = np.max(np.abs(x)) or 1
    return x / m * peak


def save(path, x):
    x = norm(x)
    fade = min(len(x), int(SR * 0.005))
    x[-fade:] *= np.linspace(1, 0, fade)
    data = (np.stack([x, x], 1) * 32767).astype(np.int16)
    with wave.open(path, "wb") as w:
        w.setnchannels(2)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(data.tobytes())


def build(out, ambient):
    def p(cat, name):
        d = os.path.join(out, cat)
        os.makedirs(d, exist_ok=True)
        return os.path.join(d, name + ".wav")

    # Swoosh & Whoosh
    for i, (sec, f0, f1) in enumerate([(0.5, 300, 4000), (0.35, 600, 6000), (0.8, 200, 2500)], 1):
        x = sweep_noise(sec, f0, f1)
        n = len(x)
        save(p("swoosh_whoosh", f"whoosh_{i}"), x * np.sin(np.linspace(0, np.pi, n)) ** 2)
    save(p("swoosh_whoosh", "whip"), sweep_noise(0.18, 1500, 9000) * env(int(SR * 0.18), 0.03, 6))

    # Impact (ضربة سينمائية: بيس نازل + ضوضاء)
    tt = t(1.6)
    boom = np.sin(2 * np.pi * (60 * tt - 12 * tt ** 2)) * env(len(tt), 0.003, 5)
    save(p("impact", "cinematic_hit"), boom + 0.3 * lowpass(rng.standard_normal(len(tt)), 900) * env(len(tt), 0.001, 12))
    rev = boom[::-1].copy()
    save(p("impact", "reverse_impact"), rev)

    # Sub
    tt = t(2.5)
    save(p("sub", "sub_drop"), np.sin(2 * np.pi * (55 * tt - 8 * tt ** 2)) * env(len(tt), 0.01, 2.5))

    # Notifications (pop / ding)
    tt = t(0.12)
    save(p("notifications", "pop"), np.sin(2 * np.pi * (900 - 2500 * tt) * tt) * env(len(tt), 0.001, 30))
    tt = t(0.9)
    save(p("notifications", "ding"), (np.sin(2 * np.pi * 1760 * tt) + 0.4 * np.sin(2 * np.pi * 2640 * tt)) * env(len(tt), 0.002, 6))

    # Keyboard (نقرات قصيرة متتالية)
    n = int(SR * 1.2)
    x = np.zeros(n)
    pos = 0
    while pos < n - 2000:
        click = lowpass(rng.standard_normal(900), 5000) * env(900, 0.0005, 18)
        x[pos:pos + 900] += click * rng.uniform(0.6, 1)
        pos += int(SR * rng.uniform(0.07, 0.14))
    save(p("keyboard", "typing"), x)

    # Mouse
    save(p("mouse", "click"), lowpass(rng.standard_normal(1200), 7000) * env(1200, 0.0003, 25))

    # Riser (نغمة تصعد + ضوضاء تكبر)
    tt = t(2.0)
    riser = np.sin(2 * np.pi * (200 * tt + 400 * tt ** 2)) * (tt / 2) ** 2
    save(p("riser", "riser"), riser + 0.5 * sweep_noise(2.0, 500, 8000) * (tt / 2) ** 2)

    # Magic (لمعة: نغمات عالية متتالية)
    tt = t(1.5)
    x = np.zeros(len(tt))
    for k, f in enumerate([1318, 1568, 1976, 2637]):
        s = int(k * SR * 0.08)
        seg = tt[: len(tt) - s]
        x[s:] += np.sin(2 * np.pi * f * seg) * env(len(seg), 0.002, 5)
    save(p("magic", "shimmer"), x)

    # Camera (شتر)
    a = lowpass(rng.standard_normal(2400), 6000) * env(2400, 0.0005, 15)
    shutter = np.concatenate([a, np.zeros(int(SR * 0.05)), a * 0.7])
    save(p("camera", "shutter"), shutter)

    # Glitch
    n = int(SR * 0.4)
    g = np.sign(np.sin(2 * np.pi * rng.uniform(80, 900) * t(0.4))) * (rng.random(n) > 0.4)
    save(p("glitch", "glitch"), g * 0.6 + 0.4 * rng.standard_normal(n) * (rng.random(n) > 0.7))

    # Ambient (اختياري): مطر خفيف / جو هادي
    if ambient:
        n = int(SR * ambient)
        rain = lowpass(rng.standard_normal(n), 3500) * 0.25
        drops = np.zeros(n)
        for pos in rng.integers(0, n - 600, size=int(ambient * 40)):
            drops[pos:pos + 600] += lowpass(rng.standard_normal(600), 6000) * env(600, 0.0002, 20) * rng.uniform(0.1, 0.4)
        pad_t = np.arange(n) / SR
        pad = 0.08 * (np.sin(2 * np.pi * 110 * pad_t) + np.sin(2 * np.pi * 164.8 * pad_t)) * (0.6 + 0.4 * np.sin(2 * np.pi * 0.05 * pad_t))
        save(p("ambient", "soft_rain"), rain + drops)
        save(p("ambient", "calm_pad"), pad + 0.3 * rain)

    print("تم توليد المؤثرات في:", os.path.abspath(out))


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("out")
    ap.add_argument("--ambient", type=float, default=0, help="طول صوت الخلفية بالثواني (0 = بدون)")
    a = ap.parse_args()
    build(a.out, a.ambient)
