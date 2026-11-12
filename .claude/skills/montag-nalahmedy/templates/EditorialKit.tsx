/**
 * EditorialKit — ستايل "الورق المقصوص والملصقات التحريرية" (مأخوذ من تحليل ريل مرجعي).
 * كل مكوّن ياخذ from/to بالفريمات (أو t بالثواني عبر s()) ويرسم حركته بنفسه.
 * الألوان: ورق أوف-وايت + أسود + أحمر تمييز. الخلفيات: ورق بيج بملمس.
 */
import React from 'react';
import { AbsoluteFill, Img, interpolate, spring, useCurrentFrame, useVideoConfig, Easing } from 'remotion';

export const ED = {
  paper: '#F1ECE1',      // ورق الكابشن والملصقات
  ink: '#141414',        // أسود الطباعة
  red: '#D97757',        // لون التمييز: برتقالي Claude (طلب نايف بدل الأحمر)
  beige: '#E6DED0',      // خلفية مشاهد الورق
  blue: '#3B82F6',       // أزرق واجهات البرامج (صناديق التحديد، زر نشر)
  green: '#22C55E',
  font: "'Segoe UI', 'Tahoma', sans-serif",
};
export const s = (t: number) => Math.round(t * 30);
const clamp = { extrapolateLeft: 'clamp', extrapolateRight: 'clamp' } as const;
const ease = Easing.bezier(0.22, 1, 0.36, 1);
const shadow = '0 10px 24px rgba(0,0,0,0.35)';

/** نافذة ظهور: يرجع null برا الوقت، ويعطي تقدم الدخول والخروج */
function useWin(from: number, to: number, inF = 8, outF = 6) {
  const f = useCurrentFrame();
  const { fps } = useVideoConfig();
  const vis = f >= from && f < to;
  const pin = spring({ frame: f - from, fps, config: { damping: 11, stiffness: 190 } });
  const pout = interpolate(f, [to - outF, to], [0, 1], clamp);
  return { f, vis, l: f - from, pin, pout, fps, lin: interpolate(f, [from, from + inF], [0, 1], { ...clamp, easing: ease }) };
}

// ===== 1) مقدمة إطار الكاميرا (REC)
export const Viewfinder: React.FC<{ from: number; to: number }> = ({ from, to }) => {
  const { vis, f, pout } = useWin(from, to);
  if (!vis) return null;
  const c: React.CSSProperties = { position: 'absolute', width: 90, height: 90, borderColor: 'white', borderStyle: 'solid', opacity: 0.9 };
  const tc = `00:00:${String(Math.floor((f - from) / 30)).padStart(2, '0')}`;
  return (
    <AbsoluteFill style={{ opacity: 1 - pout, fontFamily: 'Consolas, monospace', color: 'white' }}>
      <AbsoluteFill style={{ background: 'radial-gradient(ellipse at center, transparent 45%, rgba(0,0,0,0.6) 100%)' }} />
      <div style={{ ...c, left: 60, top: 160, borderWidth: '6px 0 0 6px' }} />
      <div style={{ ...c, right: 60, top: 160, borderWidth: '6px 6px 0 0' }} />
      <div style={{ ...c, left: 60, bottom: 220, borderWidth: '0 0 6px 6px' }} />
      <div style={{ ...c, right: 60, bottom: 220, borderWidth: '0 6px 6px 0' }} />
      <div style={{ position: 'absolute', left: 110, top: 195, fontSize: 40, display: 'flex', alignItems: 'center', gap: 14 }}>
        <span style={{ width: 22, height: 22, borderRadius: 99, background: ED.red, opacity: (f % 30) < 18 ? 1 : 0.2 }} />REC {tc}
      </div>
      <div style={{ position: 'absolute', right: 110, top: 195, fontSize: 40 }}>4K 30</div>
    </AbsoluteFill>
  );
};

