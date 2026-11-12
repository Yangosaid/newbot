import React from 'react';
import {
  AbsoluteFill, Audio, OffthreadVideo, Sequence, continueRender, delayRender,
  interpolate, staticFile, useCurrentFrame, Easing,
} from 'remotion';

const PURPLE = '#8B5CF6';
const FONT = 'Tajarib';
const T = 16; // طول الانتقال بالفريم
const ease = Easing.bezier(0.45, 0, 0.2, 1);

const fh = delayRender('font');
new FontFace(FONT, `url(${staticFile('fonts/Elgharib-Tajarib-Taweel-Bold.otf')})`)
  .load().then((f) => { (document.fonts as any).add(f); continueRender(fh); });

type Tr = 'shape_wipe' | 'ink_reveal' | 'slide_stack' | 'light_leak' | 'film_burn';

// اللقطات بالترتيب الزمني للرحلة (نهار ← مطر ← ليل)، ولا زاوية تتكرر.
// h_XX = 4K ملوّنة مباشرة من ملفات الـ 10-bit الأصلية (ضغطة وحدة قبل الرندر)
const SHOTS: { file: string; dur: number }[] = [
  { file: 'h_00', dur: 90 }, // الطريق والسماء
  { file: 'h_01', dur: 75 }, // الجبال
  { file: 'h_02', dur: 66 }, // اليد على الطارة
  { file: 'h_03', dur: 78 }, // شاحنة بالأنوار
  { file: 'h_04', dur: 78 }, // المساحات (سلو موشن)
  { file: 'h_05', dur: 90 }, // قطرات المطر (سلو موشن)
  { file: 'h_06', dur: 66 }, // طريق ضبابي
  { file: 'h_07', dur: 78 }, // شاحنة من المراية
  { file: 'h_08', dur: 75 }, // الطبلون والعاصفة
  { file: 'h_09', dur: 69 }, // اللوحة الإرشادية
  { file: 'h_10', dur: 75 }, // أنوار الليل
  { file: 'h_11', dur: 108 }, // النهاية: أضواء المدينة
];
// الانتقال بعد اللقطة رقم (من pick_transitions --mood calm)؛ الباقي قطع مباشر
const TRANS: Record<number, Tr> = {
  0: 'shape_wipe', 3: 'ink_reveal', 5: 'slide_stack', 7: 'light_leak', 9: 'film_burn', 10: 'ink_reveal',
};

const STARTS: number[] = [];
SHOTS.forEach((s, i) => {
  STARTS.push(i === 0 ? 0 : STARTS[i - 1] + SHOTS[i - 1].dur - (TRANS[i - 1] ? T : 0));
});
const VIDEO_END = STARTS[SHOTS.length - 1] + SHOTS[SHOTS.length - 1].dur;
export const RAIN_DURATION = VIDEO_END + 40;

// أوقات اللقطات في تايملاين DaVinci (لطبقة العناوين الشفافة)
export const DV_STARTS = [0, 89, 163, 228, 305, 381, 469, 534, 611, 685, 753, 827];
export const DV_END = 934;

const clamp = { extrapolateLeft: 'clamp', extrapolateRight: 'clamp' } as const;

const Shot: React.FC<{ i: number }> = ({ i }) => {
  const f = useCurrentFrame(); // محلي داخل Sequence
  const { file, dur } = SHOTS[i];
  const push = interpolate(f, [0, dur], [1.0, 1.06]); // دفع بطيء سينمائي
  const inTr = i > 0 ? TRANS[i - 1] : undefined;
  const outTr = TRANS[i];
  const pin = interpolate(f, [0, T], [0, 1], { ...clamp, easing: ease });
  const pout = interpolate(f, [dur - T, dur], [0, 1], { ...clamp, easing: ease });

  const style: React.CSSProperties = { position: 'absolute', inset: 0 };
  let z = 1;
  if (inTr && f < T) {
    z = 2;
    if (inTr === 'shape_wipe') style.clipPath = `circle(${pin * 120}% at 50% 55%)`;
    if (inTr === 'ink_reveal') {
      const r = pin * 160;
      style.WebkitMaskImage = `radial-gradient(circle at 65% 45%, black ${r}%, transparent ${r + 18}%)`;
    }
    if (inTr === 'light_leak' || inTr === 'film_burn') style.opacity = pin;
    if (inTr === 'slide_stack') z = 1;
  }
  if (outTr === 'slide_stack' && f > dur - T) {
    z = 3;
    style.transform = `translateX(${-pout * 105}%) scale(${1 - pout * 0.12})`;
    style.boxShadow = '0 0 160px rgba(0,0,0,0.8)';
  }
  return (
    <div style={{ ...style, zIndex: z, overflow: 'hidden' }}>
      <OffthreadVideo src={staticFile(`rain/${file}.mp4`)} muted
        style={{ width: '100%', height: '100%', transform: `scale(${push})` }} />
    </div>
  );
};

