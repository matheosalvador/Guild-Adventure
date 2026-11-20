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
    rank : rang minimum (0 = F, 1 = E, ...). Les rangs s'obtiennent selon story.json (des quetes terminees ET une etape de l'histoire).
    type :
      COMBAT   : proofItem = objet de quete lache par un monstre (monsters.json "lootItem") tant que la quete
                 est active ; on le rapporte au tableau.
      DELIVERY : giveItem = objet remis a l'acceptation ; target = le destinataire, par son NOM (le forgeron :
                 "Forgeron", ou le nom d'une auberge ou d'une boutique, "Comptoir du Port"...). Le joueur le lui
                 remet par le bouton "Remettre la livraison". Avec "payOnDelivery": true, la quete est payee et
                 terminee a la remise (livraison entre villes) ; sinon on recoit proofItem a rapporter au tableau.
      COLLECT  : collectItem (identifiant de items.json) x collectCount sont rapportes au tableau, qui les reprend.
    goldReward = or verse a la fin.

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
    Facultatif, pour l'histoire :
    flyHeight = hauteur de vol au-dessus du sol (m, une chauve-souris) ; boss = true : sa barre de vie reste en
    haut de l'ecran tant qu'il est proche ; killFlag / killMessage = drapeau d'histoire pose a sa mort (voir
    story.json) et la phrase affichee ; yieldPercent / yieldFlag / yieldMessage = il se rend au lieu de mourir
    quand sa vie passe sous ce % (il ne bouge plus, ne frappe plus) et le drapeau est pose.
    phases = la suite du combat, de la premiere a la derniere : chaque phase commence quand la vie passe sous
    "below" % (decroissant d'une phase a l'autre) et peut changer damage / speed / attackCooldown, soigner
    (healAmount PV toutes les healEvery s, seulement si healRequires est vrai : conditions de story.json),
    appeler des renforts (summon = id de monstre, summonCount de 1 a 6) et afficher un "message".

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
    ambushPercent = chance (0 a 100 %) d'une embuscade a chaque etape de 1500 m marchee sur la route (0 = jamais).

