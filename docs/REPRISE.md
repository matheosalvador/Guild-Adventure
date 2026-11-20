# Reprise du travail

Tenu à jour à chaque fin d'étape (voir `docs/PLAN-FABLE.md`, §9). Dernière mise à jour : 2026-11-20.

## Les branches

Tout jusqu'au prototype du monde ouvert est **fusionné dans `general_develop` et poussé** (`9e6b2a0`) ; Lucas a
ouvert la PR vers `main` le 2026-11-19.

```text
general_develop (9e6b2a0)   A à E + prototype du monde ouvert + correctif d'encodage de compile_check.bat
 └─ feature/monde-ouvert-complet   tout l'extérieur en monde ouvert, carte sans position
     └─ feature/butin-sol              le butin au sol sauvegardé (7 jours)
         └─ feature/finitions-visuelles    sol sans dents de scie, jonction route / chemin des zones
             └─ feature/menage-ancien-chemin   TownScreen, carte classique et scène d'embuscade retirés
                 └─ feature/monde-rempli          sites, choses à ramasser, voyageurs, bandit
                     └─ feature/reglage-combats       simulation des combats, chefs renforcés (à valider)
                         └─ feature/relief                collines en pleine campagne
```

Aucune de ces branches n'est fusionnée ni poussée (demandes de Lucas du 2026-11-20 : « 1 remplir le monde, 2
sauvegarder le butin au sol, 3 oui, 4 oui, 5 ok et 6 oui »).

## Où on en est

