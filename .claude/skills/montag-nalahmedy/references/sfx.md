# المؤثرات الصوتية

## المكان
مجلد `sfx\` داخل مجلد المستخدم، مقسّم بمجلدات حسب التصنيف، مثل:
camera, cinematic, dramatic, impact, keyboard, magic, mouse, nature, notifications, record, riser, sub, swoosh_whoosh, thick.
(أسماء الملفات المرفقة بالسكيل كلها حروف إنجليزية صغيرة وشرطة سفلية `_` بدون مسافات أو رموز. مجلد المستخدم ممكن تكون أسماؤه مختلفة؛ اقرأه وطابق بالمعنى.)

اقرأ محتوى المجلد كل مرة (ممكن المستخدم يضيف ملفات). **إذا المجلد مو موجود أو تصنيف ناقص:** ولّد البديل بـ `python scripts/make_sfx.py sfx` (مؤثرات مركّبة بالكود، بدون حقوق)، وقل للمستخدم إنه يقدر يستبدلها بمؤثرات أقوى لاحقاً. لا توقف المونتاج بسبب نقص المؤثرات.

## جدول الربط (الحدث ← مجلد ← ملفات مرفقة في `assets/sfx/`)

| الحدث | المجلد | ملفات مقترحة |
|---|---|---|
| انتقال قوي / زوم بنش | swoosh_whoosh / impact | swoosh_quick_stereo_whoosh, whoosh, energy_whoosh |
| ضربة كلمة كبيرة (slam) | cinematic / dramatic | cinematic_hit, heavy_dramatic_impact |
| وب بان / دخول من الجنب | swoosh_whoosh | whip_swoosh, swoosh_throwing_object |
| ظهور شعار / أيقونة / ✓ | notifications | pop_high, notification_bell |
| نص ينكتب | keyboard | typing_keyboard, mechanical_keyboards |
| نيون / توهج / سحر | magic | chime_bells_upwards_charm, practical_magic |
| بناء ترقّب قبل نتيجة | riser | riser_impact, cinematics_riser, tense_trailer_riser |
| كشف / قبل-بعد | impact | reverse_impact |
| فلاش / قطع | camera | camera_shutter |
| ضغط زر بالشاشة | mouse | best_mouse_click |
| توقف / رجوع للخلف | record | rewinding_song_decreasing_speed, vinyl_record_scratch_removing_needle |
| عداد / وقت | thick | clock_timer_digital_counter_ticking |
| نهاية / بيس قوي | sub | bass_drop_down_trailer_impact, sub_bass |
| حرق فيلم / نار | swoosh_whoosh / cinematic | whoosh_crackling_fade, fire_torch_transition_left_to_right |

البديل المولّد (`assets/sfx_generated/`): swoosh_whoosh/whoosh_1-3 و whip، impact/cinematic_hit و reverse_impact، sub/sub_drop، notifications/pop و ding، keyboard/typing، mouse/click، riser/riser، magic/shimmer، camera/shutter، glitch/glitch، ambient/soft_rain و calm_pad.

## القواعد
- **التوقيت:** المؤثر يبدأ قبل الحركة بـ 2–3 فريم (whoosh قبل الانتقال، pop على أول فريم ظهور).
- **المستوى — لا تخمّنه أبداً:** ملفات المؤثرات مو بنفس العلو (ضربة بيس ممكن تكون أعلى من whoosh بـ 20 dB)، فنفس الـ volume للكل = بعضها يغطي على الكلام. **دائماً** شغّل:
  `python scripts/sfx_levels.py <فيديو_الكلام> <ملفات المؤثرات...> --below 10`
  يطلع volume لكل ملف بحيث **أعلى لحظة بالمؤثر أوطى من الكلام بـ 10 dB** (8 للريلز الحماسية، 12 للشرح الهادي). استخدم القيم كما هي (Remotion: `volume={SFX_LEVELS[file]}`؛ DaVinci: `AudioVolume` = ‎20·log10(volume)).
  **غلطة مجرّبة:** volume ثابت 0.3–0.45 لكل المؤثرات + معايرة ‎-14 = ضربات البيس طلعت بنفس علو الكلام، والمستخدم اشتكى إنها «مرتفعة جداً».
- بدون فيديو كلام (B-roll): قارن بالموسيقى أو صوت المشهد بنفس الطريقة.
- **التنويع:** لا تستخدم نفس الملف مرتين ورا بعض، إلا pop في القوائم المتتالية.
- **التراكات (DaVinci):** A1 صوت نايف، A2 موسيقى (ducking -8 dB وقت الكلام)، A3 مؤثرات.