// ===== 2) الكابشن الورقي: شريط أوف-وايت + كلمة وحدة بمربع أحمر
export type Word = { w: string; s: number; e: number };
export const PaperCaption: React.FC<{ words: Word[]; hot: string[]; bottom?: number; hide?: [number, number][] }> = ({ words, hot, bottom = 520, hide = [] }) => {
  const f = useCurrentFrame();
  const t = f / 30;
  if (hide.some(([a, b]) => t >= a && t < b)) return null;
  // جملة الكابشن = 2-4 كلمات حول الكلمة الحالية
  const idx = words.findIndex((w) => t >= w.s - 0.05 && t < w.e + 0.3);
  if (idx < 0) return null;
  const start = Math.floor(idx / 3) * 3;
  const chunk = words.slice(start, start + 3);
  const c0 = chunk[0].s;
  const pop = interpolate(t, [c0 - 0.05, c0 + 0.08], [0.9, 1], clamp);
  return (
    <div style={{ position: 'absolute', left: 0, right: 0, bottom, display: 'flex', justifyContent: 'center' }}>
      <div style={{ transform: `scale(${pop}) rotate(-1deg)`, background: ED.paper, boxShadow: shadow, padding: '10px 26px', direction: 'rtl', display: 'flex', gap: 14, alignItems: 'center', fontFamily: ED.font, fontWeight: 800, fontSize: 52, color: ED.ink }}>
        {chunk.map((w, i) => {
          const isHot = hot.includes(w.w);
          return <span key={i} style={{ whiteSpace: 'nowrap', background: isHot ? ED.red : 'transparent', color: isHot ? 'white' : ED.ink, padding: isHot ? '0 10px' : 0 }}>{w.w}</span>;
        })}
      </div>
    </div>
  );
};

// ===== 3) ملصق عنوان: أحمر مائل / أسود / ورق — يدخل بارتداد
export const Tag: React.FC<{ text: string; from: number; to: number; x?: number; y: number; rot?: number; kind?: 'red' | 'black' | 'paper'; size?: number; tape?: boolean }> =
  ({ text, from, to, x = 540, y, rot = -4, kind = 'red', size = 78, tape = false }) => {
    const { vis, pin, pout } = useWin(from, to);
    if (!vis) return null;
    const bg = kind === 'red' ? ED.red : kind === 'black' ? ED.ink : ED.paper;
    const fg = kind === 'paper' ? ED.ink : 'white';
    return (
      <div style={{ position: 'absolute', left: x, top: y, transform: `translate(-50%, -50%) rotate(${rot}deg) scale(${pin * (1 - pout)})` }}>
        {tape && <div style={{ position: 'absolute', top: -18, left: '50%', width: 110, height: 34, background: 'rgba(240,230,200,0.75)', transform: 'translateX(-50%) rotate(3deg)' }} />}
        <div style={{ background: bg, color: fg, fontFamily: ED.font, fontWeight: 900, fontSize: size, padding: '6px 30px 12px', whiteSpace: 'nowrap', direction: 'rtl', boxShadow: shadow, border: kind === 'red' ? '5px solid white' : 'none' }}>{text}</div>
      </div>
    );
  };

// ===== 4) ملصق يتشطّب (مثل "8 ساعات")
export const StrikeTag: React.FC<{ text: string; from: number; strikeAt: number; to: number; x: number; y: number }> = ({ text, from, strikeAt, to, x, y }) => {
  const { vis, pin, pout, f } = useWin(from, to);
  if (!vis) return null;
  const st = interpolate(f, [strikeAt, strikeAt + 6], [0, 100], clamp);
  return (
    <div style={{ position: 'absolute', left: x, top: y, transform: `translate(-50%,-50%) rotate(-3deg) scale(${pin * (1 - pout)})` }}>
      <div style={{ position: 'relative', background: ED.paper, color: ED.ink, fontFamily: ED.font, fontWeight: 900, fontSize: 56, padding: '4px 22px', boxShadow: shadow, direction: 'rtl' }}>
        {text}
        <div style={{ position: 'absolute', left: 0, top: '52%', height: 8, width: `${st}%`, background: ED.red, transform: 'rotate(-6deg)' }} />
      </div>
    </div>
  );
};

// ===== 5) ختم كبير مع فلاش أبيض (مثل "دقايق")
export const Stamp: React.FC<{ text: string; from: number; to: number; y?: number }> = ({ text, from, to, y = 1250 }) => {
  const { vis, f, pout } = useWin(from, to);
  if (!vis) return null;
  const sc = interpolate(f - from, [0, 5], [2.4, 1], { ...clamp, easing: ease });
  const flash = interpolate(f - from, [0, 2, 8], [0.85, 0.85, 0], clamp);
  return (
    <>
      <AbsoluteFill style={{ background: 'white', opacity: flash }} />
      <div style={{ position: 'absolute', left: 540, top: y, transform: `translate(-50%,-50%) rotate(-7deg) scale(${sc * (1 - pout)})` }}>
        <div style={{ border: `10px solid ${ED.red}`, color: ED.red, background: 'rgba(255,255,255,0.88)', fontFamily: ED.font, fontWeight: 900, fontSize: 190, padding: '0 50px 20px', lineHeight: 1.1 }}>{text}</div>
      </div>
    </>
  );
};

