// Ilustraciones vectoriales de los espacios (sustituibles por fotografías reales).
const wrap = (alt, body, id) =>
  `<svg class="scene" viewBox="0 0 600 450" role="img" aria-label="${alt}" xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="xMidYMid slice">${body.replaceAll('ID', id)}</svg>`;

export const living = (alt, id = 'lv') => wrap(alt, `
<defs>
  <linearGradient id="ID-wall" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#efe6d8"/><stop offset="1" stop-color="#e2d4bf"/></linearGradient>
  <linearGradient id="ID-sky" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#f9eedb"/><stop offset="1" stop-color="#e9d7b7"/></linearGradient>
  <linearGradient id="ID-floor" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#c9ab86"/><stop offset="1" stop-color="#a8865f"/></linearGradient>
  <radialGradient id="ID-light" cx=".35" cy=".3" r=".8"><stop offset="0" stop-color="#fff7e6" stop-opacity=".9"/><stop offset="1" stop-color="#fff7e6" stop-opacity="0"/></radialGradient>
</defs>
<rect width="600" height="450" fill="url(#ID-wall)"/>
<rect x="60" y="40" width="330" height="270" fill="url(#ID-sky)"/>
<path d="M60 250 Q150 205 230 232 T390 220 V310 H60Z" fill="#c8b48f" opacity=".55"/>
<path d="M60 270 Q170 240 260 262 T390 258 V310 H60Z" fill="#a99a73" opacity=".5"/>
<g stroke="#8a7354" stroke-width="5" fill="none"><rect x="60" y="40" width="330" height="270"/><line x1="170" y1="40" x2="170" y2="310"/><line x1="280" y1="40" x2="280" y2="310"/></g>
<rect x="430" y="40" width="130" height="270" fill="#e6d9c4"/>
<g fill="#d3c3a8"><rect x="446" y="70" width="98" height="4"/><rect x="446" y="150" width="98" height="4"/><rect x="446" y="230" width="98" height="4"/></g>
<rect x="458" y="44" width="16" height="26" fill="#8f6c4a"/><rect x="478" y="50" width="12" height="20" fill="#b79a71"/><circle cx="520" cy="138" r="12" fill="#c7b08d"/><rect x="455" y="200" width="40" height="30" fill="#9d7b58"/><rect x="505" y="214" width="26" height="16" fill="#e9dfcf"/>
<rect y="310" width="600" height="140" fill="url(#ID-floor)"/>
<g stroke="#9c7a54" stroke-width="1.5" opacity=".55"><line x1="0" y1="340" x2="600" y2="340"/><line x1="0" y1="378" x2="600" y2="378"/><line x1="0" y1="420" x2="600" y2="420"/></g>
<ellipse cx="300" cy="392" rx="250" ry="40" fill="#e8dcc6" opacity=".9"/>
<path d="M88 340 Q86 270 160 262 L420 262 Q500 268 500 340 L500 372 Q500 384 486 384 L102 384 Q88 384 88 372Z" fill="#efe5d4"/>
<path d="M110 300 Q112 280 140 278 L450 278 Q478 280 478 300 L478 334 L110 334Z" fill="#e3d6bf"/>
<rect x="118" y="290" width="70" height="44" rx="18" fill="#d9c7a6"/><rect x="396" y="290" width="70" height="44" rx="18" fill="#cdb48c"/>
<path d="M88 340 L500 340 L500 372 Q500 384 486 384 L102 384 Q88 384 88 372Z" fill="#e6d8c0"/>
<ellipse cx="300" cy="408" rx="96" ry="16" fill="#8e7a62" opacity=".25"/>
<path d="M214 392 Q214 372 300 372 Q386 372 386 392 L380 404 Q300 418 220 404Z" fill="#cfc4b4"/>
<ellipse cx="300" cy="380" rx="86" ry="14" fill="#ddd4c6"/>
<circle cx="276" cy="374" r="9" fill="#b79a71"/><rect x="306" y="366" width="26" height="10" rx="3" fill="#8a6446"/>
<path d="M548 384 C540 330 560 300 548 250 M548 330 C565 310 585 312 590 300 M548 300 C530 285 515 290 506 276" stroke="#5d7a5f" stroke-width="5" fill="none" stroke-linecap="round"/>
<path d="M528 384 L568 384 L562 420 L534 420Z" fill="#b8a185"/>
<rect width="600" height="450" fill="url(#ID-light)"/>`, id);