// وهج ضوء/حرق فيلم فوق الانتقالات
const Flares: React.FC = () => {
  const f = useCurrentFrame();
  return (
    <>
      {Object.entries(TRANS).map(([k, t]) => {
        const i = Number(k);
        if (t !== 'light_leak' && t !== 'film_burn') return null;
        const s = STARTS[i + 1];
        const p = interpolate(f, [s - 4, s + T / 2, s + T + 4], [0, 1, 0], clamp);
        if (p <= 0) return null;
        const bg = t === 'light_leak'
          ? `linear-gradient(${100 + p * 40}deg, transparent 10%, ${PURPLE}aa 45%, #ffb36baa 60%, transparent 85%)`
          : `radial-gradient(circle at 80% 30%, #fff6e0 0%, #ff8a2a 25%, #7a1d00 55%, transparent 75%)`;
        return <AbsoluteFill key={k} style={{ background: bg, opacity: p * (t === 'film_burn' ? 0.95 : 0.8), mixBlendMode: 'screen', zIndex: 5 }} />;
      })}
    </>
  );
};

// ===== العناصر التالية مصممة على 1920×1080 وتتكبّر ×2 لـ 4K

// عنوان يرسم نفسه من اليمين (neon_draw)
const Title: React.FC<{ text: string; sub?: string; from: number; to: number; size?: number; backdrop?: boolean }> = ({ text, sub, from, to, size = 260, backdrop = true }) => {
  const f = useCurrentFrame();
  if (f < from || f >= to) return null;
  const draw = interpolate(f, [from, from + 22], [0, 1], { ...clamp, easing: ease });
  const fill = interpolate(f, [from + 16, from + 34], [0, 1], clamp);
  const out = interpolate(f, [to - 14, to], [1, 0], clamp);
  const subIn = interpolate(f, [from + 26, from + 40], [0, 1], clamp);
  return (
    <AbsoluteFill style={{ justifyContent: 'center', alignItems: 'center', opacity: out }}>
      {backdrop && <AbsoluteFill style={{ background: 'radial-gradient(ellipse 45% 35% at 50% 50%, rgba(5,5,15,0.55), transparent 75%)', opacity: draw }} />}
      <div style={{
        fontFamily: FONT, fontSize: size, color: `rgba(255,255,255,${fill})`, direction: 'rtl',
        WebkitTextStroke: '3px rgba(255,255,255,0.9)', clipPath: `inset(0 0 0 ${100 - draw * 100}%)`,
        textShadow: fill > 0.4 ? `0 0 30px ${PURPLE}, 0 0 70px ${PURPLE}88` : 'none', letterSpacing: 2,
      }}>{text}</div>
      {sub && <div style={{ fontFamily: FONT, fontSize: 46, color: '#e6e6f0', opacity: subIn, transform: `translateY(${(1 - subIn) * 20}px)`, direction: 'rtl', marginTop: -10, textShadow: '0 2px 12px #000' }}>{sub}</div>}
    </AbsoluteFill>
  );
};

// ختم الوقت يكتب نفسه حرف حرف (حرفين كل فريمين)
const TYPE_SPEED = 2;
const typeFrames = (text: string) => text.length * TYPE_SPEED;
const Stamp: React.FC<{ text: string; from: number; to: number }> = ({ text, from, to }) => {
  const f = useCurrentFrame();
  if (f < from || f >= to) return null;
  const n = Math.min(text.length, Math.floor((f - from) / TYPE_SPEED));
  const out = interpolate(f, [to - 10, to], [1, 0], clamp);
  return (
    <div style={{ position: 'absolute', right: 90, bottom: 175, opacity: out * 0.9, fontFamily: FONT, fontSize: 44, color: 'white', direction: 'rtl', textShadow: '0 2px 12px #000' }}>
      {text.slice(0, n)}
    </div>
  );
};

const EndCard: React.FC<{ from: number }> = ({ from }) => {
  const f = useCurrentFrame();
  const o = interpolate(f, [from, from + 25], [0, 1], clamp);
  if (o <= 0) return null;
  return (
    <AbsoluteFill style={{ background: '#0B0B14', opacity: o, justifyContent: 'center', alignItems: 'center' }}>
      <div style={{ fontFamily: FONT, color: 'white', textAlign: 'center' }}>
        <div style={{ fontSize: 110, textShadow: `0 0 26px ${PURPLE}` }}>نايف الأحمدي</div>
        <div style={{ fontSize: 44, color: '#22D3EE', direction: 'ltr' }}>@nalahmedy · DJI Osmo Pocket 3</div>
      </div>
    </AbsoluteFill>
  );
};

