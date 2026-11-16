# Reprise du travail

Tenu à jour à chaque fin d'étape (voir `docs/PLAN-FABLE.md`, §9). Dernière mise à jour : 2026-11-16.

## Où on en est

**Chantier A (relecture, correction, nettoyage) : en cours**, sur la branche `feature/relecture` (partie de
`general_develop` `dcffeeb`). Rien n'est fusionné ni poussé.

Fait et commité :
- `docs/` est maintenant suivi par git (plan, outils de test).
- Relecture de tout le code livré (`git diff main..general_develop`) et des fichiers voisins.
- Bugs corrigés, chacun avec sa preuve (voir `docs/RELECTURE.md` quand il sera écrit) :
  corps Bullet jamais libérés, souris capturée menu ouvert (I/J/K par-dessus un autre menu), caméra 3e personne
  dans un coin, saut répété en l'air (Espace maintenu), escalier de l'auberge, preuve de livraison perdue en
  surcharge, preuves de combat en double.
- Banc visuel : section `relectureSection`, option `-Only <section>`.

En cours / reste à faire dans le chantier A :
1. Code mort : `archive/`, `model/Town`, `model/Building` (encore utilisé par `TownScreen` : à replier),
   `libgdx/GameObject` + `PlayerController.interact()` + `Adventurer.findItem`, imports inutiles.
2. Découper `BaseWorldScreen` en collaborateurs (PNJ, objets au sol, ciel et ombres, panneaux), sans changement
   visible : comparer les captures avec `target\smoke\ref2` (référence prise après les corrections).
3. Écrire `docs/RELECTURE.md` (bugs corrigés + preuves, choix de conception laissés à Lucas).
4. Parcours complet + `mvn test`, mise à jour de la mémoire et du plan, rapport à Lucas. **S'arrêter là.**

## Vérifier l'état

```powershell
git status; git log --oneline -8
$env:JAVA_HOME='C:\Tools\java\jdk-23.0.1+11'; & "C:\Users\lucas\.m2\wrapper\dists\apache-maven-3.6.3-bin\1iopthnavndlasol9gbrbg6bf2\apache-maven-3.6.3\bin\mvn.cmd" -q -B test
.\docs\outils-test\run-smoke.ps1 -Label essai                  # parcours complet (plusieurs minutes)
.\docs\outils-test\run-smoke.ps1 -Label essai -Only relecture  # une seule section (rapide)
```

Comparer deux séries de captures (après un parcours, les classes de l'outil sont dans `target\smoke\<label>\tools`) :

```powershell
java -cp target\smoke\essai\tools Compare target\smoke\ref2\shots target\smoke\essai\shots
```

## Ensuite (ne pas commencer sans l'accord de Lucas)

Ordre du plan : les deux documents (`docs/HISTOIRE.md`, `docs/CADRAGE-MONDE-OUVERT.md`), puis B, puis C ; E quand
Lucas autorise le téléchargement d'un JDK 21.
