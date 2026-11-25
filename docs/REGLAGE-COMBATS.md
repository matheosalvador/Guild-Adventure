# Réglage des combats

Rédigé le 2026-11-20 pour Lucas, comme promis au §9 du cahier d'histoire : les chiffres des combats réglés par une
simulation, **montrés avant d'être figés**. Mis à jour le 2026-11-25 avec le combat vivant (branche
`feature/combat-vivant`). Les chefs renforcés le 2026-11-20 sont gardés tels quels : on les revoit une fois que tu
auras joué avec la garde et l'esquive (ta réponse du 2026-11-25).

## Le combat vivant

| Geste | Touche | Ce qu'il fait |
| --- | --- | --- |
| Frapper | clic gauche, **au relâchement** | un coup ; il coûte de l'endurance et son rythme dépend du poids de l'arme |
| Coup chargé | garder le clic 0,6 s, puis relâcher | dégâts multipliés (plus pour une arme lourde) ; un monstre ordinaire chancelle 0,8 s |
| Garde | clic droit tenu | on ne prend que le quart des dégâts, mais chaque coup bloqué coûte de l'endurance ; on avance deux fois moins vite et l'endurance ne remonte pas |
| Parade | lever la garde **juste avant** le coup (0,2 s) | aucun dégât, et le monstre chancelle 1,2 s (0,7 s pour un chef) |
| Esquive | Maj | un bond de 3,5 m (dans le sens des touches de marche, en arrière sinon), intouchable 0,3 s |

Les monstres **annoncent leurs coups** : ils s'arrêtent et virent à l'orange pendant 0,5 s, puis frappent. Le coup ne
touche que si l'on est encore à portée : reculer ou esquiver le fait partir dans le vide (« Esquivé » s'affiche). Le
rythme de leurs coups ne change pas (la préparation est comptée dedans). Un monstre qui chancelle vire au gris bleu.

L'endurance (barre jaune-vert sous la vie) : 100, plus 5 par niveau de la compétence Endurance (celle de la marche).
Elle remonte de 35 par seconde, 0,3 s après le dernier effort.

| Arme | Poids | Temps entre deux coups | Coût d'un coup | Coup chargé |
| --- | --- | --- | --- | --- |
| Mains nues | 0,5 kg | 0,42 s | 8 | x1,58 |
| Dague | 1 kg | 0,46 s | 10 | x1,65 |
| Épée courte | 2 kg | 0,54 s | 14 | x1,80 |
| Épée longue | 3 kg | 0,62 s | 18 | x1,95 |
| Hache de guerre | 3,5 kg | 0,66 s | 20 | x2,03 |

Avant, tous les coups partaient toutes les 0,6 s, quelle que soit l'arme. Un coup chargé coûte une fois et demie un coup
normal. Une esquive coûte 25 ; un coup bloqué, ses dégâts en endurance (un coup de Varn : 25).

Tous ces chiffres sont en tête de `src/main/java/com/lucas/guild/game/PlayerFight.java`. La préparation d'un monstre se
règle par monstre dans `monsters.json` (`"windUp"`, 0,5 s par défaut).

## La méthode

Le test `src/test/java/com/lucas/guild/CombatBalanceTest.java` fait jouer chaque combat de l'histoire avec les vraies
règles du jeu (`Monster`, `Combat`, `PlayerFight`, un personnage réellement équipé). Le personnage :

