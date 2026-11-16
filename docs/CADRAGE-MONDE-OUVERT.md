# Cadrage du monde ouvert

Document de décision : aucune ligne de code n'est écrite. Il compare trois façons de relier les lieux à pied, dit ce
que chacune réutilise et coûte, recommande une voie et propose un prototype (Aethelgard → Forêt des Loups). Il se
termine par les questions auxquelles tu dois répondre avant qu'on code quoi que ce soit.

## 1. Le monde aujourd'hui, en chiffres

| Mesure | Valeur | D'où elle vient |
| --- | --- | --- |
| Lieux | 3 villes, 2 zones sauvages, 7 routes | `world.json` |
| Distances | Aethelgard → Forêt 15 km, → Ruines 18 km, → Hautevigne 28 km, → Port-Brume 33 km ; Port-Brume → Hautevigne 52 km | positions en km de `world.json` |
| Étendue de la carte | 52 km d'est en ouest, 24 km du nord au sud | idem |
| Taille d'une scène | ville : sol de 200 m, murs à 60 m du centre ; forêt 160 m ; ruines 140 m ; bout de route 60 m | fichiers `town*.json`, `zone_*.json` |
| Contenu d'une scène | forêt : 217 pièces de décor dont 109 solides, 6 monstres, 2 coffres ; Aethelgard : 102 pièces, 90 corps solides | fichiers, et mesure du banc de test |
| Vitesse du joueur | 8 m par seconde réelle | `PlayerController` |
| Horloge | 1 heure de jeu = 60 secondes réelles | `calendar.json` |
| Vitesse de voyage sur la carte | 5 km/h de jeu | `world.json` |
| Vue | 300 m ; ombres calculées à 70 m autour du joueur | `BaseWorldScreen`, `SkyAndShadows` |

Chaque lieu est une scène fermée, construite en entier à l'entrée et détruite à la sortie. On passe de l'une à
l'autre par la carte : le temps avance d'un coup, le joueur réapparaît à l'arrivée.

## 2. La question qui commande tout : l'échelle

À l'échelle réelle, le monde ouvert n'est pas jouable :

- Aethelgard → Port-Brume, 33 km à 8 m/s, c'est **69 minutes réelles** de marche en ligne droite.
- Pendant ce temps l'horloge avance de 69 heures de jeu, alors que la carte annonce 6 h 35 : les deux se contredisent.
- Il faudrait remplir 1 250 km² de terrain, et au-delà de quelques kilomètres les positions perdent en précision
  (tremblements).

**Proposition : 1 km de carte = 100 m de terrain.** Ce n'est pas un chiffre au hasard : en une heure de jeu
(60 s réelles) le joueur parcourt 480 m, et la carte dit qu'on marche 5 km en une heure. Le rapport est de 1 pour 10,4.
Avec 1 pour 10, **les durées de trajet actuelles restent vraies sans rien changer à l'horloge** :

| Trajet | Sur la carte | Terrain à 1:10 | Marche réelle | Temps de jeu écoulé |
| --- | --- | --- | --- | --- |
| Aethelgard → Forêt des Loups | 15 km, 3 h | 1,5 km | 3 min 08 | 3 h 08 |
| Aethelgard → Port-Brume | 33 km, 6 h 35 | 3,3 km | 6 min 54 | 6 h 54 |
| Port-Brume → Hautevigne | 52 km, 10 h 25 | 5,2 km | 10 min 52 | 10 h 52 |

Le monde entier tient alors dans un rectangle d'environ 6 km sur 3 km. Un personnage chargé ou âgé marche moins
vite : son trajet dure plus longtemps tout seul, sans la formule d'aujourd'hui.

## 3. Trois approches

### A. Terrain continu, chargé par morceaux

Un seul écran pour tout l'extérieur. Le monde est découpé en carreaux (128 m de côté, par exemple) ; on ne garde en
mémoire que ceux qui entourent le joueur et on en change en marchant, sans coupure. Les villes et les zones
d'aujourd'hui sont posées à leur place dans ce monde ; entre elles, le terrain (herbe, arbres, route) est fabriqué
par le programme.

- **Se réutilise tel quel** : toutes les règles (`game/` : temps, quêtes, boutiques, événements, coffres, combat),
  les intérieurs des bâtiments (ils restent des écrans à part), les fichiers de villes et de zones (décalés à leur
  position), les modèles, le ciel et les ombres, les menus, la boucle de `BaseWorldScreen`.
