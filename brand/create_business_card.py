from pathlib import Path
import os, json, zipfile, shutil
from reportlab.pdfgen import canvas
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib.colors import CMYKColor, Color
from reportlab.lib.units import mm
from reportlab.graphics.barcode.qr import QrCodeWidget
from reportlab.graphics.shapes import Drawing
from reportlab.graphics import renderPDF
from pypdf import PdfReader, PdfWriter
from pypdf.generic import RectangleObject, NameObject, ContentStream
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import fitz

BASE = Path(__file__).resolve().parent.parent
OUT = BASE / 'output' / 'business-card'
OUT.mkdir(parents=True, exist_ok=True)
TMP = BASE / 'tmp' / 'pdfs'
TMP.mkdir(parents=True, exist_ok=True)
RUNTIME = Path(os.environ.get('CODEX_PRIMARY_RUNTIME_ROOT', '/opt/codex/runtimes/codex-primary-runtime'))
SANS = RUNTIME / 'dependencies/node/node_modules/pdfjs-dist/standard_fonts/LiberationSans-Regular.ttf'
BOLD = SANS.with_name('LiberationSans-Bold.ttf')
SERIF = Path('/usr/share/fonts/truetype/dejavu/DejaVuSerif.ttf')
for name,path in [('Sans',SANS),('SansBold',BOLD),('Serif',SERIF)]:
    pdfmetrics.registerFont(TTFont(name,str(path)))

def cmyk(h):
    h=h.lstrip('#'); r,g,b=[int(h[i:i+2],16)/255 for i in (0,2,4)]
    k=1-max(r,g,b)
    if k==1: return CMYKColor(0,0,0,1)
    return CMYKColor((1-r-k)/(1-k),(1-g-k)/(1-k),(1-b-k)/(1-k),k)

GREEN=CMYKColor(.67,.39,.58,.67); IVORY=cmyk('#f8f6f0'); GOLD=cmyk('#b79a71')
MUTED=cmyk('#58655c'); LINE=cmyk('#3e5145'); WHITE=CMYKColor(0,0,0,0)
BLACK=CMYKColor(0,0,0,1)
W,H,BLEED=85*mm,55*mm,3*mm
PHONE='+34 688 45 75 30'
EMAIL='reformamacro@gmail.com'
WEB='ymrefornova.com'
QR_URL='https://wa.me/34688457530'

def text(c,txt,x,y,size,font='Sans',color=GREEN,tracking=0,align='left'):
    width=pdfmetrics.stringWidth(txt,font,size)+(len(txt)-1)*tracking
    if align=='center': x-=width/2
    elif align=='right': x-=width
    c.setFillColor(color)
    t=c.beginText(x,y);t.setFont(font,size);t.setCharSpace(tracking);t.textOut(txt);c.drawText(t)
    return width

def line(c,x1,y1,x2,y2,color=GOLD,width=.6):
    c.setStrokeColor(color);c.setLineWidth(width);c.line(x1,y1,x2,y2)

def front(c):
    c.setFillColor(GREEN);c.rect(-BLEED,-BLEED,W+2*BLEED,H+2*BLEED,fill=1,stroke=0)
    # Quiet architectural lines at the outer corners.
    for x,y,sx,sy in [(5*mm,5*mm,1,1),(80*mm,50*mm,-1,-1)]:
        line(c,x,y,x+7*mm*sx,y,LINE,.45)
        line(c,x,y,x,y+7*mm*sy,LINE,.45)
    # House outline and Y&M monogram follow the site's existing identity.
    c.setStrokeColor(GOLD);c.setLineWidth(.72);c.setLineJoin(0)
    p=c.beginPath();p.moveTo(31*mm,27*mm);p.lineTo(31*mm,38.7*mm)
    p.lineTo(42.5*mm,47.1*mm);p.lineTo(54*mm,38.7*mm);p.lineTo(54*mm,27*mm)
    c.drawPath(p)
    line(c,31*mm,27*mm,54*mm,27*mm,GOLD,.72)
    text(c,'Y&M',42.5*mm,30.4*mm,23.2,'Serif',IVORY,align='center')
    text(c,'REFORNOVA',42.5*mm,18.8*mm,18.3,'SansBold',IVORY,tracking=1.95,align='center')
    text(c,'REFORMAS INTEGRALES · GIRONA',42.5*mm,12.6*mm,6.5,'Sans',IVORY,tracking=.65,align='center')
    text(c,'SOCIEDAD LIMITADA',42.5*mm,6.6*mm,5.5,'Sans',GOLD,tracking=1.0,align='center')

