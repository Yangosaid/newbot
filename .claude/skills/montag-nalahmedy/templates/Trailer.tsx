import React from 'react';
import {
  AbsoluteFill, Audio, Img, OffthreadVideo, Sequence, continueRender, delayRender,
  interpolate, spring, staticFile, useCurrentFrame, useVideoConfig, Easing,
} from 'remotion';
// eslint-disable-next-line @typescript-eslint/no-var-requires
const WORDS: { w: string; s: number; e: number }[] = require('../public/yt/cut_words.json');

// مقدمة قناة يوتيوب — 16:9، 4K، 24fps. التصميم على 1920×1080 ويتكبّر ×2.
const FPS = 24;
const s = (t: number) => Math.round(t * FPS);
const ACC = '#D97757';   // لون التمييز (برتقالي Claude — طلب نايف)
const DARK = '#0B0B14';
const FONT = 'Tajarib';
const UI = "'Segoe UI', Tahoma, sans-serif";
const clamp = { extrapolateLeft: 'clamp', extrapolateRight: 'clamp' } as const;
const ease = Easing.bezier(0.22, 1, 0.36, 1);
const VIDEO_END = s(48.29);
export const TRAILER_DURATION = VIDEO_END + s(2.2);
const SFX: Record<string, number> = { chime: 0.866, drop: 0.082, heavy: 0.154, hit: 0.197, magic: 1.0, pop: 0.75, reverse: 0.173, riser: 0.148, whip: 0.61, whoosh: 0.509 };

const fh = delayRender('font');
new FontFace(FONT, `url(${staticFile('fonts/Elgharib-Tajarib-Taweel-Bold.otf')})`).load().then((f) => { (document.fonts as any).add(f); continueRender(fh); });

const Sfx: React.FC<{ at: number; file: string; len?: number }> = ({ at, file, len }) => (
  <Sequence from={Math.max(0, at)} durationInFrames={len}><Audio src={staticFile(`yt/sfx/${file}.mp3`)} volume={SFX[file] ?? 0.2} /></Sequence>
);

// ===== قبل/بعد: "قبل" كاملة، وبعدها خط يمسح ويكشف "بعد"
const BeforeAfter: React.FC = () => {
  const f = useCurrentFrame();
  if (f >= s(2.55)) return null;
  const wipe = interpolate(f, [s(1.0), s(1.55)], [0, 100], { ...clamp, easing: ease });
  const tagIn = (t: number) => spring({ frame: f - s(t), fps: FPS, config: { damping: 11 } });
  const out = interpolate(f, [s(2.35), s(2.55)], [1, 0], clamp);
  return (
    <AbsoluteFill style={{ opacity: out }}>
      <OffthreadVideo src={staticFile('yt/before.mp4')} muted style={{ width: '100%', height: '100%' }} />
      <AbsoluteFill style={{ clipPath: `inset(0 0 0 ${100 - wipe}%)` }}>
        <OffthreadVideo src={staticFile('yt/after.mp4')} muted style={{ width: '100%', height: '100%' }} />
      </AbsoluteFill>
      {wipe > 0 && wipe < 100 && <div style={{ position: 'absolute', top: 0, bottom: 0, left: `${100 - wipe}%`, width: 12, background: 'white', boxShadow: `0 0 40px ${ACC}` }} />}
      <AbsoluteFill style={{ width: 1920, height: 1080, transform: 'scale(2)', transformOrigin: '0 0' }}>
        <div style={{ position: 'absolute', right: 70, top: 60, transform: `scale(${tagIn(0.4)}) rotate(3deg)`, background: 'rgba(240,240,240,0.92)', color: '#222', fontFamily: FONT, fontSize: 64, padding: '4px 36px' }}>قبل</div>
        {f >= s(1.5) && <div style={{ position: 'absolute', left: 70, top: 60, transform: `scale(${tagIn(1.5)}) rotate(-3deg)`, background: ACC, color: 'white', fontFamily: FONT, fontSize: 64, padding: '4px 36px', boxShadow: `0 0 30px ${ACC}` }}>بعد</div>}
      </AbsoluteFill>
    </AbsoluteFill>
  );
};

// ===== ملصق بارتداد
const Tag: React.FC<{ text: string; from: number; to: number; x: number; y: number; rot?: number; fill?: string; size?: number }> = ({ text, from, to, x, y, rot = -3, fill = ACC, size = 58 }) => {
  const f = useCurrentFrame();
  if (f < from || f >= to) return null;
  const p = spring({ frame: f - from, fps: FPS, config: { damping: 10, stiffness: 180 } });
  const o = interpolate(f, [to - 5, to], [1, 0], clamp);
  return <div style={{ position: 'absolute', left: x, top: y, transform: `translate(-50%,-50%) rotate(${rot}deg) scale(${p * o})`, background: fill, color: 'white', fontFamily: FONT, fontSize: size, padding: '2px 30px 8px', whiteSpace: 'nowrap', direction: 'rtl', boxShadow: '0 10px 26px rgba(0,0,0,0.4)', border: '4px solid white' }}>{text}</div>;
};

