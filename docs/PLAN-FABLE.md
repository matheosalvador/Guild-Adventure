# Guild Adventure : plan de reprise

Ce document explique où en est le projet, comment y travailler sans casser ce qui marche, et donne le travail
déjà cadré (cinq chantiers A à E : le résultat attendu et les contraintes, tu choisis le comment). Il est écrit pour un modèle qui reprend le travail (Fable) ; Lucas, le seul développeur actif,
parle français et valide chaque bloc.

---

## 1. Le projet en bref

Un RPG médiéval-fantaisie en 3D à la première ou troisième personne (touche V), en **Java, LibGDX 1.12.1
(LWJGL3), Bullet (physique), Gson, JUnit 5**. Cible de compilation : **Java 17** (`pom.xml`).

Ce qui existe : création de personnage (4 classes, 4 origines), 7 compétences qui progressent à l'usage, calendrier,
jour/nuit, saisons, vieillissement, inventaire au poids, boutiques, combat en mêlée en temps réel, mort et
pénalité, quêtes (combat, livraison, collecte) sur des tableaux par ville, 3 villes + 2 zones sauvages reliées par
une carte avec des trajets chronométrés, embuscades, butin au sol, coffres, lore (rumeurs, panneaux, journal J),
événements de marché, et un habillage 3D complet (corps articulés, PNJ, monstres, villageois, toits, ombres, ciel).

**Principe central : tout le contenu est dans des fichiers JSON** (`assets/data/`, documentés dans
`assets/data/README.txt`). Un fichier invalide donne un message en français qui nomme le fichier et le champ, et
chaque cas d'erreur a son test JUnit.

## 2. Règles de Lucas (à respecter strictement)

- **Commits SANS ligne `Co-Authored-By`** ni mention de l'IA, et sans fichiers sensibles (`save.json` est ignoré).
  Message : `feat : sujet court en ASCII sans accents` (ou `fix :`, `test :`, `chore :`).
- **Une branche par tâche.** **Ne jamais fusionner vers `main`, ni créer ou préparer de pull request** : la PR vers
  `main` est l'affaire de Lucas, après ses tests (il la fait lui-même sur GitHub). Tu ne touches pas à `main`.
  Les fusions locales dans `general_develop` ne se font que sur demande explicite de Lucas (« fusionne »,
  « met à jour general_develop »). Ne jamais pousser sans qu'il le demande. Ne jamais forcer un push.
- Ne pas supprimer de branche distante sans demande explicite ; ne pas toucher aux branches `(V1)` ni `legacy-V1` ;
  garder `feature/sauvegarde` et les branches `feature/*`.
- **Réponses en français, courtes et concrètes.** Lucas trouve que « trop cher pour rien » : pas de sous-agents ni
  d'orchestration sans accord, et le test visuel complet (plusieurs minutes) seulement en fin de bloc.
- Il fait ses tests à la main après un bloc ; il répond « ok à tout » quand un cadrage lui convient. Pour un gros
  bloc, **proposer un cadrage court avec des choix par défaut avant de coder**.

## 3. Travailler sur le projet

Dépôt : `C:\MAMP\htdocs\java\Guild-Adventure` (Windows, PowerShell ; le Bash du harnais existe aussi).

