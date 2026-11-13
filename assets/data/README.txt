DONNEES DU JEU (JSON)
=====================

Ces fichiers decrivent le contenu du jeu. On peut les modifier sans toucher au code :
un fichier invalide ou manquant fait echouer le lancement avec un message qui nomme le fichier.

Ils sont lus d'abord dans le classpath (data/...), puis dans assets/data/ du dossier courant,
donc ca marche avec Maven comme avec run.bat.

quests.json
    Les quetes du tableau de la guilde. title = nom UNIQUE (sert d'identifiant dans la sauvegarde).
    rank : rang minimum (0 = F, 1 = E, ...) ; type : COMBAT, DELIVERY ou ESCORT ;
    goldReward = or verse quand on rend la quete ; proofItem = objet a rapporter a la guilde pour la rendre
    (butin d'un monstre, recu de livraison...) ; giveItem = objet remis a l'acceptation (livraison : la lettre
    a porter) ; target = destinataire d'une livraison (le forgeron : "Forgeron").
    experienceReward n'est plus utilise.

skills.json
    Les competences. On ne les achete pas : elles progressent quand on fait l'action.
    id = identifiant (utilise par le code et par classes/backgrounds) ; trainedBy = texte affiche
    (ce qui la fait progresser) ; unit = unite de pratique (m, Po...).
    thresholds = pratique cumulee pour atteindre le niveau 1, 2, 3... (liste croissante ; le niveau 1
    "debloque" la competence). effects = ce que chaque niveau apporte (cumule) :
    speedPercent (% de vitesse), carryKg (kg de charge), buyDiscountPercent (% de reduction a l'achat),
    sellBonusPercent (% en plus a la revente), healBonusPercent (% de soin en plus des potions),
    healthBonus (PV max).
    Ce qui donne de la pratique est dans le code : marcher (1 par metre, Endurance), marcher avec au moins
    la moitie de sa charge max (Portage), 1 par Po echange en boutique (Marchandage), 1 par potion de soin
    utilisee (Soins), manger a l'auberge (1) et y dormir (2) (Vigueur).

classes.json et backgrounds.json
    Les choix de la creation du personnage (4 classes, 4 origines). Chaque choix a des avantages et des
    inconvenients CHIFFRES, que l'ecran de creation affiche tels quels :
    healthBonus (PV), carryBonus (kg), speedBonusPercent (% de vitesse), goldBonus (Po au depart),
    startingItems (identifiants de items.json), skillStart (competences deja debloquees : id -> niveau),
    practiceBonusPercent (une competence progresse plus vite (+) ou moins vite (-) : id -> %).
    Les valeurs negatives sont les inconvenients ; l'or de depart ne descend jamais sous 0.

difficulties.json
    Par difficulte (facile, normale, realiste) :
    maxWeight = poids que le personnage porte sans sac (kg) ;
    encumbranceStart = part de la charge max a partir de laquelle on ralentit (0.5 = la moitie) ;
    minSpeedPercent = vitesse (en %) quand on est charge au maximum ;
    startingGold = or de depart ; startingItems = identifiants (items.json) des objets de depart.

town.json
    La ville. "ground" = couleur du sol. Chaque batiment : nom, type, position x/z (centre, au sol),
    couleur, l'ecran d'interieur a ouvrir ("forge", "guild" ou "inn"), "exit" (ou le joueur reapparait
    en ville quand il sort) et "parts" (details du batiment : toit, porte, enseigne...).
    "decor" = tout le reste (rues, place, maisons, arbres, murs...), en coordonnees absolues.
    Une partie est une boite : x, z = centre ; y = hauteur du bas au-dessus du sol ; w, h, d = largeur,
    hauteur, profondeur (en metres) ; color = nom LibGDX (DARK_GRAY, BROWN, TAN...) ou "#RRGGBB" ;
    decal = true pour du visuel seulement (rues, eau) : on marche dessus au lieu de la heurter.
    Pour les "parts" d'un batiment, x/z sont relatifs a son centre. Viser une partie d'un batiment
    (la porte, l'enseigne) permet d'y entrer, comme pour le batiment lui-meme.

calendar.json
    Le temps. 1 heure de jeu = secondsPerGameHour secondes reelles. Une saison dure daysPerSeason jours,
    une annee = 4 saisons. Les jours de la semaine tournent sur la liste "weekdays".
    Pour chaque saison : sunrise / sunset = heures de lever et coucher du soleil (le jeu les adoucit d'une
    saison a l'autre). wakeHour = heure du reveil apres une nuit a l'auberge.
    newGame = date, heure et age du personnage au debut d'une nouvelle partie.

aging.json
    Paliers de vieillesse, du plus jeune au plus vieux (le premier doit commencer a l'age 0).
    A partir de fromAge ans, les pourcentages s'appliquent a la vie max, a la charge portee et a la vitesse.

items.json
    Catalogue de tous les objets. "itemType" = Arme, Armure, Sac, PotionSoin, PotionDeSoin, PotionDps ou PotionMix.
    id = identifiant unique (utilise par les boutiques et les kits de depart) ; poids en kg ; prix en Po.
    Arme : bonusDegats. Armure : reductionDegats. Sac : capacityBonus (kg de charge en plus quand il est porte).
    Potions : soin / degats / healingAmount selon le type.

shops.json
    Les boutiques ("forge", "guild") : title, sellPercent (% du prix paye quand on leur revend un objet)
    et stock = identifiants (items.json) des objets en vente.