// ===== 6) مشهد ورق: خلفية بيج بملمس تغطي الفيديو، وفوقها عناصر مقصوصة
export const PaperScene: React.FC<{ from: number; to: number; children: React.ReactNode }> = ({ from, to, children }) => {
  const { vis, f } = useWin(from, to);
  if (!vis) return null;
  // دخول بمسحة ورقة من تحت، وخروج بمسحة لفوق
  const yIn = interpolate(f, [from, from + 7], [100, 0], { ...clamp, easing: ease });
  const yOut = interpolate(f, [to - 6, to], [0, -100], { ...clamp, easing: Easing.in(Easing.quad) });
  return (
    <AbsoluteFill style={{ transform: `translateY(${yIn + yOut}%)`, background: ED.beige }}>
      <svg width="100%" height="100%" style={{ position: 'absolute', opacity: 0.35 }}>
        <filter id="paperNoise"><feTurbulence type="fractalNoise" baseFrequency="0.9" numOctaves="3" /><feColorMatrix values="0 0 0 0 0.45  0 0 0 0 0.4  0 0 0 0 0.33  0 0 0 0.5 0" /></filter>
        <rect width="100%" height="100%" filter="url(#paperNoise)" />
      </svg>
      <AbsoluteFill style={{ background: 'radial-gradient(ellipse at center, transparent 55%, rgba(80,60,30,0.25))' }} />
      {children}
    </AbsoluteFill>
  );
};

// صورة مقصوصة بإطار أبيض (بولارويد) + شريط لاصق
export const Cutout: React.FC<{ src: string; from: number; to?: number; x: number; y: number; w: number; rot?: number; bw?: boolean; delay?: number; round?: boolean }> =
  ({ src, from, to = 1e9, x, y, w, rot = 4, bw = false, delay = 0, round = false }) => {
    const f = useCurrentFrame(); const { fps } = useVideoConfig();
    const p = spring({ frame: f - from - delay, fps, config: { damping: 12, stiffness: 160 } });
    if (f < from + delay || f >= to) return null;
    const out = interpolate(f, [to - 6, to], [1, 0], clamp);
    return (
      <div style={{ position: 'absolute', left: x, top: y, width: w, transform: `translate(-50%,-50%) rotate(${rot}deg) scale(${p * out})`, background: 'white', padding: round ? 0 : 14, borderRadius: round ? w * 0.22 : 0, boxShadow: '0 16px 30px rgba(0,0,0,0.35)' }}>
        <Img src={src} style={{ width: '100%', display: 'block', filter: bw ? 'grayscale(1) contrast(1.1)' : 'none', borderRadius: round ? w * 0.22 : 0 }} />
        {!round && <div style={{ position: 'absolute', top: -16, left: '50%', width: 100, height: 30, background: 'rgba(240,230,200,0.8)', transform: 'translateX(-50%) rotate(-4deg)' }} />}
      </div>
    );
  };

// ===== 8) كرت موجة صوت + شرائح الكلمات بتوقيتها
export const WaveCard: React.FC<{ from: number; to: number; y?: number; file?: string; chips?: Word[] }> = ({ from, to, y = 330, file = 'voiceover.wav', chips = [] }) => {
  const { vis, f, lin, pout } = useWin(from, to);
  if (!vis) return null;
  const bars = 46;
  return (
    <div style={{ position: 'absolute', left: 90, right: 90, top: y, opacity: lin * (1 - pout), transform: `translateY(${(1 - lin) * 40}px)` }}>
      <div style={{ background: 'rgba(20,20,24,0.88)', border: '2px solid rgba(255,255,255,0.15)', borderRadius: 18, padding: '18px 26px' }}>
        <div style={{ fontFamily: 'Consolas, monospace', fontSize: 24, color: '#9CA3AF' }}>{file}</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, height: 150, marginTop: 10 }}>
          {Array.from({ length: bars }).map((_, i) => {
            const h = 25 + 70 * Math.abs(Math.sin(i * 1.7) * Math.cos(i * 0.6));
            const cut = i % 9 === 4; // أماكن السكتات تنقص
            const gone = cut && f > from + 12;
            return <div key={i} style={{ flex: 1, height: `${gone ? 6 : h}%`, background: cut ? ED.red : 'white', borderRadius: 3, transition: 'none' }} />;
          })}
        </div>
      </div>
      {chips.length > 0 && (
        <div style={{ display: 'flex', flexDirection: 'row-reverse', justifyContent: 'center', gap: 10, marginTop: 20 }}>
          {chips.map((c, i) => {
            const on = f >= from + 14 + i * 3;
            return on ? (
              <div key={i} style={{ border: `2px solid ${ED.red}`, background: 'rgba(20,20,24,0.85)', color: 'white', fontFamily: ED.font, fontWeight: 700, fontSize: 30, padding: '4px 14px', textAlign: 'center' }}>
                {c.w}<div style={{ fontFamily: 'Consolas, monospace', fontSize: 18, color: '#9CA3AF' }}>{c.s.toFixed(2)}s</div>
              </div>
            ) : null;
          })}
        </div>
      )}
    </div>
  );
};

