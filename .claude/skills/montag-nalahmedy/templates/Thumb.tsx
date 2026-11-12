import React from 'react';
import { AbsoluteFill, Img, continueRender, delayRender, staticFile } from 'remotion';

// صورة مصغرة لمقدمة القناة — 1280×720 (تترندر ×2 = 2560×1440)
const ACC = '#D97757';
const FONT = 'Tajarib';
const fh = delayRender('font');
new FontFace(FONT, `url(${staticFile('fonts/Elgharib-Tajarib-Taweel-Bold.otf')})`).load().then((f) => { (document.fonts as any).add(f); continueRender(fh); });

export const Thumb: React.FC = () => {
  const rim = [[6, 0], [-6, 0], [0, 6], [0, -6], [4, 4], [-4, 4], [4, -4], [-4, -4]].map(([x, y]) => `drop-shadow(${x}px ${y}px 0 ${ACC})`).join(' ');
  return (
    <AbsoluteFill style={{ background: '#0B0B14', overflow: 'hidden' }}>
      {/* يسار: قبل/بعد من تصويرك (اليوم الوطني) مقسومة بخط مائل */}
      <AbsoluteFill style={{ width: 760, overflow: 'hidden' }}>
        <Img src={staticFile('yt/ba_before.png')} style={{ position: 'absolute', height: '100%', left: -220, filter: 'brightness(0.95)' }} />
        <div style={{ position: 'absolute', inset: 0, clipPath: 'polygon(55% 0, 100% 0, 100% 100%, 35% 100%)' }}>
          <Img src={staticFile('yt/ba_after.png')} style={{ position: 'absolute', height: '100%', left: -220 }} />
        </div>
        <div style={{ position: 'absolute', top: 0, bottom: 0, left: '35%', width: 8, background: 'white', transform: 'skewX(-15.6deg)', transformOrigin: 'bottom', boxShadow: `0 0 24px ${ACC}` }} />
        <div style={{ position: 'absolute', left: 26, bottom: 30, background: 'rgba(235,235,235,0.95)', color: '#222', fontFamily: FONT, fontSize: 38, padding: '0 18px 6px', transform: 'rotate(-3deg)' }}>قبل</div>
        <div style={{ position: 'absolute', left: 330, bottom: 30, background: ACC, color: 'white', fontFamily: FONT, fontSize: 38, padding: '0 18px 6px', transform: 'rotate(3deg)', border: '3px solid white' }}>بعد</div>
        <AbsoluteFill style={{ background: 'linear-gradient(90deg, transparent 78%, #0B0B14 100%)' }} />
      </AbsoluteFill>

      {/* توهج خلف نايف */}
      <div style={{ position: 'absolute', right: 40, top: 120, width: 560, height: 560, borderRadius: 999, background: `radial-gradient(circle, ${ACC}66, transparent 65%)` }} />
      {/* نايف مفصول بإطار برتقالي */}
      <Img src={staticFile('yt/me_cut.png')} style={{ position: 'absolute', right: -30, bottom: 0, height: 640, filter: `${rim} drop-shadow(0 0 22px ${ACC}aa)` }} />

      {/* النص */}
      <div style={{ position: 'absolute', left: 40, top: 34, direction: 'rtl', textAlign: 'right', width: 700 }}>
        <div style={{ display: 'inline-block', background: ACC, color: 'white', fontFamily: FONT, fontSize: 104, lineHeight: 1.15, padding: '0 28px 14px', transform: 'rotate(-3deg)', border: '5px solid white', boxShadow: '0 14px 30px rgba(0,0,0,0.5)' }}>ابدأ من هنا</div>
        <div style={{ marginTop: 22, display: 'inline-block', background: '#0B0B14E6', color: 'white', fontFamily: FONT, fontSize: 36, padding: '2px 18px 8px', transform: 'rotate(1.5deg)', whiteSpace: 'nowrap' }}>ذكاء اصطناعي · تصوير · صناعة محتوى</div>
      </div>
    </AbsoluteFill>
  );
};
