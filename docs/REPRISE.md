# Reprise du travail

Tenu à jour à chaque fin d'étape (voir `docs/PLAN-FABLE.md`, §9). Dernière mise à jour : 2026-11-16.

## Où on en est

**Chantier A (relecture, correction, nettoyage) : terminé**, sur la branche `feature/relecture` (9 commits au-dessus
de `general_develop` `dcffeeb`). Rien n'est fusionné ni poussé. Compte rendu complet : `docs/RELECTURE.md`.

- 7 bugs corrigés, chacun avec sa preuve (corps Bullet jamais libérés, souris capturée menu ouvert, caméra à la
  3e personne dans un coin, saut répété en l'air, escalier de l'auberge, preuve de livraison perdue en surcharge,
  preuves de combat en double).
- Code mort supprimé (`archive/`, `Town`, `Building`, `GameObject`...), imports inutiles retirés.
- `BaseWorldScreen` découpé en collaborateurs (1 178 → 929 lignes), 173 captures sur 194 identiques au pixel près,
  les 21 autres varient à cause du hasard.
- État vérifié : 191 tests JUnit verts ; parcours complet `FIN: AUCUNE ERREUR`, 194 captures.

**En attente de Lucas** :

1. ses tests à la main (surtout l'escalier de l'auberge, seule correction visible) ;
2. ses réponses aux 11 points du §4 de `docs/RELECTURE.md` (les deux qui comptent le plus pour la suite : objets de
   quête jetables, hasard sans graine) ;
3. son accord pour fusionner `feature/relecture` dans `general_develop`, puis pour commencer la suite.

**Chantier D (cadrage du monde ouvert) : document écrit** le 2026-11-16, `docs/CADRAGE-MONDE-OUVERT.md`, sur la
branche `feature/cadrage-monde-ouvert` (partie de `feature/relecture`, qui n'est pas encore fusionnée). Aucun code.
Il recommande le terrain continu à l'échelle 1 km de carte = 100 m, avec un prototype Aethelgard → Forêt des Loups,
et se termine par 8 questions. **Ne rien coder du monde ouvert avant les réponses de Lucas.**

Lucas a répondu « oui à tout » aux 8 questions le 2026-11-16 : les défauts du §7 sont les décisions retenues, et le
prototype du monde ouvert se fera **après** l'histoire.

**Chantier B, étape 1 (cahier d'histoire) : document écrit** le 2026-11-16, `docs/HISTOIRE.md`, sur la branche
`feature/histoire-cahier` (empilée sur `feature/cadrage-monde-ouvert`). Aucun code. Il attend la validation de Lucas
sur 8 points (§10) : l'histoire, les noms, l'énigme de la Sagesse, le sceau fêlé, la fin qui arrête le
vieillissement, les rangs, les chiffres des combats, pas de nouvel équipement. **Ne rien construire avant sa
réponse.**

## Ensuite (ne pas commencer sans l'accord de Lucas)

Ordre du plan : B étape 2 (réalisation de l'histoire, une fois le cahier validé) ; puis C (confort de jeu) ; puis le
prototype du monde ouvert ; E (Java 21) quand il autorise le téléchargement d'un JDK.

Pour la réalisation de B, proposer d'abord à Lucas un découpage en sous-blocs (moteur d'histoire et rangs ; mine ;
épreuves ; château et fins), chacun avec ses données, ses tests JUnit et sa section de test visuel. Deux points de
`docs/RELECTURE.md` §4 deviennent nécessaires à ce moment-là et n'ont pas encore de réponse : les objets de quête
jetables (point 1) et le hasard sans graine (point 4).

Les nouvelles branches partent de `general_develop` **une fois `feature/relecture` fusionnée** (sinon de
`feature/relecture`, en le disant à Lucas).

## Vérifier l'état

```powershell
git status; git log --oneline -10
$env:JAVA_HOME='C:\Tools\java\jdk-23.0.1+11'; & "C:\Users\lucas\.m2\wrapper\dists\apache-maven-3.6.3-bin\1iopthnavndlasol9gbrbg6bf2\apache-maven-3.6.3\bin\mvn.cmd" -q -B test
.\docs\outils-test\run-smoke.ps1 -Label essai                  # parcours complet (plusieurs minutes)
.\docs\outils-test\run-smoke.ps1 -Label essai -Only relecture  # une seule section (quelques secondes)
```

Comparer deux séries de captures (après un parcours, les classes de l'outil sont dans `target\smoke\<label>\tools`) :

```powershell
java -cp target\smoke\essai\tools Compare target\smoke\decoupe\shots target\smoke\essai\shots
```

`target\smoke\decoupe` est le dernier parcours de référence (fin du chantier A). `target/` n'est pas suivi par git :
s'il a été vidé, refaire un parcours de référence avant de modifier le code.
