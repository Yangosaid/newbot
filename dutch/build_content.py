#!/usr/bin/env python3
"""Build dutch/src/main/assets/data.js and the audio clips from dutch/content/*.txt.

Audio: Microsoft Edge neural voices via edge-tts, one mp3 per sentence with word
timings (for the karaoke highlight and tap-a-word playback), one mp3 per core word.
Clips already on disk are reused, so re-running only synthesises new lines.

  python3 dutch/build_content.py            # needs network for new audio
  python3 dutch/build_content.py --no-audio # data.js only (uses cached timings)
"""
import asyncio, hashlib, json, os, re, sys, unicodedata

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from tools_parse import parse, tokens  # noqa: E402

C = os.path.join(HERE, 'content')
ASSETS = os.path.join(HERE, 'src', 'main', 'assets')
AUD = os.path.join(ASSETS, 'audio')
CACHE = os.path.join(HERE, 'content', 'timings.json')

VOICE = {
    'N': 'nl-NL-FennaNeural', 'S': 'nl-NL-ColetteNeural', 'D': 'nl-NL-MaartenNeural',
    'E': 'nl-NL-FennaNeural', 'M': 'nl-NL-MaartenNeural', 'P': 'nl-NL-MaartenNeural',
    'L': 'nl-BE-DenaNeural', 'O': 'nl-NL-ColetteNeural', 'X': 'nl-NL-ColetteNeural',
    'A': 'nl-NL-ColetteNeural', 'B': 'nl-NL-MaartenNeural', 'C': 'nl-NL-FennaNeural', 'G': 'nl-NL-MaartenNeural',
}
PITCH = {'A': '+28Hz', 'B': '-30Hz', 'C': '+4Hz', 'G': '+6Hz', 'X': '+8Hz', 'E': '+12Hz', 'M': '-8Hz', 'P': '-18Hz', 'O': '-14Hz'}
RATE = {'O': '-8%', 'B': '-6%'}
WORD_VOICE = 'nl-NL-ColetteNeural'


def norm(w):
    w = w.lower().replace('eén', 'één')
    return w


def read_table(name, cols):
    out = []
    for line in open(os.path.join(C, name), encoding='utf8'):
        line = line.strip()
        if not line or line.startswith('#'):
            continue
        p = [x.strip() for x in line.split('|')]
        out.append(dict(zip(cols, p)))
    return out


def read_dict():
    d = {}
    for line in open(os.path.join(C, 'dict.txt'), encoding='utf8'):
        line = line.rstrip('\n')
        if not line.strip() or line.startswith('#'):
            continue
        p = [x.strip() for x in line.split(' = ')]
        d[p[0]] = [p[1]] + ([p[2]] if len(p) > 2 else [])
    return d


def slug(s):
    s = unicodedata.normalize('NFKD', s.lower()).encode('ascii', 'ignore').decode()
    s = re.sub(r'[^a-z0-9]+', '-', s).strip('-')
    return s or hashlib.md5(s.encode()).hexdigest()[:8]


async def synth(text, voice, path, rate='+0%', pitch='+0Hz', words=False):
    import edge_tts
    proxy = os.environ.get('HTTPS_PROXY') or os.environ.get('https_proxy')
    for attempt in range(5):
        try:
            com = edge_tts.Communicate(text, voice, rate=rate, pitch=pitch,
                                       boundary='WordBoundary', proxy=proxy)
            audio, marks = bytearray(), []
            async for ch in com.stream():
                if ch['type'] == 'audio':
                    audio += ch['data']
                elif ch['type'] == 'WordBoundary':
                    marks.append([round(ch['offset'] / 1e4), round((ch['offset'] + ch['duration']) / 1e4), ch['text']])
            if not audio:
                raise RuntimeError('no audio')
            os.makedirs(os.path.dirname(path), exist_ok=True)
            with open(path, 'wb') as f:
                f.write(audio)
            return marks
        except Exception as e:  # network hiccups: back off and retry
            if attempt == 4:
                print('  FAILED:', repr(text), voice, e)
                return None
            await asyncio.sleep(2 + attempt * 3)


def align(text, marks):
    """Map TTS word marks onto our token list -> [[startMs, endMs], ...] per token."""
    toks = tokens(text)
    out, j = [], 0
    for t in toks:
        hit = None
        for k in range(j, min(j + 4, len(marks))):
            m = marks[k][2].lower().strip(".,!?;:\"'")
            if m == t.lower() or t.lower().startswith(m) or m.startswith(t.lower()):
                hit = k
                break
        if hit is None:
            out.append(None)
        else:
            out.append(marks[hit][:2])
            j = hit + 1
    # fill gaps by interpolation from neighbours
    for i, v in enumerate(out):
        if v is None:
            prev = next((out[k] for k in range(i - 1, -1, -1) if out[k]), [0, 0])
            nxt = next((out[k] for k in range(i + 1, len(out)) if out[k]), [prev[1] + 300, prev[1] + 300])
            out[i] = [prev[1], max(prev[1] + 80, nxt[0])]
    return out


