import React from 'react';
import {
  AbsoluteFill, Audio, Img, OffthreadVideo, Sequence, continueRender, delayRender,
  interpolate, spring, staticFile, useCurrentFrame, useVideoConfig, Easing,
} from 'remotion';

const PURPLE = '#8B5CF6';
const CYAN = '#22D3EE';
const DARK = '#0B0B14';
const FONT = 'Tajarib';
const s = (t: number) => Math.round(t * 30);

// تحميل خط الهوية قبل الرندر
const handle = delayRender('font');
new FontFace(FONT, `url(${staticFile('fonts/Elgharib-Tajarib-Taweel-Bold.otf')})`)
  .load().then((f) => { (document.fonts as any).add(f); continueRender(handle); });

const glow = `0 0 18px ${PURPLE}, 0 0 42px ${PURPLE}99`;
const ease = Easing.bezier(0.22, 1, 0.36, 1);

// ===== الزوم: بنش على "نجرب"، ناعم على "يساعدك"، بنش على "المكان الصحيح"، وزوم بنش انتقالي للنهاية
function zoomAt(f: number) {
  const k = (a: number, b: number, from: number, to: number) =>
    interpolate(f, [a, b], [from, to], { extrapolateLeft: 'clamp', extrapolateRight: 'clamp', easing: ease });
  if (f < s(6.02)) return 1;
  if (f < s(6.02) + 4) return k(s(6.02), s(6.02) + 4, 1, 1.3);
  if (f < s(9.1)) return k(s(6.02) + 4, s(6.02) + 18, 1.3, 1.12);
  if (f < s(10.68)) return k(s(9.1), s(10.6), 1.12, 1.2);
  if (f < s(13.16)) return 1;
  if (f < s(14.6)) return k(s(13.16), s(13.16) + 5, 1.0, 1.15);
  if (f < 472) return k(s(14.6), s(15.4), 1.15, 1.0);
  return k(472, 487, 1.0, 2.6); // zoom_punch إلى كرت النهاية
}

const Sfx: React.FC<{ at: number; file: string; vol?: number }> = ({ at, file, vol = 0.35 }) => (
  <Sequence from={Math.max(0, at)}><Audio src={staticFile(`sfx/${file}.wav`)} volume={vol} /></Sequence>
);

// ===== طبقة الشخص المفصول (Magic Mask)
const Person: React.FC<{ dir: string; start: number; count: number }> = ({ dir, start, count }) => {
  const f = useCurrentFrame();
  const i = f - start + 1;
  if (i < 1 || i > count) return null;
  return <Img src={staticFile(`${dir}/p_${String(i).padStart(4, '0')}.png`)} style={{ position: 'absolute', width: '100%', height: '100%' }} />;
};

// ===== نص كبير خلف الشخص (3 أنماط دخول مختلفة)
const BehindWord: React.FC<{ text: string; from: number; to: number; mode: 'slam' | 'whip' | 'neon'; size?: number }> = ({ text, from, to, mode, size = 230 }) => {
  const f = useCurrentFrame();
  if (f < from || f >= to) return null;
  const l = f - from;
  const out = interpolate(f, [to - 4, to], [1, 0], { extrapolateLeft: 'clamp' });
  let transform = '', opacity = out, filter = '', stroke = 0;
  if (mode === 'slam') {
    const sc = interpolate(l, [0, 6], [2.4, 1], { extrapolateRight: 'clamp', easing: ease });
    transform = `scale(${sc})`; opacity *= interpolate(l, [0, 3], [0, 1], { extrapolateRight: 'clamp' });
  } else if (mode === 'whip') {
    const x = interpolate(l, [0, 7], [900, 0], { extrapolateRight: 'clamp', easing: ease });
    transform = `translateX(${x}px)`; filter = `blur(${interpolate(l, [0, 7], [18, 0], { extrapolateRight: 'clamp' })}px)`;
  } else {
    stroke = interpolate(l, [0, 10], [0, 1], { extrapolateRight: 'clamp' });
  }
  const fill = mode === 'neon' ? interpolate(l, [8, 16], [0, 1], { extrapolateLeft: 'clamp', extrapolateRight: 'clamp' }) : 1;
  return (
    <AbsoluteFill style={{ justifyContent: 'center', alignItems: 'center', opacity }}>
      <div style={{
        fontFamily: FONT, fontSize: size, color: `rgba(255,255,255,${fill})`, transform, filter,
        textShadow: fill > 0.5 ? glow : 'none', whiteSpace: 'nowrap', direction: 'rtl', marginTop: -120,
        WebkitTextStroke: mode === 'neon' ? `${4 * stroke}px ${CYAN}` : undefined,
        clipPath: mode === 'neon' ? `inset(0 0 0 ${100 - stroke * 100}%)` : undefined,
      }}>{text}</div>
    </AbsoluteFill>
  );
};

