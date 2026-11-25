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
SAME = {'Sofia Nails', 'Sofia', 'Nails', 'WhatsApp', 'TikTok', '@adriana.torres886', 'contact@sofianailsbrussels.com', 'Sofia Nails ·'}

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
