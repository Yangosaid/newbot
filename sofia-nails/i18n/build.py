"""Génère /nl/, /en/ et /es/ à partir de index.html (français, la source).

Usage : python3 i18n/build.py   (depuis le dossier sofia-nails)
Échoue si un texte français n'a pas de traduction.
"""
import json, re, sys
from html import unescape
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(ROOT / 'i18n'))
from translations import T, SAME, JS, LANGS, ORDER  # noqa: E402

ALL = ('fr',) + ORDER
SITE = 'https://sofianailsbrussels.com'


def url(lang):
    return SITE + '/' + ('' if lang == 'fr' else f'{lang}/')


SHORT = {'fr': 'FR', **{k: v['short'] for k, v in LANGS.items()}}
NAMES = {'fr': 'Français', **{k: v['name'] for k, v in LANGS.items()}}
LABEL = {'fr': 'Langue', 'nl': 'Taal', 'en': 'Language', 'es': 'Idioma'}
SAME = set(SAME) | set(SHORT.values()) | set(NAMES.values()) | set(LABEL.values()) | {'#4a2c34', 'fr_BE', 'summary_large_image', 'website', 'width=device-width, initial-scale=1', '×'}
TRANS_ATTRS = ('alt', 'aria-label', 'placeholder', 'content', 'title', 'data-caption')
LOCAL = re.compile(r'^(?!https?:|mailto:|tel:|#|/|\.\./|data:)(styles\.css|fonts\.css|fonts/|main\.js|favicon\.svg|apple-touch-icon\.png|site\.webmanifest|img/|logo/)')


def norm(t):
    return ' '.join(unescape(t).split())


PAGES = ('index.html', 'confidentialite.html')


def leaf(page):
    return '' if page == 'index.html' else page


def href_to(target, current, page='index.html'):
    """Lien relatif vers la même page dans une autre langue."""
    up = '' if current == 'fr' else '../'
    return (up + ('' if target == 'fr' else f'{target}/') + leaf(page)) or './'


def blocks(src, lang, page='index.html'):
    links = ''.join(
        f'<a href="{href_to(l, lang, page)}" hreflang="{l}" lang="{l}" title="{NAMES[l]}"'
        f'{" aria-current=\"true\"" if l == lang else ""}>{SHORT[l]}</a>' for l in ALL)
    nav = f'<nav class="langs" aria-label="{LABEL[lang]}">{links}</nav>'
    alt = ''.join(f'<link rel="alternate" hreflang="{l}" href="{url(l) + leaf(page)}">' for l in ALL)
    alt += f'<link rel="alternate" hreflang="x-default" href="{url("fr") + leaf(page)}">'
    canon = f'<link rel="canonical" href="{url(lang) + leaf(page)}"><meta property="og:url" content="{url(lang) + leaf(page)}">'
    src = re.sub(r'<!--canon-->.*?<!--/canon-->', f'<!--canon-->{canon}<!--/canon-->', src, flags=re.S)
    src = re.sub(r'<!--langs-->.*?<!--/langs-->', f'<!--langs-->{nav}<!--/langs-->', src, flags=re.S)
    src = re.sub(r'<!--langs-foot-->.*?<!--/langs-foot-->', f'<!--langs-foot-->{nav}<!--/langs-foot-->', src, flags=re.S)
    src = re.sub(r'<!--hreflang-->.*?<!--/hreflang-->', f'<!--hreflang-->{alt}<!--/hreflang-->', src, flags=re.S)
    return src