**Chantier A** : terminé. **Chantier D** : document écrit, Lucas a répondu « oui à tout » (prototype après
l'histoire). **Chantier B** : cahier validé (« ok à tout »), **réalisation terminée** sur `feature/histoire`.

Ce que contient `feature/histoire` :

- les deux préalables de `RELECTURE.md` §4, avec leur défaut : plus de bouton « Jeter » sur les objets de quête ;
  une source de hasard unique (`game/GameRandom`) que le banc de test fixe ;
- le moteur : `assets/data/story.json` + `data/StoryData` + `game/Story` (drapeaux sauvegardés, conditions,
  chapitres, rangs F à B, dialogues d'histoire chez les PNJ et aux points du décor, objets à ramasser,
  déclencheurs, règles, fins, avantages des sceaux, effets des fins), lieux verrouillés sur la carte avec la raison,
  étages cachés, portes entre zones, arène (`game/Arena`), adversaires à phases et qui se rendent, écran de fin ;
- le contenu : mine (2 zones), Fosse des Débardeurs, cairn dans la forêt, chef gobelin dans les ruines, château de
  Corvenne (3 zones), 8 monstres et leurs modèles, 7 quêtes de rang D à B, 6 rumeurs et 3 textes qui suivent
  l'histoire. Les zones sont fabriquées par `docs/outils-test/gen_story_zones.py` ;
- nettoyage demandé par le plan : `QUESTS_PER_RANK`, `experienceReward` et `ESCORT` retirés ;
- tests : `StoryPlaythroughTest` joue toute l'histoire sans interface (épreuves réussies + Sablier brisé ; épreuves
  ratées + retentative + Sablier gardé), `StoryContentErrorsTest` (56 cas d'erreur de données) ;
  section `storySection` du banc visuel (33 vérifications, 51 captures) ; debug F10 « Histoire » : finir le
  chapitre, rang suivant.

**Chantier C** : terminé sur `feature/confort` (empilée sur `feature/histoire` : les PNJ y proposent les
dialogues d'histoire) :

- `assets/data/npcs.json` + `data/NpcsData` + `game/NpcTalk` : l'accueil et les choix des quatre PNJ (mêmes textes,
  prix et comportements qu'avant : repas, bière, rumeurs, nouvelles, livraisons, boutiques, quêtes), plus un choix
  « talk » de démonstration chez le maître de guilde ; un seul constructeur de dialogue dans `InteriorScreen` ;
- les habitants : `talk` dans `town*.json` → leurs répliques dans `npcs.json` (par ville, selon l'histoire) ; ils
  s'arrêtent et se tournent vers le joueur à moins de 3 m, E ouvre leur réplique (« Autre chose ? », « Au revoir ») ;
- menu **Options** (menu principal, et pause > Options) : sensibilité de la souris, vue au lancement, ombres, plein
  écran ; `game/Settings` lit et écrit `settings.json` à côté de `save.json` (absent ou cassé : réglages par défaut) ;
- barre de vie en bas à gauche du HUD (remplace la ligne « HP »), qui pulse sous 25 %.
- tests : `NpcsContentErrorsTest` (24 cas), `NpcTalkTest`, `SettingsTest` ; section `confortSection` du banc
  (23 vérifications, 12 captures, dont la couleur de la barre lue à l'écran).

État vérifié : **293 tests JUnit verts** ; parcours complet `FIN: AUCUNE ERREUR`, **257 captures**
(référence : `target\smoke\java23` ; ne jamais lancer `mvn clean` pendant un parcours : il vide `target/`).

**Chantier E** : terminé sur `feature/java21`. Temurin 21.0.13+9 installé dans `C:\Tools\java\jdk-21.0.13+9` (PATH
inchangé). Avec Java 21 : 293 tests verts, parcours complet sans erreur, 257 captures identiques au pixel à celles de
Java 23, même vitesse (ombres : environ -45 % d'images par seconde). Aucune correction nécessaire. Détail et mesures :
`docs/COMPATIBILITE-JAVA.md` ; mesure : `run-smoke.ps1 -Label perf -Only perf`.

**Prototype du monde ouvert** : fait sur `feature/monde-ouvert` (suite du cadrage `CADRAGE-MONDE-OUVERT.md`) :

- `assets/data/openworld.json` + `data/OpenWorldData` + `game/WorldLayout` (positions de `world.json` × 100 : les
  durées de trajet de la carte restent vraies à pied) + `game/Terrain` (arbres semés par carreau, toujours les
  mêmes) ; `libgdx/OpenWorldScreen` : un seul sol solide, carreaux de 128 m faits et défaits autour du joueur,
  troncs solides seulement tout près, Aethelgard et la Forêt des Loups posées à leur place et construites par petits
  travaux (budget de 4 ms par image), route et sol en morceaux de 32 m (sinon la brume les noie) ;
- on y entre par le panneau « Partir à pied » près de la porte d'Aethelgard (action `walk` de `town.json`) ; le
  reste du jeu passe toujours par la carte. M ouvre la carte (le joueur y est un point rouge), F5/F9 gardent la
  position (`currentScreen` = `World`), les maisons ramènent dans le monde ouvert ;
- `game/Respawns` : un monstre tué revient après `monsterRespawnDays` (3) jours, noté dans la sauvegarde ;
- performance : `GameContent` garde les villes, zones, modèles et monstres déjà lus et vérifiés (avant, chaque
  monstre qui apparaissait relisait `monsters.json` et `items.json`) ; les formes des personnages sont partagées
  (`CharacterModel.disposeShared()` à la fermeture) et celles des monstres du monde ouvert préparées au chargement ;
- tests : `OpenWorldTest`, `OpenWorldContentErrorsTest` ; section `mondeSection` du banc (25 vérifications,
  7 captures : aller et retour à pied, 3 h de jeu, plus de 60 images/s ombres allumées, aucune image de construction
  au-dessus de 50 ms, loups qui reviennent après 3 jours, F5/F9, auberge, aucune fuite Bullet ni de mémoire).

État vérifié (2026-11-19) : **315 tests JUnit verts** ; parcours complet `FIN: AUCUNE ERREUR`, **264 captures** ;
comparées à `target\smoke\java23` : identiques au pixel sauf 3 vues de la ville où l'on voit le nouveau panneau
« Partir à pied ». Mesures d'une marche : environ 125 à 135 images/s, pire image de construction 29 ms à l'aller
(premier loup compris), 23 ms au retour. Restent quelques images lentes (40 à 55 ms) **sans aucune construction**,
qui changent d'un parcours à l'autre (machine ou pilote ?) : à surveiller.

Défauts vus sur les captures, pas corrigés : petits points sombres sur l'herbe (ombres) ; une marche visible là où
la route du monde rejoint le chemin de la forêt.

**Monde ouvert complet** : fait sur `feature/monde-ouvert-complet`. Le monde ouvert est maintenant **le jeu** :

- une partie neuve commence dans le monde ouvert, devant la porte de la ville de départ ; « Continuer » et F9 y
  reprennent (une ancienne sauvegarde dans une ville ou une zone est convertie en position du monde) ; le panneau
  « Partir à pied » et l'action `walk` sont retirés ;
- les 3 villes et les 2 zones sauvages sont posées dans le monde (villes resserrées à 70 m de demi-côté : rien
  n'existe hors de leurs murs), avec leurs habitants qui parlent (code partagé avec `TownScreen` : `Villagers`),
  leur tableau d'annonces, leurs panneaux, le marché du jour annoncé en entrant ; les monstres de chaque zone sont
  retenus chez eux (bornes par monstre dans `CombatScene`) et retirés seuls quand leur zone est défaite ;
- les lieux fermés de l'histoire (mine, fosse, château) sont des **entrées** (`portals` de `openworld.json` : un
  petit décor et une porte, action `enter`) : on y entre comme dans une maison (lieu fermé : la raison s'affiche),
  on en ressort devant la porte ; les étages cachés ressortent par l'entrée de leur lieu ; l'histoire, les arènes
  et les portes de ces lieux sont inchangées (`ZoneScreen`) ;
- les **routes sont tracées par le jeu** (`WorldLayout`), une par liaison de `world.json` : de la porte d'un lieu,
  autour des murs par les coins, jusqu'à la porte de l'autre ; `roads` (points `via`) ne sert qu'à corriger un tracé
  (aucun n'en a besoin aujourd'hui). À pied, les trajets durent 5 à 10 % de plus que sur la carte (le détour par
  les portes). Les morceaux de route sont fusionnés avec leur carreau (sinon ~800 morceaux dessinés à chaque image :
  les images/s tombaient de moitié) ;
- la carte (M) : **elle ne montre pas où l'on est** (demande de Lucas : ni point, ni lieu ni routes mis en avant,
  pas de mini-carte ; seuls les lieux déjà atteints ont leur nom) ; **voyage rapide** vers les lieux déjà atteints seulement, durée
  comptée depuis là où l'on est (`World.fastTravelRoutes`) ;
- **embuscades sur la route** : en chemin, tous les 1500 m, une chance selon la difficulté (`ambushPercent`, et les
  effets de l'histoire) ; 1 ou 2 monstres de `world.json` (« ambush ») surgissent devant ; pas de F5 tant qu'ils
  sont là ; laissés à plus de 200 m, ils abandonnent. Le voyage rapide n'a pas d'embuscade ;
- tests : `OpenWorldTest` (une route par liaison, aucune ne traverse un lieu, durées, voyage rapide, étages
  cachés), `OpenWorldContentErrorsTest` ; section `mondeSection` du banc refaite (39 vérifications, 12 captures :
  partie neuve, aller-retour à pied par la route tracée, Port-Brume et son tableau, la fosse aller-retour, la mine
  fermée, voyage rapide, embuscade forcée, aucune fuite).

**La suite du 2026-11-20** (une branche par point, dans l'ordre ci-dessus) :

- **Carte** : elle ne montre plus où l'on est (ni point, ni lieu ni routes mis en avant, voyage rapide sans distance),
  pas de mini-carte (demande de Lucas).
- **Butin au sol** : rangé dans le personnage (`model/GroundDrop`, `Adventurer.groundItems`), donc sauvegardé ; il
  disparaît au bout de 7 jours de jeu (`GroundItems.LIFETIME_DAYS`).
- **Finitions** : le sol du monde ouvert est fait de surfaces plates (les côtés des boîtes se disputaient le dessus
  des voisines : des dents de scie au loin) ; la route s'arrête net à l'entrée des zones, à la hauteur de leur chemin.
- **Ménage** : `TownScreen`, la carte classique (`World.routesFrom/travel`), la scène d'embuscade (`zone_route.json`,
  `ZoneScreen.ambush`, le voyage coupé) et le drapeau « dans le monde ouvert » sont retirés : tout passe par le monde
  ouvert. `TownBuilding` garde l'ouverture des intérieurs ; le tirage d'embuscade est `World.rollRoadAmbush`. Les
  tests marchent d'un lieu à l'autre avec `Trips` (test) et le banc avec `walkTo`, `town(...)`, `place(...)`.
  `ZoneScreen` sert aux lieux fermés et au banc (il ouvre une zone seule, avec ses coordonnées de fichier).
- **Monde rempli** (`openworld.json`) : six sites près des routes (camp de bandits, tour de guet de la garnison,
  ferme et ses fermiers, stèle des voyageurs, campement gobelin, cabane du bûcheron), de l'herbe de soin et du minerai
  à ramasser (semés par carreau, ils repoussent ; `game/Gathering`, `Adventurer.gathered`), trois voyageurs qui font
  la route et parlent (`Villagers.travelers` : leur place dépend de l'heure). Nouveau monstre : le bandit. Les
  constructions sont découpées en petits travaux (une partie de bâtiment, un habitant, ce qu'on ramasse).
- **Combats** : `CombatBalanceTest` simule chaque combat de l'histoire avec les vraies règles ; chefs renforcés
  (Holt 300 PV, Varn 460 PV et 25 de dégâts, Aldric 760 PV). Compte rendu **à valider par Lucas** :
  `docs/REGLAGE-COMBATS.md`.
- **Relief** : collines douces en pleine campagne (`Terrain.height`, 7 m au plus, pentes sous 20°), plates sur les
  routes et dans les lieux ; sol en grille de hauteurs (dessinée, et solide près du joueur :
  `btHeightfieldTerrainShape`) ; arbres, choses à ramasser, monstres et objets au sol suivent le sol.
- **Banc** : sections `rempli` (sites, cueillette, voyageurs) et `relief` (tenir debout et monter une pente). La
  vérification de fluidité porte sur le temps des constructions (40 ms au plus) : pendant ces travaux, un autre jeu
  tournait sur la machine de Lucas et faussait les images par seconde.

État vérifié (2026-11-20, sur `feature/relief`) : **336 tests JUnit verts** ; parcours complet `FIN: AUCUNE ERREUR`,
**273 captures**. Avant (fin du monde ouvert complet) : 323 tests, 269 captures, 247 vérifications. Comparées au parcours d'avant ce chantier : les différences sont les écrans qui ouvrent
maintenant le monde ouvert (F9, Continuer, création), le panneau retiré, et le hasard des combats et des événements
décalé (le monde ouvert tire aussi des nombres). Marche : environ 160 images/s, pire image de construction 15 à
29 ms.

Les décisions du cadrage du monde ouvert sont toutes réalisées.

## En attente de Lucas

1. Ses tests à la main du chantier A (l'escalier de l'auberge), de l'histoire et du chantier C (parler aux
   habitants, Options, plein écran : le banc ne le clique pas, la fenêtre de test est cachée).
2. Les **chiffres des combats** : réglés par simulation, **à valider** (`docs/REGLAGE-COMBATS.md`, trois questions).
3. Les autres points de `RELECTURE.md` §4 (3 et 5 à 11), toujours sans réponse.
4. Son accord pour les fusions (dans l'ordre des branches ci-dessus).

## Ensuite (ne pas commencer sans l'accord de Lucas)

Les six points du 2026-11-20 sont faits. Selon l'avis de Lucas après avoir joué (`.\compile_check.bat` puis
`.\run.bat`, Nouvelle partie) : valider les combats (`docs/REGLAGE-COMBATS.md`), puis les fusions et une PR vers
`main` (brouillon à lui montrer). Idées notées, pas commencées : plus de sites et de voyageurs, des collines plus
hautes (constantes de `Terrain`), utiliser l'herbe de soin (fabriquer des potions ?).

## Vérifier l'état

```powershell
git status; git log --oneline -12
$env:JAVA_HOME='C:\Tools\java\jdk-23.0.1+11'; & "C:\Users\lucas\.m2\wrapper\dists\apache-maven-3.6.3-bin\1iopthnavndlasol9gbrbg6bf2\apache-maven-3.6.3\bin\mvn.cmd" -q -B test
.\docs\outils-test\run-smoke.ps1 -Label essai               # parcours complet (plusieurs minutes)
.\docs\outils-test\run-smoke.ps1 -Label essai -Only story   # une seule section (story, relecture...)
python docs\outils-test\gen_story_zones.py                  # refabriquer les zones de l'histoire
```

Comparer deux séries de captures (après un parcours, les classes de l'outil sont dans `target\smoke\<label>\tools`) :

```powershell
java -cp target\smoke\essai\tools Compare target\smoke\java23\shots target\smoke\essai\shots
```

Le hasard est fixé par le banc : deux parcours sur le même code donnent les mêmes captures. `target/` n'est pas suivi
par git : s'il a été vidé, refaire un parcours de référence avant de modifier le code.