// ===== الاسم (Lower third)
const NameCard: React.FC<{ from: number; to: number }> = ({ from, to }) => {
  const f = useCurrentFrame();
  if (f < from || f >= to) return null;
  const p = interpolate(f, [from, from + 10], [0, 1], { ...clamp, easing: ease });
  const line = interpolate(f, [from + 4, from + 16], [0, 100], clamp);
  const o = interpolate(f, [to - 8, to], [1, 0], clamp);
  return (
    <div style={{ position: 'absolute', right: 90, top: 300, opacity: o, transform: `translateX(${(1 - p) * 80}px)`, direction: 'rtl', textAlign: 'right' }}>
      <div style={{ fontFamily: FONT, fontSize: 92, color: 'white', textShadow: '0 4px 20px rgba(0,0,0,0.6)', whiteSpace: 'nowrap' }}>نايف الأحمدي</div>
      <div style={{ height: 6, width: `${line}%`, background: ACC, margin: '4px 0 10px' }} />
      <div style={{ fontFamily: FONT, fontSize: 38, color: '#F5E6DC', textShadow: '0 2px 12px rgba(0,0,0,0.7)', whiteSpace: 'nowrap' }}>تصوير · ذكاء اصطناعي · صناعة محتوى</div>
    </div>
  );
};

