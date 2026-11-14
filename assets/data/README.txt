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
    utilisee (Soins), manger a l'auberge (1) et y dormir (2) (Vigueur), 1 par coup porte (Melee), 1 par coup
    recu (Robustesse). Effets supplementaires : meleePercent (% de degats), defenseBonus (points de defense).

monsters.json
    Les monstres, par identifiant. health = PV ; damage = degats d'un coup (avant la defense du joueur) ;
    speed = vitesse de course (m/s) ; aggroRange = distance a laquelle il repere le joueur (m) ;
    attackRange = distance de frappe (m, <= aggroRange) ; attackCooldown = secondes entre deux coups ;
    goldMin/goldMax = or lache a la mort ; lootItem = objet de quete lache SI une quete active le demande
    comme preuve (proofItem) ; color = nom LibGDX ou "#RRGGBB" ; w/h/d = taille de la boite qui le represente.
    drops = objets (identifiants de items.json) qui tombent AU SOL a la mort, chacun avec sa "chance" (1 a 100 %) ;
    on les ramasse avec E. Ce sont en general des "Butin" (a revendre en boutique).
    Pour qu'un monstre apparaisse : Monster.spawn("giant_rat", x, z) dans un ecran (voir CellarScreen),
    ou l'ecrire dans une zone sauvage (zone_*.json).

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
    startingGold = or de depart ; startingItems = identifiants (items.json) des objets de depart ;
    ambushPercent = chance (0 a 100 %) d'une embuscade a chaque voyage entre deux villes (0 = jamais).

world.json
    Le monde : les lieux (villes) et les routes. Chaque lieu : id, name, townFile (le fichier de sa ville :
    town.json, town_portbrume.json...) et sa position x/z sur la carte du monde, EN KM. Les routes sont des
    paires d'identifiants. La duree d'un trajet n'est pas ecrite : elle vient de la distance et de
    walkingKmh (vitesse de marche, km/h), et elle s'allonge si le personnage est charge ou age.
    startPlace = ou commence une nouvelle partie. Pour ajouter une ville : une ligne dans "places", une ou
    plusieurs dans "links", et son fichier de ville.
    Un lieu est une ville (townFile) OU une zone sauvage (zoneFile : zone_foret.json...), jamais les deux.
    "ambush" (facultatif) = les embuscades entre villes : zoneFile (le decor de la route, zone_route.json),
    monsters (qui attaque) et min/max (combien d'agresseurs). Le voyage est coupe a la moitie ; on repart
    en vainquant les agresseurs. Si on tombe, on se reveille a l'auberge de la ville de depart.

zone_foret.json, zone_ruines.json, zone_route.json (une zone sauvage par fichier)
    Un terrain carre : "ground" (couleur), "size" (cote, en m), "arrival" (ou on arrive), "exit" (la borne de
    route : la viser ouvre la carte du monde), "decor" (boites, comme pour une ville : arbres, murs...),
    "monsters" (id de monsters.json + x/z ; ils reapparaissent a chaque visite) et "chests" (x/z, gold, items =
    identifiants de items.json, respawnDays = jours avant qu'un coffre ouvert se remplisse de nouveau).
    Les objets jetes ou lacher par les monstres restent au sol tant que la partie tourne (non sauvegardes).

town.json, town_portbrume.json, town_hautevigne.json (un fichier par ville)
    La ville. "ground" = couleur du sol. "arrival" = ou on apparait en arrivant par la route (devant la porte).
    Chaque batiment : nom, type, position x/z (centre, au sol), couleur, l'ecran d'interieur a ouvrir
    ("forge", "guild", "inn" ou "store"), "exit" (ou le joueur reapparait en ville quand il sort), "parts"
    (details du batiment : toit, porte, enseigne...) et, pour un "store", "shop" (la boutique, dans shops.json).
    "decor" = tout le reste (rues, place, maisons, arbres, murs...), en coordonnees absolues.
    Une partie est une boite : x, z = centre ; y = hauteur du bas au-dessus du sol ; w, h, d = largeur,
    hauteur, profondeur (en metres) ; color = nom LibGDX (DARK_GRAY, BROWN, TAN...) ou "#RRGGBB" ;
    decal = true pour du visuel seulement (rues, eau) : on marche dessus au lieu de la heurter ;
    action = "gate" pour la porte de la ville (E ouvre la carte du monde).
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
    stock = identifiants (items.json) des objets en vente, et priceFactor = prix de la boutique par rapport au
    catalogue, en % (100 par defaut ; 90 = 10 % moins cher, 115 = 15 % plus cher : c'est ce qui fait varier
    les prix d'une ville a l'autre).
