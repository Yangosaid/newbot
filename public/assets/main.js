(() => {
  'use strict';
  const $ = (s, r = document) => r.querySelector(s);
  const $$ = (s, r = document) => [...r.querySelectorAll(s)];

  // Cabecera al hacer scroll
  const header = $('[data-header]');
  if (header && !header.classList.contains('is-scrolled')) {
    const onScroll = () => header.classList.toggle('is-scrolled', window.scrollY > 30);
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
  }

  // Menú móvil
  const menuBtn = $('[data-menu]');
  const mobileNav = $('[data-mobile-nav]');
  if (menuBtn && mobileNav) {
    const setOpen = (open) => {
      mobileNav.classList.toggle('is-open', open);
      menuBtn.setAttribute('aria-expanded', String(open));
      menuBtn.setAttribute('aria-label', open ? menuBtn.dataset.labelClose : menuBtn.dataset.labelOpen);
      document.body.style.overflow = open ? 'hidden' : '';
      if (open) header?.classList.add('is-scrolled');
    };
    menuBtn.addEventListener('click', () => setOpen(!mobileNav.classList.contains('is-open')));
    $$('a', mobileNav).forEach((a) => a.addEventListener('click', () => setOpen(false)));
    document.addEventListener('keydown', (e) => e.key === 'Escape' && setOpen(false));
  }

  // Cerrar el selector de idioma al hacer clic fuera
  document.addEventListener('click', (e) => {
    $$('details.lang[open]').forEach((d) => { if (!d.contains(e.target)) d.open = false; });
  });

  // Aparición suave
  const reveals = $$('.reveal');
  if ('IntersectionObserver' in window) {
    const io = new IntersectionObserver((entries) => entries.forEach((en) => {
      if (en.isIntersecting) { en.target.classList.add('is-in'); io.unobserve(en.target); }
    }), { rootMargin: '0px 0px -8% 0px' });
    reveals.forEach((el) => io.observe(el));
  } else reveals.forEach((el) => el.classList.add('is-in'));

  // Filtro de espacios
  const count = $('[data-count]');
  $$('[data-filter]').forEach((btn) => btn.addEventListener('click', () => {
    const f = btn.dataset.filter;
    $$('[data-filter]').forEach((b) => b.setAttribute('aria-pressed', String(b === btn)));
    let n = 0;
    $$('.space').forEach((s) => { const show = f === 'all' || s.dataset.cat === f; s.hidden = !show; if (show) { n++; s.classList.add('is-in'); } });
    if (count) count.textContent = `${count.dataset.label}: ${n}`;
  }));

  // Formulario de presupuesto
  const form = $('[data-quote]');
  const dataEl = $('#i18n');
  if (!form || !dataEl) return;
  const T = JSON.parse(dataEl.textContent);
  let step = 1;

  const steps = $$('[data-step]', form);
  const err = $('[data-error]', form);
  const status = $('[data-status]', form);
  const btnNext = $('[data-next]', form);
  const btnBack = $('[data-back]', form);
  const btnWa = $('[data-send-wa]', form);
  const btnMail = $('[data-send-mail]', form);
  const btnCopy = $('[data-copy]', form);
  const summary = $('[data-summary]', form);
  const stepLabel = $('[data-step-label]', form);

  const val = (name) => {
    const el = form.elements[name];
    if (!el) return '';
    if (el instanceof RadioNodeList) return el.value || '';
    return (el.value || '').trim();
  };
  const flash = (msg) => { status.textContent = msg; clearTimeout(flash.t); flash.t = setTimeout(() => (status.textContent = ''), 3500); };

  function message() {
    const m = T.msg;
    const lines = [m.hello, ''];
    const add = (label, v, suffix = '') => v && lines.push(`• ${label}: ${v}${suffix}`);
    add(m.reform, val('reform'));
    add(m.home, val('home'));
    add(m.town, val('town'));
    add(m.size, val('size'), val('size') && /^\d+([.,]\d+)?$/.test(val('size')) ? ' m²' : '');
    add(m.state, val('state'));
    add(m.timing, val('timing'));
    add(m.occupied, val('occupied'));
    add(m.name, val('name'));
    if (val('details')) lines.push('', `${m.idea}:`, val('details'));
    lines.push('', `${m.lang}: ${T.langName}`);
    return lines.join('\n');
  }

  function render() {
    steps.forEach((s) => (s.hidden = Number(s.dataset.step) !== step));
    $$('[data-progress]', form).forEach((li) => {
      const n = Number(li.dataset.progress);
      li.classList.toggle('is-active', n === step);
      li.classList.toggle('is-done', n < step);
    });
    stepLabel.textContent = `${T.stepOf.replace('{n}', step)}: ${T.steps[step - 1]}`;
    btnBack.hidden = step === 1;
    btnNext.hidden = step === 3;
    btnWa.hidden = btnMail.hidden = btnCopy.hidden = step !== 3;
    if (step === 3) updateSummary();
    err.textContent = '';
  }

  function updateSummary() {
    const text = message();
    summary.textContent = text;
    btnWa.href = `${T.wa}?text=${encodeURIComponent(text)}`;
    btnMail.href = `mailto:${T.email}?subject=${encodeURIComponent(`${T.msg.subject} — ${val('reform') || ''}`.trim())}&body=${encodeURIComponent(text)}`;
  }

  function validate() {
    if (step === 1 && !val('reform')) return T.errType;
    if (step === 2 && !val('town')) return T.errTown;
    if (step === 3 && !val('name')) return T.errName;
    return '';
  }

  const focusFirst = () => {
    const field = $(`[data-step="${step}"] input, [data-step="${step}"] select, [data-step="${step}"] textarea`, form);
    field?.focus({ preventScroll: true });
  };

  btnNext.addEventListener('click', () => {
    const e = validate();
    if (e) { err.textContent = e; return; }
    step = Math.min(3, step + 1);
    render();
    form.scrollIntoView({ behavior: 'smooth', block: 'start' });
    focusFirst();
  });
  btnBack.addEventListener('click', () => { step = Math.max(1, step - 1); render(); focusFirst(); });

  [btnWa, btnMail].forEach((b) => b.addEventListener('click', (e) => {
    updateSummary();
    const msg = validate();
    if (msg) { e.preventDefault(); err.textContent = msg; }
  }));
  form.addEventListener('input', () => { err.textContent = ''; if (step === 3) updateSummary(); });
  form.addEventListener('submit', (e) => e.preventDefault());

  btnCopy.addEventListener('click', async () => {
    try { await navigator.clipboard.writeText(message()); flash(T.copied); }
    catch { const r = document.createRange(); r.selectNodeContents(summary); const s = getSelection(); s.removeAllRanges(); s.addRange(r); }
  });

  // Borrador local (solo en el navegador de quien visita)
  const fields = ['home', 'town', 'size', 'state', 'timing', 'occupied', 'name', 'details'];
  const store = {
    get() { try { return JSON.parse(localStorage.getItem(T.storageKey) || 'null'); } catch { return null; } },
    set(v) { try { localStorage.setItem(T.storageKey, JSON.stringify(v)); return true; } catch { return false; } },
    del() { try { localStorage.removeItem(T.storageKey); } catch { /* sin almacenamiento */ } },
  };
  $('[data-save]', form).addEventListener('click', () => {
    const radio = $('input[name="reform"]:checked', form);
    const data = { reform: radio?.dataset.key || '', step };
    fields.forEach((f) => { const el = form.elements[f]; data[f] = el.tagName === 'SELECT' ? el.selectedIndex : el.value; });
    if (store.set(data)) flash(T.saved);
  });
  $('[data-clear]', form).addEventListener('click', () => {
    store.del(); form.reset(); step = 1; render(); flash(T.cleared);
  });
  const draft = store.get();
  if (draft) {
    if (draft.reform) { const r = $(`input[name="reform"][data-key="${draft.reform}"]`, form); if (r) r.checked = true; }
    fields.forEach((f) => {
      const el = form.elements[f]; if (!el || draft[f] == null) return;
      if (el.tagName === 'SELECT') el.selectedIndex = Number(draft[f]) || 0; else el.value = draft[f];
    });
  }

  // Botones «Quiero esta reforma»: preseleccionan el servicio y llevan al formulario
  $$('[data-pick]').forEach((b) => b.addEventListener('click', () => {
    const r = $(`input[name="reform"][data-key="${b.dataset.pick}"]`, form);
    if (r) r.checked = true;
    step = 1; render();
    form.scrollIntoView({ behavior: 'smooth', block: 'start' });
    r?.focus({ preventScroll: true });
  }));

  render();
})();
