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
    Arbres de competences, par classe ("Caserne"...).
    prerequisiteSkillName : competence requise (absent = aucune).
    attackToLearn.attaqueType : AttaqueSimple, AttaqueMelee ou AttaqueDistance.

difficulties.json
    Poids maximum de l'inventaire par difficulte (facile, normale, realiste).

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

