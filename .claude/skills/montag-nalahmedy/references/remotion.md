# المسار B: المونتاج بالكود (Remotion + كلود كود)

الفيديو كله يُبنى ككود React ويُصدّر MP4 جاهز. نفس القواعد الذهبية في `SKILL.md` إلزامية.

## التجهيز
- مشروع Remotion داخل مجلد `montage-project/` بجانب الفيديو. إذا مو موجود:
  ```
  npx create-video@latest montage-project --template blank
  cd montage-project
  npm i @remotion/transitions @remotion/media-utils @remotion/google-fonts
  ```
- انسخ الخطوط (`Elgharib-*.otf/.ttf`) و `sfx/` واللقطات إلى `montage-project/public/`.
- **ffmpeg** يُثبّت تلقائياً مع Remotion للرندر. لقص الصمت مسبقاً يمكن استخدام `npx remotion ffmpeg`.

## البنية
```
src/
  Root.tsx              ← Composition (1920x1080 أو 1080x1920، 30fps)
  Montage.tsx           ← يقرأ edit_plan.json ويبني التايملاين
  transitions/          ← ملف لكل id من transitions.md
  overlays/AppName.tsx  ← أنماط pop_badge / type_glow / slam / neon_draw
  overlays/Graphics.tsx ← سهم، دائرة، عداد، قائمة، قبل/بعد
  effects/Zoom.tsx      ← زوم إن (interpolate + Easing.inOut)
  edit_plan.json        ← ناتج خطوة التحليل
```

## ربط كل عنصر بالكود

| العنصر | طريقة Remotion |
|---|---|
| انتقالات الحركة | `TransitionSeries` من `@remotion/transitions` (`slide`, `wipe`, `fade`, `flip`, `clockWipe`) + presentations مخصصة لـ `zoom_punch`, `glitch_rgb`, `spin_blur`, `light_leak`... |
| Magic Mask | افصل الشخص بـ `rembg` (راجع transitions.md، البديل الخارجي) إلى PNG sequence بقناة Alpha، ثم رتّب الطبقات: المقطع الجاي ← النص/الخلفية ← الشخص المفصول فوق |
| اسم التطبيق | `<Sequence from={frame}>` + `spring()` للدخول؛ الأنماط الأربعة في animations.md |
| الرسوم | SVG مع `strokeDashoffset` للرسم الذاتي، و `interpolate` للعداد |
| الزوم | `transform: scale()` + `transformOrigin` على مكان العنصر |
| المؤثرات الصوتية | `<Audio src={staticFile('sfx/...')} />` داخل نفس `Sequence` الخاص بالحركة، يبدأ قبلها بـ 2–3 فريم، `volume` ≈ 0.25–0.4 |
| الموسيقى + Ducking | `volume={(f) => ...}` ينخفض في فترات الكلام من التفريغ |

## مشروع جاهز (مجرّب)
القوالب المجرّبة موجودة داخل السكيل في `templates/` (`Montage.tsx` لفيديو كلام، `Rain.tsx` للقطات بدون كلام، `Root.tsx`، `index.ts`، `package.json`). انسخها لـ `montage-project/src/` (و`package.json` للجذر) وشغّل `npm install`. في `Montage.tsx`، وفيه المكونات جاهزة: `BehindWord` (slam/whip/neon)، `Person` (Magic Mask)، `PopBadge`، `Checklist`، `TypeGlow`، `EndCard`، `Sfx`، `zoomAt`.

## مشاكل معروفة وحلولها (على ويندوز)
- **`spawn EFTYPE` عند الرندر:** الويندوز يمنع متصفح Remotion. أضف دائماً:
  `--browser-executable="C:\Program Files\Google\Chrome\Application\chrome.exe"`
  وشغّل أوامر Remotion من PowerShell مو Bash.
- **ffmpeg مو مثبت بالنظام:** استخدم `python -c "import imageio_ffmpeg;print(imageio_ffmpeg.get_ffmpeg_exe())"`.
- **التفريغ:** `faster_whisper` على GPU يطلب `cublas64_12.dll` غير موجود؛ استخدم `device='cpu', compute_type='int8'` بموديل `medium` (30 ثانية تاخذ أقل من دقيقة).
- **Magic Mask:** استخدم دائماً `scripts/person_mask.py` (BiRefNet-portrait + تنعيم زمني ±2 فريم + feather). **لا تستخدم rembg فريم-فريم بدون تنعيم**: جربناه وطلعت رجفة واضحة بين الكلمة والخلفية وحدود الشخص (فرق لين 2.3% بين الفريمات). الموديل ياخذ ~7 ثواني/فريم على CPU، فعالج فقط النوافذ اللي فيها تأثير وشغّله بالخلفية. بعد التوليد افحص الرجفة (متوسط الفرق بين الفريمات لازم يكون أقل من 0.3%).
- **النص العربي:** أي كشف أو مسح للنص يبدأ من **اليمين** (`clipPath: inset(0 0 0 X%)`).
- **قص الصمت:** اقطع على مستوى ffmpeg (trim + concat) قبل Remotion، وحساب الأوقات يكون على الملف المقصوص.

## التحقق قبل الرندر النهائي
1. شغّل `npx remotion studio` وخل نايف يشوف المعاينة.
2. رندر عينة 10 ثواني حول أول انتقال Magic Mask:
   `npx remotion render Montage out/test.mp4 --frames=0-300`
3. بعد الموافقة: `npx remotion render Montage out/final.mp4 --codec=h264 --crf=18`
4. طبّق قائمة الفحص الموجودة في `SKILL.md`.
