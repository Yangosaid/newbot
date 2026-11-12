import React from 'react';
import { AbsoluteFill, Audio, Img, OffthreadVideo, Sequence, interpolate, staticFile, useCurrentFrame, Easing } from 'remotion';
import { ED, s, Viewfinder, PaperCaption, Tag, StrikeTag, Stamp, PaperScene, Cutout, PromptBox, PhonePost, DMCard, MoodyGrade, Word } from './EditorialKit';
// eslint-disable-next-line @typescript-eslint/no-var-requires
const WORDS: Word[] = require('../public/r7490/cut_words.json');

// ريل تجريبي بستايل "الورق المقصوص" — كل جملة لها استعارة بصرية
const VIDEO_END = 666;
export const EDITORIAL_DURATION = VIDEO_END + 45;
const clamp = { extrapolateLeft: 'clamp', extrapolateRight: 'clamp' } as const;
const ease = Easing.bezier(0.22, 1, 0.36, 1);
const A = (n: string) => staticFile(`r7490/apps/${n}.png`);
const SFX_LEVELS: Record<string, number> = { chime: 0.325, drop: 0.031, heavy: 0.058, hit: 0.074, magic: 0.731, pop: 0.282, reverse: 0.065, riser: 0.056, whip: 0.229, whoosh: 0.191 };

const MASKS = { a: { t: 6.6, n: 78 }, b: { t: 11.0, n: 57 }, c: { t: 15.4, n: 96 } };
// الشخص المفصول (Magic Mask). outline = إطار حوله بلون التمييز يطلع مع تغيير الخلفية
const Person: React.FC<{ id: 'a' | 'b' | 'c'; outline?: [number, number] }> = ({ id, outline }) => {
  const f = useCurrentFrame();
  const { t, n } = MASKS[id];
  const i = f - s(t) + 1;
  if (i < 1 || i > n) return null;
  let filter = 'none';
  if (outline && f >= outline[0] && f < outline[1]) {
    const w = 7 * Math.min(interpolate(f, [outline[0], outline[0] + 8], [0, 1], clamp), interpolate(f, [outline[1] - 8, outline[1]], [1, 0], clamp));
    const c = ED.red;
    filter = [[w, 0], [-w, 0], [0, w], [0, -w], [w * 0.7, w * 0.7], [-w * 0.7, w * 0.7], [w * 0.7, -w * 0.7], [-w * 0.7, -w * 0.7]]
      .map(([x, y]) => `drop-shadow(${x}px ${y}px 0 ${c})`).join(' ') + ` drop-shadow(0 0 18px ${c})`;
  }
  return <Img src={staticFile(`r7490/mask_${id}/p_${String(i).padStart(4, '0')}.png`)} style={{ position: 'absolute', width: '100%', height: '100%', objectFit: 'cover', filter }} />;
};

function zoomAt(f: number) {
  const k = (a: number, b: number, x: number, y: number) => interpolate(f, [a, b], [x, y], { ...clamp, easing: ease });
  if (f >= s(17.2) && f < s(20.4)) return k(s(17.2), s(18.0), 1, 1.2);
  if (f >= s(20.4) && f < s(21.0)) return k(s(20.4), s(21.0), 1.2, 1);
  return 1.04 + 0.02 * Math.sin(f / 60); // حركة كاميرا خفيفة دائمة (مثل المرجع)
}

// خلفية بديلة خلف الشخص (Magic Mask)
const BgFill: React.FC<{ from: number; to: number; bg: string }> = ({ from, to, bg }) => {
  const f = useCurrentFrame();
  if (f < from || f >= to) return null;
  const o = Math.min(interpolate(f, [from, from + 8], [0, 1], clamp), interpolate(f, [to - 8, to], [1, 0], clamp));
  return <AbsoluteFill style={{ background: bg, opacity: o }} />;
};

// كلام كبير خلف الشخص — بخط الستايل وخط أحمر تحته
const Behind: React.FC<{ text: string; from: number; to: number }> = ({ text, from, to }) => {
  const f = useCurrentFrame();
  if (f < from || f >= to) return null;
  const sc = interpolate(f - from, [0, 6], [1.8, 1], { ...clamp, easing: ease });
  const o = interpolate(f, [from, from + 3, to - 4, to], [0, 1, 1, 0], clamp);
  const u = interpolate(f - from, [4, 12], [0, 100], clamp);
  return (
    <AbsoluteFill style={{ alignItems: 'center', opacity: o }}>
      <div style={{ marginTop: 470, transform: `scale(${sc})`, textAlign: 'center' }}>
        <div style={{ fontFamily: ED.font, fontWeight: 900, fontSize: 230, color: ED.ink, lineHeight: 1, direction: 'rtl', whiteSpace: 'nowrap' }}>{text}</div>
        <div style={{ height: 22, width: `${u}%`, background: ED.red, margin: '6px auto 0' }} />
      </div>
    </AbsoluteFill>
  );
};

const Sfx: React.FC<{ at: number; file: string; len?: number }> = ({ at, file, len }) => (
  <Sequence from={Math.max(0, at)} durationInFrames={len}><Audio src={staticFile(`r7490/sfx/${file}.mp3`)} volume={SFX_LEVELS[file] ?? 0.1} /></Sequence>
);

