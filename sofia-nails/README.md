# Sofia Nails — Bruxelles

Site vitrine : épilation à la cire, manucure, gel, acrylique et pédicure médicale.
Soins uniquement sur rendez-vous, au domicile de la praticienne à Bruxelles.
Contact : +32 470 70 17 05 (appel / WhatsApp).

## Langues

| Langue | Page |
|---|---|
| Français (source) | `index.html` |
| Nederlands (België) | `nl/index.html` |
| English | `en/index.html` |
| Español | `es/index.html` |

**On ne modifie que `index.html` (français)** puis on régénère les autres langues :

```bash
python3 i18n/build.py
```

Les traductions sont dans `i18n/translations.py`. Le script s'arrête et liste les phrases
si un texte français n'a pas de traduction : il suffit de les ajouter puis de relancer.

Dans les messages de réservation en NL/EN/ES, le nom français du soin est ajouté entre
parenthèses et la langue du client est indiquée, pour que Sofia sache toujours ce qui est demandé.

## Fichiers

- `index.html`, `styles.css`, `main.js` : la page, le style, les interactions (tarifs, réservation, galerie).
- `confidentialite.html` : confidentialité et mentions légales (traduite comme l'accueil par `i18n/build.py`).
- `404.html` : page « introuvable » en quatre langues (servie automatiquement par Netlify).
- `fonts.css`, `fonts/` : polices hébergées sur le site (aucun appel à Google, conforme RGPD).
- `site.webmanifest` : ajout à l'écran d'accueil du téléphone. `_headers` : cache et sécurité sur Netlify.
- `sitemap.xml`, `robots.txt` : générés par `i18n/build.py`.
- `img/` : photos optimisées (WebP + JPG). `logo/` : logo en SVG et PNG (profil Instagram, versions claire et foncée).
- Prix : attributs `data-price` **et** texte affiché dans `index.html`.

Aucun serveur nécessaire : publier le dossier tel quel sur un hébergement statique.
