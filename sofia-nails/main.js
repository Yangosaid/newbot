(() => {
  'use strict';
  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => [...r.querySelectorAll(s)];
  const EMAIL = 'contact@sofianailsbrussels.com';
  // Textes : français par défaut, remplacés par le bloc #i18n des autres langues
  const T = Object.assign({
    from: 'dès ', fromLong: 'à partir de ', later: 'à convenir', hello: 'Bonjour Sofia,',
    want: 'Je souhaite prendre rendez-vous pour :', total: 'Total indicatif', date: 'Date souhaitée',
    moment: 'Moment', any: 'indifférent', name: 'Nom', phone: 'Téléphone', message: 'Message', thanks: 'Merci !',
    errSoin: 'Choisissez au moins un soin.', errNom: 'Indiquez votre prénom et votre nom.',
    copied: 'Message copié.', copyFail: 'Copie impossible sur cet appareil.', subject: 'Rendez-vous Sofia Nails',
    add: 'Ajouter {s} au rendez-vous', remove: 'Retirer {s} du rendez-vous', lang: '', locale: 'fr-BE',
  }, (() => { try { return JSON.parse($('#i18n')?.textContent || '{}'); } catch { return {}; } })());
  const euro = (n) => `${n} €`;

  // En-tête
  const header = $('[data-header]');
  const onScroll = () => header.classList.toggle('is-scrolled', scrollY > 10);
  onScroll(); addEventListener('scroll', onScroll, { passive: true });

  // Onglets des tarifs
  const tabs = $$('[data-tab]');
  const selectTab = (name, focus) => tabs.forEach((t) => {
    const on = t.dataset.tab === name;
    t.setAttribute('aria-selected', on); t.tabIndex = on ? 0 : -1;
    $('#' + t.getAttribute('aria-controls')).hidden = !on;
    if (on && focus) t.focus();
  });
  tabs.forEach((t, i) => {
    t.addEventListener('click', () => selectTab(t.dataset.tab));
    t.addEventListener('keydown', (e) => {
      if (e.key !== 'ArrowRight' && e.key !== 'ArrowLeft') return;
      const next = tabs[(i + (e.key === 'ArrowRight' ? 1 : tabs.length - 1)) % tabs.length];
      selectTab(next.dataset.tab, true);
    });
  });
  $$('[data-tab-link]').forEach((a) => a.addEventListener('click', () => selectTab(a.dataset.tabLink)));

  // Liste des soins, lue depuis la grille des tarifs (source unique)
  const services = $$('.prices li').map((li, i) => ({
    id: 's' + i,
    name: li.dataset.service,
    fr: li.dataset.fr || li.dataset.service,
    price: Number(li.dataset.price),
    from: li.hasAttribute('data-from'),
    group: li.closest('.group').querySelector('h3').textContent,
    li,
  }));

  // Sélecteur dans le formulaire
  const picker = $('[data-picker]');
  const groups = [...new Set(services.map((s) => s.group))];
  picker.innerHTML = groups.map((g) => `<div><h4>${g}</h4><div class="chips">${services.filter((s) => s.group === g).map((s) =>
    `<span class="chip"><input type="checkbox" id="${s.id}" value="${s.id}"><label for="${s.id}">${s.name} <b>${s.from ? T.from : ''}${euro(s.price)}</b></label></span>`).join('')}</div></div>`).join('');

  // Boutons « + » dans la grille des tarifs
  services.forEach((s) => {
    const b = document.createElement('button');
    b.type = 'button'; b.className = 'add'; b.textContent = '+';
    b.setAttribute('aria-pressed', 'false');
    b.setAttribute('aria-label', T.add.replace('{s}', s.name));
    b.addEventListener('click', () => { const c = $('#' + s.id); c.checked = !c.checked; update(); });
    s.li.append(b);
    s.btn = b;
  });

  const totalEl = $('[data-total]');
  const stickyTotal = $('[data-sticky-total]');
  const chosen = () => services.filter((s) => $('#' + s.id).checked);
  function update() {
    const list = chosen();
    const sum = list.reduce((a, s) => a + s.price, 0);
    const from = list.some((s) => s.from);
    totalEl.textContent = (from ? T.from : '') + euro(sum);
    stickyTotal.textContent = list.length ? `${list.length} · ${euro(sum)}` : '';
    services.forEach((s) => {
      const on = $('#' + s.id).checked;
      s.btn.setAttribute('aria-pressed', on); s.btn.textContent = on ? '✓' : '+';
      s.btn.setAttribute('aria-label', (on ? T.remove : T.add).replace('{s}', s.name));
    });
  }
  picker.addEventListener('change', update);

  // Date minimale : aujourd'hui
  const date = $('[data-date]');
  const d = new Date(); d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
  date.min = d.toISOString().slice(0, 10);

  // Message de réservation
  const form = $('[data-form]');
  const err = $('[data-error]');
  const status = $('[data-status]');
  function message() {
    const f = form.elements;
    const list = chosen();
    const sum = list.reduce((a, s) => a + s.price, 0);
    const sep = T.locale.startsWith('fr') ? ' :' : ':';
    const when = f.date.value ? new Date(f.date.value + 'T12:00').toLocaleDateString(T.locale, { weekday: 'long', day: 'numeric', month: 'long' }) : T.later;
    // Pour Sofia, le nom français du soin est ajouté entre parenthèses dans les autres langues
    const label = (s) => (s.fr !== s.name ? `${s.name} (${s.fr})` : s.name);
    const opt = f.moment.options[f.moment.selectedIndex];
    const mom = f.moment.selectedIndex > 0 ? (opt.text !== opt.value ? `${opt.text} (${opt.value})` : opt.text) : T.any;
    return [
      T.hello, '', T.want,
      ...list.map((s) => `• ${label(s)} — ${s.from ? T.fromLong : ''}${euro(s.price)}`),
      `${T.total}${sep} ${list.some((s) => s.from) ? T.fromLong : ''}${euro(sum)}`, '',
      `${T.date}${sep} ${when}`,
      `${T.moment}${sep} ${mom}`, '',
      `${T.name}${sep} ${f.nom.value.trim()}`,
      f.tel.value.trim() ? `${T.phone}${sep} ${f.tel.value.trim()}` : null,
      f.message.value.trim() ? `\n${T.message}${sep} ${f.message.value.trim()}` : null,
      T.lang ? `\n(${T.lang})` : null,
      '', T.thanks,
    ].filter((l) => l !== null).join('\n');
  }
  function check() {
    if (!chosen().length) return T.errSoin;
    if (!form.elements.nom.value.trim()) return T.errNom;
    return '';
  }
  form.addEventListener('submit', (e) => {
    e.preventDefault();
    const m = check(); err.textContent = m; if (m) return;
    const subject = `${T.subject} — ${form.elements.nom.value.trim()}`;
    location.href = `mailto:${EMAIL}?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(message())}`;
  });
  $('[data-wa]').addEventListener('click', () => {
    const m = check(); err.textContent = m; if (m) return;
    window.open(`https://wa.me/32470701705?text=${encodeURIComponent(message())}`, '_blank', 'noopener');
  });
  $('[data-copy]').addEventListener('click', async () => {
    const m = check(); err.textContent = m; if (m) return;
    try { await navigator.clipboard.writeText(message()); status.textContent = T.copied; }
    catch { status.textContent = T.copyFail; }
    setTimeout(() => (status.textContent = ''), 3000);
  });
  form.addEventListener('input', () => (err.textContent = ''));

  // Barre mobile : visible seulement entre le hero et le formulaire
  const sticky = $('[data-sticky]');
  if ('IntersectionObserver' in window) {
    const seen = new Map();
    const io = new IntersectionObserver((entries) => {
      entries.forEach((en) => seen.set(en.target, en.isIntersecting));
      sticky.classList.toggle('is-hidden', [...seen.values()].some(Boolean));
    }, { threshold: 0.15 });
    io.observe($('.hero')); io.observe($('#rendez-vous'));
  } else sticky.classList.remove('is-hidden');

  // Galerie : agrandissement
  const box = $('[data-lightbox]');
  if (box && box.showModal) {
    $$('[data-full]').forEach((b) => b.addEventListener('click', () => {
      const img = b.querySelector('img');
      box.querySelector('img').src = b.dataset.full;
      box.querySelector('img').alt = img.alt;
      box.querySelector('p').textContent = b.dataset.caption;
      box.showModal();
    }));
    $('[data-close]', box).addEventListener('click', () => box.close());
    box.addEventListener('click', (e) => { if (e.target === box) box.close(); });
  }

  $('[data-year]').textContent = new Date().getFullYear();
  update();
})();
