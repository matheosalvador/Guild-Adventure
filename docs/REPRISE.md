# Reprise du travail

Tenu à jour à chaque fin d'étape (voir `docs/PLAN-FABLE.md`, §9). Dernière mise à jour : 2026-11-19.

## Les branches

Les chantiers A, D, B et C sont **fusionnés dans `general_develop`** (local, `54446f9`, en avance rapide, demande de
Lucas du 2026-11-16). Rien n'est poussé : `origin/general_develop` est resté à la fin du chantier A (`2a8b366`).

```text
general_develop (54446f9)   A + D + B + C
 └─ feature/java21               chantier E : rapport docs/COMPATIBILITE-JAVA.md, section perf du banc
     └─ feature/monde-ouvert     prototype du monde ouvert (Aethelgard -> Forêt des Loups à pied)
```

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

## En attente de Lucas

1. Ses tests à la main du chantier A (l'escalier de l'auberge), de l'histoire et du chantier C (parler aux
   habitants, Options, plein écran : le banc ne le clique pas, la fenêtre de test est cachée).
2. Les **chiffres des combats** : ils sont ceux du cahier (§9), **pas encore réglés par un vrai combat simulé**.
   Le test de l'histoire vérifie les phases, les renforts, le soin et la reddition, pas la difficulté.
3. Les autres points de `RELECTURE.md` §4 (3 et 5 à 11), toujours sans réponse.
4. Son accord pour les fusions (dans l'ordre des branches ci-dessus).

## Ensuite (ne pas commencer sans l'accord de Lucas)

Tous les chantiers du plan (A à E) et le prototype du monde ouvert sont faits. Selon l'avis de Lucas après l'avoir
essayé (`.\run.bat`, Nouvelle partie, panneau « Partir à pied » près de la porte) : étendre le monde ouvert aux
autres villes et zones, ou le laisser en l'état.

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
