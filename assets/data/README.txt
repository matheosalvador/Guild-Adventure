DONNEES DU JEU (JSON)
=====================

Ces fichiers decrivent le contenu du jeu. On peut les modifier sans toucher au code :
un fichier invalide ou manquant fait echouer le lancement avec un message qui nomme le fichier.

Ils sont lus d'abord dans le classpath (data/...), puis dans assets/data/ du dossier courant,
donc ca marche avec Maven comme avec run.bat.

quests.json
    Liste des quetes du tableau de la guilde.
    rank : rang minimum (0 = F, 1 = E, ...) ; type : COMBAT, DELIVERY ou ESCORT.

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
    La ville : ses batiments (nom, type, position x/z, couleur LibGDX comme DARK_GRAY, BROWN, TAN),
    l'ecran d'interieur a ouvrir ("forge", "guild" ou "inn") et "exit", la position du joueur en
    ville quand il sort du batiment.

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
