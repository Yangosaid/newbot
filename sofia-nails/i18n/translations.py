# Traductions de la page Sofia Nails. Le français (index.html) est la source.
# Chaque entrée : 'texte français': (néerlandais/flamand, anglais, espagnol)

LANGS = {
    'nl': {'html': 'nl-BE', 'og': 'nl_BE', 'name': 'Nederlands', 'short': 'NL', 'date': 'nl-BE'},
    'en': {'html': 'en', 'og': 'en_GB', 'name': 'English', 'short': 'EN', 'date': 'en-GB'},
    'es': {'html': 'es', 'og': 'es_ES', 'name': 'Español', 'short': 'ES', 'date': 'es-ES'},
}
ORDER = ('nl', 'en', 'es')

T = {
    # En-tête et métadonnées
    'Sofia Nails · Ongles, manucure et épilation à la cire à Bruxelles': (
        'Sofia Nails · Nagels, manicure en harsen in Brussel',
        'Sofia Nails · Nails, manicure and waxing in Brussels',
        'Sofia Nails · Uñas, manicura y depilación con cera en Bruselas'),
    'Épilation à la cire, manucure, semi-permanent, gel, acrylique et pédicure médicale à Uccle, Bruxelles. Uniquement sur rendez-vous, chez moi, dans un espace privé. Tarifs dès 10 €.': (
        'Harsen, manicure, semi-permanente lak, gel, acryl en medische pedicure in Ukkel, Brussel. Enkel op afspraak, bij mij thuis, in een privéruimte. Prijzen vanaf 10 €.',
        'Waxing, manicure, semi-permanent polish, gel, acrylic and medical pedicure in Uccle, Brussels. By appointment only, at my home, in a private space. Prices from 10 €.',
        'Depilación con cera, manicura, semipermanente, gel, acrílico y pedicura médica en Uccle, Bruselas. Solo con cita previa, en mi casa, en un espacio privado. Precios desde 10 €.'),
    'Sofia Nails · Beauté à Bruxelles': (
        'Sofia Nails · Schoonheid in Brussel', 'Sofia Nails · Beauty in Brussels', 'Sofia Nails · Belleza en Bruselas'),
    'Épilation à la cire et beauté des mains et des pieds, sur rendez-vous à Bruxelles.': (
        'Harsen en verzorging van handen en voeten, op afspraak in Brussel.',
        'Waxing and hand and foot care, by appointment in Brussels.',
        'Depilación con cera y belleza de manos y pies, con cita previa en Bruselas.'),
    'Aller au contenu': ('Naar de inhoud', 'Skip to content', 'Ir al contenido'),
    'Sofia Nails, accueil': ('Sofia Nails, startpagina', 'Sofia Nails, home', 'Sofia Nails, inicio'),
    'Drapeau de la Colombie': ('Vlag van Colombia', 'Flag of Colombia', 'Bandera de Colombia'),
    'Beauté · Bruxelles': ('Schoonheid · Brussel', 'Beauty · Brussels', 'Belleza · Bruselas'),
    'Navigation principale': ('Hoofdnavigatie', 'Main navigation', 'Navegación principal'),
    'Soins': ('Behandelingen', 'Treatments', 'Servicios'),
    'Réalisations': ('Realisaties', 'Portfolio', 'Trabajos'),
    'Tarifs': ('Prijzen', 'Prices', 'Precios'),
    'Déroulement': ('Verloop', 'How it works', 'Cómo funciona'),
    'Questions': ('Vragen', 'FAQ', 'Preguntas'),
    'Prendre rendez-vous': ('Afspraak maken', 'Book an appointment', 'Pedir cita'),

    # Hero
    'Ongles en amande au vernis bordeaux, French rouge et nail art floral avec strass, réalisés par Sofia Nails': (
        'Amandelvormige nagels met bordeauxrode lak, rode French en bloemen-nailart met strass, gemaakt door Sofia Nails',
        'Almond nails in burgundy polish, red French tips and floral nail art with rhinestones, by Sofia Nails',
        'Uñas almendradas en burdeos, francesa roja y nail art floral con strass, realizadas por Sofia Nails'),
    'Bruxelles · Sur rendez-vous': ('Brussel · Op afspraak', 'Brussels · By appointment', 'Bruselas · Con cita previa'),
    '— ongles, manucure et épilation à la cire à Bruxelles': (
        '— nagels, manicure en harsen in Brussel', '— nails, manicure and waxing in Brussels', '— uñas, manicura y depilación con cera en Bruselas'),
    'Ongles · Épilation à la cire · Pédicure médicale': (
        'Nagels · Harsen · Medische pedicure', 'Nails · Waxing · Medical pedicure', 'Uñas · Depilación con cera · Pedicura médica'),
    'Des mains soignées et une peau douce, en toute intimité. Je vous reçois chez moi à Bruxelles, uniquement sur rendez-vous.': (
        'Verzorgde handen en een zachte huid, in alle rust en privacy. Ik ontvang u bij mij thuis in Brussel, enkel op afspraak.',
        'Beautiful hands and smooth skin, in complete privacy. I welcome you at my home in Brussels, by appointment only.',
        'Manos cuidadas y piel suave, con total intimidad. Te recibo en mi casa en Bruselas, solo con cita previa.'),
    'Réserver un soin': ('Nu reserveren', 'Book now', 'Reservar cita'),
    'Voir les tarifs': ('Bekijk de prijzen', 'See prices', 'Ver precios'),
    'Appeler': ('Bellen', 'Call', 'Llamar'),
    'Écrire sur': ('Stuur een bericht via', 'Message on', 'Escribir por'),
    'dès 10 €': ('vanaf 10 €', 'from 10 €', 'desde 10 €'),
    'Épilation à la cire': ('Harsen', 'Waxing', 'Depilación con cera'),
    'dès 20 €': ('vanaf 20 €', 'from 20 €', 'desde 20 €'),
    'Manucure': ('Manicure', 'Manicure', 'Manicura'),
    'dès 35 €': ('vanaf 35 €', 'from 35 €', 'desde 35 €'),
    'Gel et acrylique': ('Gel en acryl', 'Gel and acrylic', 'Gel y acrílico'),
    'Pédicure médicale': ('Medische pedicure', 'Medical pedicure', 'Pedicura médica'),

    # Soins
    'Mes soins': ('Mijn behandelingen', 'My treatments', 'Mis servicios'),
    'Deux savoir-faire,': ('Twee specialiteiten,', 'Two specialities,', 'Dos especialidades,'),
    'une même attention.': ('dezelfde zorg.', 'the same care.', 'el mismo cuidado.'),
    'Visage, corps, maillot et jambes. Des sourcils redessinés à l’épilation complète des jambes, pour femmes et hommes (torse, épaules, dos).': (
        'Gezicht, lichaam, bikinilijn en benen. Van mooi gevormde wenkbrauwen tot volledig harsen van de benen, voor vrouwen en mannen (borst, schouders, rug).',
        'Face, body, bikini line and legs. From shaped eyebrows to full leg waxing, for women and men (chest, shoulders, back).',
        'Rostro, cuerpo, ingles y piernas. Desde cejas perfiladas hasta piernas completas, para mujeres y hombres (pecho, hombros, espalda).'),
    '17 zones · dès 10 €': ('17 zones · vanaf 10 €', '17 areas · from 10 €', '17 zonas · desde 10 €'),
    'Beauté des mains et des pieds': ('Verzorging van handen en voeten', 'Hand and foot care', 'Belleza de manos y pies'),
    'Manucure classique ou avec semi-permanent, pose de gel ou d’acrylique, et pédicure médicale pour des pieds sains et confortables.': (
        'Klassieke manicure of met semi-permanente lak, gel- of acrylnagels, en medische pedicure voor gezonde, comfortabele voeten.',
        'Classic manicure or with semi-permanent polish, gel or acrylic nails, and medical pedicure for healthy, comfortable feet.',
        'Manicura clásica o con esmalte semipermanente, uñas de gel o acrílico y pedicura médica para unos pies sanos y cómodos.'),
    '5 soins · dès 20 €': ('5 behandelingen · vanaf 20 €', '5 treatments · from 20 €', '5 servicios · desde 20 €'),

    # Réalisations
    'Mes dernières': ('Mijn nieuwste', 'My latest', 'Mis últimas'),
    'créations.': ('creaties.', 'creations.', 'creaciones.'),
    'French, nail art floral, bordeaux profond ou nude délicat : chaque pose est pensée pour vous.': (
        'French, bloemen-nailart, diep bordeaux of zacht nude: elke set wordt voor u ontworpen.',
        'French tips, floral nail art, deep burgundy or soft nude: every set is designed for you.',
        'Francesa, nail art floral, burdeos intenso o nude delicado: cada diseño está pensado para ti.'),
    'Fleur bordeaux et léopard': ('Bordeaux bloem en luipaard', 'Burgundy flower and leopard', 'Flor burdeos y leopardo'),
    'Ongles amande nude avec fleur bordeaux, perles argentées, French bordeaux et motif léopard': (
        'Nude amandelnagels met bordeaux bloem, zilveren pareltjes, bordeaux French en luipaardprint',
        'Nude almond nails with a burgundy flower, silver beads, burgundy French tip and leopard print',
        'Uñas almendradas nude con flor burdeos, perlas plateadas, francesa burdeos y estampado de leopardo'),
    'Fleur peinte et perles dorées': ('Handgeschilderde bloem en gouden pareltjes', 'Hand-painted flower and gold beads', 'Flor pintada y perlas doradas'),
    'Ongles amande avec fleur rose peinte à la main, French blanche et perles dorées': (
        'Amandelnagels met handgeschilderde roze bloem, witte French en gouden pareltjes',
        'Almond nails with a hand-painted pink flower, white French tips and gold beads',
        'Uñas almendradas con flor rosa pintada a mano, francesa blanca y perlas doradas'),
    'Bordeaux et aquarelle': ('Bordeaux en aquarel', 'Burgundy and watercolour', 'Burdeos y acuarela'),
    'Ongles amande bordeaux brillants et fleurs aquarelle rouges sur base rose': (
        'Glanzende bordeaux amandelnagels en rode aquarelbloemen op een roze basis',
        'Glossy burgundy almond nails and red watercolour flowers on a pink base',
        'Uñas almendradas burdeos brillantes y flores rojas en acuarela sobre base rosa'),
    'French blanche en V': ('Witte French in V-vorm', 'White V-shaped French', 'Francesa blanca en V'),
    'Ongles carrés nude avec French blanche en V': (
        'Vierkante nude nagels met witte French in V-vorm', 'Square nude nails with a white V-shaped French tip', 'Uñas cuadradas nude con francesa blanca en V'),
    'Photo agrandie': ('Vergrote foto', 'Enlarged photo', 'Foto ampliada'),
    'Fermer': ('Sluiten', 'Close', 'Cerrar'),
    'Plus de créations sur TikTok': ('Meer creaties op TikTok', 'More creations on TikTok', 'Más creaciones en TikTok'),

    # Tarifs
    'Des prix clairs,': ('Duidelijke prijzen,', 'Clear prices,', 'Precios claros,'),
    'sans surprise.': ('zonder verrassingen.', 'no surprises.', 'sin sorpresas.'),
    'Ajoutez les soins qui vous intéressent : le total s’affiche dans le formulaire de rendez-vous.': (
        'Voeg de behandelingen toe die u interesseren: het totaal verschijnt in het afsprakenformulier.',
        'Add the treatments you’d like: the total appears in the booking form.',
        'Añade los servicios que te interesan: el total aparece en el formulario de cita.'),
    'Catégories de soins': ('Categorieën', 'Treatment categories', 'Categorías de servicios'),
    'Mains et pieds': ('Handen en voeten', 'Hands and feet', 'Manos y pies'),
    'Visage': ('Gezicht', 'Face', 'Rostro'),
    'Entretien sourcils': ('Wenkbrauwen onderhoud', 'Eyebrow tidy', 'Mantenimiento de cejas'),
    'Création sourcils': ('Wenkbrauwen nieuwe vorm', 'Eyebrow shaping', 'Diseño de cejas'),
    'Lèvres ou menton': ('Bovenlip of kin', 'Upper lip or chin', 'Labio o mentón'),
    'Visage complet': ('Volledig gezicht', 'Full face', 'Rostro completo'),
    'Bras et buste': ('Armen en buik', 'Arms and stomach', 'Brazos y abdomen'),
    'Aisselles': ('Oksels', 'Underarms', 'Axilas'),
    'Demi-bras': ('Halve armen', 'Half arms', 'Medio brazo'),
    'Bras complets': ('Volledige armen', 'Full arms', 'Brazos completos'),
    'Ventre': ('Buik', 'Stomach', 'Abdomen'),
    'Maillot': ('Bikinilijn', 'Bikini', 'Ingles'),
    'Bikini simple': ('Bikinilijn klassiek', 'Bikini line', 'Ingles sencillas'),
    'Bikini brésilien': ('Braziliaanse bikini', 'Brazilian', 'Ingles brasileñas'),
    'Bikini intégral': ('Volledige bikini (Hollywood)', 'Hollywood (full)', 'Ingles completas'),
    'Jambes': ('Benen', 'Legs', 'Piernas'),
    'Demi-jambes': ('Halve benen', 'Half legs', 'Medias piernas'),
    'Cuisses': ('Dijen', 'Thighs', 'Muslos'),
    'Jambes complètes': ('Volledige benen', 'Full legs', 'Piernas completas'),
    'Torse et dos': ('Borst en rug', 'Chest and back', 'Pecho y espalda'),
    'Épaules': ('Schouders', 'Shoulders', 'Hombros'),
    'Torse': ('Borst', 'Chest', 'Pecho'),
    'Dos complet': ('Volledige rug', 'Full back', 'Espalda completa'),
    'Mains': ('Handen', 'Hands', 'Manos'),
    'Manucure normale': ('Gewone manicure', 'Classic manicure', 'Manicura normal'),
    'Manucure + semi-permanent': ('Manicure + semi-permanente lak', 'Manicure + semi-permanent polish', 'Manicura + semipermanente'),
    'Gel': ('Gel', 'Gel', 'Gel'),
    'à partir de': ('vanaf', 'from', 'desde'),
    'Acrylique': ('Acryl', 'Acrylic', 'Acrílico'),
    'Pieds': ('Voeten', 'Feet', 'Pies'),
    'Les tarifs « à partir de » dépendent de la longueur et du travail souhaité. Le prix exact est confirmé avant le soin.': (
        'Prijzen “vanaf” hangen af van de lengte en het gewenste werk. De exacte prijs wordt vóór de behandeling bevestigd.',
        '“From” prices depend on length and the work required. The exact price is confirmed before the treatment.',
        'Los precios “desde” dependen del largo y del trabajo deseado. El precio exacto se confirma antes del servicio.'),

    # Déroulement
    'Simple, du premier message': ('Eenvoudig, van het eerste bericht', 'Simple, from your first message', 'Sencillo, desde el primer mensaje'),
    'jusqu’au soin.': ('tot de behandeling.', 'to your treatment.', 'hasta tu cita.'),
    'Choisissez vos soins': ('Kies uw behandelingen', 'Choose your treatments', 'Elige tus servicios'),
    'Sélectionnez les soins dans le formulaire et indiquez le jour et le moment qui vous conviennent.': (
        'Selecteer de behandelingen in het formulier en geef aan welke dag en welk moment u past.',
        'Select your treatments in the form and tell me which day and time suit you.',
        'Selecciona los servicios en el formulario e indica el día y el momento que te vienen bien.'),
    'Je confirme le rendez-vous': ('Ik bevestig de afspraak', 'I confirm your appointment', 'Confirmo tu cita'),
    'Je vous réponds rapidement pour confirmer la date, l’heure et l’adresse exacte.': (
        'Ik antwoord u snel om de datum, het uur en het exacte adres te bevestigen.',
        'I reply quickly to confirm the date, time and exact address.',
        'Te respondo enseguida para confirmar la fecha, la hora y la dirección exacta.'),
    'Profitez du soin': ('Geniet van uw behandeling', 'Enjoy your treatment', 'Disfruta de tu cita'),
    'Vous êtes reçue dans un espace privé et tranquille, sans salle d’attente ni foule.': (
        'U wordt ontvangen in een rustige privéruimte, zonder wachtzaal of drukte.',
        'You are welcomed in a calm, private space, with no waiting room or crowds.',
        'Te recibo en un espacio privado y tranquilo, sin sala de espera ni aglomeraciones.'),

    # Conseils
    'Conseils': ('Tips', 'Advice', 'Consejos'),
    'Prendre soin de vous,': ('Goed voor uzelf zorgen,', 'Taking care of yourself,', 'Cuidarte,'),
    'avant et après.': ('voor en na.', 'before and after.', 'antes y después.'),
    'Quelques gestes simples pour un résultat plus net et plus durable.': (
        'Enkele eenvoudige tips voor een mooier en langer resultaat.',
        'A few simple habits for a cleaner, longer-lasting result.',
        'Unos gestos sencillos para un resultado más limpio y duradero.'),
    'Avant le rendez-vous': ('Voor de afspraak', 'Before your appointment', 'Antes de la cita'),
    'Laissez pousser les poils environ 5 mm, soit 2 à 3 semaines sans rasage.': (
        'Laat de haartjes ongeveer 5 mm groeien, dus 2 à 3 weken niet scheren.',
        'Let the hair grow to about 5 mm, which means 2 to 3 weeks without shaving.',
        'Deja crecer el vello unos 5 mm, es decir, 2 o 3 semanas sin afeitar.'),
    'Exfoliez la zone la veille et évitez crème ou huile le jour même.': (
        'Scrub de zone de dag ervoor en gebruik op de dag zelf geen crème of olie.',
        'Exfoliate the area the day before and avoid cream or oil on the day itself.',
        'Exfolia la zona el día anterior y evita cremas o aceites el mismo día.'),
    'Après le soin': ('Na de behandeling', 'After your treatment', 'Después del servicio'),
    'Évitez soleil, sauna, piscine et sport intense pendant 24 à 48 heures.': (
        'Vermijd zon, sauna, zwembad en intensief sporten gedurende 24 à 48 uur.',
        'Avoid sun, sauna, swimming pools and intense exercise for 24 to 48 hours.',
        'Evita el sol, la sauna, la piscina y el deporte intenso durante 24 a 48 horas.'),
    'Exfoliez doucement 2 à 3 jours après, puis hydratez régulièrement pour éviter les poils incarnés.': (
        'Scrub zachtjes 2 à 3 dagen later en hydrateer daarna regelmatig om ingegroeide haartjes te vermijden.',
        'Exfoliate gently 2 to 3 days later, then moisturise regularly to prevent ingrown hairs.',
        'Exfolia suavemente 2 o 3 días después y luego hidrata con regularidad para evitar vellos encarnados.'),
    'Ongles': ('Nagels', 'Nails', 'Uñas'),
    'Au quotidien': ('Elke dag', 'Every day', 'En el día a día'),
    'Appliquez un peu d’huile à cuticules chaque jour pour garder des ongles souples.': (
        'Breng elke dag een beetje nagelriemolie aan voor soepele nagels.',
        'Apply a little cuticle oil every day to keep your nails supple.',
        'Aplica un poco de aceite de cutículas cada día para mantener las uñas flexibles.'),
    'Portez des gants pour le ménage et la vaisselle.': (
        'Draag handschoenen bij het poetsen en de afwas.',
        'Wear gloves for cleaning and washing up.',
        'Usa guantes para limpiar y fregar los platos.'),
    'N’utilisez pas vos ongles comme outils et ne retirez pas le gel vous-même.': (
        'Gebruik uw nagels niet als werktuig en verwijder de gel niet zelf.',
        'Don’t use your nails as tools, and don’t remove the gel yourself.',
        'No uses las uñas como herramienta y no retires el gel tú misma.'),
    'Prévoyez un remplissage ou une nouvelle pose toutes les 3 à 4 semaines.': (
        'Plan elke 3 à 4 weken een opvulling of een nieuwe set.',
        'Plan a fill or a new set every 3 to 4 weeks.',
        'Programa un relleno o una nueva aplicación cada 3 o 4 semanas.'),

    # Infos pratiques
    'Infos pratiques': ('Praktische info', 'Practical information', 'Información práctica'),
    'À Bruxelles,': ('In Brussel,', 'In Brussels,', 'En Bruselas,'),
    'chez moi, sur rendez-vous.': ('bij mij thuis, op afspraak.', 'at my home, by appointment.', 'en mi casa, con cita previa.'),
    'L’espace de Sofia Nails : table de manucure blanche, lampe, étagère de vernis et fleurs': (
        'De ruimte van Sofia Nails: witte manicuretafel, lamp, rek met nagellak en bloemen',
        'The Sofia Nails studio: white manicure table, lamp, polish shelf and flowers',
        'El espacio de Sofia Nails: mesa de manicura blanca, lámpara, estantería de esmaltes y flores'),
    'Adresse': ('Adres', 'Address', 'Dirección'),
    'Uccle, Bruxelles': ('Ukkel, Brussel', 'Uccle, Brussels', 'Uccle, Bruselas'),
    'Près de l’arrêt Héros': ('Vlak bij de halte Helden', 'Near the Héros stop', 'Cerca de la parada Héros'),
    'L’adresse exacte vous est envoyée à la confirmation du rendez-vous.': (
        'Het exacte adres krijgt u bij de bevestiging van uw afspraak.',
        'The exact address is sent when your appointment is confirmed.',
        'La dirección exacta se envía al confirmar la cita.'),
    'Voir le quartier sur Google Maps': ('Bekijk de buurt op Google Maps', 'See the area on Google Maps', 'Ver la zona en Google Maps'),
    'Rendez-vous': ('Afspraken', 'Appointments', 'Citas'),
    'Uniquement sur réservation, pour vous accorder tout le temps nécessaire.': (
        'Enkel op reservatie, zodat ik alle tijd voor u heb.',
        'By booking only, so I can give you all the time you need.',
        'Solo con reserva, para dedicarte todo el tiempo necesario.'),
    'Téléphone et WhatsApp': ('Gsm en WhatsApp', 'Phone and WhatsApp', 'Teléfono y WhatsApp'),
    'écrire sur WhatsApp': ('stuur een WhatsApp', 'message on WhatsApp', 'escribir por WhatsApp'),
    'E-mail': ('E-mail', 'Email', 'Correo'),

    # FAQ
    'Bon à savoir': ('Goed om te weten', 'Good to know', 'Conviene saber'),
    'avant de venir.': ('voor uw bezoek.', 'before you come.', 'antes de venir.'),
    'Où se déroulent les soins ?': ('Waar vinden de behandelingen plaats?', 'Where do treatments take place?', '¿Dónde se realizan los servicios?'),
    'Chez moi, à Uccle (Bruxelles), près de l’arrêt Héros, dans un espace dédié et privé. Je ne me déplace pas à domicile. L’adresse exacte vous est envoyée avec la confirmation du rendez-vous.': (
        'Bij mij thuis in Ukkel (Brussel), vlak bij de halte Helden, in een aparte privéruimte. Ik kom niet aan huis. Het exacte adres krijgt u samen met de bevestiging van uw afspraak.',
        'At my home in Uccle (Brussels), near the Héros stop, in a dedicated private space. I don’t do home visits. The exact address is sent with your booking confirmation.',
        'En mi casa en Uccle (Bruselas), cerca de la parada Héros, en un espacio privado y dedicado. No hago servicios a domicilio. La dirección exacta se envía con la confirmación de la cita.'),
    'Comment réserver ?': ('Hoe reserveer ik?', 'How do I book?', '¿Cómo reservo?'),
    'Remplissez le formulaire de rendez-vous : il prépare un message WhatsApp ou un e-mail avec vos soins, le total indicatif et vos disponibilités. Vous pouvez aussi m’appeler ou m’écrire sur WhatsApp au +32 470 70 17 05.': (
        'Vul het afsprakenformulier in: het maakt een WhatsApp-bericht of e-mail klaar met uw behandelingen, het indicatieve totaal en uw beschikbaarheid. U kunt me ook bellen of een WhatsApp sturen op +32 470 70 17 05.',
        'Fill in the booking form: it prepares a WhatsApp message or email with your treatments, the estimated total and your availability. You can also call me or message me on WhatsApp at +32 470 70 17 05.',
        'Rellena el formulario de cita: prepara un mensaje de WhatsApp o un correo con tus servicios, el total orientativo y tu disponibilidad. También puedes llamarme o escribirme por WhatsApp al +32 470 70 17 05.'),
    'Peut-on combiner plusieurs soins ?': ('Kan ik meerdere behandelingen combineren?', 'Can I combine several treatments?', '¿Puedo combinar varios servicios?'),
    'Oui, par exemple jambes complètes et aisselles, ou manucure et pédicure. Sélectionnez-les ensemble dans le formulaire pour un seul rendez-vous.': (
        'Ja, bijvoorbeeld volledige benen en oksels, of manicure en pedicure. Selecteer ze samen in het formulier voor één afspraak.',
        'Yes, for example full legs and underarms, or manicure and pedicure. Select them together in the form for a single appointment.',
        'Sí, por ejemplo piernas completas y axilas, o manicura y pedicura. Selecciónalos juntos en el formulario para una sola cita.'),
    'Que signifie « à partir de » pour le gel et l’acrylique ?': (
        'Wat betekent “vanaf” bij gel en acryl?', 'What does “from” mean for gel and acrylic?', '¿Qué significa “desde” en gel y acrílico?'),
    'Le prix de base est de 35 €. Il peut varier selon la longueur et le travail demandé ; je vous confirme le tarif avant de commencer.': (
        'De basisprijs is 35 €. Die kan variëren volgens de lengte en het gevraagde werk; ik bevestig de prijs voor we beginnen.',
        'The base price is 35 €. It can vary with length and the work requested; I confirm the price before we start.',
        'El precio base es de 35 €. Puede variar según el largo y el trabajo solicitado; te confirmo el precio antes de empezar.'),
    'Les hommes sont-ils les bienvenus ?': ('Zijn mannen ook welkom?', 'Are men welcome?', '¿Atiendes también a hombres?'),
    'Oui. L’épilation du torse, des épaules et du dos fait partie des soins proposés.': (
        'Ja. Harsen van borst, schouders en rug hoort bij de behandelingen.',
        'Yes. Chest, shoulder and back waxing are part of the treatments.',
        'Sí. La depilación de pecho, hombros y espalda forma parte de los servicios.'),

    # Réservation
    'Réservez': ('Reserveer', 'Book', 'Reserva'),
    'votre moment.': ('uw moment.', 'your moment.', 'tu momento.'),
    'Choisissez vos soins et vos disponibilités. Le formulaire prépare votre message sur votre appareil : envoyez-le par WhatsApp ou par e-mail.': (
        'Kies uw behandelingen en wanneer u beschikbaar bent. Het formulier maakt uw bericht klaar op uw toestel: verstuur het via WhatsApp of e-mail.',
        'Choose your treatments and availability. The form prepares your message on your device: send it by WhatsApp or email.',
        'Elige tus servicios y tu disponibilidad. El formulario prepara tu mensaje en tu dispositivo: envíalo por WhatsApp o por correo.'),
    'Ou contactez-moi directement :': ('Of neem rechtstreeks contact op:', 'Or contact me directly:', 'O contáctame directamente:'),
    '(appel ou WhatsApp)': ('(bellen of WhatsApp)', '(call or WhatsApp)', '(llamada o WhatsApp)'),
    '1. Vos soins': ('1. Uw behandelingen', '1. Your treatments', '1. Tus servicios'),
    'Total indicatif': ('Indicatief totaal', 'Estimated total', 'Total orientativo'),
    '2. Vos disponibilités': ('2. Uw beschikbaarheid', '2. Your availability', '2. Tu disponibilidad'),
    'Date souhaitée': ('Gewenste datum', 'Preferred date', 'Fecha deseada'),
    'Moment de la journée': ('Moment van de dag', 'Time of day', 'Momento del día'),
    'Indifférent': ('Maakt niet uit', 'Any time', 'Indiferente'),
    'Matin': ('Voormiddag', 'Morning', 'Mañana'),
    'Après-midi': ('Namiddag', 'Afternoon', 'Tarde'),
    'Fin de journée': ('Einde van de dag', 'Late afternoon', 'Última hora'),
    '3. Vos coordonnées': ('3. Uw gegevens', '3. Your details', '3. Tus datos'),
    'Prénom et nom': ('Voor- en achternaam', 'First and last name', 'Nombre y apellido'),
    'Téléphone': ('Gsm', 'Phone', 'Teléfono'),
    'Message (facultatif)': ('Bericht (optioneel)', 'Message (optional)', 'Mensaje (opcional)'),
    'Une précision, une allergie, une question…': ('Een opmerking, een allergie, een vraag…', 'A detail, an allergy, a question…', 'Un detalle, una alergia, una pregunta…'),
    'Envoyer par WhatsApp': ('Versturen via WhatsApp', 'Send via WhatsApp', 'Enviar por WhatsApp'),
    'Envoyer par e-mail': ('Versturen per e-mail', 'Send by email', 'Enviar por correo'),
    'Copier le message': ('Bericht kopiëren', 'Copy message', 'Copiar mensaje'),
    'Aucune donnée n’est envoyée par ce site : le message s’ouvre dans WhatsApp ou votre messagerie et vous décidez de l’envoyer.': (
        'Deze site verstuurt geen gegevens: het bericht opent in WhatsApp of uw mailprogramma en u beslist zelf of u het verstuurt.',
        'This site sends no data: the message opens in WhatsApp or your email app and you decide whether to send it.',
        'Este sitio no envía ningún dato: el mensaje se abre en WhatsApp o en tu correo y tú decides si enviarlo.'),

    # Page confidentialité
    'Confidentialité et mentions légales · Sofia Nails': (
        'Privacy en wettelijke vermeldingen · Sofia Nails', 'Privacy and legal notice · Sofia Nails', 'Privacidad y aviso legal · Sofia Nails'),
    'Comment Sofia Nails traite vos données : aucun cookie, aucun suivi, messages de réservation utilisés uniquement pour organiser votre rendez-vous.': (
        'Hoe Sofia Nails met uw gegevens omgaat: geen cookies, geen tracking, reservatieberichten enkel gebruikt om uw afspraak te regelen.',
        'How Sofia Nails handles your data: no cookies, no tracking, booking messages used only to arrange your appointment.',
        'Cómo trata Sofia Nails tus datos: sin cookies, sin seguimiento, mensajes de reserva usados solo para organizar tu cita.'),
    'Informations légales': ('Wettelijke informatie', 'Legal information', 'Información legal'),
    'Confidentialité': ('Privacy', 'Privacy', 'Privacidad'),
    'et mentions légales.': ('en wettelijke vermeldingen.', 'and legal notice.', 'y aviso legal.'),
    'Dernière mise à jour : novembre 2026': ('Laatst bijgewerkt: november 2026', 'Last updated: November 2026', 'Última actualización: noviembre de 2026'),
    'Qui sommes-nous ?': ('Wie zijn we?', 'Who we are', '¿Quiénes somos?'),
    'Sofia Nails : soins des ongles, épilation à la cire et pédicure médicale, sur rendez-vous à Uccle (Bruxelles).': (
        'Sofia Nails: nagelverzorging, harsen en medische pedicure, op afspraak in Ukkel (Brussel).',
        'Sofia Nails: nail care, waxing and medical pedicure, by appointment in Uccle (Brussels).',
        'Sofia Nails: cuidado de uñas, depilación con cera y pedicura médica, con cita previa en Uccle (Bruselas).'),
    'Contact :': ('Contact:', 'Contact:', 'Contacto:'),
    'Données personnelles': ('Persoonsgegevens', 'Personal data', 'Datos personales'),
    'Ce site ne collecte et n’enregistre aucune donnée : il n’envoie rien à un serveur et ne demande aucun compte.': (
        'Deze site verzamelt en bewaart geen gegevens: hij stuurt niets naar een server en vraagt geen account.',
        'This site does not collect or store any data: it sends nothing to a server and requires no account.',
        'Este sitio no recoge ni guarda ningún dato: no envía nada a un servidor y no pide ninguna cuenta.'),
    'Le formulaire de rendez-vous prépare un message sur votre appareil. Si vous choisissez de l’envoyer par WhatsApp ou par e-mail, Sofia Nails reçoit les informations que vous y avez indiquées (nom, téléphone, soins souhaités, message) et les utilise uniquement pour organiser votre rendez-vous. Elles ne sont ni vendues ni transmises à des tiers.': (
        'Het afsprakenformulier maakt een bericht klaar op uw toestel. Als u het via WhatsApp of e-mail verstuurt, ontvangt Sofia Nails de informatie die u invulde (naam, gsm, gewenste behandelingen, bericht) en gebruikt die enkel om uw afspraak te regelen. Ze wordt niet verkocht en niet aan derden doorgegeven.',
        'The booking form prepares a message on your device. If you choose to send it by WhatsApp or email, Sofia Nails receives the information you entered (name, phone, requested treatments, message) and uses it only to arrange your appointment. It is never sold or passed on to third parties.',
        'El formulario de cita prepara un mensaje en tu dispositivo. Si decides enviarlo por WhatsApp o por correo, Sofia Nails recibe la información que has indicado (nombre, teléfono, servicios deseados, mensaje) y la usa únicamente para organizar tu cita. No se vende ni se cede a terceros.'),
    'Durée de conservation': ('Bewaartermijn', 'How long we keep it', 'Plazo de conservación'),
    'Ces informations sont conservées le temps nécessaire au suivi de vos rendez-vous, puis supprimées.': (
        'Deze informatie wordt bewaard zolang nodig is voor de opvolging van uw afspraken en daarna verwijderd.',
        'This information is kept only as long as needed to follow up your appointments, then deleted.',
        'Esta información se conserva el tiempo necesario para el seguimiento de tus citas y después se elimina.'),
    'Vos droits': ('Uw rechten', 'Your rights', 'Tus derechos'),
    'Conformément au RGPD, vous pouvez demander l’accès à vos données, leur rectification ou leur suppression en écrivant à': (
        'Volgens de AVG kunt u inzage, verbetering of verwijdering van uw gegevens vragen door te schrijven naar',
        'Under the GDPR, you can ask to access, correct or delete your data by writing to',
        'Según el RGPD, puedes solicitar el acceso, la rectificación o la supresión de tus datos escribiendo a'),
    'Vous pouvez aussi introduire une réclamation auprès de l’': (
        'U kunt ook een klacht indienen bij de', 'You can also file a complaint with the', 'También puedes presentar una reclamación ante la'),
    'Autorité de protection des données': ('Gegevensbeschermingsautoriteit', 'Belgian Data Protection Authority', 'Autoridad belga de protección de datos'),
    'Cookies et suivi': ('Cookies en tracking', 'Cookies and tracking', 'Cookies y seguimiento'),
    'Ce site n’utilise aucun cookie, aucun outil de mesure d’audience et aucune publicité. Les polices sont hébergées sur le site lui-même : aucune ressource n’est chargée depuis un service tiers.': (
        'Deze site gebruikt geen cookies, geen bezoekersstatistieken en geen reclame. De lettertypes staan op de site zelf: er wordt niets geladen van externe diensten.',
        'This site uses no cookies, no analytics and no advertising. Fonts are hosted on the site itself: nothing is loaded from third-party services.',
        'Este sitio no usa cookies, ni herramientas de medición de audiencia, ni publicidad. Las fuentes están alojadas en el propio sitio: no se carga nada desde servicios de terceros.'),
    'Liens externes': ('Externe links', 'External links', 'Enlaces externos'),
    'Les liens vers WhatsApp, TikTok et Google Maps ouvrent des services externes, soumis à leurs propres règles de confidentialité.': (
        'De links naar WhatsApp, TikTok en Google Maps openen externe diensten, met hun eigen privacyregels.',
        'Links to WhatsApp, TikTok and Google Maps open external services, which have their own privacy rules.',
        'Los enlaces a WhatsApp, TikTok y Google Maps abren servicios externos, sujetos a sus propias normas de privacidad.'),
    'Retour à l’accueil': ('Terug naar de startpagina', 'Back to home', 'Volver al inicio'),

    # Élégance : hero, bandeau, phrase de marque
    'Découvrir': ('Ontdekken', 'Discover', 'Descubrir'),
    'Spécialités': ('Specialiteiten', 'Specialities', 'Especialidades'),
    'Prendre le temps,': ('De tijd nemen,', 'Taking the time,', 'Tomarse el tiempo,'),
    'soigner chaque détail.': ('voor elk detail zorgen.', 'caring for every detail.', 'cuidar cada detalle.'),
    'Sofia Nails · Uccle': ('Sofia Nails · Ukkel', 'Sofia Nails · Uccle', 'Sofia Nails · Uccle'),

    # Pied de page
    'Épilation à la cire · Manucure · Pédicure médicale': (
        'Harsen · Manicure · Medische pedicure', 'Waxing · Manicure · Medical pedicure', 'Depilación con cera · Manicura · Pedicura médica'),
    'Uniquement sur rendez-vous, chez moi à Bruxelles.': (
        'Enkel op afspraak, bij mij thuis in Brussel.', 'By appointment only, at my home in Brussels.', 'Solo con cita previa, en mi casa en Bruselas.'),
    'Haut de page ↑': ('Naar boven ↑', 'Back to top ↑', 'Volver arriba ↑'),
    'Contact rapide': ('Snel contact', 'Quick contact', 'Contacto rápido'),
    'Réserver': ('Reserveren', 'Book', 'Reservar'),
}