async def main():
    do_audio = '--no-audio' not in sys.argv
    stories = parse(os.path.join(C, 'stories.txt'))
    kids = parse(os.path.join(C, 'kids.txt'))
    for s in kids:
        s['sec'] = 'kids'
    stories += kids
    dic = read_dict()
    words = read_table('words.txt', ['nl', 'ar', 'pic', 'group'])
    sounds = read_table('sounds.txt', ['sym', 'how', 'ex'])
    cache = json.load(open(CACHE)) if os.path.exists(CACHE) else {}

    missing = sorted({norm(t) for s in stories for l in s['lines'] for t in tokens(l['nl'])} - set(dic))
    if missing:
        print('WARNING: words without a gloss:', ' '.join(missing))

    jobs = []  # (key, text, voice, path, rate, pitch)
    for s in stories:
        for i, l in enumerate(s['lines']):
            v = VOICE.get(l['spk'], VOICE['N'])
            rate = RATE.get(l['spk'], '+0%')
            if s['level'] == 'Start':
                rate = '-12%'
            key = hashlib.md5(f"{l['nl']}|{v}|{rate}|{PITCH.get(l['spk'], '')}".encode()).hexdigest()[:12]
            l['a'] = f"audio/s/{s['id']}_{i:02d}_{key}.mp3"
            jobs.append((key, l['nl'], v, os.path.join(ASSETS, l['a']), rate, PITCH.get(l['spk'], '+0Hz')))
    wordset = {w['nl'] for w in words}
    for snd in sounds:
        for ex in snd['ex'].split(','):
            wordset.add(ex.split(':')[0].strip())
    wordset |= {k for k in dic if k not in ("'s", "d'orange")}
    wav = {}
    for w in sorted(wordset):
        p = f"audio/w/{slug(w)}.mp3"
        wav[w] = p
        jobs.append(('w:' + w, w, WORD_VOICE, os.path.join(ASSETS, p), '-10%', '+0Hz'))

    if do_audio:
        sem = asyncio.Semaphore(6)
        done = [0]

        async def run(job):
            key, text, v, path, rate, pitch = job
            if os.path.exists(path) and (key.startswith('w:') or key in cache):
                return
            async with sem:
                marks = await synth(text, v, path, rate, pitch)
                if marks is None:
                    return
                if not key.startswith('w:'):
                    cache[key] = marks
                done[0] += 1
                if done[0] % 25 == 0:
                    print('  synthesised', done[0])
                    json.dump(cache, open(CACHE, 'w'))
        await asyncio.gather(*(run(j) for j in jobs))
        json.dump(cache, open(CACHE, 'w'))

    for s in stories:
        for l in s['lines']:
            key = l['a'].rsplit('_', 1)[1][:-4]
            l['t'] = align(l['nl'], cache.get(key, []))
    data = {
        'stories': stories,
        'dict': dic,
        'words': words,
        'sounds': [{'sym': x['sym'], 'how': x['how'],
                    'ex': [[e.split(':')[0].strip(), e.split(':')[1].strip()] for e in x['ex'].split(',')]} for x in sounds],
        'wav': wav,
    }
    with open(os.path.join(ASSETS, 'data.js'), 'w', encoding='utf8') as f:
        f.write('window.DATA=' + json.dumps(data, ensure_ascii=False, separators=(',', ':')) + ';\n')
    # remove clips no longer referenced (edited sentences)
    keep = {os.path.normpath(os.path.join(ASSETS, l['a'])) for s in stories for l in s['lines']}
    keep |= {os.path.normpath(os.path.join(ASSETS, p)) for p in wav.values()}
    for sub in ('s', 'w'):
        d = os.path.join(AUD, sub)
        if os.path.isdir(d):
            for fn in os.listdir(d):
                p = os.path.normpath(os.path.join(d, fn))
                if p not in keep:
                    os.remove(p)
    n = sum(len(s['lines']) for s in stories)
    print(f'{len(stories)} stories, {n} sentences, {len(dic)} glosses, {len(words)} core words, {len(wav)} word clips')


if __name__ == '__main__':
    asyncio.run(main())
