// Genera el sitio estático en /public para los cinco idiomas.
// Uso: node src/build.mjs   (opcional: DEV_EMAIL=... para mostrar el correo del desarrollador en el pie)
import { mkdir, writeFile } from 'node:fs/promises';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { living, kitchen, bath, girona } from './scenes.mjs';
import es from './i18n/es.mjs';
import ca from './i18n/ca.mjs';
import fr from './i18n/fr.mjs';
import en from './i18n/en.mjs';
import ar from './i18n/ar.mjs';

const ROOT = join(dirname(fileURLToPath(import.meta.url)), '..', 'public');
const SITE = 'https://ymrefornova.com';
const LANGS = [es, ca, fr, en, ar];

const CO = {
  name: 'Y&M REFORNOVA SOCIEDAD LIMITADA',
  short: 'Y&M REFORNOVA',
  nif: 'B88687660',
  street: 'Calle Josep Aguilera i Marti, 17',
  zip: '17003', city: 'Girona', region: 'Catalunya',
  phone: '+34 688 45 75 30', tel: '+34688457530',
  wa: 'https://wa.me/34688457530',
  email: 'reformamacro@gmail.com',
  dev: { email: process.env.DEV_EMAIL || '', phone: process.env.DEV_PHONE || '+34 641 05 61 60' },
};
CO.dev.tel = CO.dev.phone.replace(/[^+\d]/g, '');

