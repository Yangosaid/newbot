import React from 'react';
import {
  AbsoluteFill, Audio, Img, OffthreadVideo, Sequence, continueRender, delayRender,
  interpolate, spring, staticFile, useCurrentFrame, useVideoConfig, Easing,
} from 'remotion';
// eslint-disable-next-line @typescript-eslint/no-var-requires
const CAPS: { text: string; s: number; e: number; words: { w: string; s: number; e: number }[] }[] = require('../public/r7490/caps.json');

// ريل تجريبي بالسكيل: كل أمر قاله نايف بصوته يتنفّذ بلحظته
const PURPLE = '#8B5CF6';
const CYAN = '#22D3EE';
const DARK = '#0B0B14';
const FONT = 'Tajarib';
const s = (t: number) => Math.round(t * 30);
const ease = Easing.bezier(0.22, 1, 0.36, 1);
const clamp = { extrapolateLeft: 'clamp', extrapolateRight: 'clamp' } as const;
const glow = `0 0 24px ${PURPLE}, 0 0 60px ${PURPLE}99`;

const fh = delayRender('font');
new FontFace(FONT, `url(${staticFile('fonts/Elgharib-Tajarib-Taweel-Bold.otf')})`)
  .load().then((f) => { (document.fonts as any).add(f); continueRender(fh); });

// مستوى كل مؤثر من scripts/sfx_levels.py: كلها أوطى من الكلام بـ 10 dB
const SFX_LEVELS: Record<string, number> = {"chime": 0.325, "drop": 0.031, "heavy": 0.058, "hit": 0.074, "magic": 0.731, "pop": 0.282, "reverse": 0.065, "riser": 0.056, "whip": 0.229, "whoosh": 0.191};
const VIDEO_END = 666; // 22.2 ث
export const REEL_DURATION = VIDEO_END + 45;

// نوافذ Magic Mask (بداية النافذة بالثواني، عدد الفريمات)
const MASKS = { a: { t: 6.6, n: 78 }, b: { t: 11.0, n: 57 }, c: { t: 15.4, n: 96 } };

// ===== الزوم: بنش على ChatGPT، وزوم على الوجه عند "خلي وجهي أحسن"
function zoomAt(f: number) {
  const k = (a: number, b: number, x: number, y: number) => interpolate(f, [a, b], [x, y], { ...clamp, easing: ease });
  if (f >= s(4.6) && f < s(6.5)) return f < s(4.6) + 4 ? k(s(4.6), s(4.6) + 4, 1, 1.18) : k(s(4.6) + 4, s(4.6) + 16, 1.18, 1.08);
  if (f >= s(17.2) && f < s(20.4)) return k(s(17.2), s(18.0), 1, 1.2);
  if (f >= s(20.4) && f < s(21.0)) return k(s(20.4), s(21.0), 1.2, 1);
  return 1;
}

const Person: React.FC<{ id: 'a' | 'b' | 'c' }> = ({ id }) => {
  const f = useCurrentFrame();
  const { t, n } = MASKS[id];
  const i = f - s(t) + 1;
  if (i < 1 || i > n) return null;
  return <Img src={staticFile(`r7490/mask_${id}/p_${String(i).padStart(4, '0')}.png`)} style={{ position: 'absolute', width: '100%', height: '100%', objectFit: 'cover' }} />;
};

// ===== "غيّر الخلفية": خلفية نيون متحركة تنكشف بدائرة (mm_reveal)
const NeonBg: React.FC<{ from: number; to: number }> = ({ from, to }) => {
  const f = useCurrentFrame();
  if (f < from || f >= to) return null;
  const pin = interpolate(f, [from, from + 14], [0, 1], { ...clamp, easing: ease });
  const pout = interpolate(f, [to - 12, to], [1, 0], { ...clamp, easing: ease });
  const r = Math.min(pin, pout) * 130;
  const a = (f - from) * 1.5;
  return (
    <AbsoluteFill style={{
      clipPath: `circle(${r}% at 50% 40%)`,
      background: `radial-gradient(circle at ${50 + 25 * Math.sin(a / 40)}% ${30 + 10 * Math.cos(a / 30)}%, ${PURPLE} 0%, transparent 45%),
                   radial-gradient(circle at ${40 - 20 * Math.sin(a / 35)}% ${75}%, ${CYAN} 0%, transparent 40%), ${DARK}`,
    }}>
      {/* شبكة نيون خفيفة */}
      <AbsoluteFill style={{ backgroundImage: `linear-gradient(${PURPLE}33 2px, transparent 2px), linear-gradient(90deg, ${PURPLE}33 2px, transparent 2px)`, backgroundSize: '90px 90px', opacity: 0.6, transform: `translateY(${(f * 2) % 90}px)` }} />
    </AbsoluteFill>
  );
};