export const kitchen = (alt, id = 'kt') => wrap(alt, `
<defs>
  <linearGradient id="ID-wall" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#ece5da"/><stop offset="1" stop-color="#dcd2c3"/></linearGradient>
  <linearGradient id="ID-wood" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#4a3323"/><stop offset=".5" stop-color="#5c4030"/><stop offset="1" stop-color="#432e20"/></linearGradient>
  <linearGradient id="ID-marble" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#faf7f2"/><stop offset="1" stop-color="#e1dbd1"/></linearGradient>
  <radialGradient id="ID-glow" cx=".5" cy="0" r=".7"><stop offset="0" stop-color="#ffe9b8" stop-opacity=".9"/><stop offset="1" stop-color="#ffe9b8" stop-opacity="0"/></radialGradient>
</defs>
<rect width="600" height="450" fill="url(#ID-wall)"/>
<rect x="30" y="40" width="540" height="150" fill="url(#ID-wood)"/>
<g stroke="#2f2017" stroke-width="2"><line x1="120" y1="40" x2="120" y2="190"/><line x1="210" y1="40" x2="210" y2="190"/><line x1="390" y1="40" x2="390" y2="190"/><line x1="480" y1="40" x2="480" y2="190"/></g>
<rect x="210" y="40" width="180" height="150" fill="#e9e1d4"/>
<rect x="222" y="54" width="156" height="122" fill="#f6efe2"/>
<g stroke="#c9bba5" stroke-width="2"><line x1="300" y1="54" x2="300" y2="176"/><line x1="222" y1="115" x2="378" y2="115"/></g>
<rect x="30" y="190" width="540" height="56" fill="url(#ID-marble)"/>
<path d="M60 200 Q140 230 220 210 M330 236 Q420 205 540 226" stroke="#cfc6b8" stroke-width="1.5" fill="none"/>
<rect x="30" y="246" width="540" height="10" fill="#f3efe8"/>
<rect x="30" y="256" width="540" height="94" fill="url(#ID-wood)"/>
<g stroke="#2f2017" stroke-width="2"><line x1="120" y1="256" x2="120" y2="350"/><line x1="210" y1="256" x2="210" y2="350"/><line x1="300" y1="256" x2="300" y2="350"/><line x1="390" y1="256" x2="390" y2="350"/><line x1="480" y1="256" x2="480" y2="350"/></g>
<g fill="#b79a71"><rect x="96" y="270" width="3" height="20"/><rect x="186" y="270" width="3" height="20"/><rect x="276" y="270" width="3" height="20"/><rect x="366" y="270" width="3" height="20"/><rect x="456" y="270" width="3" height="20"/></g>
<rect y="350" width="600" height="100" fill="#c5b49c"/>
<g stroke="#b3a188" stroke-width="1.5"><line x1="0" y1="385" x2="600" y2="385"/><line x1="0" y1="420" x2="600" y2="420"/></g>
<rect x="110" y="300" width="380" height="24" fill="url(#ID-marble)"/>
<path d="M140 308 Q240 318 320 306 T470 314" stroke="#d2c9bb" stroke-width="1.5" fill="none"/>
<rect x="124" y="324" width="352" height="90" fill="#3e2b1e"/>
<rect x="124" y="324" width="352" height="6" fill="#2b1d14"/>
<g stroke="#b79a71" stroke-width="3"><line x1="160" y1="340" x2="160" y2="436"/><line x1="440" y1="340" x2="440" y2="436"/></g>
<rect x="140" y="336" width="40" height="8" rx="2" fill="#b79a71"/><rect x="420" y="336" width="40" height="8" rx="2" fill="#b79a71"/>
<g stroke="#8f7350" stroke-width="1.5"><line x1="200" y1="0" x2="200" y2="210"/><line x1="300" y1="0" x2="300" y2="210"/><line x1="400" y1="0" x2="400" y2="210"/></g>
<g fill="#c7a76f"><path d="M178 210 Q200 186 222 210Z"/><path d="M278 210 Q300 186 322 210Z"/><path d="M378 210 Q400 186 422 210Z"/></g>
<rect x="100" y="200" width="400" height="140" fill="url(#ID-glow)"/>
<path d="M330 300 Q338 284 352 290 Q362 282 372 300Z" fill="#7d9a6a"/><rect x="236" y="288" width="40" height="12" rx="4" fill="#d6c3a5"/>`, id);