def qr(c,x,y,size):
    widget=QrCodeWidget(QR_URL,barLevel='M',barBorder=4,barFillColor=BLACK)
    b=widget.getBounds();scale=size/(b[2]-b[0])
    c.setFillColor(WHITE);c.rect(x,y,size,size,fill=1,stroke=0)
    d=Drawing(size,size,transform=[scale,0,0,scale,-b[0]*scale,-b[1]*scale])
    d.add(widget);renderPDF.draw(d,c,x,y)
    c.linkURL(QR_URL,(x,y,x+size,y+size),relative=1,thickness=0)

def back(c):
    c.setFillColor(IVORY);c.rect(-BLEED,-BLEED,W+2*BLEED,H+2*BLEED,fill=1,stroke=0)
    text(c,'Youssef',6*mm,44.3*mm,21.8,'Serif',GREEN)
    text(c,'PROPIETARIO',6.4*mm,39.5*mm,6.2,'SansBold',MUTED,tracking=1.25)
    text(c,'GIRONA',79*mm,45.1*mm,6.3,'SansBold',GREEN,tracking=1.2,align='right')
    line(c,6.4*mm,36.4*mm,16*mm,36.4*mm,GOLD,1.1)
    text(c,PHONE,6.2*mm,29.9*mm,10.8,'SansBold',GREEN)
    text(c,EMAIL,6.4*mm,24.0*mm,8.5,'Sans',GREEN)
    text(c,WEB,6.4*mm,18.3*mm,9.0,'SansBold',GREEN)
    c.linkURL('tel:+34688457530',(6*mm,29*mm,53*mm,34*mm),relative=1,thickness=0)
    c.linkURL('mailto:'+EMAIL,(6*mm,23*mm,54*mm,28*mm),relative=1,thickness=0)
    c.linkURL('https://'+WEB+'/',(6*mm,17*mm,54*mm,22*mm),relative=1,thickness=0)
    qr(c,60.0*mm,21.0*mm,20*mm)
    text(c,'WhatsApp',70*mm,17.6*mm,6.9,'SansBold',GREEN,align='center')
    c.setFillColor(GREEN);c.rect(-BLEED,-BLEED,W+2*BLEED,12.0*mm+BLEED,fill=1,stroke=0)
    line(c,-BLEED,12*mm,W+BLEED,12*mm,GOLD,.65)
    text(c,'Reformas · Electricidad · Fontanería · Pladur',42.5*mm,7.8*mm,6.7,'Sans',IVORY,align='center')
    text(c,'Cocinas · Baños · Pintura · Suelos',42.5*mm,4.3*mm,6.7,'Sans',IVORY,align='center')

raw=TMP/'business-card-master.pdf'
c=canvas.Canvas(str(raw),pagesize=(W+2*BLEED,H+2*BLEED),pageCompression=1)
c.setTitle('Y&M REFORNOVA - Youssef - Tarjeta de visita')
c.setAuthor('Y&M REFORNOVA SOCIEDAD LIMITADA')
for draw in [front,back]:
    c.saveState();c.translate(BLEED,BLEED);draw(c);c.restoreState();c.showPage()
c.save()
pdf=OUT/'YM-REFORNOVA-Youssef-Imprenta.pdf'
r=PdfReader(raw);writer=PdfWriter()
for p in r.pages:
    contents=ContentStream(p.get_contents(),r)
    active_font=None;used_fonts=set()
    for operands,operator in contents.operations:
        if operator==b'Tf': active_font=str(operands[0])
        if operator in [b'Tj',b'TJ',b"'",b'"']:
            used_fonts.add(active_font)
    contents.operations=[(a,o) for a,o in contents.operations if not(o==b'Tf' and str(a[0]) not in used_fonts)]
    p[NameObject('/Contents')]=contents
    fonts=p['/Resources']['/Font']
    for key in list(fonts.keys()):
        if str(key) not in used_fonts: del fonts[key]
    p.trimbox=RectangleObject([BLEED,BLEED,W+BLEED,H+BLEED])
    p.bleedbox=RectangleObject([0,0,W+2*BLEED,H+2*BLEED])
    writer.add_page(p)
writer.add_metadata({'/Title':'Y&M REFORNOVA | Youssef | Tarjeta de visita','/Author':'Y&M REFORNOVA SOCIEDAD LIMITADA','/Subject':'85 x 55 mm, 3 mm de sangrado por lado, anverso y reverso','/Keywords':'Youssef, Y&M REFORNOVA, tarjeta de visita, Girona'})
with pdf.open('wb') as f: writer.write(f)