def translate(src, lang):
    i = ORDER.index(lang)
    missing = set()

    def tr(text):
        key = norm(text)
        if not key or not re.search(r'[A-Za-zÀ-ÿ]', key) or key in SAME:
            return None
        if key in T:
            return T[key][i]
        missing.add(key)
        return None

    # Nœuds de texte (hors script/style)
    parts = re.split(r'(<script\b.*?</script>|<style\b.*?</style>|<!--.*?-->)', src, flags=re.S)
    for n, part in enumerate(parts):
        if n % 2:
            continue

        def text_node(m):
            raw = m.group(1)
            lead, core, trail = re.match(r'(\s*)(.*?)(\s*)$', raw, re.S).groups()
            out = tr(core)
            return '>' + (lead + out + trail if out is not None else raw) + '<'
        parts[n] = re.sub(r'>([^<>]+)<', text_node, part)
    src = ''.join(parts)

    # Attributs
    def attr(m):
        name, val = m.group(1), m.group(2)
        if val.startswith('http') or val.isdigit():
            return m.group(0)
        if name == 'content' and LOCAL.match(val):
            return m.group(0)
        out = tr(val)
        return f'{name}="{out}"' if out is not None else m.group(0)
    src = re.sub(r'\b(' + '|'.join(TRANS_ATTRS) + r')="([^"]*)"', attr, src)

    # Noms des soins (lus par le script) + nom français conservé pour Sofia
    def service(m):
        fr = m.group(1)
        return f'data-service="{T[fr][i]}" data-fr="{fr}"'
    src = re.sub(r'data-service="([^"]+)"', service, src)

    if missing:
        sys.exit(f'[{lang}] textes sans traduction :\n  ' + '\n  '.join(sorted(missing)))
    return src


def relocate(src):
    """Chemins des ressources locales depuis un sous-dossier."""
    def fix(m):
        name, val = m.group(1), m.group(2)
        items = [x.strip() for x in val.split(',')] if 'srcset' in name else [val]
        out = ['../' + x if LOCAL.match(x) else x for x in items]
        return f'{name}="{", ".join(out)}"'
    return re.sub(r'\b(href|src|srcset|imagesrcset|content)="([^"]*)"', fix, src)


def main():
    for name in PAGES:
        src_path = ROOT / name
        src = blocks(src_path.read_text(encoding='utf-8'), 'fr', name)
        src_path.write_text(src, encoding='utf-8')

        for lang in ORDER:
            L = LANGS[lang]
            page = translate(blocks(src, lang, name), lang)
            page = page.replace('<html lang="fr-BE">', f'<html lang="{L["html"]}">', 1)
            page = page.replace('content="fr_BE"', f'content="{L["og"]}"', 1)
            page = relocate(page)
            if '<script src="../main.js" defer></script>' in page:
                data = json.dumps(JS[lang], ensure_ascii=False).replace('</', '<\\/')
                page = page.replace('<script src="../main.js" defer></script>',
                                    f'<script id="i18n" type="application/json">{data}</script>\n<script src="../main.js" defer></script>', 1)
                assert 'id="i18n"' in page
            out = ROOT / lang / name
            out.parent.mkdir(exist_ok=True)
            out.write_text(page, encoding='utf-8')
            print(f'{lang}/{name}')

    # Plan du site et robots.txt pour Google
    from datetime import date
    entries = ''
    for name in PAGES:
        alts = ''.join(f'\n    <xhtml:link rel="alternate" hreflang="{l}" href="{url(l) + leaf(name)}"/>' for l in ALL)
        entries += ''.join(f'\n  <url>\n    <loc>{url(l) + leaf(name)}</loc>\n    <lastmod>{date.today()}</lastmod>{alts}\n  </url>' for l in ALL)
    (ROOT / 'sitemap.xml').write_text(
        '<?xml version="1.0" encoding="UTF-8"?>\n<urlset xmlns="http://www.sitemaps.org/schemas/sitemap/0.9" '
        f'xmlns:xhtml="http://www.w3.org/1999/xhtml">{entries}\n</urlset>\n', encoding='utf-8')
    (ROOT / 'robots.txt').write_text(f'User-agent: *\nAllow: /\n\nSitemap: {SITE}/sitemap.xml\n', encoding='utf-8')
    print('sitemap.xml, robots.txt')


if __name__ == '__main__':
    main()