export const bath = (alt, id = 'bt') => wrap(alt, `
<defs>
  <linearGradient id="ID-wall" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#e9e3d9"/><stop offset="1" stop-color="#d8cfc1"/></linearGradient>
  <linearGradient id="ID-glass" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#ffffff" stop-opacity=".45"/><stop offset="1" stop-color="#cfe0dc" stop-opacity=".2"/></linearGradient>
  <radialGradient id="ID-mirror" cx=".5" cy=".5" r=".6"><stop offset=".72" stop-color="#ece7de"/><stop offset=".86" stop-color="#fff2d6"/><stop offset="1" stop-color="#fff2d6" stop-opacity="0"/></radialGradient>
</defs>
<rect width="600" height="450" fill="url(#ID-wall)"/>
<g stroke="#cfc4b4" stroke-width="1.2" opacity=".8">
  <line x1="0" y1="90" x2="600" y2="90"/><line x1="0" y1="170" x2="600" y2="170"/><line x1="0" y1="250" x2="600" y2="250"/><line x1="0" y1="330" x2="600" y2="330"/>
  <line x1="100" y1="0" x2="100" y2="370"/><line x1="200" y1="0" x2="200" y2="370"/><line x1="300" y1="0" x2="300" y2="370"/><line x1="400" y1="0" x2="400" y2="370"/><line x1="500" y1="0" x2="500" y2="370"/>
</g>
<rect x="30" y="30" width="220" height="370" fill="#d3c8b6"/>
<rect x="30" y="30" width="220" height="370" fill="url(#ID-glass)" stroke="#9fa49b" stroke-width="3"/>
<line x1="140" y1="30" x2="140" y2="400" stroke="#9fa49b" stroke-width="2"/>
<rect x="160" y="60" width="50" height="6" rx="3" fill="#b79a71"/><rect x="183" y="60" width="4" height="70" fill="#b79a71"/><circle cx="185" cy="140" r="9" fill="#b79a71"/>
<g stroke="#ffffff" stroke-width="2" opacity=".55"><line x1="54" y1="300" x2="110" y2="80"/><line x1="70" y1="330" x2="120" y2="140"/></g>
<ellipse cx="430" cy="140" rx="92" ry="100" fill="url(#ID-mirror)"/>
<ellipse cx="430" cy="140" rx="68" ry="74" fill="#e6e1d7" stroke="#b79a71" stroke-width="2.5"/>
<path d="M392 108 Q420 96 438 118" stroke="#fff" stroke-width="3" fill="none" opacity=".7"/>
<rect x="310" y="252" width="240" height="16" fill="#efe9df"/>
<ellipse cx="430" cy="252" rx="54" ry="10" fill="#f8f4ed" stroke="#d7cdbf"/>
<rect x="426" y="226" width="4" height="22" fill="#b79a71"/><rect x="426" y="226" width="20" height="4" fill="#b79a71"/>
<rect x="310" y="268" width="240" height="96" fill="#7a5a40"/>
<g stroke="#5c4230" stroke-width="2"><line x1="430" y1="268" x2="430" y2="364"/></g>
<rect x="398" y="304" width="24" height="3" fill="#b79a71"/><rect x="438" y="304" width="24" height="3" fill="#b79a71"/>
<rect y="370" width="600" height="80" fill="#bfb3a2"/>
<rect y="370" width="600" height="4" fill="#a99c89"/>
<rect x="320" y="226" width="22" height="26" rx="4" fill="#c9b79b"/><path d="M520 252 Q514 222 526 210 Q538 222 532 252Z" fill="#7d9a6a"/>
<rect x="270" y="190" width="10" height="120" rx="2" fill="#b79a71" opacity=".7"/><rect x="262" y="200" width="26" height="80" rx="4" fill="#ede6da"/>`, id);

