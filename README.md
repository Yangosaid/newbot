# Y&M REFORNOVA — ymrefornova.com

Sitio web estático de **Y&M REFORNOVA SOCIEDAD LIMITADA**, empresa de reformas en Girona.
Identidad visual tomada de la tarjeta de visita (verde, marfil y dorado; casa con monograma Y&M).

## Idiomas

| Idioma | URL |
|---|---|
| Español (original) | `/` |
| Català | `/ca/` |
| Français | `/fr/` |
| English | `/en/` |
| العربية (RTL) | `/ar/` |

Los textos viven en `src/i18n/*.mjs`. **El español (`es.mjs`) es la referencia**; el resto sigue su misma estructura.

## Estructura

```
src/build.mjs      plantilla y generador (HTML, sitemap, robots, vCard, favicon)
src/i18n/          textos por idioma
src/scenes.mjs     ilustraciones SVG de salón, cocina, baño y Girona
public/            sitio generado, listo para publicar (incluye assets/styles.css y assets/main.js)
brand/             tarjeta de visita para imprenta y su script
```

## Uso

```bash
npm run build      # regenera public/ tras editar textos o plantilla
npm run dev        # genera y sirve en http://localhost:4173
```

Opcional: `DEV_EMAIL=correo@ejemplo.com npm run build` añade el correo del desarrollador en el pie
(sin la variable solo se muestra el teléfono).

## Funciones

- Formulario de presupuesto en 3 pasos que prepara el mensaje en el dispositivo y lo envía por WhatsApp o correo
  (sin servidor), con borrador guardado en el navegador.
- Filtro de espacios, preguntas frecuentes, guía de presupuesto, botón «Guardar contacto» (vCard).
- SEO: `hreflang`, canonical, Open Graph, datos estructurados `HomeAndConstructionBusiness` y `FAQPage`, `sitemap.xml`.
- Adaptado a móvil con barra fija de Llamar / Presupuesto / WhatsApp.

## Publicar

Sube el contenido de `public/` a cualquier hosting estático (Netlify, Vercel, Cloudflare Pages, GitHub Pages…)
y apunta el dominio `ymrefornova.com`. Las ilustraciones SVG se pueden sustituir por fotografías reales de obras.
