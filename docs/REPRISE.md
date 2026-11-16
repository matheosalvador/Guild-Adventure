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

## Ensuite (ne pas commencer sans l'accord de Lucas)

Ordre du plan : `docs/HISTOIRE.md` (le cahier d'histoire, à lui faire valider) ; puis B (réalisation de l'histoire) ;
puis C (confort de jeu) ; E (Java 21) quand il autorise le téléchargement d'un JDK.

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