const STAMPS = [
  { text: '٦:٠١ م', shot: 1, off: 6, len: 54 },
  { text: '٦:١٠ م — بدأ المطر', shot: 5, off: 10, len: 65 },
  { text: '٦:١٩ م', shot: 10, off: 6, len: 49 },
];

// طبقة النصوص (مشتركة بين نسخة الكود ونسخة DaVinci)
const Overlays: React.FC<{ starts: number[]; end: number; endCard: boolean }> = ({ starts, end, endCard }) => {
  const last = starts[starts.length - 1];
  return (
    <AbsoluteFill style={{ width: 1920, height: 1080, transform: 'scale(2)', transformOrigin: '0 0', zIndex: 8 }}>
      <Title text="مطر" sub="رحلة على الطريق — ٣ أكتوبر" from={18} to={84} />
      {STAMPS.map((s) => <Stamp key={s.text} text={s.text} from={starts[s.shot] + s.off} to={starts[s.shot] + s.off + s.len} />)}
      <Title text="بعض الطرق… أجمل بالمطر" from={last + 18} to={end + 2} size={110} backdrop={false} />
      {endCard && <EndCard from={end - 25} />}
    </AbsoluteFill>
  );
};

const Sfx: React.FC<{ at: number; file: string; vol?: number; len?: number }> = ({ at, file, vol = 0.35, len }) => (
  <Sequence from={Math.max(0, at)} durationInFrames={len}><Audio src={staticFile(`sfx/${file}.wav`)} volume={vol} /></Sequence>
);

// مؤثرات النصوص (صوت الكيبورد بطول الكتابة فقط)
const OverlaySfx: React.FC<{ starts: number[]; end: number }> = ({ starts, end }) => {
  const last = starts[starts.length - 1];
  return (
    <>
      <Sfx at={14} file="sub" vol={0.45} />
      <Sfx at={20} file="magic" vol={0.18} />
      {STAMPS.map((s) => <Sfx key={s.text} at={starts[s.shot] + s.off} file="typing" vol={0.14} len={typeFrames(s.text) + 2} />)}
      <Sfx at={last + 2} file="cineriser" vol={0.25} />
      <Sfx at={last + 16} file="magic" vol={0.15} />
      <Sfx at={end - 20} file="sub" vol={0.3} />
    </>
  );
};

// ===== نسخة الكود الكاملة (4K)
export const Rain: React.FC = () => (
  <AbsoluteFill style={{ background: 'black' }}>
    {SHOTS.map((s, i) => (
      <Sequence key={i} from={STARTS[i]} durationInFrames={s.dur}><Shot i={i} /></Sequence>
    ))}
    <Flares />
    <Overlays starts={STARTS} end={VIDEO_END} endCard />

    {/* أشرطة سينمائية 2.39:1 */}
    <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: 276, background: 'black', zIndex: 10 }} />
    <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, height: 276, background: 'black', zIndex: 10 }} />

    {/* الصوت: موسيقى AEJuice + مطر حقيقي من التصوير تحتها */}
    <Audio src={staticFile('rain/music.wav')}
      volume={(fr) => interpolate(fr, [0, 30, RAIN_DURATION - 60, RAIN_DURATION], [0, 0.85, 0.85, 0], clamp)} />
    <Audio src={staticFile('rain/ambience.wav')}
      volume={(fr) => interpolate(fr, [0, 20, RAIN_DURATION - 60, RAIN_DURATION], [0, 0.4, 0.4, 0], clamp)} />

    {Object.entries(TRANS).map(([k, t]) => {
      const s = STARTS[Number(k) + 1];
      const file = t === 'film_burn' ? 'crackle' : t === 'slide_stack' ? 'lowwhoosh' : t === 'shape_wipe' ? 'reverse' : 'whoosh';
      return <Sfx key={k} at={s - 3} file={file} vol={t === 'film_burn' ? 0.25 : 0.3} len={t === 'film_burn' ? 45 : undefined} />;
    })}
    <OverlaySfx starts={STARTS} end={VIDEO_END} />
  </AbsoluteFill>
);

// ===== طبقة العناوين الشفافة لتايملاين DaVinci (ProRes 4444 بقناة ألفا)
export const RainTitlesDV: React.FC = () => (
  <AbsoluteFill>
    <Overlays starts={DV_STARTS} end={DV_END} endCard={false} />
    {/* الأشرطة السينمائية هنا لأن Output Blanking في DaVinci يسوّد الشاشة كلها عبر السكربت */}
    <div style={{ position: 'absolute', top: 0, left: 0, right: 0, height: 276, background: 'black' }} />
    <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, height: 276, background: 'black' }} />
  </AbsoluteFill>
);

// مؤثرات النصوص لتايملاين DaVinci (ملف صوت منفصل)
export const RainTitlesDVSfx: React.FC = () => (
  <AbsoluteFill><OverlaySfx starts={DV_STARTS} end={DV_END} /></AbsoluteFill>
);
