# Compatibilité Java 21

Chantier E du plan (`docs/PLAN-FABLE.md`), fait le 2026-11-16 sur la branche `feature/java21`.

## Conclusion

**Le jeu marche tel quel avec Java 21.** Aucune correction n'a été nécessaire :

- les tests passent ;
- les captures sont identiques à celles de Java 23 ;
- la vitesse est la même.

Le projet reste compilé pour Java 17 (`pom.xml`), qui tourne aussi sur 21.

## Le JDK installé

- **Version** : Eclipse Temurin **21.0.13+9 LTS**, téléchargé depuis l'API d'Adoptium. Son empreinte SHA-256 a été
  vérifiée avec celle que publie Adoptium.
- **Emplacement** : `C:\Tools\java\jdk-21.0.13+9`, à côté de `jdk-23.0.1+11`.
- **Ton système n'a pas changé** : le PATH ni `JAVA_HOME` n'ont été modifiés. Le Java du PATH reste le JDK 11.

Pour lancer le jeu ou Maven avec Java 21, dans une fenêtre PowerShell :

```powershell
$env:JAVA_HOME='C:\Tools\java\jdk-21.0.13+9'; $env:Path="$env:JAVA_HOME\bin;$env:Path"
.\compile_check.bat; .\run.bat
```

Ces deux lignes ne valent que pour cette fenêtre.

## Ce qui a été vérifié

| Vérification | Java 21 | Java 23 (référence) |
|---|---|---|
| `mvn clean test` | 293 tests, 0 échec, `BUILD SUCCESS` | 293 tests, 0 échec |
| Avertissements de compilation (`mvn compile`) | aucun | aucun |
| Parcours visuel complet (`run-smoke.ps1`) | `FIN: AUCUNE ERREUR`, 257 captures | `FIN: AUCUNE ERREUR`, 257 captures |
| Comparaison des captures (`Compare`) | **identiques au pixel, 257 sur 257** | |

Le parcours visuel est compilé par le `javac` du JDK testé, sans option de version. Le code est donc aussi compilé
au niveau de langage de Java 21, sans erreur.

## Images par seconde

Les mesures viennent de la section `perf` du banc de test :

```powershell
.\docs\outils-test\run-smoke.ps1 -Label perf -Only perf
```

Cette commande écrit les lignes `PERF` dans `target\smoke\perf\log.txt`.

Comment la mesure est faite :

- la fenêtre de 800 × 600 est cachée et la synchronisation verticale est coupée ;
- on prend 600 images après 60 images de mise en route ;
- on mesure en ville (Aethelgard, en regardant vers la grand-rue) et dans la Forêt des Loups ;
- chaque cas est mesuré deux fois, le tableau donne la moyenne.

Toutes les mesures sont faites sur le même PC.

| Scène | Java 21 | Java 23 |
|---|---|---|
| Ville, ombres | 119 images/s (116 et 122) | 120 images/s (112 et 128) |
| Ville, sans ombres | 225 images/s (203 et 246) | 229 images/s (224 et 233) |
| Forêt, ombres | 167 images/s (164 et 169) | 166 images/s (170 et 161) |
| Forêt, sans ombres | 291 images/s (288 et 293) | 284 images/s (294 et 273) |

Ce qu'il faut en retenir :

- **Java 21 et Java 23 vont aussi vite l'un que l'autre.** Les écarts sont plus petits que la différence entre deux
  essais de la même version.
- **Les ombres coûtent environ 45 % des images par seconde** : 225 → 119 en ville, 291 → 167 en forêt. Même avec les
  ombres, on reste à environ 120 images/s.
- Dans le vrai jeu, la synchronisation verticale est active : l'affichage est plafonné à la fréquence de l'écran
  (souvent 60 Hz). Sur ce PC, les ombres ne font donc pas baisser la fluidité.

## Ce qui n'a pas été testé

- **Le jeu dans une vraie fenêtre visible**, joué à la main, avec Java 21. Le banc tourne en fenêtre cachée.
- **Le passage de la cible de compilation à 21** dans `pom.xml`. Le plan demandait seulement de vérifier que le jeu
  tourne avec Java 21, et rien n'oblige à changer : le code n'utilise rien de propre à Java 21. Ce changement
  t'appartient s'il te sert un jour.