// ===== اسم بكبسولة (pop_badge)
const PopBadge: React.FC<{ text: string; sub: string; from: number; to: number }> = ({ text, sub, from, to }) => {
  const f = useCurrentFrame(); const { fps } = useVideoConfig();
  if (f < from || f >= to) return null;
  const sc = spring({ frame: f - from, fps, config: { damping: 9, stiffness: 180 } });
  const out = interpolate(f, [to - 6, to], [1, 0], { extrapolateLeft: 'clamp' });
  return (
    <div style={{ position: 'absolute', left: 120, bottom: 150, transform: `scale(${sc * out})`, transformOrigin: 'left center', display: 'flex', alignItems: 'center', gap: 22, direction: 'rtl', background: `${DARK}CC`, border: `3px solid ${PURPLE}`, borderRadius: 999, padding: '18px 40px', boxShadow: `0 0 30px ${PURPLE}88` }}>
      <div style={{ width: 22, height: 22, borderRadius: 99, background: CYAN, boxShadow: `0 0 16px ${CYAN}` }} />
      <div style={{ fontFamily: FONT, color: 'white' }}>
        <div style={{ fontSize: 64, lineHeight: 1.1 }}>{text}</div>
        <div style={{ fontSize: 30, color: CYAN }}>{sub}</div>
      </div>
    </div>
  );
};

// ===== قائمة نقاط (رسم توضيحي)
const Checklist: React.FC<{ items: [string, number][]; to: number }> = ({ items, to }) => {
  const f = useCurrentFrame(); const { fps } = useVideoConfig();
  if (f < items[0][1] || f >= to) return null;
  const out = interpolate(f, [to - 6, to], [1, 0], { extrapolateLeft: 'clamp' });
  return (
    <div style={{ position: 'absolute', right: 110, top: 300, display: 'flex', flexDirection: 'column', gap: 26, opacity: out }}>
      {items.map(([t, at]) => {
        const p = spring({ frame: f - at, fps, config: { damping: 10, stiffness: 200 } });
        return f < at ? null : (
          <div key={t} style={{ transform: `translateX(${(1 - p) * 120}px) scale(${p})`, display: 'flex', alignItems: 'center', gap: 20, direction: 'rtl', background: `${DARK}D9`, border: `2px solid ${PURPLE}`, borderRadius: 22, padding: '14px 32px' }}>
            <div style={{ width: 54, height: 54, borderRadius: 99, background: CYAN, color: DARK, fontSize: 36, display: 'flex', justifyContent: 'center', alignItems: 'center', fontWeight: 900 }}>✓</div>
            <div style={{ fontFamily: FONT, fontSize: 70, color: 'white', textShadow: glow }}>{t}</div>
          </div>
        );
      })}
    </div>
  );
};

// ===== نص ينكتب (type_glow)
const TypeGlow: React.FC<{ text: string; from: number; to: number }> = ({ text, from, to }) => {
  const f = useCurrentFrame();
  if (f < from || f >= to) return null;
  const n = Math.min(text.length, Math.floor((f - from) / 1.6));
  const out = interpolate(f, [to - 5, to], [1, 0], { extrapolateLeft: 'clamp' });
  const g = interpolate(f, [from + text.length * 1.6, from + text.length * 1.6 + 6], [0, 1], { extrapolateLeft: 'clamp', extrapolateRight: 'clamp' });
  return (
    <AbsoluteFill style={{ alignItems: 'center', paddingTop: 70, opacity: out }}>
      <div style={{ fontFamily: FONT, fontSize: 110, color: 'white', direction: 'rtl', background: `${DARK}B3`, padding: '6px 46px', borderRadius: 26, border: `3px solid ${g > 0 ? PURPLE : 'transparent'}`, textShadow: g > 0 ? glow : 'none' }}>
        {text.slice(0, n)}<span style={{ color: CYAN, opacity: (f % 16) < 8 ? 1 : 0 }}>|</span>
      </div>
    </AbsoluteFill>
  );
};