- est équipé comme on peut l'être à ce moment de l'histoire (voir la colonne « Équipement ») ;
- reste **planté sur place** et frappe dès que son arme et son endurance le permettent ;
- boit une potion quand sa vie passe sous 35 %, ou quand deux coups des monstres présents pourraient le tuer
  (boire prend le temps d'un coup) ;
- enchaîne les groupes d'un lieu sans se reposer.

Il joue chaque combat de deux façons :

- **planté** : il ne se protège jamais. C'est le pire cas, et c'est sur lui que les objectifs sont vérifiés ;
- **en garde** : il lève la garde quand le plus dangereux des monstres annonce un coup (un peu tôt : il bloque, il ne
  pare pas), puis la baisse. Il n'esquive jamais et ne recule jamais.

La simulation ne sait ni parer au bon moment, ni esquiver, ni reculer, ni attirer les monstres un par un : en jouant,
on fait mieux que ces deux joueurs.

Les objectifs : les combats ordinaires se gagnent avec au plus deux potions ; les chefs en demandent de deux à cinq ;
Aldric, avec des sceaux fêlés, demande tout ce qu'on a. Le test échoue si un combat sort de ces objectifs : si
quelqu'un change un chiffre de `monsters.json` ou de `PlayerFight`, il le saura.

## Le résultat

| Combat | Équipement | Planté : issue | Vie restante | Potions bues / permises | Durée | Coups reçus | Objectif | En garde : issue, vie, potions, durée |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Cave : 3 rats ensemble | Départ : sans arme ni armure, 1 potion | gagné | 85 % | 0 / 1 | 2 s | 3 | oui | gagné, 96 %, 0 potion(s), 2 s |
| Forêt : 1 loup | Départ : sans arme ni armure, 1 potion | gagné | 92 % | 0 / 1 | 1 s | 1 | oui | gagné, 98 %, 0 potion(s), 1 s |
| Forêt : 3 paires de loups | Chapitre 1 : épée courte, armure de cuir, Mêlée 1, 3 potions | gagné | 46 % | 0 / 2 | 6 s | 9 | oui | gagné, 65 %, 0 potion(s), 9 s |
| Ruines : 2 + 3 gobelins | Chapitre 1 : épée courte, armure de cuir, Mêlée 1, 3 potions | gagné | 45 % | 0 / 2 | 8 s | 11 | oui | gagné, 66 %, 0 potion(s), 11 s |
| Ruines : le chef et ses 2 gobelins | Chapitre 1 : épée courte, armure de cuir, Mêlée 1, 3 potions | gagné | 37 % | 0 / 3 | 6 s | 11 | oui | gagné, 60 %, 0 potion(s), 9 s |
| Camp de bandits : 3 bandits | Chapitre 1 : épée courte, armure de cuir, Mêlée 1, 3 potions | gagné | 51 % | 0 / 2 | 4 s | 7 | oui | gagné, 74 %, 0 potion(s), 7 s |
| Campement gobelin : 3 gobelins | Chapitre 1 : épée courte, armure de cuir, Mêlée 1, 3 potions | gagné | 65 % | 0 / 2 | 4 s | 7 | oui | gagné, 84 %, 0 potion(s), 6 s |
| Mine : chauves-souris (3 + 3) puis squelettes (2 + 2) | Chapitres 2-3 : hache, cotte de mailles, Mêlée 2, Robustesse 2, 4 potions | gagné | 50 % | 0 / 2 | 13 s | 17 | oui | gagné, 42 %, 1 potion(s), 22 s |
| Mine : le sergent Holt | Chapitres 2-3 : hache, cotte de mailles, Mêlée 2, Robustesse 2, 4 potions | gagné | 49 % | 3 / 3 | 15 s | 21 | oui | gagné, 25 %, 4 potion(s), 25 s |
| Fosse : les trois vagues de l'arène | Chapitres 2-3 : hache, cotte de mailles, Mêlée 2, Robustesse 2, 4 potions | gagné | 51 % | 2 / 3 | 19 s | 51 | oui | gagné, 42 %, 3 potion(s), 38 s |
| Corvenne : les soldats de la cour (2 + 2 + 1) | Chapitre 4 : épée longue, plaques, Mêlée 3, Robustesse 3, 4 potions + 2 grandes | gagné | 72 % | 0 / 2 | 9 s | 11 | oui | gagné, 79 %, 0 potion(s), 18 s |
| Corvenne : le capitaine Varn | Chapitre 4 : épée longue, plaques, Mêlée 3, Robustesse 3, 4 potions + 2 grandes | gagné | 62 % | 2 / 4 | 12 s | 11 | oui | gagné, 60 %, 1 potion(s), 19 s |
| Corvenne : Aldric, sceaux intacts | Chapitre 4 : épée longue, plaques, Mêlée 3, Robustesse 3, 4 potions + 2 grandes | gagné | 37 % | 5 / 5 | 24 s | 39 | oui | gagné, 25 %, 6 potion(s), 42 s |
| Corvenne : Aldric, sceaux fêlés | Chapitre 4 : épée longue, plaques, Mêlée 3, Robustesse 3, 4 potions + 2 grandes | gagné | 17 % | 6 / 6 | 27 s | 45 | oui | **perdu**, 0 %, 6 potion(s), 38 s |

**Ce qu'on lit :**

- Le joueur planté atteint de nouveau tous les objectifs. Au premier essai, l'endurance et les armes lourdes plus
  lentes avaient rendu Holt et Aldric trop durs ; j'ai resserré l'écart entre les armes et accéléré la reprise de
  l'endurance.
- La garde aide beaucoup contre un seul adversaire (Varn : une potion au lieu de deux) et contre les petits groupes.
- La garde aide moins quand des renforts arrivent (Holt, Aldric). Pendant qu'on bloque le chef, on ne frappe pas, et
  ses renforts frappent toujours. En jeu, la réponse est d'esquiver, de reculer et d'abattre les renforts d'abord, ce
  que la simulation ne sait pas faire.

## Les chefs (réglage du 2026-11-20, gardé)

| Chef | Avant | Après | Pourquoi |
| --- | --- | --- | --- |
| Sergent Holt | 220 PV | **300 PV** | battu en 10 s avec une seule potion ; le cahier en annonçait deux ou trois |
| Capitaine Varn | 320 PV, 20 de dégâts | **460 PV, 25 de dégâts** | battu en 7 s **sans aucune potion** : plus facile que Holt |
| Aldric | 600 PV | **760 PV** | 18 s seulement pour le dernier adversaire de l'histoire |

## Ce qui reste à voir avec toi

1. **Joue avec la garde et l'esquive**, puis dis-moi si les chefs sont trop durs, trop faciles ou bien.
2. **La parade** (0,2 s) : trop facile, ou trop dure à placer ?
3. **Les armes** : la hache reste la plus forte au coup chargé, mais c'est la plus lente. Faut-il qu'elle frappe plus
   fort au coup normal (9 de dégâts en plus aujourd'hui, contre 12 pour l'épée longue) ?
4. Le personnage simulé n'a pas de classe. Un guerrier (+20 PV) a plus de marge, un herboriste (-30 % en Mêlée) moins.

Pour relancer la simulation : `mvn test -Dtest=CombatBalanceTest`, puis lire `target/test-run/combats.txt`.
