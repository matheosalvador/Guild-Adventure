# Relecture du code (chantier A)

Relecture de tout ce qui a été livré depuis `main` (`git diff main..general_develop` : lore, événements, quêtes entre
villes, vue à la 3e personne, PNJ et monstres en 3D, décor, finitions) et des fichiers voisins. Branche :
`feature/relecture`.

Règle suivie : un bug n'est corrigé que s'il est **prouvé** par un test ou une capture qui échouait avant et passe
après. Ce qui demande un choix de ta part est listé au §4, sans être touché.

## 1. Bugs corrigés

| # | Ce que voyait le joueur | Cause | Preuve |
| --- | --- | --- | --- |
| 1 | Rien de visible, mais la mémoire grossissait à chaque porte franchie | Les corps solides du décor (murs, sol, PNJ) n'étaient jamais libérés : LibGDX les garde dans une table globale jusqu'à leur `dispose()`. La forme du joueur était laissée au ramasse-miettes. | Banc visuel : corps Bullet vivants après 4 allers-retours ville/forge, **90 → 470 avant, 90 → 90 après**. Le journal du parcours contenait 162 lignes « Disposing ... due to garbage collection », il n'y en a plus. |
| 2 | Dans un dialogue, une boutique ou la pause : appuyer deux fois sur I, et les boutons ne répondent plus à la souris (elle est capturée) | I ouvrait l'inventaire par-dessus un autre menu ; en se refermant il recapturait la souris. Même défaut avec K par-dessus le panneau de debug. | Banc visuel : « pause : la souris reste libre », « dialogue : après I, I la souris reste libre » (captures `r00`, `r01`). |
| 3 | À la 3e personne, dos dans un coin, on voyait à travers le mur | La caméra était reculée le long d'un simple rayon : dans un coin elle finissait à 5 cm d'un mur voisin, que la caméra découpe en dessous de 16 cm. | Banc visuel : distance minimale caméra-mur sur 192 directions, **0,05 m avant, 0,25 m après** (captures `r10` à `r12`). |
| 4 | En gardant Espace enfoncé on montait sans fin : 10,9 m en 5 secondes, de quoi passer les haies des zones et tomber hors du monde | « Au sol » était déduit d'une vitesse verticale nulle, ce qui est vrai aussi au sommet d'un saut. | Banc visuel : hauteur max du joueur, Espace maintenu 5 s, **10,89 m avant, 2,20 m après** (un saut). |
| 5 | Conséquence du n°4 : l'étage de l'auberge (le lit) n'était atteignable **que** grâce à ce bug | Les marches faisaient 1 m alors qu'un saut monte de 0,70 m, et le haut de l'escalier ne touchait aucun plancher (4 m de vide). | L'escalier a maintenant 20 marches de 0,5 m et un palier. Banc visuel : le joueur monte en maintenant Z + Espace, traverse le palier, arrive au lit et y dort (captures `r30` à `r32`). |
| 6 | Livraison remise en étant en surcharge (possible avec l'âge) : la lettre disparaît, la preuve aussi, la quête est perdue | La preuve était ajoutée comme un objet ramassé, donc refusée si « trop lourd ». | `RelectureTest.deliveringWhileOverloadedStillGivesTheProof` (échouait : 0 preuve au lieu de 1). |
| 7 | Quête de combat : chaque monstre tué redonnait une preuve (3 rats = 3 queues), et celles en trop restaient dans le sac pour toujours | Aucune vérification que le joueur portait déjà sa preuve. | `RelectureTest.combatProofIsGivenOnlyOnce` (échouait : 3 au lieu de 1). |

**À regarder en jouant** : le n°5 change le décor de l'auberge (escalier plus fin, palier en haut). C'est la seule
correction qui se voit. Si tu préférais l'autre solution (garder les marches d'1 m et faire sauter plus haut), dis-le.

## 2. Nettoyage

**Supprimé** (après vérification qu'aucun code, script ou `pom.xml` ne s'en sert) :

- `archive/` : les 72 fichiers de la V1 (ils restent dans la branche `legacy-V1`) ;
- `model/Town`, `model/Building` (le peu qui servait est dans `TownScreen`), `libgdx/GameObject` et ce qui ne servait
  qu'à lui (`PlayerController.interact`, `Adventurer.findItem`) ;
- des méthodes jamais appelées (`Shop.listPrice`, `Guild.displayAvailableQuests`, `Guild.getQuestsForRank`) et
  11 imports inutiles.

**Gardé exprès** : `Attaque*`, `Combatant`, `PotionSoin`/`PotionDps`/`PotionMix` (les anciennes sauvegardes les
contiennent) ; `Quest.experienceReward`, `QuestType.ESCORT`, `QUESTS_PER_RANK` (leur sort est prévu au chantier B).

**`BaseWorldScreen` découpé** (1 178 → 929 lignes), sans changement de comportement :