world.json
    Le monde : les lieux (villes) et les routes. Chaque lieu : id, name, townFile (le fichier de sa ville :
    town.json, town_portbrume.json...) et sa position x/z sur la carte du monde, EN KM (dans le monde ouvert :
    x 100 m, voir openworld.json). Les routes sont des paires d'identifiants : le jeu trace une route a pied pour
    chacune. La duree d'un voyage rapide vient de la distance et de walkingKmh (vitesse de marche, km/h), et
    elle s'allonge si le personnage est charge ou age.
    startPlace = ou commence une nouvelle partie. Pour ajouter une ville : une ligne dans "places", une ou
    plusieurs dans "links", et son fichier de ville.
    Un lieu est une ville (townFile) OU une zone sauvage (zoneFile : zone_foret.json...), jamais les deux.
    "ambush" (facultatif) = les embuscades sur les routes du monde ouvert : monsters (qui attaque) et min/max
    (combien d'agresseurs surgissent devant le joueur). Pas de sauvegarde tant qu'ils sont la ; laisses loin
    derriere, ils abandonnent. Si on tombe, on se reveille a l'auberge de la derniere ville.
    Facultatif, pour l'histoire : "requires" = conditions (voir story.json) pour pouvoir s'y rendre, et
    "lockedText" = la raison affichee (a sa porte, et sur la carte) tant que le lieu est ferme ; "hidden" = true pour une zone
    qui n'est pas sur la carte (un etage de donjon : on n'y arrive que par une porte, aucune route n'y mene).
    Le lieu de depart ne peut etre ni ferme ni cache.

zone_foret.json, zone_ruines.json, zone_mine_*.json... (une zone par fichier)
    Un terrain carre : "ground" (couleur), "size" (cote, en m), "arrival" (ou on arrive), "exit" (la borne de
    sortie d'un lieu ferme : elle ramene devant sa porte dans le monde ouvert ; la foret et les ruines sont
    posees dans le monde ouvert et n'en ont pas besoin), "decor" (boites, comme pour une ville : arbres, murs...),
    "monsters" (id de monsters.json + x/z ; ils reapparaissent a chaque visite) et "chests" (x/z, gold, items =
    identifiants de items.json, respawnDays = jours avant qu'un coffre ouvert se remplisse de nouveau).
    Les objets jetes ou laches par les monstres restent au sol 7 jours de jeu (sauvegardes avec le personnage).
    Les zones de l'histoire (zone_mine_*, zone_fosse, zone_corvenne_*) sont fabriquees par
    docs/outils-test/gen_story_zones.py : modifier le script puis le relancer plutot que les fichiers a la main.
    Facultatif :
    "exit" n'est obligatoire que si la zone n'a aucune porte ;
    "indoor" = true pour un lieu ferme (mine, salle) : pas de ciel ni d'ombres, lumiere fixe ;
    "background" = couleur du fond (au-dessus des murs) ; "wall" = couleur du tour de la zone ;
    "ambient" = lumiere ambiante de 0 a 1 (0 = 0,4 dehors, 0,3 dedans) ;
    un monstre peut avoir "requires" (conditions de story.json) : il n'est la que si elles sont vraies ;
    "doors" = portes vers une autre zone : x/z, to (un lieu de world.json qui est une zone), label (texte
    quand on la vise), spawn (ou on arrive de l'autre cote ; sinon a son "arrival"), color, et s'il faut une
    condition : requires + lockedText (la raison affichee tant qu'elle est fermee) ;
    "arena" = une epreuve en vagues : startFlag (le drapeau d'histoire qui la lance), waves (listes de
    monstres id/x/z), winSets / failSets (drapeaux poses si on gagne, ou si on tombe ou sort avant la fin),
    winMessage / failMessage. On ne peut pas sauvegarder pendant l'arene ;
    une partie de decor peut avoir action = "story" + text = un point de story.json ("cairn"...) : E ouvre
    ses dialogues d'histoire.

town.json, town_portbrume.json, town_hautevigne.json (un fichier par ville)
    La ville. "ground" = couleur du sol. "arrival" = ou on apparait en arrivant par la route (devant la porte).
    Chaque batiment : nom, type, position x/z (centre, au sol), couleur, l'ecran d'interieur a ouvrir
    ("forge", "guild", "inn" ou "store"), "exit" (ou le joueur reapparait en ville quand il sort), "parts"
    (details du batiment : toit, porte, enseigne...) et, pour un "store", "shop" (la boutique, dans shops.json).
    "decor" = tout le reste (rues, place, maisons, arbres, murs...), en coordonnees absolues.
    Une partie est une boite : x, z = centre ; y = hauteur du bas au-dessus du sol ; w, h, d = largeur,
    hauteur, profondeur (en metres) ; color = nom LibGDX (DARK_GRAY, BROWN, TAN...) ou "#RRGGBB" ;
    decal = true pour du visuel seulement (rues, eau) : on marche dessus au lieu de la heurter ;
    action = "gate" pour la porte de la ville (dans le monde ouvert, elle est ouverte : la route part de la) ;
    shape = box (par defaut), cylinder, sphere, cone ou gable (toit a deux pentes : w = longueur de la crete,
    d = largeur, h = hauteur ; la crete court le long de X) ; une forme autre que box est VISUELLE seulement
    (on ne la heurte pas), comme "rot" [x, y, z] (rotation en degres) ; anim = "water" : la surface ondule.
    "villagers" (au niveau de la ville) : les habitants qui se promenent : model (models.json), speed (m/s) et route
    (au moins 2 points {x, z}, parcourus en boucle) et "talk" = ses repliques (un habitant de npcs.json). Avec
    "talk", il s'arrete et se tourne vers le joueur qui s'approche, et E lui parle ; sans, il ne fait que passer.
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
    Une rumeur peut avoir "requires" (conditions de story.json) : elle ne se raconte qu'a partir de ce moment.

story.json
    L'histoire. Tout repose sur des DRAPEAUX (des noms, sauvegardes avec le personnage, avec l'heure ou ils
    ont ete poses) et des CONDITIONS. Une liste de conditions est vraie quand toutes le sont. Une condition
    s'ecrit type:argument, precedee de "!" pour dire le contraire :
      flag:nom          le drapeau est pose
      quest:Titre       la quete (quests.json) est terminee
      lore:text:id      le texte (lore.json) a ete lu ; lore:rumor:id : la rumeur a ete entendue
      rank:3            le personnage est au moins de ce rang (0 = F, 1 = E...)
      quests:7          au moins 7 quetes terminees
      item:id           le personnage porte cet objet (items.json)
      since:nom:4320    le drapeau a ete pose il y a au moins 4320 minutes de jeu (3 jours)
      within:nom:840    le drapeau a ete pose il y a au plus 840 minutes (14 heures)
    Le jeu pose lui-meme : started:<chapitre>, done:<chapitre>, ended:<fin>, trigger:<declencheur>.
    "ranks" : les rangs de la Guilde a partir de 1 (E), dans l'ordre : rank, label (la lettre), requires.
      Un rang n'est donne que si le precedent l'est ; il ne redescend jamais.
    "chapters" : id, title, intro et outro (textes montres au debut et a la fin), requires (quand il
      commence), objectives (text = la ligne du journal, done = conditions qui la cochent), goldReward et
      sets (drapeaux en plus a la fin). Un chapitre est fini quand tous ses objectifs sont coches.
    "points" : les endroits du decor qui ne sont pas des PNJ (id -> nom), relies par action "story".
    "dialogs" : id, at (ou : guildmaster@ville, innkeeper@ville, blacksmith@ville, shopkeeper@ville, ou un
      point), requires, label (le bouton), text. Sans "options", ses effets s'appliquent a la lecture ;
      avec, chaque option (label, text, requires, next = un autre dialogue) a ses effets. Effets : sets /
      unsets (drapeaux), gives / takes (objets de items.json ; refuse sans rien changer si c'est trop lourd
      ou si l'objet manque).
    "pickups" : objets d'histoire au sol (une boite doree) : scene (EcranSimple@lieu, ex. CellarScreen@aethelgard
      ou ZoneScreen@mine_galeries), x/z, name, flag (pose quand on le ramasse), requires, message.
    "triggers" : cercles au sol (scene, x/z, radius, requires) qui, la premiere fois qu'on y entre, posent
      "sets" et affichent "message" (une ligne) ou "title" + "text" (une fenetre).
    "rules" : des drapeaux poses tout seuls des que requires est vrai.
    "endings" : id, title, text (l'epilogue, sur son ecran de fin), requires, goldReward. Le jeu continue apres.
    "boons" : avantages de combat dans certains lieux (places) quand requires est vrai : damagePercent,
      healthBonus (les sceaux, au chateau).
    "effects" : ce qu'une fin change au monde tant que requires est vrai : ambushPercent (% de la chance
      normale d'embuscade), priceFactor (boutique -> nouveau facteur de prix), stopAging (le personnage ne
      vieillit plus), guildClosed (la Guilde ne lui donne plus de quetes), title (affiche dans le journal).
    Pour tester sans tout rejouer : mode debug (F10), section Histoire : "Finir le chapitre en cours",
    "Rang suivant" ; la section "Voyager" mene aussi aux lieux fermes ou caches.

npcs.json
    Ce que disent les personnages.
    "npcs" : les quatre PNJ des batiments, chacun une fois : guildmaster, innkeeper, blacksmith, shopkeeper.
      prompt (le texte quand on le vise, sans "(E)"), greeting (la premiere ligne ; {lieu} = le nom du batiment),
      leave (le bouton qui ferme, "Partir" par defaut) et options, montrees dans l'ordre apres les dialogues
      d'histoire de story.json. Chaque option : action, label (le prix s'y ajoute : "Manger un repas (5 Po)"),
      requires (conditions de story.json : l'option n'apparait qu'a ce moment), et selon l'action :
        quests   le tableau des quetes (a la guilde) ; closedText = la reponse quand la Guilde ne donne plus
                 rien (fin "garder" de l'histoire)
        shop     shop = une boutique de shops.json ; chez le commercant, sans shop : celle du batiment (town.json)
        deliver  remettre une livraison ; target = le destinataire des quetes (sans : le nom du batiment)
        serve    un service paye : price, heal (PV rendus), vigor (entrainement de la Vigueur), text (le message),
                 noGoldText (quand l'or manque ; {prix} = le prix)
        rumor    une rumeur de lore.json : price, intro, emptyText (plus rien a raconter : rien n'est paye),
                 noGoldText
        news     les evenements des autres villes : intro, emptyText (aucun)
        talk     une simple replique : text
      rumor, news et talk ont un bouton "back" ("Merci" par defaut) qui revient a l'accueil.
    "villagers" : les repliques des habitants qui se promenent (relies par "talk" dans town*.json) : id, name
      (affiche au-dessus du dialogue) et lines. Chaque ligne : text, town (seulement dans cette ville) et
      requires (seulement a ce moment de l'histoire). L'habitant dit d'abord la DERNIERE ligne qui vaut (ecrire
      les lignes d'histoire a la fin), puis fait le tour des autres ("Autre chose ?").

openworld.json (le monde ouvert, voir docs/CADRAGE-MONDE-OUVERT.md)
    Tout l'exterieur est un seul terrain : une partie commence devant la porte de la ville de depart, on marche
    d'un lieu a l'autre. scale = metres de terrain par km de carte (100 : les durees de trajet de la carte restent
    a peu pres vraies a pied) ; chaque lieu est pose a sa position de world.json multipliee par scale.
    tileSize = cote d'un carreau de campagne (m) ; loadRadius = carreaux dessines autour du joueur ; solidRadius =
    carreaux dont les arbres sont solides ; viewDistance = distance de vue (la brume cache le reste) ;
    placeLoadDistance = un lieu est construit a moins de cette distance de son bord. ground / road = couleurs du
    sol et de la route, roadWidth = largeur de la route, treesPerTile = arbres semes par carreau (jamais sur une
    route ni dans un lieu ; un carreau est toujours le meme), monsterRespawnDays = jours avant qu'un monstre tue
    revienne.
    "places" : les villes et les zones sauvages : id (world.json), half = demi-cote du carre du lieu (son sol, ses
    murs ou sa haie ; rien n'y est seme), entrance = pour une zone, l'ouverture de la haie (x, z sur la haie, width).
    "portals" : les lieux fermes (mine, fosse, chateau) : on y entre comme dans une maison. id (world.json, une
    zone), half, decor = les formes de l'entree (meme format que le decor des villes, x/z relatifs au lieu) ; une
    partie a l'action "enter" : c'est la porte (E y entre ; les "requires" du lieu dans world.json la tiennent
    fermee). Un etage cache (mine_chambre...) ressort par l'entree posee a la meme position. Tout lieu non cache
    de world.json doit etre dans "places" ou dans "portals".
    Les routes sont tracees par le jeu, une par liaison de world.json ("links") : de la porte d'un lieu (porte de
    la ville, ouverture de la haie, porte de l'entree : le cote ou elle se trouve), droit devant, puis autour des
    murs par les coins si besoin, jusqu'a la porte de l'autre lieu. "roads" ne sert qu'a corriger un trace qui
    couperait un autre lieu : from, to (une liaison) et via = points de passage {x, z} en m dans le monde (un test
    verifie qu'aucune route ne traverse un lieu).
    En jeu : M ouvre la carte (elle ne montre pas ou l'on est : ni point, ni lieu mis en avant, et seuls les lieux
    deja atteints y ont leur nom) et propose le voyage rapide vers les lieux deja atteints ; F5 sauvegarde la position dans le monde ; en chemin, tous les 1500 m, une embuscade peut surgir
    (chance de la difficulte, "ambushPercent" de difficulties.json ; monstres de "ambush" dans world.json).

settings.json (pas dans ce dossier : a cote de save.json, cree par le jeu)
    Les reglages du menu Options (menu principal, ou pause > Options), communs a toutes les parties :
    mouseSensitivity (0,2 a 3 ; 1 = vitesse d'origine), thirdPerson (la vue au lancement ; V la bascule
    toujours), shadows, fullscreen. Chaque changement est enregistre tout de suite. Fichier absent ou illisible :
    reglages par defaut ; valeur hors limites : ramenee dans les limites. Le jeu ne plante jamais a cause de lui.

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
    "fallAxis" : comment le corps s'effondre a la mort : "x" (a la renverse, par defaut) ou "z" (sur le cote, pour
    les betes). Hors donnees : en plein air (villes, zones), le soleil porte des ombres autour du joueur, la lune
    et les etoiles se levent la nuit et des nuages passent ; le menu pause (Echap) a un bouton "Ombres" pour les
    couper si le jeu est trop lent. Un coup fait gicler de petits eclats, et un monstre vaincu s'effondre.