const esc = (s) => String(s).replace(/&(?!amp;|lt;|gt;|quot;|#)/g, '&amp;').replace(/"/g, '&quot;');
const ltr = (s) => `<bdi dir="ltr">${s}</bdi>`;

// Iconos de trazo
const I = {
  phone: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M22 16.9v3a2 2 0 0 1-2.2 2 19.8 19.8 0 0 1-8.6-3.1 19.5 19.5 0 0 1-6-6A19.8 19.8 0 0 1 2.1 4.2 2 2 0 0 1 4.1 2h3a2 2 0 0 1 2 1.7c.1.9.4 1.8.7 2.7a2 2 0 0 1-.5 2.1L8 9.8a16 16 0 0 0 6 6l1.3-1.3a2 2 0 0 1 2.1-.4c.9.3 1.8.6 2.7.7a2 2 0 0 1 1.7 2z"/></svg>',
  wa: '<svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M17.5 14.4c-.3-.1-1.8-.9-2-1-.3-.1-.5-.1-.7.1-.2.3-.8 1-.9 1.2-.2.2-.3.2-.6.1-.3-.1-1.3-.5-2.4-1.5-.9-.8-1.5-1.8-1.700-2.1-.2-.3 0-.5.1-.6l.4-.5c.2-.2.2-.3.3-.5.1-.2 0-.4 0-.5l-.9-2.2c-.2-.6-.5-.5-.7-.5h-.6c-.2 0-.5.1-.8.4-.3.3-1 1-1 2.5s1.1 2.9 1.2 3.1c.1.2 2.1 3.2 5.1 4.5.7.3 1.3.5 1.7.6.7.2 1.4.2 1.9.1.6-.1 1.8-.7 2-1.4.2-.7.2-1.3.2-1.4-.1-.2-.3-.3-.6-.4zM12 21.8c-1.8 0-3.5-.5-5-1.4l-.4-.2-3.700 1 1-3.6-.2-.4a9.8 9.8 0 1 1 8.3 4.6zm8.4-18.2A11.8 11.8 0 0 0 1.7 17.8L0 24l6.3-1.7a11.8 11.8 0 0 0 5.7 1.4A11.9 11.9 0 0 0 20.4 3.6z"/></svg>',
  mail: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><rect x="2" y="4" width="20" height="16" rx="2"/><path d="m22 6-10 7L2 6"/></svg>',
  pin: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0z"/><circle cx="12" cy="10" r="3"/></svg>',
  arrow: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" class="flip"><path d="M5 12h14M13 6l6 6-6 6"/></svg>',
  user: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>',
  doc: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><path d="M14 2v6h6M8 13h8M8 17h5"/></svg>',
  menu: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" aria-hidden="true"><path d="M3 7h18M3 12h18M3 17h18"/></svg>',
  globe: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><circle cx="12" cy="12" r="10"/><path d="M2 12h20M12 2a15 15 0 0 1 0 20M12 2a15 15 0 0 0 0 20"/></svg>',
};
const serviceIcons = {
  integral: '<svg class="service__icon" viewBox="0 0 48 48" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true"><path d="M6 22 24 8l18 14"/><path d="M10 19v21h28V19"/><path d="M10 30h14v10M24 19v21"/></svg>',
  kitchen: '<svg class="service__icon" viewBox="0 0 48 48" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true"><rect x="6" y="22" width="36" height="18"/><path d="M6 28h36M18 22v18M30 22v18"/><path d="M14 22v-6h8v6M30 16a4 4 0 0 1 8 0"/><path d="M34 8v4"/></svg>',
  bath: '<svg class="service__icon" viewBox="0 0 48 48" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true"><path d="M4 24h40v4a10 10 0 0 1-10 10H14A10 10 0 0 1 4 28z"/><path d="M10 24V10a4 4 0 0 1 8 0"/><path d="M14 38l-2 4M34 38l2 4"/></svg>',
  finish: '<svg class="service__icon" viewBox="0 0 48 48" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true"><rect x="8" y="6" width="28" height="10" rx="2"/><path d="M36 11h4v10H22v6"/><rect x="19" y="27" width="6" height="15" rx="1"/></svg>',
};

const logoMark = `<svg class="logo__mark" viewBox="0 0 44 40" aria-hidden="true"><path d="M3 38V17L22 3l19 14v21z" fill="none" stroke="#b79a71" stroke-width="1.6"/><text x="22" y="32" text-anchor="middle" font-family="Fraunces, Georgia, serif" font-size="14" fill="currentColor">Y&amp;M</text></svg>`;
const logo = (href) => `<a class="logo" href="${href}" aria-label="Y&amp;M REFORNOVA">${logoMark}<span class="logo__text" dir="ltr"><span class="logo__name">REFORNOVA</span><span class="logo__sub">SOCIEDAD LIMITADA</span></span></a>`;

const pageUrl = (L, page) => (page === 'home' ? L.path : L.path + L.privacySlug);

function head(t, page) {
  const title = page === 'home' ? t.meta.title : `${t.privacyPage.title} | ${CO.short}`;
  const desc = page === 'home' ? t.meta.description : t.privacyPage.lead;
  const canonical = SITE + pageUrl(t, page);
  const alternates = LANGS.map((L) => `<link rel="alternate" hreflang="${L.code}" href="${SITE + pageUrl(L, page)}">`).join('\n  ')
    + `\n  <link rel="alternate" hreflang="x-default" href="${SITE + pageUrl(es, page)}">`;
  const fonts = t.code === 'ar'
    ? 'family=Amiri:ital,wght@0,400;0,700;1,400&family=IBM+Plex+Sans+Arabic:wght@400;500;700&family=Manrope:wght@400;600;700;800'
    : 'family=Fraunces:ital,opsz,wght@0,9..144,300..600;1,9..144,300..600&family=Manrope:wght@400;500;600;700;800';
  const ld = {
    '@context': 'https://schema.org', '@type': 'HomeAndConstructionBusiness',
    name: CO.name, alternateName: CO.short, url: SITE + '/', telephone: CO.tel, email: CO.email, taxID: CO.nif,
    image: SITE + '/assets/og-image.png', logo: SITE + '/assets/favicon.svg',
    address: { '@type': 'PostalAddress', streetAddress: CO.street, postalCode: CO.zip, addressLocality: CO.city, addressRegion: CO.region, addressCountry: 'ES' },
    areaServed: { '@type': 'City', name: 'Girona' },
    founder: { '@type': 'Person', name: 'Youssef', jobTitle: 'Propietario' },
    knowsLanguage: LANGS.map((L) => L.code),
    makesOffer: t.services.items.map((s) => ({ '@type': 'Offer', itemOffered: { '@type': 'Service', name: s.seo } })),
  };
  const faqLd = page === 'home' ? {
    '@context': 'https://schema.org', '@type': 'FAQPage', inLanguage: t.code,
    mainEntity: t.faq.items.map((f) => ({ '@type': 'Question', name: f.q, acceptedAnswer: { '@type': 'Answer', text: f.a } })),
  } : null;
  return `<!doctype html>
<html lang="${t.htmlLang}" dir="${t.dir}">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${esc(title)}</title>
  <meta name="description" content="${esc(desc)}">
  <link rel="canonical" href="${canonical}">
  ${alternates}
  <meta name="theme-color" content="#263c32">
  <meta property="og:type" content="website">
  <meta property="og:site_name" content="${CO.short}">
  <meta property="og:title" content="${esc(title)}">
  <meta property="og:description" content="${esc(desc)}">
  <meta property="og:url" content="${canonical}">
  <meta property="og:locale" content="${t.locale}">
  <meta property="og:image" content="${SITE}/assets/og-image.png">
  <meta name="twitter:card" content="summary_large_image">
  <link rel="icon" href="/assets/favicon.svg" type="image/svg+xml">
  <link rel="apple-touch-icon" href="/assets/apple-touch-icon.png">
  <link rel="preconnect" href="https://fonts.googleapis.com">
  <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
  <link rel="stylesheet" href="https://fonts.googleapis.com/css2?${fonts}&display=swap">
  <link rel="stylesheet" href="/assets/styles.css">
  <script type="application/ld+json">${JSON.stringify(ld)}</script>${faqLd ? `\n  <script type="application/ld+json">${JSON.stringify(faqLd)}</script>` : ''}
</head>`;
}

const langLinks = (t, page) => LANGS.map((L) => `<li><a href="${pageUrl(L, page)}" hreflang="${L.code}" lang="${L.code}"${L.code === t.code ? ' aria-current="true"' : ''}>${L.name}</a></li>`).join('');

function header(t, page) {
  const base = page === 'home' ? '' : t.path;
  const n = t.nav;
  const links = `<a href="${base}#servicios">${n.services}</a><a href="${base}#espacios">${n.spaces}</a><a href="${base}#proceso">${n.process}</a><a href="${base}#preguntas">${n.faq}</a><a href="${base}#contacto">${n.contact}</a>`;
  return `<a class="skip" href="#main">${t.ui.skip}</a>
<header class="header${page === 'home' ? '' : ' is-scrolled'}" data-header>
  <div class="wrap header__inner">
    ${logo(t.path)}
    <nav class="nav" aria-label="Menu">${links}</nav>
    <div class="header__tools">
      <details class="lang">
        <summary aria-label="${t.ui.lang}">${I.globe}<span>${t.code.toUpperCase()}</span></summary>
        <ul>${langLinks(t, page)}</ul>
      </details>
      <a class="btn btn--gold header__cta" href="${base}#presupuesto">${n.cta}</a>
      <button class="menu-btn" type="button" aria-expanded="false" aria-controls="mobile-nav" aria-label="${t.ui.menu}" data-label-open="${t.ui.menu}" data-label-close="${t.ui.close}" data-menu>${I.menu}</button>
    </div>
  </div>
</header>
<nav class="mobile-nav" id="mobile-nav" aria-label="Menu" data-mobile-nav>
  ${links}
  <a class="btn btn--gold" href="${base}#presupuesto">${n.cta}</a>
  <ul class="mobile-nav__langs">${langLinks(t, page)}</ul>
</nav>`;
}

function footer(t, page) {
  const base = page === 'home' ? '' : t.path;
  const f = t.footer;
  const devLines = [
    CO.dev.email && `<a href="mailto:${CO.dev.email}">${CO.dev.email}</a>`,
    CO.dev.phone && `<a href="tel:${CO.dev.tel}">${ltr(CO.dev.phone)}</a>`,
  ].filter(Boolean).join('<br>');
  return `<footer class="footer">
  <div class="wrap">
    <div class="footer__grid">
      <div>
        ${logo(t.path)}
        <p class="footer__tagline">${f.tagline}</p>
      </div>
      <div>
        <h4>${f.services}</h4>
        <ul>${t.services.items.map((s) => `<li><a href="${base}#servicios">${s.seo}</a></li>`).join('')}<li><a href="${base}#guia">${f.guide}</a></li></ul>
      </div>
      <div>
        <h4>${f.company}</h4>
        <ul>
          <li><strong>${CO.name}</strong></li>
          <li>${f.desc} · NIF ${CO.nif}</li>
          <li>${f.office}<br>${CO.street}<br>${CO.zip} ${CO.city}</li>
          <li><a href="tel:${CO.tel}">${ltr(CO.phone)}</a></li>
          <li><a href="mailto:${CO.email}">${CO.email}</a></li>
        </ul>
      </div>
      <div>
        <h4>${f.languages}</h4>
        <ul>${langLinks(t, page)}</ul>
        ${devLines ? `<div class="footer__dev">
          <h4>${f.devTitle}</h4>
          <p>${f.devText}</p>
          <p>${devLines}</p>
        </div>` : ''}
      </div>
    </div>
    <div class="footer__bottom">
      <span>© ${new Date().getFullYear()} ${CO.short}</span>
      <nav aria-label="Legal"><a href="${t.path + t.privacySlug}">${f.privacy}</a><a href="${t.path + t.privacySlug}#empresa">${f.company_info}</a><a href="#top">${t.ui.backTop} ↑</a></nav>
    </div>
  </div>
</footer>
<nav class="mobile-bar" aria-label="${t.nav.contact}">
  <a href="tel:${CO.tel}">${I.phone}${t.bar.call}</a>
  <a class="is-main" href="${base}#presupuesto">${I.doc}${t.bar.quote}</a>
  <a href="${CO.wa}" target="_blank" rel="noopener">${I.wa}${t.bar.whatsapp}</a>
</nav>`;
}

const radio = (name, value, label, i) =>
  `<div class="option"><input type="radio" id="${name}-${i}" name="${name}" value="${esc(label)}" data-key="${value}"><label for="${name}-${i}">${label}</label></div>`;
const select = (name, label, opts) =>
  `<div class="field"><label for="f-${name}">${label}</label><select id="f-${name}" name="${name}"><option value="">—</option>${opts.map((o) => `<option>${o}</option>`).join('')}</select></div>`;

function home(t) {
  const q = t.quote;
  const scenes = { living, kitchen, bath };
  const clientStrings = { ...q, langName: t.name, wa: CO.wa, email: CO.email, storageKey: 'ymr-quote-draft' };
  return `${head(t, 'home')}
<body id="top">
${header(t, 'home')}
<main id="main">
  <section class="hero">
    <span class="hero__corner hero__corner--tl" aria-hidden="true"></span>
    <div class="wrap hero__grid">
      <div class="reveal">
        <p class="eyebrow">${t.hero.eyebrow}</p>
        <h1>${t.hero.h1.map((l) => `<span>${l}</span>`).join('')}</h1>
        <p class="lead">${t.hero.lead}</p>
        <div class="hero__actions">
          <a class="btn btn--gold" href="#presupuesto">${t.hero.cta1} ${I.arrow}</a>
          <a class="btn btn--ghost" href="#servicios">${t.hero.cta2}</a>
        </div>
        <div class="hero__contact">
          <a href="tel:${CO.tel}">${I.phone}${ltr(CO.phone)}</a>
          <a href="${CO.wa}" target="_blank" rel="noopener">${I.wa}${t.hero.whatsapp}</a>
        </div>
      </div>
      <div class="hero__visual reveal">
        ${living(t.hero.imgAlt, 'hero')}
        <p class="hero__tag"><small>${t.hero.tagSmall}</small><strong>${t.hero.tag}</strong></p>
      </div>
    </div>
    <span class="hero__corner hero__corner--br" aria-hidden="true"></span>
  </section>

  <div class="strip">
    <div class="wrap">
      <ul class="strip__list">${t.strip.map((s, i) => `<li><span>0${i + 1}</span>${s}</li>`).join('')}</ul>
    </div>
  </div>

  <section class="section" id="servicios">
    <div class="wrap">
      <div class="section-head reveal">
        <div><p class="eyebrow">${t.services.eyebrow}</p><h2>${t.services.h2}</h2></div>
        <p class="lead">${t.services.lead}</p>
      </div>
      <div class="services">
        ${t.services.items.map((s, i) => `<article class="service reveal">
          <div class="service__top"><span class="service__num">0${i + 1}</span>${serviceIcons[s.key]}</div>
          <h3>${s.title}</h3>
          <p>${s.text}</p>
          <ul class="chips">${s.chips.map((c) => `<li>${c}</li>`).join('')}</ul>
          <div class="service__foot"><small>${s.seo}</small><button type="button" class="link-arrow" data-pick="${s.key}">${t.services.want} ${I.arrow}</button></div>
        </article>`).join('')}
      </div>
      <p class="trades reveal">${t.services.trades}</p>
      <div class="pillars reveal">
        <p class="pillars__title">${t.pillars.title}</p>
        <div class="pillars__list">${t.pillars.items.map((p, i) => `<div class="pillar"><span>0${i + 1}</span><h4>${p.t}</h4><p>${p.p}</p></div>`).join('')}</div>
      </div>
    </div>
  </section>

  <section class="section section--green" id="espacios">
    <div class="wrap">
      <div class="section-head reveal">
        <div><p class="eyebrow">${t.spaces.eyebrow}</p><h2>${t.spaces.h2}</h2></div>
        <p class="lead">${t.spaces.lead}</p>
      </div>
      <div class="filters" role="group" aria-label="${t.spaces.count}">
        ${Object.entries(t.spaces.filters).map(([k, v]) => `<button type="button" class="filter" data-filter="${k}" aria-pressed="${k === 'all'}">${v}</button>`).join('')}
        <output data-count data-label="${t.spaces.count}">${t.spaces.count}: 3</output>
      </div>
      <div class="spaces">
        ${t.spaces.items.map((s, i) => `<article class="space reveal" data-cat="${s.cat}">
          ${scenes[s.cat](s.alt, 'sp' + i)}
          <div class="space__body">
            <div class="space__meta"><span>${s.label}</span><span dir="ltr">0${i + 1} / 03</span></div>
            <h3>${s.title}</h3>
            <ul class="chips">${s.chips.map((c) => `<li>${c}</li>`).join('')}</ul>
            <p>${s.text}</p>
            <button type="button" class="link-arrow" data-pick="${s.cat === 'living' ? 'integral' : s.cat}">${t.spaces.want} ${I.arrow}</button>
          </div>
        </article>`).join('')}
      </div>
      <div class="palette reveal">
        <div><p class="eyebrow">${t.palette.eyebrow}</p><h2>${t.palette.h2}</h2><p class="lead">${t.palette.lead}</p></div>
        <div class="swatches">${['wood', 'stone', 'sand', 'gold'].map((k, i) => `<div class="swatch swatch--${k}"><span>${t.palette.swatches[i]}</span></div>`).join('')}</div>
      </div>
    </div>
  </section>

  <section class="section section--sand" id="proceso">
    <div class="wrap">
      <div class="section-head reveal">
        <div><p class="eyebrow">${t.process.eyebrow}</p><h2>${t.process.h2}</h2></div>
        <p class="lead">${t.process.lead}</p>
      </div>
      <ol class="steps">${t.process.steps.map((s, i) => `<li class="step reveal"><span class="step__n">0${i + 1}</span><h3>${s.t}</h3><p>${s.p}</p></li>`).join('')}</ol>
    </div>
  </section>

  <section class="section" id="girona">
    <div class="wrap girona">
      <div class="reveal">
        <p class="eyebrow">${t.girona.eyebrow}</p>
        <h2>${t.girona.h2}</h2>
        <p class="lead girona__p">${t.girona.p}</p>
        <p class="girona__note">${t.girona.note}</p>
        <a class="btn btn--green" href="#presupuesto">${t.girona.cta} ${I.arrow}</a>
      </div>
      <figure class="girona__card reveal">
        ${girona(t.girona.alt, 'gi')}
        <figcaption><small>${t.girona.capSmall}</small><strong>${t.girona.cap}</strong></figcaption>
      </figure>
    </div>
  </section>

  <section class="section section--sand" id="preguntas">
    <div class="wrap">
      <div class="faq-grid">
        <div class="reveal">
          <p class="eyebrow">${t.faq.eyebrow}</p>
          <h2>${t.faq.h2}</h2>
          <div class="guide-card" id="guia">
            <p class="eyebrow">${t.guide.eyebrow}</p>
            <h3>${t.guide.title}</h3>
            <p>${t.guide.text}</p>
            <details class="guide">
              <summary class="link-arrow">${t.guide.open}</summary>
              <ol>${t.guide.points.map((p) => `<li>${p}</li>`).join('')}</ol>
            </details>
          </div>
        </div>
        <div class="faq reveal">
          ${t.faq.items.map((f, i) => `<details${i === 0 ? ' open' : ''}><summary>${f.q}</summary><p>${f.a}</p></details>`).join('')}
        </div>
      </div>
      <div class="prep reveal">
        <div><p class="eyebrow">${t.prep.eyebrow}</p><h3 class="pillars__title">${t.prep.title}</h3><p class="lead">${t.prep.lead}</p></div>
        <div class="prep__list">${t.prep.items.map((p, i) => `<div class="prep__item"><span>0${i + 1}</span><h4>${p.t}</h4><p>${p.p}</p></div>`).join('')}</div>
      </div>
    </div>
  </section>

  <section class="section section--green" id="contacto">
    <div class="wrap contact">
      <div class="reveal">
        <p class="eyebrow">${t.contact.eyebrow}</p>
        <h2>${t.contact.h2}</h2>
        <p class="lead contact__lead">${t.contact.lead}</p>
        <ul class="contact__list">
          <li><a href="tel:${CO.tel}">${I.phone}<span><small>${t.contact.call}</small><strong>${ltr(CO.phone)}</strong></span></a></li>
          <li><a href="mailto:${CO.email}">${I.mail}<span><small>${t.contact.email}</small><strong>${CO.email}</strong></span></a></li>
          <li><div>${I.pin}<span><small>${t.contact.location}</small><strong>${t.contact.locationValue}</strong></span></div></li>
        </ul>
        <div class="contact__actions">
          <a class="btn btn--gold" href="${CO.wa}" target="_blank" rel="noopener">${I.wa}${t.contact.whatsapp}</a>
          <a class="btn btn--ghost" href="/assets/youssef-ymrefornova.vcf" download>${I.user}${t.contact.save}</a>
        </div>
        <p class="contact__hint">${t.contact.hint}</p>
      </div>

      <form class="quote reveal" id="presupuesto" novalidate data-quote>
        <p class="quote__brand" dir="ltr">${CO.short}</p>
        <h3>${q.title}</h3>
        <p>${q.intro}</p>
        <ol class="progress" aria-hidden="true">${q.steps.map((s, i) => `<li data-progress="${i + 1}"><b>${i + 1}</b>${s}</li>`).join('')}</ol>
        <p class="sr-only" aria-live="polite" data-step-label></p>

        <div data-step="1">
          <fieldset class="fieldset">
            <legend>${q.s1Legend}</legend>
            <div class="options">${t.services.items.map((s, i) => radio('reform', s.key, s.title, i)).join('')}</div>
            <div class="field"><label for="f-home">${q.homeType}</label><select id="f-home" name="home">${q.homeTypes.map((o) => `<option>${o}</option>`).join('')}</select></div>
            <small class="quote__note">${q.s1Note}</small>
          </fieldset>
        </div>

        <div data-step="2" hidden>
          <fieldset class="fieldset">
            <legend>${q.s2Legend}</legend>
            <div class="row2">
              <div class="field"><label for="f-town">${q.town}</label><input id="f-town" name="town" autocomplete="address-level2" placeholder="${q.townPh}" dir="auto"></div>
              <div class="field"><label for="f-size">${q.size}</label><input id="f-size" name="size" inputmode="numeric" placeholder="${q.sizePh}"></div>
            </div>
            ${select('state', q.state, q.states)}
            <div class="row2">${select('timing', q.timing, q.timings)}${select('occupied', q.occupied, q.occupiedOpts)}</div>
          </fieldset>
        </div>

        <div data-step="3" hidden>
          <fieldset class="fieldset">
            <legend>${q.s3Legend}</legend>
            <div class="field"><label for="f-name">${q.name}</label><input id="f-name" name="name" autocomplete="name" placeholder="${q.namePh}" dir="auto"></div>
            <div class="field"><label for="f-details">${q.details}</label><textarea id="f-details" name="details" placeholder="${q.detailsPh}" dir="auto"></textarea><small>${q.photos}</small></div>
            <div class="field"><span class="field__label">${q.review}</span><pre class="summary" data-summary dir="auto"></pre></div>
          </fieldset>
        </div>

        <p class="quote__error" role="alert" data-error></p>
        <div class="quote__nav">
          <button type="button" class="btn btn--ghost" data-back hidden>${q.back}</button>
          <span class="spacer"></span>
          <button type="button" class="btn btn--green" data-next>${q.next} ${I.arrow}</button>
          <a class="btn btn--gold" data-send-wa hidden target="_blank" rel="noopener" href="${CO.wa}">${I.wa}${q.sendWa}</a>
          <a class="btn btn--green" data-send-mail hidden href="mailto:${CO.email}">${I.mail}${q.sendMail}</a>
        </div>
        <div class="quote__nav quote__nav--small">
          <button type="button" class="text-btn" data-save>${q.saveDraft}</button>
          <button type="button" class="text-btn" data-clear>${q.clearDraft}</button>
          <button type="button" class="text-btn" data-copy hidden>${q.copy}</button>
          <span class="status" role="status" data-status></span>
        </div>
        <p class="quote__privacy">${q.privacy}</p>
      </form>
    </div>
  </section>
</main>
${footer(t, 'home')}
<script id="i18n" type="application/json">${JSON.stringify(clientStrings).replace(/</g, '\\u003c')}</script>
<script src="/assets/main.js" defer></script>
</body>
</html>
`;
}

function privacy(t) {
  const p = t.privacyPage;
  return `${head(t, 'privacy')}
<body id="top">
${header(t, 'privacy')}
<main id="main">
  <section class="page-hero">
    <div class="wrap">
      <p class="eyebrow">${CO.short}</p>
      <h1>${p.title}</h1>
      <p class="lead">${p.lead}</p>
    </div>
  </section>
  <section class="section">
    <div class="wrap prose">
      <h2 id="empresa">${p.companyTitle}</h2>
      <table>
        <tr><th>${p.rows.name}</th><td>${CO.name}</td></tr>
        <tr><th>${p.rows.nif}</th><td>${CO.nif}</td></tr>
        <tr><th>${p.rows.office}</th><td>${CO.street}, ${CO.zip} ${CO.city} (${CO.region})</td></tr>
        <tr><th>${p.rows.phone}</th><td><a href="tel:${CO.tel}">${ltr(CO.phone)}</a></td></tr>
        <tr><th>${p.rows.email}</th><td><a href="mailto:${CO.email}">${CO.email}</a></td></tr>
        <tr><th>${p.rows.activity}</th><td>${p.activity}</td></tr>
      </table>
      ${p.sections.map((s) => `<h2>${s.h}</h2>${s.p.map((x) => `<p>${x}</p>`).join('')}`).join('\n      ')}
      <p class="prose__back"><a class="btn btn--green" href="${t.path}">${p.back}</a></p>
    </div>
  </section>
</main>
${footer(t, 'privacy')}
<script src="/assets/main.js" defer></script>
</body>
</html>
`;
}

async function out(rel, content) {
  const file = join(ROOT, rel);
  await mkdir(dirname(file), { recursive: true });
  await writeFile(file, content);
}

for (const t of LANGS) {
  const dir = t.path.replace(/^\//, '');
  await out(join(dir, 'index.html'), home(t));
  await out(join(dir, t.privacySlug), privacy(t));
}

const today = new Date().toISOString().slice(0, 10);
const urls = ['home', 'privacy'].flatMap((page) => LANGS.map((L) => `  <url>
    <loc>${SITE + pageUrl(L, page)}</loc>
    <lastmod>${today}</lastmod>
${LANGS.map((A) => `    <xhtml:link rel="alternate" hreflang="${A.code}" href="${SITE + pageUrl(A, page)}"/>`).join('\n')}
  </url>`));
await out('sitemap.xml', `<?xml version="1.0" encoding="UTF-8"?>
<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" xmlns:xhtml="http://www.w3.org/1999/xhtml">
${urls.join('\n')}
</urlset>
`);
await out('robots.txt', `User-agent: *\nAllow: /\n\nSitemap: ${SITE}/sitemap.xml\n`);
await out('assets/youssef-ymrefornova.vcf', [
  'BEGIN:VCARD', 'VERSION:3.0', 'N:;Youssef;;;', 'FN:Youssef', `ORG:${CO.name}`, 'TITLE:Propietario',
  `TEL;TYPE=WORK,VOICE:${CO.tel}`, `EMAIL;TYPE=WORK:${CO.email}`, `URL:${SITE}/`,
  `ADR;TYPE=WORK:;;${CO.street};${CO.city};${CO.region};${CO.zip};España`, 'END:VCARD', '',
].join('\r\n'));
await out('assets/favicon.svg', `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64"><rect width="64" height="64" rx="10" fill="#263c32"/><path d="M12 52V27L32 12l20 15v25z" fill="none" stroke="#b79a71" stroke-width="2.5"/><text x="32" y="45" text-anchor="middle" font-family="Georgia, serif" font-size="17" fill="#f8f6f0">Y&amp;M</text></svg>\n`);

console.log(`Built ${LANGS.length * 2} pages into ${ROOT}`);