// ===== "خلفية سودا": الخلفية تسوَدّ تدريجياً مع توهج حول الشخص
const BlackBg: React.FC<{ from: number; to: number }> = ({ from, to }) => {
  const f = useCurrentFrame();
  if (f < from || f >= to) return null;
  const o = Math.min(interpolate(f, [from, from + 10], [0, 1], clamp), interpolate(f, [to - 10, to], [1, 0], clamp));
  return <AbsoluteFill style={{ background: `radial-gradient(ellipse 60% 45% at 50% 45%, #1a1030 0%, #000 70%)`, opacity: o }} />;
};

// ===== كلام كبير خلف الشخص (mm_text_behind)
const Behind: React.FC<{ text: string; from: number; to: number }> = ({ text, from, to }) => {
  const f = useCurrentFrame();
  if (f < from || f >= to) return null;
  const sc = interpolate(f - from, [0, 6], [2.2, 1], { ...clamp, easing: ease });
  const o = interpolate(f, [from, from + 3, to - 4, to], [0, 1, 1, 0], clamp);
  return (
    <AbsoluteFill style={{ justifyContent: 'center', alignItems: 'center', opacity: o }}>
      <div style={{ fontFamily: FONT, fontSize: 290, color: 'white', textShadow: glow, transform: `scale(${sc})`, marginTop: -520, direction: 'rtl', whiteSpace: 'nowrap' }}>{text}</div>
    </AbsoluteFill>
  );
};

// ===== أيقونة التطبيق: كبيرة وبألوانها الأصلية، 3 أنماط دخول مختلفة (pop / slam / neon)
const AppName: React.FC<{ name: string; sub?: string; icon: string; from: number; to: number; mode: 'pop' | 'slam' | 'neon'; color?: string }> = ({ name, sub, icon, from, to, mode, color = PURPLE }) => {
  const f = useCurrentFrame(); const { fps } = useVideoConfig();
  if (f < from || f >= to) return null;
  const l = f - from;
  const outP = interpolate(f, [to - 7, to], [0, 1], { ...clamp, easing: ease });
  const sp = spring({ frame: l, fps, config: { damping: 8, stiffness: 150 } });
  let transform = '';
  if (mode === 'pop') transform = `scale(${sp}) rotate(${interpolate(l, [0, 12], [-25, 0], clamp)}deg)`;
  if (mode === 'slam') {
    const y = interpolate(l, [0, 7], [-700, 0], { ...clamp, easing: Easing.in(Easing.quad) });
    const sq = l >= 7 && l < 11 ? 1 - 0.12 * Math.sin(((l - 7) / 4) * Math.PI) : 1;
    transform = `translateY(${y}px) scale(${1 / sq}, ${sq})`;
  }
  if (mode === 'neon') transform = `scale(${interpolate(l, [0, 10], [0.4, 1], { ...clamp, easing: ease })})`;
  const ring = interpolate(l, [0, 14], [0, 360], clamp); // حلقة نيون ترسم نفسها حول الأيقونة
  const shine = interpolate(l, [8, 20], [-120, 220], clamp); // لمعة تمر على الأيقونة
  const label = interpolate(l, [6, 14], [0, 1], { ...clamp, easing: ease });
  return (
    <div style={{ position: 'absolute', top: 210, left: 0, right: 0, display: 'flex', flexDirection: 'column', alignItems: 'center',
      opacity: 1 - outP, transform: `scale(${1 - outP * 0.3})` }}>
      <div style={{ position: 'relative', width: 300, height: 300, transform }}>
        {/* توهج بلون التطبيق */}
        <div style={{ position: 'absolute', inset: -40, borderRadius: 120, background: `radial-gradient(circle, ${color}AA, transparent 70%)`, opacity: sp }} />
        {mode === 'neon' && (
          <div style={{ position: 'absolute', inset: -16, borderRadius: 86, background: `conic-gradient(${CYAN} ${ring}deg, transparent ${ring}deg)`, WebkitMaskImage: 'radial-gradient(circle, transparent 62%, black 63%)', filter: `drop-shadow(0 0 12px ${CYAN})` }} />
        )}
        <div style={{ position: 'absolute', inset: 0, borderRadius: 66, overflow: 'hidden', boxShadow: '0 20px 50px rgba(0,0,0,0.55)' }}>
          <Img src={staticFile(`r7490/apps/${icon}.png`)} style={{ width: '100%', height: '100%' }} />
          <div style={{ position: 'absolute', top: 0, bottom: 0, width: 80, left: `${shine}%`, background: 'linear-gradient(100deg, transparent, rgba(255,255,255,0.55), transparent)', transform: 'skewX(-15deg)' }} />
        </div>
      </div>
      <div style={{ marginTop: 28, opacity: label, transform: `translateY(${(1 - label) * 30}px)`, background: `${DARK}D9`, borderRadius: 999, padding: '8px 40px', textAlign: 'center', border: `3px solid ${color}` }}>
        <div style={{ fontFamily: 'Arial Black, Arial, sans-serif', fontWeight: 900, fontSize: 64, color: 'white' }}>{name}</div>
        {sub && <div style={{ fontFamily: FONT, fontSize: 34, color: CYAN, direction: 'rtl', marginTop: -6 }}>{sub}</div>}
      </div>
    </div>
  );
};