// ===== "ثلاث أشياء" + البطاقات: تتجمع على اليمين، والحالية مضيئة
const CARDS = [
  { n: 1, title: 'تجربة أدوات الذكاء الاصطناعي', icons: ['claude', 'chatgpt', 'googlegemini'], from: 18.08, iconsAt: 19.36 },
  { n: 2, title: 'شرح التصوير', icons: ['apple', 'davinciresolve', 'blackmagicdesign'], from: 24.24, iconsAt: 25.48 },
  { n: 3, title: 'صناعة المحتوى', icons: ['youtube', 'tiktok', 'instagram'], from: 29.34, iconsAt: 30.56 },
];
const Pillars: React.FC = () => {
  const f = useCurrentFrame();
  if (f < s(15.24) || f >= s(35.7)) return null;
  const out = interpolate(f, [s(35.3), s(35.7)], [1, 0], clamp);
  const three = spring({ frame: f - s(16.6), fps: FPS, config: { damping: 9 } });
  const threeOut = interpolate(f, [s(17.8), s(18.1)], [1, 0], clamp);
  return (
    <AbsoluteFill style={{ opacity: out }}>
      {f < s(18.1) && f >= s(16.6) && (
        <div style={{ position: 'absolute', right: 150, top: 330, transform: `scale(${three * threeOut}) rotate(-4deg)`, textAlign: 'center', fontFamily: FONT, color: 'white' }}>
          <div style={{ fontSize: 260, lineHeight: 1, color: ACC, textShadow: '0 10px 40px rgba(0,0,0,0.5)' }}>3</div>
          <div style={{ fontSize: 56, background: DARK + 'CC', padding: '0 24px' }}>أشياء بتلقاها هنا</div>
        </div>
      )}
      <div style={{ position: 'absolute', right: 90, top: 170, display: 'flex', flexDirection: 'column', gap: 26, width: 560 }}>
        {CARDS.filter((c) => f >= s(c.from)).map((c) => {
          const active = f < s((CARDS[c.n] ?? { from: 99 }).from);
          const p = spring({ frame: f - s(c.from), fps: FPS, config: { damping: 12, stiffness: 170 } });
          return (
            <div key={c.n} style={{ transform: `translateX(${(1 - p) * 200}px) scale(${active ? 1 : 0.88})`, transformOrigin: 'right center', opacity: active ? 1 : 0.55, background: active ? 'rgba(11,11,20,0.88)' : 'rgba(11,11,20,0.6)', border: `3px solid ${active ? ACC : 'rgba(255,255,255,0.15)'}`, borderRadius: 22, padding: '18px 26px', direction: 'rtl', display: 'flex', alignItems: 'center', gap: 20, boxShadow: active ? `0 0 34px ${ACC}66` : 'none' }}>
              <div style={{ width: 70, height: 70, borderRadius: 99, background: ACC, color: 'white', fontFamily: UI, fontWeight: 900, fontSize: 40, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>{c.n}</div>
              <div style={{ flex: 1 }}>
                <div style={{ fontFamily: FONT, fontSize: 44, color: 'white', lineHeight: 1.2 }}>{c.title}</div>
                <div style={{ display: 'flex', gap: 12, marginTop: 10 }}>
                  {c.icons.map((ic, k) => {
                    const ip = spring({ frame: f - s(c.iconsAt) - k * 3, fps: FPS, config: { damping: 9 } });
                    return f >= s(c.iconsAt) + k * 3 ? <Img key={ic} src={staticFile(`yt/apps/${ic}.png`)} style={{ width: 66, height: 66, borderRadius: 15, transform: `scale(${ip})` }} /> : null;
                  })}
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </AbsoluteFill>
  );
};

// ===== ختم "مجرَّب قبلك"
const Stamp: React.FC<{ text: string; from: number; to: number }> = ({ text, from, to }) => {
  const f = useCurrentFrame();
  if (f < from || f >= to) return null;
  const sc = interpolate(f - from, [0, 5], [2.3, 1], { ...clamp, easing: ease });
  const o = interpolate(f, [to - 6, to], [1, 0], clamp);
  return (
    <div style={{ position: 'absolute', left: 90, top: 330, transform: `rotate(-8deg) scale(${sc})`, opacity: o }}>
      <div style={{ border: `8px solid ${ACC}`, color: ACC, background: 'rgba(255,255,255,0.9)', fontFamily: FONT, fontSize: 96, padding: '0 40px 12px' }}>{text}</div>
    </div>
  );
};

// ===== زر الاشتراك + الجرس
const Subscribe: React.FC<{ from: number; pressAt: number; bellAt: number; to: number }> = ({ from, pressAt, bellAt, to }) => {
  const f = useCurrentFrame();
  if (f < from || f >= to) return null;
  const p = spring({ frame: f - from, fps: FPS, config: { damping: 11 } });
  const pressed = f >= pressAt;
  const press = interpolate(f, [pressAt - 3, pressAt, pressAt + 3], [1, 0.9, 1], clamp);
  const bell = f >= bellAt ? Math.sin((f - bellAt) * 1.1) * 18 * Math.max(0, 1 - (f - bellAt) / 20) : 0;
  const cursorX = interpolate(f, [from, pressAt - 2], [260, 0], { ...clamp, easing: ease });
  return (
    <div style={{ position: 'absolute', left: 50, top: 440, transform: `scale(${p})`, transformOrigin: 'left center', display: 'flex', alignItems: 'center', gap: 22, direction: 'rtl' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 16, background: 'rgba(11,11,20,0.85)', borderRadius: 999, padding: '12px 14px 12px 30px' }}>
        <Img src={staticFile('yt/avatar.jpg')} style={{ width: 74, height: 74, borderRadius: 99, objectFit: 'cover' }} />
        <div style={{ fontFamily: FONT, color: 'white', fontSize: 40 }}>نايف الأحمدي<div style={{ fontFamily: UI, fontSize: 24, color: '#aaa', direction: 'ltr', textAlign: 'right' }}>@nalahmedy</div></div>
        <div style={{ transform: `scale(${press})`, background: pressed ? '#3a3a3a' : '#FF0000', color: 'white', fontFamily: FONT, fontSize: 44, padding: '6px 34px 12px', borderRadius: 999, marginRight: 10 }}>{pressed ? 'مشترك ✓' : 'اشترك'}</div>
        <div style={{ fontSize: 54, transform: `rotate(${bell}deg)`, transformOrigin: '50% 10%', opacity: f >= bellAt ? 1 : 0.4 }}>🔔</div>
      </div>
      {!pressed && <div style={{ position: 'absolute', left: 120 + cursorX, top: 60, fontSize: 60 }}>👆</div>}
    </div>
  );
};

// ===== كابشن (التشغيل التلقائي في صفحة القناة يكون بدون صوت)
const Captions: React.FC<{ hide: [number, number][] }> = ({ hide }) => {
  const f = useCurrentFrame();
  const t = f / FPS;
  if (hide.some(([a, b]) => t >= a && t < b)) return null;
  const idx = WORDS.findIndex((w) => t >= w.s - 0.05 && t < w.e + 0.3);
  if (idx < 0) return null;
  const start = Math.floor(idx / 4) * 4;
  const chunk = WORDS.slice(start, start + 4);
  return (
    <div style={{ position: 'absolute', left: 0, right: 0, bottom: 70, display: 'flex', justifyContent: 'center' }}>
      <div style={{ background: 'rgba(11,11,20,0.78)', borderRadius: 18, padding: '6px 26px 12px', direction: 'rtl', display: 'flex', gap: 14, fontFamily: FONT, fontSize: 50, color: 'white' }}>
        {chunk.map((w, i) => {
          const on = t >= w.s && t < w.e + 0.05;
          return <span key={i} style={{ whiteSpace: 'nowrap', color: on ? 'white' : '#ddd', background: on ? ACC : 'transparent', borderRadius: 10, padding: '0 8px' }}>{w.w}</span>;
        })}
      </div>
    </div>
  );
};

// ===== زوم: بنش خفيف على الاسم، ودفع بطيء على "ما أبيع لك وهم"
function zoomAt(f: number) {
  const k = (a: number, b: number, x: number, y: number) => interpolate(f, [a, b], [x, y], { ...clamp, easing: ease });
  if (f >= s(5.34) && f < s(15.2)) return f < s(5.34) + 4 ? k(s(5.34), s(5.34) + 4, 1, 1.1) : k(s(5.34) + 4, s(6.2), 1.1, 1.05);
  if (f >= s(35.88) && f < s(41.9)) return k(s(35.88), s(41.9), 1.0, 1.12);
  return 1;
}

export const Trailer: React.FC = () => {
  const f = useCurrentFrame();
  const endIn = interpolate(f, [VIDEO_END - 6, VIDEO_END + 6], [0, 1], clamp);
  return (
    <AbsoluteFill style={{ background: 'black' }}>
      <AbsoluteFill style={{ transform: `scale(${zoomAt(f)})`, transformOrigin: '50% 35%' }}>
        <OffthreadVideo src={staticFile('yt/cut.mp4')} style={{ width: '100%', height: '100%' }} />
      </AbsoluteFill>
      <BeforeAfter />

      <AbsoluteFill style={{ width: 1920, height: 1080, transform: 'scale(2)', transformOrigin: '0 0' }}>
        <Tag text="أداة صح ✓" from={s(3.16)} to={s(5.3)} x={1450} y={380} rot={-4} />
        <Tag text="طريقة صح ✓" from={s(3.94)} to={s(5.3)} x={1470} y={510} rot={3} fill={DARK} />
        <NameCard from={s(6.1)} to={s(14.8)} />
        <Pillars />
        <Tag text="ما أبيع لك وهم" from={s(36.0)} to={s(38.6)} x={420} y={360} rot={-3} fill={DARK} size={62} />
        <Stamp text="مجرَّب قبلك ✓" from={s(38.66)} to={s(41.8)} />
        <Subscribe from={s(42.0)} pressAt={s(44.96)} bellAt={s(45.86)} to={VIDEO_END} />
        <Captions hide={[[0, 2.55], [42.0, 99]]} />

        {/* كرت النهاية */}
        <AbsoluteFill style={{ opacity: endIn, background: `radial-gradient(circle at 50% 45%, #2a1a12, ${DARK} 70%)`, justifyContent: 'center', alignItems: 'center' }}>
          {endIn > 0 && (
            <div style={{ textAlign: 'center', fontFamily: FONT, color: 'white' }}>
              <div style={{ fontSize: 120, textShadow: `0 0 30px ${ACC}` }}>نايف الأحمدي</div>
              <div style={{ fontSize: 46, color: ACC }}>تجربة أدوات الذكاء الاصطناعي · التصوير · صناعة المحتوى</div>
              <div style={{ fontFamily: UI, fontSize: 40, color: '#ccc', marginTop: 10, direction: 'ltr' }}>@nalahmedy</div>
            </div>
          )}
        </AbsoluteFill>
      </AbsoluteFill>

      {/* المؤثرات (مستويات sfx_levels.py: أوطى من الكلام بـ 10 dB) */}
      <Sfx at={s(0.4)} file="pop" />
      <Sfx at={s(1.0) - 2} file="whoosh" />
      <Sfx at={s(1.5)} file="pop" />
      <Sfx at={s(3.16)} file="pop" />
      <Sfx at={s(3.94)} file="pop" />
      <Sfx at={s(5.34) - 2} file="whip" />
      <Sfx at={s(16.6)} file="hit" />
      <Sfx at={s(18.08) - 2} file="whoosh" />
      <Sfx at={s(24.24) - 2} file="whoosh" />
      <Sfx at={s(29.34) - 2} file="whoosh" />
      <Sfx at={s(38.66) - 1} file="heavy" />
      <Sfx at={s(42.0)} file="whip" />
      <Sfx at={s(44.96)} file="pop" />
      <Sfx at={s(45.86)} file="chime" len={s(1.2)} />
      <Sfx at={VIDEO_END - 4} file="riser" />
    </AbsoluteFill>
  );
};
