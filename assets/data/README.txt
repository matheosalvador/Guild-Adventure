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