| Besoin | Commande |
|---|---|
| Tests JUnit (191) | `$env:JAVA_HOME='C:\Tools\java\jdk-23.0.1+11'; & "C:\Users\lucas\.m2\wrapper\dists\apache-maven-3.6.3-bin\1iopthnavndlasol9gbrbg6bf2\apache-maven-3.6.3\bin\mvn.cmd" -q -B test` (Maven n'est **pas** dans le PATH de Lucas) |
| Parcours complet du jeu (fenêtre cachée, ~194 captures) | `.\docs\outils-test\run-smoke.ps1 -Label essai` → doit finir par `FIN: AUCUNE ERREUR`, captures dans `target\smoke\essai\shots` |
| Une seule section du parcours (quelques secondes) | `.\docs\outils-test\run-smoke.ps1 -Label essai -Only relecture` (nom de la méthode sans `Section`) |
| Voir les modèles 3D (face, profil, marche, coup) | `.\docs\outils-test\run-viewer.ps1 -Label essai` |
| Lancer le jeu | `.\compile_check.bat` puis `.\run-debug.bat` (mode debug, F10) ou `.\run.bat` |

Détails utiles :
- Dans PowerShell, **`.\` est obligatoire** devant un `.bat` (même `cmd` ne cherche pas dans le dossier courant sur
  ce PC). Le Java du PATH de Lucas est le **JDK 11** (le code y compile) ; les JDK 23/25/26 sont aussi installés.
  Aucun JDK 21 : Lucas a demandé s'il pouvait en télécharger un (non fait).
- Les tests tournent dans `target/test-run` : le vrai `save.json` n'est jamais touché.
- **Toujours regarder les captures** (outil Read sur les PNG) : elles ont révélé des bugs que les tests de logique ne
  voient pas (messages jamais affichés, caméra dans un mur, boutons hors écran...).
- Le test visuel est `docs/outils-test/Smoke.java` : une méthode par bloc (`worldSection`, `wildSection`,
  `loreSection`, `design8dSection`...). **En ajouter une à chaque nouveau bloc** (aides : `step`, `frames`, `key`,
  `click("texte du bouton")`, `faceAt`, `shot`, `check`). Un bouton hors de l'écran n'est pas cliquable par le
  banc : appeler la logique directement (cf. `DebugActions`).
- Mémoire détaillée de mes sessions (historique, pièges) : `C:\Users\lucas\.claude\projects\c--MAMP-htdocs-java-Guild-Adventure\memory\`
  (lire `project-progress-and-testing.md`).

**Pièges rencontrés**
- Dans les heredocs du Bash du harnais, les apostrophes et les `\n` / `\r\n` cassent les scripts : écrire le fichier
  avec l'outil Write puis l'exécuter. Ne jamais écrire `cat > fichier` sans entrée (le harnais se bloque).
- `.gitignore` contient `*.json` sauf `!assets/data/*.json` : un nouveau JSON hors de ce dossier n'est pas suivi.
- Après une fusion git, les fichiers de données peuvent passer en fins de ligne CRLF ; les tests de « données cassées »
  normalisent en LF (`WorldContentErrorsTest`, `ContentErrorsTest`).
- `GameContent.quests()` recrée les objets `Quest` à chaque appel : comparer par titre, pas par `equals`.
- Ne jamais appeler `setScreen` pendant `update()` (crash natif) : voir `deathPending` dans `BaseWorldScreen`.
- Bullet : le joueur est dans le groupe de collision 32 (les rayons de visée l'ignorent) ; ne pas utiliser les
  groupes 1 et 2 (Default/Static).
- La police par défaut ne dessine pas les guillemets français « » : utiliser des guillemets droits dans les textes.
- Les intérieurs n'ont pas d'ombres portées (le plafond rendrait tout noir) : seulement `hasSky()` (ville, zones).
- Tout corps Bullet créé doit être libéré (`dispose()`), sinon LibGDX le garde pour toujours : le banc visuel affiche
  « ATTENTION : ... ressource(s) Bullet » quand il en reste.
- Un saut monte de 0,70 m : une marche ou un rebord à franchir fait 0,5 m au plus.
- Le hasard (butin, embuscades, événements) n'a pas de graine : une vingtaine de captures de combat et de butin
  diffèrent d'un parcours à l'autre sans que rien n'ait changé. Comparer les autres au pixel près.

## 4. Carte du code (`src/main/java/com/lucas/guild/`)

- `data/` : lecture et validation des JSON. `GameContent` est le point d'entrée (`validateAll()` au lancement).
  Classes de données : `WorldData`, `TownData`, `ZoneData`, `MonsterData`, `ModelsData`, `LoreData`, `EventsData`...
- `game/` : règles du jeu **sans dépendance LibGDX** (donc testables) : `GameSession`, `World` (trajets),
  `Combat`, `Monster`, `QuestLog`, `Shop`, `WorldEvents`, `Lore`, `Chests`, `GroundItems`, `RouteWalker`,
  `DeathPenalty`, `DebugMode`/`DebugActions`, `SaveManager`.
- `model/` : personnage (`Adventurer`, sauvegardé tel quel avec Gson), inventaire, objets, horloge, calendrier.
- `libgdx/` : écrans et affichage. `BaseWorldScreen` (930 lignes : boucle de jeu, caméra, physique Bullet, décor, HUD,
  touches) → `TownScreen`, `ZoneScreen`, `InteriorScreen` → `ForgeScreen`, `GuildScreen`, `InnScreen`,
  `StoreScreen`, `CellarScreen`. Ses collaborateurs : `WorldPanels` (menus), `NpcGroup`, `GroundItemViews`,
  `SkyAndShadows`, `Readable` (textes à lire), `WaterWaves`, `Toasts`, `CombatScene`. Aussi : `CharacterModel` (corps
  articulés d'après `models.json`), `Shapes`, `PlayerController`, `SkyDome`, `Sparks`, `QuestBoardPanel`,
  `JournalPanel`, `WorldMapScreen`.
- Tests : `src/test/java/com/lucas/guild/`.

## 5. État du dépôt

- `main` = PR #4 fusionnée (jusqu'au sous-bloc 7b : zones sauvages).
- `general_develop` (local, `dcffeeb`) contient **en plus** : 7c lore, 7d événements, 7e quêtes entre villes, 8a vue à
  la 3ᵉ personne, 8b PNJ et monstres, 8c décor, 8d finitions. `origin/general_develop` est resté au 7e.
  **Ces 7 commits ne sont pas dans `main` et ceux du bloc 8 ne sont pas poussés.** Ce n'est pas ton affaire : Lucas
  s'occupe de pousser et de la PR quand il l'a décidé.
- Toutes les branches `feature/*` sont des ancêtres de `general_develop` (chaîne linéaire), **sauf `feature/relecture`**
  (chantier A, terminé le 2026-11-16, 9 commits au-dessus de `general_develop`, ni fusionnée ni poussée, en attente
  des tests de Lucas). Son compte rendu : `docs/RELECTURE.md`.
- Tests sur `feature/relecture` : 191 JUnit verts ; parcours complet : 194 captures, aucune erreur.
- Lucas a testé à la main jusqu'à 7b (« rien à corriger »). **7c, 7d, 7e et tout le bloc 8 n'ont pas encore été testés
  par lui.**

## 6. Travail à faire

Cinq chantiers. Ils disent **le résultat attendu et les contraintes**, pas comment y arriver : tu choisis la
conception, tu découpes toi-même en branches et en commits. Ne pose une question à Lucas que pour les
« Décisions à lui soumettre ».

### A. Relecture, correction et nettoyage
- **Résultat** : le code livré (`git diff main..general_develop`, ~110 fichiers) est relu ; les vrais bugs sont corrigés,
  chacun avec un test ou une capture qui le prouve (rien de supposé) ; ce qui demande un choix de conception est
  listé dans `docs/RELECTURE.md` sans être corrigé.
- **À regarder en priorité** : sauvegarde et anciens fichiers, `GameClock` / événements / rattrapage de jours,
  voyage interrompu et `QuestLog`, arrondis de `Shop`, poids de l'inventaire, libération des ressources LibGDX
  (changements d'écran répétés), caméra à la 3ᵉ personne dans un coin.
- **Nettoyage** : découper `BaseWorldScreen` (~1 170 lignes) en collaborateurs (PNJ, objets au sol, ciel et ombres,
  panneaux), **sans changement visible** (mêmes tests, mêmes captures) ; supprimer le code mort confirmé
  (`model/Town`, `model/Building`, `archive/` : vérifier avant) et les imports inutiles.

### B. L'histoire, du début à la fin
- **Voulu par Lucas** : une vraie histoire de bout en bout : un début, des enquêtes puis des **épreuves**, un
  **château**, un adversaire final et au moins deux **fins** avec un épilogue ; le monde reste jouable après la fin.
- **Étape 1 : le cahier** (`docs/HISTOIRE.md`, en français, sans code), **à faire valider par Lucas avant de
  construire** : synopsis, héros, menace avec un retournement au milieu, personnages, chapitres (lieu, objectif,
  récompense, textes d'intro et de conclusion), lieux et monstres nouveaux, fins. Trame par défaut, adaptable :
  prologue (guilde, cave) → les ruines gobelines (la garnison disparue) → la mine fermée de Hautevigne (donjon, gardien,
  clé et lettre) → **trois épreuves** (Force : arène à vagues ; Sagesse : énigme qui s'appuie sur les panneaux, rumeurs
  et journal du joueur ; Endurance : trajet chronométré en portant une charge) donnant trois sceaux → **le château**
  (ouvert par les sceaux ; capitaine puis seigneur final en plusieurs phases, un choix avant le coup final) → fins et
  épilogue.
- **Étape 2 : la réalisation**, jouable de bout en bout. Ce que Lucas attend, à toi de concevoir le comment :
  - un moteur d'histoire piloté par les données (chapitres, conditions, drapeaux sauvegardés en champs optionnels,
    lieux verrouillés sur la carte avec la raison affichée, déclencheurs de zone simples, chapitre courant dans le
    journal, écran de fin) ;
  - la **progression** qu'il faut pour l'histoire : rangs pilotés par des données jusqu'à B (aujourd'hui constante
    `QuestLog.QUESTS_PER_RANK`, `experienceReward` inutilisé, type `ESCORT` jamais utilisé : à nettoyer), quêtes de
    rangs supérieurs avec une économie cohérente ;
  - le **contenu** : la mine (zone plus sombre, nouveaux monstres comme squelette et chauve-souris, boss), les trois
    épreuves (chacune réussissable **et ratable sans bloquer l'histoire**), le château (plusieurs zones ou intérieurs,
    construits avec les formes et variantes de `models.json`), le boss final, les fins, les rumeurs et panneaux qui
    suivent l'avancement ;
  - un **test automatique de logique** qui joue toute l'histoire sans interface, plus des sections de test visuel.
- **Décisions à lui soumettre** : le cahier (étape 1), l'énigme de la Sagesse (un exemple écrit), l'équilibrage des
  deux boss.

### C. Confort de jeu
- **Résultat** : les dialogues des PNJ ne sont plus écrits en dur dans les écrans mais dans un fichier de données
  (mêmes comportements qu'avant) et les villageois deviennent interpellables ; un menu **Options** (sensibilité de la
  souris, vue par défaut, ombres, plein écran) enregistré dans un `settings.json` hors des sauvegardes ; une barre de vie
  dessinée dans le HUD.
- **Contraintes** : un fichier de réglages absent ou illisible ne plante pas ; pas de remappage des touches.

### D. Cadrage du monde ouvert (un document, aucun code)
- **Résultat** : `docs/CADRAGE-MONDE-OUVERT.md` qui compare au moins trois approches (terrain continu par morceaux ;
  scènes enchaînées sans carte ; carte actuelle avec traversée à pied) en disant ce qui se réutilise du code existant,
  ce qu'il faut écrire, les risques (performances Bullet, mémoire, sauvegarde) et le coût, avec une recommandation et
  un plan de prototype (Aethelgard → Forêt des Loups). Il répond à : échelle (33 km entre villes), temps qui passe en
  marchant, position sauvegardée en coordonnées du monde, persistance des PNJ / coffres / butin, génération du terrain,
  rôle de la carte ensuite. Il se termine par les questions à poser à Lucas. **Ne rien coder avant sa réponse.**

### E. Java 21 (petit)
- **Dépend de** l'accord de Lucas pour télécharger un JDK. Télécharger Temurin 21 dans `C:\Tools\java\` (sans toucher à
  son PATH), lancer `mvn test` et le test visuel avec, mesurer les images par seconde avec et sans ombres, et écrire un
  court rapport `docs/COMPATIBILITE-JAVA.md`. Ne corriger que si quelque chose casse.

## 7. Pour chaque chantier

- Des branches dédiées depuis `general_develop`, des commits par sujet (règles du §2).
- Les données et leur validation d'abord : message en français nommant le fichier et le champ, documentation dans
  `assets/data/README.txt`, **un test JUnit par cas d'erreur** (style de `WorldContentErrorsTest`).
- La logique dans `game/` sans LibGDX et testée ; une section de test visuel par bloc, lancée **en fin de bloc**, dont tu
  **regardes les captures**.
- `mvn test` vert **et** `FIN: AUCUNE ERREUR` au test visuel ; anciennes sauvegardes toujours chargeables (champs
  ajoutés optionnels).
- Mémoire (`project-progress-and-testing.md`) et ce plan mis à jour ; rapport à Lucas en français (fait / à savoir /
  suite). **Il teste à la main et décide des fusions.**

## 8. Ordre

**A** d'abord. Ensuite les deux documents (**cahier d'histoire** et **cadrage du monde ouvert**) à faire lire à Lucas
pendant qu'il teste ; puis **B** (réalisation) ; puis **C**. **E** quand Lucas autorise le téléchargement. Tu ne
fusionnes rien toi-même.

## 9. S'arrêter et reprendre

Une limite (quota, contexte plein, session fermée) peut interrompre le travail à tout moment, sans prévenir. Pour ne rien perdre :
- **Commite souvent** sur ta branche, même un travail en cours (message `wip : ...` accepté sur une branche de travail), et
  ne laisse jamais de gros changements non commités.
- **Tiens `docs/REPRISE.md` à jour** : ce qui est fait, ce qui est en cours (fichiers touchés), la prochaine étape précise,
  les commandes pour vérifier l'état. Mets-le à jour à chaque fin d'étape, pas seulement à la fin.
- **Arrête-toi toi-même à la fin de chaque chantier** : rapport à Lucas, puis attends. Ne passe pas au chantier suivant
  sans son accord, ni aux fusions.
- Au début de chaque session : lis ce plan, `docs/REPRISE.md` et la mémoire, lance `git status` et `git log -5`.

