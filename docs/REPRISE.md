# Reprise du travail

Tenu à jour à chaque fin d'étape (voir `docs/PLAN-FABLE.md`, §9). Dernière mise à jour : 2026-11-16.

## Les branches (aucune n'est fusionnée ni poussée)

Elles sont empilées, chacune part de la précédente :

```text
general_develop (dcffeeb)
 └─ feature/relecture              chantier A : relecture, 7 bugs corrigés, nettoyage (docs/RELECTURE.md)
     └─ feature/cadrage-monde-ouvert   chantier D : docs/CADRAGE-MONDE-OUVERT.md (décisions de Lucas notées)
         └─ feature/histoire-cahier        chantier B étape 1 : docs/HISTOIRE.md (validé par Lucas)
             └─ feature/histoire               chantier B étape 2 : l'histoire jouable
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

État vérifié : **250 tests JUnit verts** ; parcours complet `FIN: AUCUNE ERREUR`, **245 captures**
(référence : `target\smoke\histoire3`).

## En attente de Lucas

1. Ses tests à la main du chantier A (l'escalier de l'auberge) et de l'histoire.
2. Les **chiffres des combats** : ils sont ceux du cahier (§9), **pas encore réglés par un vrai combat simulé**.
   Le test de l'histoire vérifie les phases, les renforts, le soin et la reddition, pas la difficulté.
3. Les autres points de `RELECTURE.md` §4 (3 et 5 à 11), toujours sans réponse.
4. Son accord pour les fusions (dans l'ordre des branches ci-dessus).

## Ensuite (ne pas commencer sans l'accord de Lucas)

C (confort de jeu : dialogues en données, villageois interpellables, menu Options, barre de vie) ; puis le
prototype du monde ouvert ; E (Java 21) quand il autorise le téléchargement d'un JDK.

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
java -cp target\smoke\essai\tools Compare target\smoke\histoire3\shots target\smoke\essai\shots
```

Le hasard est fixé par le banc : deux parcours sur le même code donnent les mêmes captures. `target/` n'est pas suivi
par git : s'il a été vidé, refaire un parcours de référence avant de modifier le code.