# Render the exact final PDF, cropped only to its declared trim box for previews.
doc=fitz.open(pdf);imgs=[]
for i,p in enumerate(doc):
    clip=fitz.Rect(BLEED,BLEED,W+BLEED,H+BLEED)
    pix=p.get_pixmap(matrix=fitz.Matrix(600/72,600/72),clip=clip,alpha=False)
    image=Image.frombytes('RGB',[pix.width,pix.height],pix.samples)
    name='Anverso' if i==0 else 'Reverso'
    image.save(OUT/f'YM-REFORNOVA-Youssef-{name}.png',dpi=(600,600))
    imgs.append(image)

# Crisp, straight-on presentation of both faces, with print artwork unchanged.
PW=1300; CW=1100; CH=round(CW*55/85); GAP=55; TOP=135; BOTTOM=85
PH=TOP+2*CH+GAP+BOTTOM
preview=Image.new('RGB',(PW,PH),'#e9e7e0')
draw=ImageDraw.Draw(preview)
f=ImageFont.truetype(str(BOLD),24); fs=ImageFont.truetype(str(SANS),18)
draw.text((100,52),'Y&M REFORNOVA',fill='#263c32',font=f)
draw.text((1200,56),'YOUSSEF  /  85 × 55 mm',fill='#58655c',font=fs,anchor='ra')
for i,im in enumerate(imgs):
    x=100;y=TOP+i*(CH+GAP)
    shadow=Image.new('RGBA',preview.size)
    sd=ImageDraw.Draw(shadow);sd.rectangle((x+2,y+9,x+CW+2,y+CH+9),fill=(22,40,30,35))
    shadow=shadow.filter(ImageFilter.GaussianBlur(13));preview=Image.alpha_composite(preview.convert('RGBA'),shadow)
    preview.paste(im.resize((CW,CH),Image.Resampling.LANCZOS),(x,y))
preview.convert('RGB').save(OUT/'YM-REFORNOVA-Youssef-Vista-previa.png')

readme='''Y&M REFORNOVA - Tarjeta de Youssef

CONTENIDO
- PDF de imprenta de dos páginas: anverso y reverso.
- PNG de cada cara a 600 ppp, cortados al tamaño final.
- Vista previa de ambas caras.
- Código de composición editable en create_business_card.py.

INDICACIONES PARA IMPRIMIR
Formato final: 85 x 55 mm, horizontal.
Sangrado: 3 mm en cada lado. Cada página PDF mide 91 x 61 mm.
El PDF define TrimBox y BleedBox. No escalar para ajustar a la página.
Colores CMYK y fuentes incrustadas. El negro del QR utiliza solo K.
Acabado propuesto: papel mate de 350 g/m², impresión a dos caras.
Confirmar el perfil de color y requisitos finales con la imprenta.
El dorado es un color CMYK, sin estampación metálica.

DATOS
Youssef - Propietario
Y&M REFORNOVA SOCIEDAD LIMITADA
Teléfono y WhatsApp: +34 688 45 75 30
Correo: reformamacro@gmail.com
Web: https://ymrefornova.com/
QR: https://wa.me/34688457530

El nombre se ha transliterado como Youssef a partir de يوسف.
La tarjeta usa el teléfono de la empresa; no incluye el contacto del desarrollador.

EDICIÓN TÉCNICA
El archivo Python usa reportlab, pypdf, pymupdf y pillow.
Configurar las rutas SANS, BOLD y SERIF a Liberation Sans y DejaVu Serif.
Los textos, colores y medidas se definen al principio y en front()/back().
'''
(OUT/'LEEME-Impresion.txt').write_text(readme,encoding='utf-8')
pack=OUT/'YM-REFORNOVA-Youssef-Pack.zip'
with zipfile.ZipFile(pack,'w',zipfile.ZIP_DEFLATED) as z:
    for f in sorted(OUT.iterdir()):
        if f.name!=pack.name: z.write(f,f.name)
    z.write(__file__,'create_business_card.py')

print(json.dumps({'pdf':str(pdf),'preview':str(OUT/'YM-REFORNOVA-Youssef-Vista-previa.png'),'pack':str(pack),'pages':len(r.pages),'final_mm':[85,55],'bleed_mm':3,'qr':QR_URL},ensure_ascii=False))