// ===== 9) جوال: منشور + زر "نشر" يتحول "تم النشر ✓"
export const PhonePost: React.FC<{ from: number; publishAt: number; to: number; img: string; handle: string; x?: number; y?: number }> = ({ from, publishAt, to, img, handle, x = 540, y = 760 }) => {
  const { vis, f, pin, pout } = useWin(from, to);
  if (!vis) return null;
  const done = f >= publishAt;
  const press = interpolate(f, [publishAt - 4, publishAt - 1, publishAt + 2], [1, 0.9, 1], clamp);
  return (
    <div style={{ position: 'absolute', left: x, top: y, transform: `translate(-50%,-50%) rotate(-6deg) scale(${pin * (1 - pout)})` }}>
      <div style={{ width: 420, height: 820, borderRadius: 56, background: '#111', padding: 16, boxShadow: '0 30px 60px rgba(0,0,0,0.45)' }}>
        <div style={{ width: '100%', height: '100%', borderRadius: 42, background: 'white', overflow: 'hidden', fontFamily: ED.font }}>
          <div style={{ padding: '40px 24px 10px', fontWeight: 800, fontSize: 28, color: '#111', direction: 'ltr' }}>{handle}</div>
          <Img src={img} style={{ width: '100%', height: 470, objectFit: 'cover' }} />
          <div style={{ display: 'flex', justifyContent: 'center', marginTop: 40 }}>
            <div style={{ transform: `scale(${press})`, background: done ? ED.green : ED.blue, color: 'white', fontWeight: 900, fontSize: 46, padding: '10px 60px', borderRadius: 14, direction: 'rtl' }}>{done ? 'تم النشر ✓' : 'نشر'}</div>
          </div>
        </div>
      </div>
    </div>
  );
};