- **À écrire** :
  - le chargement et la libération par carreau (aujourd'hui tout le décor appartient à l'écran et n'est libéré qu'à
    sa fermeture) ;
  - le fabricant de terrain entre les lieux ;
  - « dans quel lieu suis-je ? » d'après la position (pour les boutiques, tableaux, événements, auberge de réveil) ;
  - la sauvegarde en coordonnées du monde, avec reprise des anciennes ;
  - des monstres attachés à leur coin de terrain (aujourd'hui les bords de la zone les retiennent) ;
  - le joueur affiché sur la carte.
- **Risques** :
  - *Affichage* : chaque pièce de décor est dessinée séparément, deux fois quand les ombres sont allumées, et rien
    n'est écarté quand c'est hors champ. Avec 25 carreaux chargés, on passerait d'environ 300 pièces (la forêt) à
    2 000 ou plus (estimation, à mesurer). Parade : fusionner le décor fixe de chaque carreau en un seul objet (LibGDX le fournit :
    `ModelCache`) et ne pas dessiner les carreaux hors champ.
  - *Physique* : de l'ordre de 1 000 corps solides au lieu de 90 (estimation). Bullet sait le faire ; le danger est l'à-coup au moment
    où un carreau se crée. Parade : ne rendre solides que les carreaux proches et étaler la création sur plusieurs
    images.
  - *Mémoire* : c'est exactement le genre de fuite corrigé au chantier A (corps jamais libérés). La vérification
    du banc (« corps Bullet vivants ») servira de preuve après un aller-retour.
  - *Sauvegarde* : un changement de format, donc une reprise des anciennes parties à tester.
- **Coût** : le plus élevé. Un bloc pour le prototype, puis trois à quatre blocs pour tout convertir (les cinq lieux,
  les embuscades, la carte, les tests).

### B. Scènes enchaînées, sans carte

On garde une scène par lieu, comme aujourd'hui, mais elles se touchent : sortir par un bord charge la scène voisine.
Entre deux lieux, la route devient une suite de tronçons fabriqués (le « bout de route » des embuscades existe déjà).
La carte ne sert plus à voyager.

- **Se réutilise** : presque tout, y compris `TownScreen` et `ZoneScreen` ; la sauvegarde change à peine (un lieu ou
  un tronçon, plus la position dedans).
- **À écrire** : des sorties sur les bords à la place des haies et de la porte, le fabricant de tronçons, la position
  « sur telle route, tronçon n » à sauvegarder.
- **Risques** : faibles. Mais ce n'est pas un monde ouvert : une coupure à chaque bord, on ne voit pas la ville au
  loin, on ne peut pas quitter la route.
- **Coût** : un bloc.

### C. La carte actuelle, mais on fait le trajet à pied

La carte reste le point de départ. Choisir une route ne téléporte plus : on se retrouve sur un chemin à parcourir,
dont la longueur dépend de la distance, et l'arrivée est au bout. Les embuscades s'y jouent. Un trajet déjà fait à
pied peut être refait d'un clic.

- **Se réutilise** : tout, y compris la carte et le mécanisme du voyage interrompu par une embuscade.
- **À écrire** : le fabricant de chemin, l'arrivée au bout, le demi-tour, la sauvegarde en cours de route (interdite
  aujourd'hui pendant une embuscade).
- **Risques** : les plus faibles. C'est la version la moins « ouverte » : un couloir par route.
- **Coût** : un demi-bloc à un bloc.

### En résumé

| | A. Terrain continu | B. Scènes enchaînées | C. Carte + trajet à pied |
| --- | --- | --- | --- |
| On marche d'une ville à l'autre sans coupure | oui | non (une coupure par bord) | non (carte, puis couloir) |
| On peut quitter la route, explorer | oui | non | non |
| On voit les lieux de loin | oui, jusqu'à 300 m | non | non |
| Code existant conservé | les règles et les intérieurs ; l'extérieur est refait | presque tout | tout |
| Risque technique | élevé (affichage, à-coups, mémoire) | faible | très faible |
| Coût | 1 bloc de prototype + 3 à 4 blocs | 1 bloc | ½ à 1 bloc |

## 4. Recommandation

**L'approche A, à l'échelle 1:10, sur terrain plat, en commençant par un prototype qui peut dire non.**

- C'est la seule qui donne ce que tu as demandé : un monde qu'on traverse à pied.
- L'échelle 1:10 enlève les deux gros obstacles (durée de marche, précision) et garde les durées actuelles.
- Le terrain reste plat pour l'instant : tout le jeu suppose un sol à hauteur fixe (décor, monstres, villageois,
  étincelles). Le relief est faisable plus tard (Bullet a ce qu'il faut) mais c'est un chantier à part.
- Les trois risques de A se mesurent sur 1,5 km de terrain. Si le prototype échoue sur l'affichage ou les à-coups,
  on se replie sur B : le fabricant de terrain et les coordonnées du monde écrits pour le prototype y resservent.

## 5. Réponses aux questions du plan

- **Échelle** : 1 km de carte = 100 m de terrain (§2). Le monde fait 6 km sur 3 km.
- **Le temps qui passe en marchant** : rien à écrire. L'horloge avance déjà en temps réel ; à 1:10 elle donne les
  mêmes durées que la carte.
- **Position sauvegardée** : en mètres dans le monde. Le « lieu courant » reste enregistré, déduit de la position. Une
  ancienne sauvegarde (lieu + position dans le lieu) se convertit en ajoutant la position du lieu ; les champs
  nouveaux sont optionnels, comme pour les blocs précédents.
- **Coffres** : rien ne change. Ils sont déjà sauvegardés par lieu et numéro, avec leur délai de remplissage.
- **Monstres** : aujourd'hui ils reviennent tous dès qu'on rentre dans la zone. En monde ouvert ils reviendraient
  dans le dos du joueur. Proposition : un monstre tué revient après quelques jours de jeu, comme un coffre.
- **Butin et objets au sol** : aujourd'hui en mémoire, perdus à la fermeture du jeu (point 2 de `RELECTURE.md`). En
  monde ouvert on s'éloigne et on revient : à sauvegarder, avec une durée de vie pour ne pas grossir sans fin.
- **PNJ** : les villageois suivent leur parcours tant que leur ville est chargée ; les PNJ des bâtiments ne changent
  pas, les intérieurs restent des écrans à part.
- **Embuscades** : plus de scène à part. Des monstres apparaissent près de la route, avec la même chance par
  difficulté qu'aujourd'hui.
- **Fabrication du terrain** : les cinq lieux gardent leurs fichiers faits à la main. Entre eux, chaque carreau est
  fabriqué à la demande à partir de sa position (toujours le même au même endroit) : herbe, arbres, et la route
  tracée le long des liaisons de `world.json`. Aucun fichier à écrire pour les 17 km² de campagne.
- **Rôle de la carte ensuite** : elle montre où est le joueur, et sert de voyage rapide vers les lieux déjà atteints
  à pied (le temps passe comme aujourd'hui).

## 6. Plan du prototype : Aethelgard → Forêt des Loups

Sur une branche à part. On y entre par un choix en plus à la porte d'Aethelgard (« Partir à pied vers la forêt ») :
le reste du jeu n'est pas touché et continue de passer par la carte.

1. **Coordonnées du monde** (règles, sans affichage, testées) : position d'un lieu en mètres, passage position
   locale ↔ position du monde, lieu courant d'après la position.
2. **L'écran du monde** : carreaux de 128 m, chargés autour du joueur et libérés derrière lui ; Aethelgard (murs et
   porte gardés) et la forêt (sans ses haies) posées à 1,5 km l'une de l'autre.
3. **La campagne entre les deux** : herbe, arbres semés, route.
4. **Monstres attachés à leur coin**, qui reviennent après quelques jours ; coffres inchangés.
5. **Sauvegarde** en coordonnées du monde, et reprise d'une ancienne sauvegarde.
6. **La carte** affiche le joueur.

**Ce que le prototype doit prouver** (le banc de test fait l'aller-retour à pied) :

| Vérification | Seuil |
| --- | --- |
| Aucune fuite : corps Bullet vivants au retour = au départ | égalité stricte |
| Fluidité, ombres allumées, sur ton PC | 60 images par seconde en moyenne, aucune image de plus de 50 ms au changement de carreau |
| Mémoire après l'aller-retour | revenue à 10 % près de celle du départ |
| Temps de jeu écoulé sur l'aller | 3 h, à 15 minutes près |
| Sauvegarder au milieu de la route puis recharger | même position, même heure |
| Tout le reste du jeu | les 191 tests et le parcours complet toujours verts |

**Hors prototype** : le relief, les autres lieux, les embuscades sur la route, la suppression de la carte.

**Coût** : un bloc, de la taille de celui des zones sauvages (7b).

**Deux préalables**, déjà listés dans `RELECTURE.md` : un hasard reproductible (point 4), sans quoi le terrain
fabriqué et les tests ne sont pas comparables d'une fois à l'autre ; et les données gardées en mémoire au lieu
d'être relues à chaque appel (point 3), sinon chaque carreau relit les fichiers.

## 7. Questions pour toi

Les défauts proposés sont ceux du document ; « ok à tout » les valide.

1. **Quelle approche ?** *Défaut : A (terrain continu), avec repli sur B si le prototype échoue.*
2. **Quelle échelle ?** *Défaut : 1 km de carte = 100 m de terrain (3 minutes d'Aethelgard à la forêt).* Plus grand
   rallonge les trajets et désaccorde l'horloge.
3. **Terrain plat pour commencer ?** *Défaut : oui, le relief plus tard.*
4. **Le voyage rapide par la carte reste-t-il ?** *Défaut : oui, seulement vers les lieux déjà atteints à pied.*
5. **Les monstres tués reviennent quand ?** *Défaut : après 3 jours de jeu.*
6. **Le butin laissé au sol est-il sauvegardé ?** *Défaut : oui, 7 jours de jeu.*
7. **Les villes gardent-elles leurs murs et leur porte ?** *Défaut : oui, on n'entre que par la porte.*
8. **Quand faire le prototype ?** *Défaut : après l'histoire (chantier B).* Les lieux de l'histoire (mine, château,
   épreuves) s'écrivent dans le format actuel et se poseront dans le monde ouvert comme les autres. Le faire avant
   repousse l'histoire d'un bloc.
