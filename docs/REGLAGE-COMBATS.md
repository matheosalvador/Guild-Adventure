# Réglage des combats

Rédigé le 2026-11-20 pour Lucas, comme promis au §9 du cahier d'histoire : les chiffres des combats réglés par une
simulation, **montrés avant d'être figés**. Rien n'est fusionné tant que tu n'as pas dit oui.

## La méthode

Le test `src/test/java/com/lucas/guild/CombatBalanceTest.java` fait jouer chaque combat de l'histoire avec les vraies
règles du jeu (les classes `Monster` et `Combat`, un personnage réellement équipé). Le personnage :

- est équipé comme on peut l'être à ce moment de l'histoire (voir la colonne « Équipement ») ;
- reste **planté sur place** et frappe dès qu'il peut (toutes les 0,6 s). C'est le pire cas : en jeu, on peut reculer,
  tourner autour, attirer les monstres un par un ;
- boit une potion quand sa vie passe sous 35 %, ou quand deux coups des monstres présents pourraient le tuer
  (boire prend le temps d'un coup) ;
- enchaîne les groupes d'un lieu sans se reposer.

Les objectifs : les combats ordinaires se gagnent avec au plus deux potions ; les chefs en demandent de deux à cinq ;
Aldric, avec des sceaux fêlés, demande tout ce qu'on a. Le test échoue si un combat sort de ces objectifs : si
quelqu'un change un chiffre de `monsters.json`, il le saura.

## Le résultat

| Combat | Équipement | Issue | Vie restante | Potions bues / permises | Durée | Coups reçus | Objectif |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Cave : 3 rats ensemble | Départ : sans arme ni armure, 1 potion | gagné | 70 % | 0 / 1 | 3 s | 6 | oui |
| Forêt : 1 loup | Départ : sans arme ni armure, 1 potion | gagné | 92 % | 0 / 1 | 1 s | 1 | oui |
| Forêt : 3 paires de loups | Chapitre 1 : épée courte, armure de cuir, Mêlée 1, 3 potions | gagné | 46 % | 0 / 2 | 6 s | 9 | oui |
| Ruines : 2 + 3 gobelins | Chapitre 1 : épée courte, armure de cuir, Mêlée 1, 3 potions | gagné | 40 % | 0 / 2 | 8 s | 12 | oui |
| Ruines : le chef et ses 2 gobelins | Chapitre 1 : épée courte, armure de cuir, Mêlée 1, 3 potions | gagné | 38 % | 1 / 3 | 7 s | 15 | oui |
| Camp de bandits : 3 bandits | Chapitre 1 : épée courte, armure de cuir, Mêlée 1, 3 potions | gagné | 44 % | 0 / 2 | 5 s | 8 | oui |
| Campement gobelin : 3 gobelins | Chapitre 1 : épée courte, armure de cuir, Mêlée 1, 3 potions | gagné | 60 % | 0 / 2 | 5 s | 8 | oui |
| Mine : chauves-souris (3 + 3) puis squelettes (2 + 2) | Chapitres 2-3 : hache, cotte de mailles, Mêlée 2, Robustesse 2, 4 potions | gagné | 72 % | 0 / 2 | 9 s | 10 | oui |
| Mine : le sergent Holt | Chapitres 2-3 : hache, cotte de mailles, Mêlée 2, Robustesse 2, 4 potions | gagné | 57 % | 3 / 3 | 14 s | 19 | oui |
| Fosse : les trois vagues de l'arène | Chapitres 2-3 : hache, cotte de mailles, Mêlée 2, Robustesse 2, 4 potions | gagné | 57 % | 1 / 3 | 14 s | 37 | oui |
| Corvenne : les soldats de la cour (2 + 2 + 1) | Chapitre 4 : épée longue, plaques, Mêlée 3, Robustesse 3, 4 potions + 2 grandes | gagné | 77 % | 0 / 2 | 7 s | 9 | oui |
| Corvenne : le capitaine Varn | Chapitre 4 : épée longue, plaques, Mêlée 3, Robustesse 3, 4 potions + 2 grandes | gagné | 75 % | 2 / 4 | 11 s | 10 | oui |
| Corvenne : Aldric, sceaux intacts | Chapitre 4 : épée longue, plaques, Mêlée 3, Robustesse 3, 4 potions + 2 grandes | gagné | 52 % | 5 / 5 | 22 s | 36 | oui |
| Corvenne : Aldric, sceaux fêlés | Chapitre 4 : épée longue, plaques, Mêlée 3, Robustesse 3, 4 potions + 2 grandes | gagné | 30 % | 6 / 6 | 25 s | 41 | oui |

## Ce que j'ai changé (dans `monsters.json`)

Les combats ordinaires étaient déjà bien : on en sort avec 40 à 70 % de sa vie, rarement plus d'une potion. Seuls
les chefs étaient trop faciles :

| Chef | Avant | Après | Pourquoi |
| --- | --- | --- | --- |
| Sergent Holt | 220 PV | **300 PV** | battu en 10 s avec une seule potion ; le cahier en annonçait deux ou trois |
| Capitaine Varn | 320 PV, 20 de dégâts | **460 PV, 25 de dégâts** | battu en 7 s **sans aucune potion** : plus facile que Holt |
| Aldric | 600 PV | **760 PV** | 18 s seulement pour le dernier adversaire de l'histoire |

Avec ces chiffres : Holt demande trois potions, Varn deux, Aldric cinq avec les sceaux intacts et les six avec des
sceaux fêlés (il se soigne alors toutes les 12 s). Les sceaux comptent donc vraiment, comme le voulait le cahier.

## Ce qui reste à voir avec toi

1. **Ces trois réglages** te vont-ils ? Ou préfères-tu des chefs encore plus durs (ou moins) ?
2. Les combats sont courts : 5 à 25 s planté sur place. En jeu, on vise et on se déplace, c'est plus long. Si tu
   trouves qu'ils passent trop vite en jouant, on peut ralentir le rythme des coups du joueur (0,6 s aujourd'hui).
3. Le personnage simulé n'a pas de classe. Un guerrier (+20 PV) a plus de marge, un herboriste (-30 % en Mêlée) moins.

Pour relancer la simulation : `mvn test -Dtest=CombatBalanceTest`, puis lire `target/test-run/combats.txt`.