// ===== 10) صندوق برومبت ينكتب
export const PromptBox: React.FC<{ from: number; to: number; text: string; y?: number; cps?: number }> = ({ from, to, text, y = 620, cps = 1.5 }) => {
  const { vis, f, lin, pout } = useWin(from, to);
  if (!vis) return null;
  const n = Math.min(text.length, Math.max(0, Math.floor((f - from - 6) / cps)));
  return (
    <div style={{ position: 'absolute', left: 80, right: 80, top: y, opacity: lin * (1 - pout), transform: `scale(${0.9 + 0.1 * lin})` }}>
      <div style={{ background: '#FBF8F2', borderRadius: 26, padding: '22px 28px 80px', boxShadow: shadow, fontFamily: ED.font, direction: 'rtl', minHeight: 120 }}>
        <div style={{ fontSize: 24, color: '#9CA3AF' }}>اكتب طلبك…</div>
        <div style={{ fontSize: 44, fontWeight: 700, color: ED.ink, marginTop: 8 }}>{text.slice(0, n)}<span style={{ opacity: (f % 16) < 8 ? 1 : 0 }}>|</span></div>
        <div style={{ position: 'absolute', left: 30, bottom: 22, width: 64, height: 64, borderRadius: 99, background: n >= text.length ? ED.red : ED.ink, color: 'white', fontSize: 34, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>➤</div>
      </div>
    </div>
  );
};

// ===== 11) كرت محادثة خاص (اسم + متصل الآن + رسائل)
export const DMCard: React.FC<{ from: number; to: number; name: string; avatar: string; msgs: { text: string; at: number; me?: boolean }[]; y?: number }> = ({ from, to, name, avatar, msgs, y = 1080 }) => {
  const { vis, f, lin, pout } = useWin(from, to);
  if (!vis) return null;
  return (
    <div style={{ position: 'absolute', left: 90, right: 90, top: y, opacity: lin * (1 - pout), transform: `translateY(${(1 - lin) * 60}px)` }}>
      <div style={{ background: '#FBF8F2', borderRadius: 22, padding: 24, boxShadow: shadow, fontFamily: ED.font, direction: 'rtl', minHeight: 230 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <Img src={avatar} style={{ width: 70, height: 70, borderRadius: 99, objectFit: 'cover' }} />
          <div><div style={{ fontWeight: 900, fontSize: 36, color: ED.ink }}>{name}</div><div style={{ fontSize: 24, color: ED.green }}>متصل الآن</div></div>
        </div>
        <div style={{ marginTop: 18, display: 'flex', flexDirection: 'column', gap: 12 }}>
          {msgs.filter((m) => f >= m.at).map((m, i) => {
            const p = interpolate(f, [m.at, m.at + 6], [0, 1], { ...clamp, easing: ease });
            return (
              <div key={i} style={{ alignSelf: m.me ? 'flex-start' : 'flex-end', background: m.me ? ED.ink : ED.red, color: 'white', fontSize: 34, fontWeight: 700, padding: '10px 22px', borderRadius: 18, transform: `scale(${p})` }}>{m.text}</div>
            );
          })}
        </div>
      </div>
    </div>
  );
};

// ===== 12) شريط تقدم + عداد أحمر
export const ProgressTimer: React.FC<{ from: number; to: number; label: string; seconds: number; y?: number }> = ({ from, to, label, seconds, y = 1150 }) => {
  const { vis, f, lin, pout } = useWin(from, to);
  if (!vis) return null;
  const p = interpolate(f, [from + 6, to - 8], [0, 1], clamp);
  const sec = Math.round(p * seconds);
  return (
    <div style={{ position: 'absolute', left: 110, right: 110, top: y, opacity: lin * (1 - pout) }}>
      <div style={{ background: ED.paper, padding: '14px 22px', boxShadow: shadow, fontFamily: ED.font, direction: 'rtl' }}>
        <div style={{ fontWeight: 800, fontSize: 34, color: ED.ink }}>{label}</div>
        <div style={{ height: 26, background: '#D6D0C4', marginTop: 10, borderRadius: 13, overflow: 'hidden' }}>
          <div style={{ width: `${p * 100}%`, height: '100%', background: ED.ink, marginRight: 0, marginLeft: 'auto' }} />
        </div>
      </div>
      <div style={{ position: 'absolute', left: -20, bottom: -70, background: ED.red, color: 'white', fontFamily: 'Consolas, monospace', fontWeight: 900, fontSize: 54, padding: '2px 18px', transform: 'rotate(-4deg)', boxShadow: shadow }}>0:{String(sec).padStart(2, '0')}</div>
    </div>
  );
};

// ===== 13) ساعة مرسومة بعقارب تدور
export const Clock: React.FC<{ from: number; to: number; x: number; y: number; size?: number }> = ({ from, to, x, y, size = 150 }) => {
  const { vis, f, pin, pout } = useWin(from, to);
  if (!vis) return null;
  const a = (f - from) * 18;
  return (
    <div style={{ position: 'absolute', left: x, top: y, width: size, height: size, transform: `translate(-50%,-50%) scale(${pin * (1 - pout)}) rotate(8deg)` }}>
      <svg viewBox="0 0 100 100" width={size} height={size} style={{ filter: 'drop-shadow(0 8px 12px rgba(0,0,0,0.4))' }}>
        <circle cx="50" cy="50" r="46" fill="white" stroke="#222" strokeWidth="6" />
        {Array.from({ length: 12 }).map((_, i) => <line key={i} x1="50" y1="9" x2="50" y2="16" stroke="#222" strokeWidth="3" transform={`rotate(${i * 30} 50 50)`} />)}
        <line x1="50" y1="50" x2="50" y2="22" stroke="#222" strokeWidth="5" strokeLinecap="round" transform={`rotate(${a / 12} 50 50)`} />
        <line x1="50" y1="50" x2="50" y2="14" stroke={ED.red} strokeWidth="3" strokeLinecap="round" transform={`rotate(${a} 50 50)`} />
        <circle cx="50" cy="50" r="4" fill="#222" />
      </svg>
    </div>
  );
};

// ===== 14) تلوين "غامق مزاجي" فوق الفيديو: فينيت + حبيبات فيلم
export const MoodyGrade: React.FC<{ strength?: number }> = ({ strength = 1 }) => {
  const f = useCurrentFrame();
  return (
    <>
      <AbsoluteFill style={{ background: 'radial-gradient(ellipse 70% 60% at 50% 42%, transparent 35%, rgba(0,0,0,0.72) 100%)', opacity: strength }} />
      <svg width="100%" height="100%" style={{ position: 'absolute', inset: 0, opacity: 0.09 * strength, mixBlendMode: 'overlay' }}>
        <filter id="grain"><feTurbulence type="fractalNoise" baseFrequency="0.85" numOctaves="2" seed={f % 12} /></filter>
        <rect width="100%" height="100%" filter="url(#grain)" />
      </svg>
    </>
  );
};