# Textes identiques dans toutes les langues
SAME = {'Sofia Nails', 'Sofia', 'Nails', 'WhatsApp', 'TikTok', 'French', 'Nail art', '@adriana.torres886', 'contact@sofianailsbrussels.com', 'Sofia Nails ·'}

# Textes du script (formulaire de réservation)
JS = {
    'fr': {
        'from': 'dès ', 'fromLong': 'à partir de ', 'later': 'à convenir', 'hello': 'Bonjour Sofia,',
        'want': 'Je souhaite prendre rendez-vous pour :', 'total': 'Total indicatif', 'date': 'Date souhaitée',
        'moment': 'Moment', 'any': 'indifférent', 'name': 'Nom', 'phone': 'Téléphone', 'message': 'Message',
        'thanks': 'Merci !', 'errSoin': 'Choisissez au moins un soin.', 'errNom': 'Indiquez votre prénom et votre nom.',
        'copied': 'Message copié.', 'copyFail': 'Copie impossible sur cet appareil.', 'subject': 'Rendez-vous Sofia Nails',
        'add': 'Ajouter {s} au rendez-vous', 'remove': 'Retirer {s} du rendez-vous', 'lang': '', 'locale': 'fr-BE',
    },
    'nl': {
        'from': 'vanaf ', 'fromLong': 'vanaf ', 'later': 'in overleg', 'hello': 'Hallo Sofia,',
        'want': 'Ik wil graag een afspraak maken voor:', 'total': 'Indicatief totaal', 'date': 'Gewenste datum',
        'moment': 'Moment', 'any': 'maakt niet uit', 'name': 'Naam', 'phone': 'Gsm', 'message': 'Bericht',
        'thanks': 'Bedankt!', 'errSoin': 'Kies minstens één behandeling.', 'errNom': 'Vul uw voor- en achternaam in.',
        'copied': 'Bericht gekopieerd.', 'copyFail': 'Kopiëren is niet mogelijk op dit toestel.', 'subject': 'Afspraak Sofia Nails',
        'add': '{s} toevoegen aan de afspraak', 'remove': '{s} verwijderen uit de afspraak', 'lang': 'Taal van de klant: Nederlands', 'locale': 'nl-BE',
    },
    'en': {
        'from': 'from ', 'fromLong': 'from ', 'later': 'to be agreed', 'hello': 'Hello Sofia,',
        'want': 'I would like to book an appointment for:', 'total': 'Estimated total', 'date': 'Preferred date',
        'moment': 'Time', 'any': 'any time', 'name': 'Name', 'phone': 'Phone', 'message': 'Message',
        'thanks': 'Thank you!', 'errSoin': 'Please choose at least one treatment.', 'errNom': 'Please enter your first and last name.',
        'copied': 'Message copied.', 'copyFail': 'Copying isn’t possible on this device.', 'subject': 'Sofia Nails appointment',
        'add': 'Add {s} to the appointment', 'remove': 'Remove {s} from the appointment', 'lang': 'Client language: English', 'locale': 'en-GB',
    },
    'es': {
        'from': 'desde ', 'fromLong': 'desde ', 'later': 'a convenir', 'hello': 'Hola Sofia,',
        'want': 'Me gustaría pedir cita para:', 'total': 'Total orientativo', 'date': 'Fecha deseada',
        'moment': 'Momento', 'any': 'indiferente', 'name': 'Nombre', 'phone': 'Teléfono', 'message': 'Mensaje',
        'thanks': '¡Gracias!', 'errSoin': 'Elige al menos un servicio.', 'errNom': 'Indica tu nombre y apellido.',
        'copied': 'Mensaje copiado.', 'copyFail': 'No se puede copiar en este dispositivo.', 'subject': 'Cita Sofia Nails',
        'add': 'Añadir {s} a la cita', 'remove': 'Quitar {s} de la cita', 'lang': 'Idioma del cliente: español', 'locale': 'es-ES',
    },
}
