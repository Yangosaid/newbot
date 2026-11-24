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
SHORT = {'fr': 'FR', **{k: v['short'] for k, v in LANGS.items()}}
NAMES = {'fr': 'Français', **{k: v['name'] for k, v in LANGS.items()}}
LABEL = {'fr': 'Langue', 'nl': 'Taal', 'en': 'Language', 'es': 'Idioma'}
SAME = set(SAME) | set(SHORT.values()) | set(NAMES.values()) | set(LABEL.values()) | {'#4a2c34', 'fr_BE', 'summary_large_image', 'website', 'width=device-width, initial-scale=1', '×'}
TRANS_ATTRS = ('alt', 'aria-label', 'placeholder', 'content', 'title', 'data-caption')
LOCAL = re.compile(r'^(?!https?:|mailto:|tel:|#|/|\.\./|data:)(styles\.css|main\.js|favicon\.svg|apple-touch-icon\.png|img/|logo/)')


def norm(t):
    return ' '.join(unescape(t).split())


def href_to(target, current):
    """Lien relatif d'une page de langue vers une autre."""
    up = '' if current == 'fr' else '../'
    return up + ('' if target == 'fr' else f'{target}/') or './'


def blocks(src, lang):
    links = ''.join(
        f'<a href="{href_to(l, lang) or "./"}" hreflang="{l}" lang="{l}" title="{NAMES[l]}"'
        f'{" aria-current=\"true\"" if l == lang else ""}>{SHORT[l]}</a>' for l in ALL)
    nav = f'<nav class="langs" aria-label="{LABEL[lang]}">{links}</nav>'
    alt = ''.join(f'<link rel="alternate" hreflang="{l}" href="{href_to(l, lang) or "./"}">' for l in ALL)
    alt += f'<link rel="alternate" hreflang="x-default" href="{href_to("fr", lang) or "./"}">'
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
    src_path = ROOT / 'index.html'
    src = blocks(src_path.read_text(encoding='utf-8'), 'fr')
    src_path.write_text(src, encoding='utf-8')

    for lang in ORDER:
        L = LANGS[lang]
        page = translate(blocks(src, lang), lang)
        page = page.replace('<html lang="fr-BE">', f'<html lang="{L["html"]}">', 1)
        page = page.replace('content="fr_BE"', f'content="{L["og"]}"', 1)
        page = relocate(page)
        data = json.dumps(JS[lang], ensure_ascii=False).replace('</', '<\\/')
        page = page.replace('<script src="../main.js" defer></script>',
                            f'<script id="i18n" type="application/json">{data}</script>\n<script src="../main.js" defer></script>', 1)
        assert 'id="i18n"' in page
        out = ROOT / lang / 'index.html'
        out.parent.mkdir(exist_ok=True)
        out.write_text(page, encoding='utf-8')
        print(f'{lang}/index.html')


if __name__ == '__main__':
    main()