// ===== كرت النهاية
const EndCard: React.FC<{ from: number }> = ({ from }) => {
  const f = useCurrentFrame(); const { fps } = useVideoConfig();
  if (f < from) return null;
  const l = f - from;
  const sc = interpolate(l, [0, 10], [2.6, 1], { extrapolateRight: 'clamp', easing: ease });
  const bl = interpolate(l, [0, 10], [24, 0], { extrapolateRight: 'clamp' });
  const btn = spring({ frame: l - 14, fps, config: { damping: 9 } });
  return (
    <AbsoluteFill style={{ background: `radial-gradient(circle at 50% 45%, #2a1a5e, ${DARK} 70%)`, justifyContent: 'center', alignItems: 'center', opacity: interpolate(l, [0, 4], [0, 1], { extrapolateRight: 'clamp' }) }}>
      <div style={{ transform: `scale(${sc})`, filter: `blur(${bl}px)`, textAlign: 'center', fontFamily: FONT, color: 'white' }}>
        <div style={{ fontSize: 150, textShadow: glow }}>نايف الأحمدي</div>
        <div style={{ fontSize: 60, color: CYAN, direction: 'ltr' }}>@nalahmedy</div>
        <div style={{ marginTop: 40, display: 'inline-block', transform: `scale(${btn})`, background: '#FF0033', borderRadius: 999, padding: '14px 60px', fontSize: 64 }}>اشترك 🔔</div>
      </div>
    </AbsoluteFill>
  );
};

export const Montage: React.FC = () => {
  const f = useCurrentFrame();
  const z = zoomAt(f);
  const punchBlur = f >= 472 ? interpolate(f, [472, 487], [0, 22]) : 0;
  const dim1 = interpolate(f, [s(2.6), s(2.75), s(6.0), s(6.15)], [0, 0.5, 0.5, 0], { extrapolateLeft: 'clamp', extrapolateRight: 'clamp' });
  // mm_text_behind (الانتقال عند القطع 10.68): لوح بنفسجي يمسح الخلفية خلف نايف
  const wipe = interpolate(f, [s(10.3), s(10.6), s(10.8), s(11.3)], [-1, 0, 0, 1], { extrapolateLeft: 'clamp', extrapolateRight: 'clamp', easing: ease });
  const flash = f === s(10.68) || f === s(10.68) + 1 ? 0.6 : 0;

  return (
    <AbsoluteFill style={{ background: DARK }}>
      <AbsoluteFill style={{ transform: `scale(${z})`, transformOrigin: '50% 38%', filter: `blur(${punchBlur}px)` }}>
        <OffthreadVideo src={staticFile('talk.mp4')} />
        <AbsoluteFill style={{ background: DARK, opacity: dim1 }} />
        {/* Magic Mask: كلمات خلف نايف */}
        <BehindWord text="التصوير" from={s(2.7)} to={s(3.5)} mode="slam" />
        <BehindWord text="الذكاء الاصطناعي" from={s(3.5)} to={s(4.7)} mode="whip" size={190} />
        <BehindWord text="صناعة المحتوى" from={s(4.7)} to={s(6.1)} mode="neon" size={200} />
        <Person dir="mask" start={75} count={111} />
        {/* انتقال mm_text_behind */}
        {wipe > -1 && wipe < 1 && (
          <AbsoluteFill style={{ background: `linear-gradient(90deg, ${PURPLE}, #4c1d95)`, transform: `translateX(${wipe * 100}%)` }} />
        )}
        <BehindWord text="مهتم؟" from={s(10.45)} to={s(11.35)} mode="slam" size={300} />
        <Person dir="mask2" start={303} count={45} />
      </AbsoluteFill>

      <AbsoluteFill style={{ background: 'white', opacity: flash }} />
      <PopBadge text="نايف الأحمدي" sub="تصوير • ذكاء اصطناعي • صناعة محتوى" from={s(1.02)} to={s(2.6)} />
      <Checklist items={[['نجرب', s(6.02)], ['نتعلم', s(7.0)], ['نشارك', s(7.76)]]} to={s(9.6)} />
      <TypeGlow text="المكان الصحيح ✓" from={s(13.16)} to={s(14.9)} />
      <EndCard from={480} />

      {/* ===== المؤثرات الصوتية: كل حركة لها صوت */}
      <Sfx at={s(1.02) - 2} file="pop" />
      <Sfx at={s(1.02)} file="tiny" vol={0.25} />
      <Sfx at={s(2.7) - 2} file="hit" vol={0.4} />
      <Sfx at={s(3.5) - 3} file="whip" />
      <Sfx at={s(4.7) - 2} file="energy" vol={0.3} />
      <Sfx at={s(6.02) - 3} file="whoosh" vol={0.4} />
      <Sfx at={s(6.02) + 2} file="pop" vol={0.3} />
      <Sfx at={s(7.0)} file="pop" vol={0.3} />
      <Sfx at={s(7.76)} file="pop" vol={0.3} />
      <Sfx at={s(10.3)} file="reverse" vol={0.35} />
      <Sfx at={s(10.68) - 1} file="shutter" vol={0.35} />
      <Sfx at={s(13.16)} file="typing" vol={0.25} />
      <Sfx at={s(14.3)} file="riser" vol={0.3} />
      <Sfx at={469} file="whoosh" vol={0.45} />
      <Sfx at={496} file="chime" vol={0.3} />
    </AbsoluteFill>
  );
};
