import re,json,sys
WORD=re.compile(r"'s|[A-Za-zÀ-ÿ]+(?:'[a-z]+)?")
def parse(path):
    stories=[];cur=None
    for raw in open(path,encoding='utf8'):
        line=raw.rstrip('\n')
        if not line.strip() or line.startswith('#'):continue
        if line.startswith('@'):
            p=[x.strip() for x in line[1:].split('|')]
            cur={'id':p[0],'level':p[1],'title':p[2],'titleAr':p[3],'lines':[]};stories.append(cur);continue
        spk,rest=line.split(':',1)
        p=[x.strip() for x in rest.split('|')]
        cur['lines'].append({'spk':spk.strip(),'nl':p[0],'ar':p[1],'pic':p[2] if len(p)>2 else ''})
    return stories
def tokens(s):return [m.group(0) for m in WORD.finditer(s)]
if __name__=='__main__':
    st=parse('stories.txt');forms={}
    for s in st:
        for l in s['lines']:
            for t in tokens(l['nl']):
                k=t.lower();forms.setdefault(k,s['id'])
    print(len(st),'stories',sum(len(s['lines']) for s in st),'lines',len(forms),'forms')
    json.dump(sorted(forms),open('/tmp/claude-0/-home-user-newbot/d0208d20-c5bc-55a0-a867-ff7c65b72e52/scratchpad/forms.json','w'),ensure_ascii=False)