// ===== إضاءة "وجهي أحسن": بقعة ضوء ناعمة + لمعة
const FaceGlow: React.FC<{ from: number; to: number }> = ({ from, to }) => {
  const f = useCurrentFrame();
  if (f < from || f >= to) return null;
  const o = Math.min(interpolate(f, [from, from + 15], [0, 1], clamp), interpolate(f, [to - 15, to], [1, 0], clamp));
  return <AbsoluteFill style={{ background: 'radial-gradient(ellipse 30% 18% at 50% 38%, rgba(255,240,225,0.28), transparent 70%)', mixBlendMode: 'screen', opacity: o }} />;
};

// ===== الكابشن: 4 كلمات، الكلمة المنطوقة تتظلل. داخل المنطقة الآمنة (الحافة السفلية ≤ 1500)
const Captions: React.FC<{ hide: [number, number][] }> = ({ hide }) => {
  const f = useCurrentFrame();
  const t = f / 30;
  if (hide.some(([a, b]) => t >= a && t < b)) return null;
  const c = CAPS.find((x) => t >= x.s - 0.05 && t < x.e + 0.25);
  if (!c) return null;
  const pop = interpolate(t, [c.s - 0.05, c.s + 0.1], [0.85, 1], clamp);
  return (
    <div style={{ position: 'absolute', left: 90, right: 90, bottom: 470, display: 'flex', justifyContent: 'center' }}>
      <div style={{ transform: `scale(${pop})`, direction: 'rtl', background: `${DARK}CC`, borderRadius: 26, padding: '14px 34px', fontFamily: FONT, fontSize: 70, lineHeight: 1.3, textAlign: 'center' }}>
        {c.words.map((w, i) => {
          const on = t >= w.s && t < w.e + 0.05;
          return <span key={i} style={{ color: on ? DARK : 'white', background: on ? CYAN : 'transparent', borderRadius: 14, padding: '0 10px', margin: '0 2px', whiteSpace: 'nowrap', display: 'inline-block' }}>{w.w}</span>;
        })}
      </div>
    </div>
  );
};