| Nouveau fichier | Rôle |
| --- | --- |
| `WorldPanels` | construit et remplit les menus : dialogue, inventaire, personnage, journal, debug, pause |
| `NpcGroup` | les PNJ : ils respirent et se tournent vers le joueur |
| `GroundItemViews` | les objets posés au sol : affichage, ramassage |
| `SkyAndShadows` | ciel et ombres des écrans en plein air |
| `Readable` | les panneaux, stèles et livres à lire |
| `WaterWaves` | l'eau qui ondule |
| `Toasts` | les messages en haut de l'écran |

Au passage, l'identifiant du lieu pour les objets au sol est calculé une fois par écran au lieu d'une fois par image
(il relisait `world.json` à chaque image).

**Outils de test** : `docs/` est maintenant suivi par git ; le banc visuel a une section `relecture`, une option
`-Only <section>` pour ne jouer qu'une section en quelques secondes, et il signale les ressources Bullet oubliées.

## 3. Relu sans trouver de bug

- **Sauvegarde et anciens fichiers** : écriture atomique, fichier illisible mis de côté, champs absents réparés au
  chargement (tests existants pour les versions 0 à 3).
- **Horloge, événements, rattrapage** : le rattrapage est plafonné à 60 jours, un événement fini pendant le rattrapage
  n'est pas annoncé, un seul événement par ville.
- **Voyage interrompu** : mourir dans une embuscade abandonne bien le trajet ; sauvegarder y est interdit ; recharger
  ou revenir au menu repart d'une partie propre.
- **Arrondis de la boutique** : nouveau test `RelectureTest.noShopBuysBackForMoreThanItSells` (4 boutiques × tous les
  objets × 6 niveaux de Marchandage × sans et avec chaque événement) : jamais de rachat plus cher que l'achat, prix
  d'achat toujours au moins 1 Po.
- **Poids de l'inventaire** : capacité, sac, encombrement, effet de l'âge (tests existants).

## 4. À toi de décider (rien n'est changé)

1. **Un objet de quête peut être jeté.** Il tombe au sol, et les objets au sol ne sont pas sauvegardés : si on quitte,
   la quête reste « en cours » pour toujours (ni rendue, ni reprise). *Défaut proposé : pas de bouton « Jeter » sur
   les objets de quête.* Autre option : pouvoir abandonner une quête au tableau.
2. **Les objets au sol disparaissent à la fermeture du jeu** (butin non ramassé, objets posés). Les sauvegarder ?
3. **Les données sont relues et revérifiées à chaque appel** (`GameContent` relit le JSON : `world.json` à chaque
   `getTownId()`, `quests.json` à chaque monstre tué...). Sans effet visible aujourd'hui, mais c'est du travail
   inutile à chaque image. *Défaut proposé : garder les objets lus en mémoire ; à mesurer au chantier E.*
4. **Le hasard n'est pas reproductible** (butin, embuscades, événements : cinq générateurs sans graine). Conséquence :
   d'un parcours de test à l'autre, une quarantaine de captures sur 194 diffèrent sans que rien n'ait changé.
   *Défaut proposé : une seule source de hasard, que les tests peuvent fixer.*
5. **Le message du marché revient à chaque sortie de bâtiment** tant qu'un événement dure. Ne l'afficher qu'à
   l'arrivée en ville et au début de l'événement ?
6. **Aucune sauvegarde automatique au réveil après une mort**, et « Retour au Menu » quitte sans sauvegarder ni
   prévenir.
7. **Le lit fait dormir jusqu'au matin même en plein jour** (jusqu'à 24 h pour 10 Po) et le repas se paie à pleine
   vie.
8. **Commerce entre villes** : acheter à Port-Brume pendant une fête et revendre à Hautevigne pendant le filon
   rapporte environ 15 %. Voulu (c'est du commerce) ou à resserrer ?
9. **Une sauvegarde faite dans un lieu retiré des données** retombe sur la ville de départ mais garde l'ancienne
   position du joueur (il peut apparaître dans un mur).
10. **Textes en anglais** : « Player », « HP » sur le HUD, « Start / Settings / Credits / Exit » au menu (Settings et
    Credits ne font rien). Prévu au chantier C (Options, barre de vie).
11. **`BaseWorldScreen` fait encore 929 lignes.** Les deux morceaux suivants à sortir seraient le HUD et la
    construction du décor ; je me suis arrêté là pour ne pas déplacer plus de code sans besoin.

## 5. Vérification finale

- `mvn test` : 191 tests verts (188 + 3 nouveaux).
- Parcours complet : `FIN: AUCUNE ERREUR`, 194 captures (185 + 9 de la section relecture).
- Captures avant / après le découpage de `BaseWorldScreen` : **173 identiques au pixel près** sur 194 (tous les
  menus, intérieurs, PNJ, villes, ciel, ombres, eau). Les 21 autres sont des scènes de combat et de butin, qui varient
  déjà d'un parcours à l'autre à cause du hasard (voir §4, point 4) ; regardées à l'œil, elles sont correctes.
