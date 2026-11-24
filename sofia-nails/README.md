# Sofia Nails — Bruxelles

Site vitrine en français : épilation à la cire, manucure, gel, acrylique et pédicure médicale.
Soins uniquement sur rendez-vous, au domicile de la praticienne à Bruxelles.

- `index.html` : contenu et grille tarifaire (source unique des prix : attributs `data-service` / `data-price`).
- `main.js` : onglets, ajout des soins au rendez-vous, total indicatif, préparation de l'e-mail de réservation.
- `styles.css`, `favicon.svg`.

Aucun serveur ni outil de build : publier le dossier tel quel sur n'importe quel hébergement statique.
Pour modifier un prix, changer `data-price` **et** le texte affiché dans `index.html`.