const EndCard: React.FC<{ from: number }> = ({ from }) => {
  const f = useCurrentFrame(); const { fps } = useVideoConfig();
  if (f < from) return null;
  const r = interpolate(f, [from, from + 14], [0, 130], { ...clamp, easing: ease }); // shape_wipe
  const b = spring({ frame: f - from - 14, fps, config: { damping: 9 } });
  return (
    <AbsoluteFill style={{ clipPath: `circle(${r}% at 50% 50%)`, background: `radial-gradient(circle at 50% 45%, #2a1a5e, ${DARK} 70%)`, justifyContent: 'center', alignItems: 'center' }}>
      <div style={{ textAlign: 'center', fontFamily: FONT, color: 'white' }}>
        <div style={{ fontSize: 130, textShadow: glow }}>نايف الأحمدي</div>
        <div style={{ fontSize: 60, color: CYAN, direction: 'ltr' }}>@nalahmedy</div>
        <div style={{ marginTop: 40, display: 'inline-block', transform: `scale(${b})`, background: '#FF0050', borderRadius: 999, padding: '16px 70px', fontSize: 66 }}>تابعني 🔔</div>
      </div>
    </AbsoluteFill>
  );
};

const Sfx: React.FC<{ at: number; file: string; vol?: number; len?: number }> = ({ at, file, vol = 0.35, len }) => (
  <Sequence from={Math.max(0, at)} durationInFrames={len}><Audio src={staticFile(`r7490/sfx/${file}.mp3`)} volume={SFX_LEVELS[file] ?? 0.1} /></Sequence>
);

export const Reel: React.FC = () => {
  const f = useCurrentFrame();
  const z = zoomAt(f);
  return (
    <AbsoluteFill style={{ background: 'black' }}>
      <AbsoluteFill style={{ transform: `scale(${z})`, transformOrigin: '50% 38%' }}>
        <OffthreadVideo src={staticFile('r7490/graded.mp4')} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
        {/* "غيّر الخلفية" */}
        <NeonBg from={s(6.9)} to={s(9.15)} />
        <Person id="a" />
        {/* "حط لي الكلام خلفي" */}
        <Behind text="الكلام" from={s(11.26)} to={s(11.8)} />
        <Behind text="خلفي" from={s(11.8)} to={s(12.85)} />
        <Person id="b" />
        {/* "حط لي خلفية سودا" */}
        <BlackBg from={s(15.92)} to={s(18.55)} />
        <Person id="c" />
        {/* "خلي وجهي أحسن" */}
        <FaceGlow from={s(17.6)} to={s(20.3)} />
      </AbsoluteFill>

      <AppName name="Claude" icon="claude" from={s(0.84)} to={s(3.7)} mode="pop" color="#D97757" />
      <AppName name="ChatGPT" icon="chatgpt" from={s(4.68)} to={s(6.5)} mode="slam" color="#10A37F" />
      <AppName name="iPhone 16" sub="مصوّر بالجوال" icon="apple" color="#A3AAAE" from={s(9.78)} to={s(10.9)} mode="neon" />
      <Captions hide={[[11.2, 12.9], [20.7, 30]]} />
      <EndCard from={s(21.2)} />

      {/* ===== المؤثرات (12 مؤثر في 23 ثانية — ضمن حد الريلز) */}
      <Sfx at={s(0.84) - 1} file="pop" />
      <Sfx at={s(4.6) - 3} file="whoosh" vol={0.4} />
      <Sfx at={s(4.68) + 5} file="hit" vol={0.35} />
      <Sfx at={s(6.9) - 2} file="reverse" vol={0.35} />
      <Sfx at={s(9.78)} file="chime" vol={0.25} />
      <Sfx at={s(11.26) - 2} file="heavy" vol={0.35} />
      <Sfx at={s(11.8) - 2} file="whip" vol={0.3} />
      <Sfx at={s(15.92) - 2} file="drop" vol={0.35} />
      <Sfx at={s(17.6)} file="magic" vol={0.2} len={60} />
      <Sfx at={s(20.5)} file="riser" vol={0.25} />
      <Sfx at={s(21.2) - 2} file="whoosh" vol={0.35} />
      <Sfx at={s(21.2) + 14} file="pop" vol={0.3} />
    </AbsoluteFill>
  );
};
