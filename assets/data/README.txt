DONNEES DU JEU (JSON)
=====================

Ces fichiers decrivent le contenu du jeu. On peut les modifier sans toucher au code :
un fichier invalide ou manquant fait echouer le lancement avec un message qui nomme le fichier.

Ils sont lus d'abord dans le classpath (data/...), puis dans assets/data/ du dossier courant,
donc ca marche avec Maven comme avec run.bat.

quests.json
    Les quetes, reparties sur les TABLEAUX des villes. title = nom UNIQUE (sert d'identifiant dans la sauvegarde).
    town = la ville dont le tableau propose la quete (absent = la ville de depart : le tableau de la guilde
    d'Aethelgard ; Port-Brume et Hautevigne ont un tableau d'annonces pres de la porte). Chaque quete se
    rend la ou elle a ete prise.
    rank : rang minimum (0 = F, 1 = E, ...). Le rang monte d'un cran toutes les 3 quetes terminees.
    type :
      COMBAT   : proofItem = objet de quete lache par un monstre (monsters.json "lootItem") tant que la quete
                 est active ; on le rapporte au tableau.
      DELIVERY : giveItem = objet remis a l'acceptation ; target = le destinataire, par son NOM (le forgeron :
                 "Forgeron", ou le nom d'une auberge ou d'une boutique, "Comptoir du Port"...). Le joueur le lui
                 remet par le bouton "Remettre la livraison". Avec "payOnDelivery": true, la quete est payee et
                 terminee a la remise (livraison entre villes) ; sinon on recoit proofItem a rapporter au tableau.
      COLLECT  : collectItem (identifiant de items.json) x collectCount sont rapportes au tableau, qui les reprend.
    goldReward = or verse a la fin. experienceReward n'est plus utilise.

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
    action = "gate" pour la porte de la ville (E ouvre la carte du monde) ;
    action = "board" pour un tableau d'annonces (E l'ouvre : les quetes de la ville, voir quests.json) ;
    action = "read" + text = un identifiant de lore.json pour un panneau, une stele, un livre : E le lit
    (la premiere lecture l'ajoute au journal).
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

lore.json
    Le savoir du monde. "rumors" : les rumeurs de l'aubergiste, dans l'ordre ou il les raconte ; chacune a un
    id, un text, et un "town" (la ville ou on l'entend ; sans "town" : partout). Une bière (2 Po) = une
    rumeur nouvelle ; quand il n'en reste plus, l'aubergiste n'a rien a dire et rien n'est depense.
    "texts" : les textes a lire (id -> title + text), reliés aux panneaux par action "read" (voir town.json
    et zone_*.json). Tout ce qu'on a entendu ou lu est dans le journal (touche J) et sauvegarde.
    Eviter les guillemets francais « » : la police du jeu ne les dessine pas (utiliser des guillemets droits).

events.json
    Les evenements du monde. "dailyChance" = chance (%) qu'un evenement demarre chaque jour dans une ville qui
    n'en a pas deja un. Chaque evenement : id, name, description, days (duree, le jour du debut compte),
    buyChangePercent (variation en % des prix d'ACHAT : -15 = 15 % moins cher, +40 = 40 % plus cher),
    sellChangePercent (variation en % de ce que la boutique PAIE quand on lui revend : +20), items
    (identifiants de items.json concernes ; absent = tous) et towns (villes ou il peut avoir lieu ; absent =
    toutes). Il faut au moins un effet. L'evenement joue sur TOUTES les boutiques de sa ville (la forge et la
    guilde d'Aethelgard comprises). Le joueur le voit : message en arrivant en ville, ligne dans la boutique,
    "Demander des nouvelles" chez l'aubergiste (evenements des AUTRES villes), journal (J, villes connues).
    L'etat est sauvegarde avec le personnage. Debug (F10) : demarrer un evenement dans la ville courante.

models.json
    Les modeles 3D des personnages, faits de formes simples sur des articulations. "models" : un modele par
    nom ("human" = le joueur). Chaque modele : "joints" (point de pivot x/y/z en metres et "parent" ; un parent
    est defini AVANT ses enfants), "parts" (les formes), "pose" (angle fixe de repos de certaines articulations),
    "walk" (balancement des articulations a la marche : axis, amp en degres, phase) et "attack" (le geste du
    coup : duration, et pour chaque articulation un angle de depart "from" a "to").
    Une forme : shape (box, cylinder, sphere, cone ou capsule), w/h/d (taille en m), x/y/z (centre), rot (3 angles
    en degres, facultatif), color (nom LibGDX ou "#RRGGBB"), joint (l'articulation qui la porte ; absent = le corps).
    "slot": "armor" = la forme prend la couleur de l'armure portee (armorColors, par identifiant d'objet).
    Repere : l'origine est entre les pieds, au sol ; le modele regarde vers -Z ; +X est a sa droite ; +Y en haut.
    "gear" : des formes en plus pour un objet porte (arme en main, casque d'une armure, sac), par identifiant
    d'objet de items.json. Toute arme, armure et tout sac du catalogue doit avoir son aspect (un test le verifie).
    En jeu : V bascule entre la vue a la 1re et a la 3e personne.
    VARIANTES : un modele peut etre la variante d'un autre ("base") : il en reprend le squelette, les formes, la
    marche, le repos ("idle") et le coup, et ajoute : "roles" (couleur de chaque role de forme : skin, hair, tunic,
    pants, boots, belt, cloak... ; le role d'une forme s'ecrit "role"), "hide" (roles caches), "extra" (formes en
    plus : barbe, tablier, oreilles...), "gear" (accessoires "prop_..." portes, definis dans "gear"), "scale"
    [x, y, z] (etirement : un nain est large et petit) et "pose" (pose propre). Une base n'est pas elle-meme une
    variante. Modeles fournis : human (le joueur), blacksmith, innkeeper, guildmaster, merchant, goblin ;
    quadruped (base a quatre pattes), wolf, giant_rat.
    PNJ et monstres : un monstre (monsters.json) a un champ "model" (son modele 3D ; la boite de collision reste
    celle de w/h/d). Un PNJ est pose dans un ecran par addNpc("innkeeper", x, z, ...) : il respire (idle) et se
    tourne vers le joueur quand il approche.