export const EditorialDemo: React.FC = () => {
  const f = useCurrentFrame();
  return (
    <AbsoluteFill style={{ background: 'black' }}>
      {/* ===== الفيديو + ماسكات الخلفية */}
      <AbsoluteFill style={{ transform: `scale(${zoomAt(f)})`, transformOrigin: '50% 38%' }}>
        <OffthreadVideo src={staticFile('r7490/graded.mp4')} style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
        {/* "غيّر الخلفية" → ورق بيج خلفه */}
        <BgFill from={s(7.6)} to={s(9.15)} bg={`linear-gradient(160deg, ${ED.beige}, #CFC4B0)`} />
        <Person id="a" outline={[s(7.6), s(9.15)]} />
        {/* "الكلام خلفي" */}
        <Behind text="خـلـفـي" from={s(11.4)} to={s(12.85)} />
        <Person id="b" />
        {/* "خلفية سودا" */}
        <BgFill from={s(15.92)} to={s(18.55)} bg="radial-gradient(ellipse 60% 45% at 50% 45%, #2a2a2a, #000 70%)" />
        <Person id="c" outline={[s(15.92), s(18.55)]} />
      </AbsoluteFill>
      <MoodyGrade strength={0.8} />

      {/* ===== 0.0 مقدمة إطار الكاميرا */}
      <Viewfinder from={0} to={s(1.0)} />

      {/* ===== 0.8 "يا كلود حط لي كلود هنا": أيقونة مقصوصة + ملصق أحمر */}
      <Cutout src={A('claude')} from={s(0.84)} to={s(3.9)} x={760} y={330} w={230} rot={8} round />
      <Tag text="كلود هنا" from={s(1.82)} to={s(3.9)} x={420} y={470} rot={-5} />

      {/* ===== 4.7 "حط لي ChatGPT": نفس معاملة كلود — نايف يبقى بالكادر + أيقونة مقصوصة + ملصق */}
      <Cutout src={A('chatgpt')} from={s(4.68)} to={s(6.6)} x={320} y={330} w={230} rot={-8} round />
      <Tag text="ChatGPT" from={s(4.9)} to={s(6.6)} x={660} y={470} rot={4} kind="black" size={78} />

      {/* ===== 6.6 "غيّر الخلفية": برومبت ينكتب ← الخلفية تتغير */}
      <PromptBox from={s(6.6)} to={s(7.7)} text="غيّر الخلفية" y={280} cps={1.2} />
      <Tag text="تم ✓" from={s(7.7)} to={s(9.0)} x={540} y={330} rot={-4} kind="black" size={70} />

      {/* ===== 9.8 "أصور بآيفون 16": نفس معاملة كلود — نايف بالكادر + أيقونة Apple + ملصق */}
      <Cutout src={A('apple')} from={s(9.78)} to={s(11.1)} x={760} y={330} w={230} rot={7} round />
      <Tag text="آيفون 16" from={s(9.95)} to={s(11.1)} x={400} y={470} rot={-4} kind="black" size={78} />

      {/* ===== 13.0 "يا كلود ظبطني" */}
      <Tag text="ظبطني" from={s(14.5)} to={s(15.6)} x={540} y={330} rot={-5} />

      {/* ===== 15.4 "خلفية سودا": الخلفية القديمة تنشطب */}
      <StrikeTag text="خلفية الصالة" from={s(15.4)} strikeAt={s(15.95)} to={s(17.0)} x={540} y={330} />
      <Tag text="خلفية سودا" from={s(16.5)} to={s(18.4)} x={540} y={430} rot={3} kind="paper" size={74} tape />

      {/* ===== 18.7 "وجهي أحسن": ختم كبير */}
      <Stamp text="أحسن" from={s(18.72)} to={s(20.3)} y={380} />

      {/* ===== 20.7 "انتهينا": منشور يتنشر ← محادثة خاص */}
      <PaperScene from={s(20.7)} to={EDITORIAL_DURATION + 10}>
        <PhonePost from={s(20.7)} publishAt={s(21.5)} to={s(22.4)} img={staticFile('r7490/still_me.jpg')} handle="@nalahmedy" y={820} />
        <DMCard from={s(22.3)} to={EDITORIAL_DURATION + 10} name="نايف الأحمدي" avatar={staticFile('r7490/avatar.jpg')} y={700}
          msgs={[{ text: 'تابعني وتواصل معي 🔔', at: s(22.5) }, { text: 'أبشر 👍', at: s(23.0), me: true }]} />
      </PaperScene>

      {/* ===== الكابشن الورقي (كلمة وحدة حمراء) */}
      <PaperCaption words={WORDS} hot={['كلود', 'ChatGPT', 'للخلفية', 'بآيفون', 'خلفي', 'ظبطني', 'سودا', 'احسن', 'انتهينا']} hide={[[20.7, 30]]} />

      {/* ===== المؤثرات (مستويات sfx_levels.py) */}
      <Sfx at={s(0.84)} file="pop" />
      <Sfx at={s(1.82) - 1} file="pop" />
      <Sfx at={s(4.68)} file="pop" />
      <Sfx at={s(4.9) - 1} file="pop" />
      <Sfx at={s(6.6)} file="whoosh" />
      <Sfx at={s(7.6) - 2} file="reverse" />
      <Sfx at={s(9.78)} file="pop" />
      <Sfx at={s(11.4) - 2} file="heavy" />
      <Sfx at={s(14.5)} file="pop" />
      <Sfx at={s(15.95)} file="hit" />
      <Sfx at={s(18.72) - 1} file="heavy" />
      <Sfx at={s(20.7) - 2} file="whoosh" />
      <Sfx at={s(21.5)} file="chime" len={25} />
      <Sfx at={s(22.5)} file="pop" />
    </AbsoluteFill>
  );
};