export const girona = (alt, id = 'gi') => wrap(alt, `
<defs>
  <linearGradient id="ID-sky" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#2c463a"/><stop offset="1" stop-color="#4b6352"/></linearGradient>
  <linearGradient id="ID-water" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#365246"/><stop offset="1" stop-color="#1d2f27"/></linearGradient>
</defs>
<rect width="600" height="450" fill="url(#ID-sky)"/>
<circle cx="480" cy="90" r="34" fill="#d8c4a5" opacity=".85"/>
<path d="M0 230 Q120 200 240 215 T600 205 V300 H0Z" fill="#3a5446" opacity=".7"/>
<g fill="#b79a71" opacity=".95">
  <rect x="250" y="120" width="60" height="110"/><path d="M250 120 L280 70 L310 120Z"/><rect x="274" y="40" width="12" height="34"/>
  <rect x="200" y="150" width="60" height="80"/><rect x="300" y="160" width="90" height="70"/>
</g>
<g fill="#1d2f27" opacity=".55"><rect x="270" y="140" width="20" height="30" rx="10"/><circle cx="280" cy="105" r="8"/></g>
<g>
  <rect x="0" y="230" width="62" height="100" fill="#c88a5a"/><rect x="62" y="215" width="54" height="115" fill="#e0c07d"/>
  <rect x="116" y="238" width="58" height="92" fill="#a9563f"/><rect x="174" y="222" width="50" height="108" fill="#e6d6b8"/>
  <rect x="224" y="232" width="64" height="98" fill="#d39b5f"/><rect x="288" y="214" width="52" height="116" fill="#b2674a"/>
  <rect x="340" y="236" width="60" height="94" fill="#e9c98e"/><rect x="400" y="220" width="56" height="110" fill="#c47b52"/>
  <rect x="456" y="234" width="70" height="96" fill="#ddb57a"/><rect x="526" y="218" width="74" height="112" fill="#a65a43"/>
</g>
<g fill="#1d2f27" opacity=".5">
  ${Array.from({ length: 10 }, (_, i) => [8, 70, 124, 182, 234, 296, 348, 408, 466, 536][i]).map((x) =>
    `<rect x="${x + 8}" y="250" width="10" height="16"/><rect x="${x + 30}" y="250" width="10" height="16"/><rect x="${x + 8}" y="284" width="10" height="16"/><rect x="${x + 30}" y="284" width="10" height="16"/>`).join('')}
</g>
<rect y="330" width="600" height="120" fill="url(#ID-water)"/>
<g opacity=".35">
  <rect x="0" y="334" width="62" height="40" fill="#c88a5a"/><rect x="62" y="334" width="54" height="46" fill="#e0c07d"/><rect x="116" y="334" width="58" height="36" fill="#a9563f"/>
  <rect x="174" y="334" width="50" height="44" fill="#e6d6b8"/><rect x="224" y="334" width="64" height="38" fill="#d39b5f"/><rect x="288" y="334" width="52" height="46" fill="#b2674a"/>
  <rect x="340" y="334" width="60" height="36" fill="#e9c98e"/><rect x="400" y="334" width="56" height="44" fill="#c47b52"/><rect x="456" y="334" width="70" height="38" fill="#ddb57a"/><rect x="526" y="334" width="74" height="44" fill="#a65a43"/>
</g>
<path d="M120 352 Q300 300 480 352" stroke="#b72f2a" stroke-width="6" fill="none"/>
<g stroke="#b72f2a" stroke-width="2"><line x1="170" y1="340" x2="170" y2="352"/><line x1="230" y1="328" x2="230" y2="352"/><line x1="300" y1="322" x2="300" y2="352"/><line x1="370" y1="328" x2="370" y2="352"/><line x1="430" y1="340" x2="430" y2="352"/></g>
<line x1="110" y1="352" x2="490" y2="352" stroke="#b72f2a" stroke-width="4"/>
<g stroke="#d8c4a5" stroke-width="1.5" opacity=".35"><line x1="40" y1="400" x2="140" y2="400"/><line x1="320" y1="415" x2="460" y2="415"/><line x1="200" y1="432" x2="280" y2="432"/></g>`, id);
