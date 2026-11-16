"""Fabrique les zones de l'histoire (mine, fosse, chateau) et ajoute le cairn a la foret et le chef gobelin aux ruines.

Usage, depuis la racine du depot :   python docs/outils-test/gen_story_zones.py
Le script reecrit les six fichiers zone_mine_*, zone_fosse, zone_corvenne_* et complete zone_foret / zone_ruines
(sans rien y ajouter deux fois). Les positions sont en metres ; "y" est la hauteur du bas de la piece au-dessus du sol.
"""
import json
import os
import random

DATA = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "assets", "data")


def box(x, z, w, h, d, color, y=0.0, **extra):
    part = {"x": x, "z": z, "y": y, "w": w, "h": h, "d": d, "color": color}
    part.update(extra)
    return part


def shape(kind, x, z, w, h, d, color, y=0.0, **extra):
    return box(x, z, w, h, d, color, y, shape=kind, **extra)


def decal(x, z, w, d, color, **extra):
    return box(x, z, w, 0.04, d, color, 0.0, decal=True, **extra)


def spawn(monster, x, z, requires=None):
    s = {"id": monster, "x": x, "z": z}
    if requires:
        s["requires"] = requires
    return s


def save(name, zone):
    with open(os.path.join(DATA, name), "w", encoding="utf-8", newline="\n") as f:
        json.dump(zone, f, ensure_ascii=False, indent=1)
        f.write("\n")
    print("ecrit", name, "-", len(zone.get("decor", [])), "pieces de decor")


def load(name):
    with open(os.path.join(DATA, name), encoding="utf-8") as f:
        return json.load(f)


ROCK = "#4A443C"
ROCK_DARK = "#3A352F"
WOOD = "#5A4632"
WOOD_DARK = "#3E2F20"
LAMP = "#FFD27A"
BONE = "#E6E0CF"
STONE = "#6E6A62"
STONE_DARK = "#55524C"
RED = "#7A1E22"
GOLD = "#C8A64A"
ASH = "#8C8C96"


def lamp(x, z):
    """Un poteau et sa lanterne (une boite claire : elle ne brille pas, mais on la voit dans le noir)."""
    return [box(x, z, 0.25, 2.6, 0.25, WOOD_DARK), box(x, z, 0.45, 0.45, 0.45, LAMP, y=2.6, decal=True)]


def prop_frame(x, z, along_x=True):
    """Un cadre de soutenement : deux poteaux et une poutre."""
    if along_x:
        return [box(x - 2.2, z, 0.4, 4.2, 0.4, WOOD), box(x + 2.2, z, 0.4, 4.2, 0.4, WOOD), box(x, z, 5.0, 0.4, 0.5, WOOD, y=4.2, decal=True)]
    return [box(x, z - 2.2, 0.4, 4.2, 0.4, WOOD), box(x, z + 2.2, 0.4, 4.2, 0.4, WOOD), box(x, z, 0.5, 0.4, 5.0, WOOD, y=4.2, decal=True)]


# ------------------------------------------------------------------ la mine : les galeries
def mine_galeries():
    rnd = random.Random(11)
    decor = []
    # des parois qui coupent la salle en couloirs ; le passage reste ouvert autour des rails
    decor += [box(-20.5, 6, 29, 9, 3, ROCK), box(21.5, 6, 27, 9, 3, ROCK),
              box(21, -10, 28, 9, 3, ROCK), box(-26, -26, 14, 9, 3, ROCK_DARK)]
    # piliers de roche
    for (x, z) in [(-20, 22), (13, 19), (26, 14), (-8, -8), (-28, -8), (10, -24), (24, -26)]:
        s = 3.0 + rnd.random() * 2.0
        decor.append(box(x, z, s, 9, s, ROCK_DARK if rnd.random() < 0.5 else ROCK))
    # rails et wagonnets
    decor += [decal(0, 8, 0.25, 54, "#2A2A2E"), decal(1.4, 8, 0.25, 54, "#2A2A2E")]
    for z in range(-18, 34, 3):
        decor.append(decal(0.7, z, 2.2, 0.35, WOOD_DARK))
    decor += [box(0.7, 18, 1.6, 1.1, 2.4, "#4E3B28"), box(0.7, -4, 1.6, 1.1, 2.4, "#4E3B28"), box(0.7, 18, 1.2, 0.5, 2.0, ROCK, y=1.1, decal=True)]
    # soutenements et lanternes le long du chemin
    for z in (26, 14, -2, -18):
        decor += prop_frame(0.7, z)
    for (x, z) in [(4, 28), (-4, 12), (5, 0), (-4, -14), (4, -28), (-22, -14), (22, 18), (20, -20)]:
        decor += lamp(x, z)
    # caisses, tonneaux, eboulis
    for (x, z) in [(-10, 26), (-12, 24), (14, 26), (-30, 14), (28, -2), (-16, -20)]:
        decor.append(box(x, z, 1.3, 1.2, 1.3, WOOD))
    for (x, z) in [(30, 30), (31, 27), (-31, 28), (-30, -31), (31, -31)]:
        decor.append(box(x, z, 2.6, 1.6 + rnd.random(), 2.4, ROCK))
    # l'inscription des premiers mineurs : une dalle dressee, pres de l'entree
    decor += [box(6, 25, 2.2, 2.6, 0.5, "#7A756A", action="read", text="inscription_mine")]
    # le contremaitre d'autrefois (des os), la ou repose la cle
    decor += [decal(-24, -18.6, 1.6, 0.5, BONE), decal(-24.6, -17.6, 0.4, 1.4, BONE), decal(-23.4, -17.6, 0.4, 1.4, BONE),
              box(-24, -19.4, 0.5, 0.45, 0.5, BONE, decal=True)]
    return {
        "ground": "#3B352E", "size": 70,
        "arrival": {"x": 0.7, "y": 2, "z": 30.0}, "exit": {"x": -5, "z": 33.0},
        "indoor": True, "background": "#020203", "wall": "#2E2A26", "ambient": 0.24,
        "decor": decor,
        "monsters": [spawn("bat", -10, 16), spawn("bat", -8, 18), spawn("bat", -12, 19),
                     spawn("bat", 16, -2), spawn("bat", 18, 0), spawn("bat", 14, -4),
                     spawn("skeleton_miner", -20, -14), spawn("skeleton_miner", -27, -20),
                     spawn("skeleton_miner", 6, -18), spawn("skeleton_miner", 22, -22)],
        "chests": [{"x": 28, "z": 22, "gold": 30, "items": ["healing_potion", "healing_potion"], "respawnDays": 7}],
        "doors": [{"x": 0.7, "z": -34.2, "to": "mine_chambre", "label": "Descendre vers la chambre",
                   "requires": ["flag:mine_key"], "lockedText": "Une grille de fer, fermée à clé. On entend cogner derrière.",
                   "color": "#3A3A40"}],
    }


# ------------------------------------------------------------------ la mine : la chambre du gardien
def mine_chambre():
    decor = []
    for (x, z) in [(-13, -13), (13, -13), (-13, 8), (13, 8)]:
        decor.append(box(x, z, 3, 9, 3, ROCK_DARK))
    # la porte scellee des premiers mineurs, au fond, et le trou qu'on y creuse
    decor += [box(0, -19.2, 9, 7, 1.2, "#26242A"), box(0, -18.5, 0.3, 7, 0.2, "#8C8C96", decal=True),
              box(-3.2, -17.6, 2.4, 1.2, 1.8, ROCK), box(3.4, -17.4, 2.0, 0.9, 1.6, ROCK), box(1.2, -16.6, 1.2, 0.6, 1.2, ROCK)]
    for (x, z) in [(-8, 14), (8, 14), (-9, -4), (9, -4)]:
        decor += lamp(x, z)
    decor += [box(-16, 2, 1.3, 1.2, 1.3, WOOD), box(-16, 3.6, 1.3, 1.2, 1.3, WOOD), box(16, -2, 2.2, 1.0, 1.4, WOOD)]
    return {
        "ground": "#2F2A25", "size": 40,
        "arrival": {"x": 0, "y": 2, "z": 15.0},
        "indoor": True, "background": "#020203", "wall": "#2A2622", "ambient": 0.22,
        "decor": decor,
        "monsters": [spawn("sergeant_holt", 0, -9, ["!flag:letter_found"])],
        "chests": [{"x": -15, "z": -15.5, "gold": 60, "items": ["greater_healing_potion"], "respawnDays": 10}],
        "doors": [{"x": 0, "z": 19.2, "to": "mine_galeries", "spawn": {"x": 0.7, "y": 2, "z": -29.0},
                   "label": "Remonter vers les galeries", "color": "#3A3A40"}],
    }


# ------------------------------------------------------------------ la Fosse des Debardeurs
def fosse():
    decor = []
    # gradins de bois sur trois cotes
    decor += [box(-20, -2, 2.4, 1.0, 30, WOOD), box(-21, -2, 1.2, 2.0, 30, WOOD_DARK),
              box(20, -2, 2.4, 1.0, 30, WOOD), box(21, -2, 1.2, 2.0, 30, WOOD_DARK),
              box(0, -20, 34, 1.0, 2.4, WOOD), box(0, -21, 34, 2.0, 1.2, WOOD_DARK)]
    # le sable taché, la ligne de depart, quelques caisses pres de l'entree
    decor += [decal(0, -4, 26, 24, "#A88C5C"), decal(0, 8.5, 26, 0.4, "#6B5236"),
              decal(-5, -8, 3.0, 2.2, "#7A5A40"), decal(6, -2, 2.4, 3.0, "#7A5A40")]
    decor += [box(-14, 17, 1.4, 1.3, 1.4, WOOD), box(-12.4, 17.4, 1.2, 1.1, 1.2, WOOD_DARK), box(14, 17, 1.6, 1.4, 1.6, WOOD)]
    # mats et fanions
    for (x, z) in [(-19, 12), (19, 12), (-19, -16), (19, -16)]:
        decor += [box(x, z, 0.3, 7, 0.3, WOOD_DARK), box(x, z, 0.1, 1.2, 1.6, RED, y=5.6, decal=True)]
    # le maitre de la Fosse : son pupitre (c'est lui qu'on vise), et la regle affichee
    decor += [box(7, 15.5, 1.6, 1.3, 0.9, "#6B5236", action="story", text="fosse_master"),
              box(7, 15.5, 1.3, 0.5, 0.6, "#E2D9BC", y=1.3, decal=True),
              box(-7, 15.5, 2.0, 2.4, 0.3, "#8A6B3E", action="read", text="plaque_fosse")]
    return {
        "ground": "#B59A6A", "size": 44,
        "arrival": {"x": 0, "y": 2, "z": 17.0}, "exit": {"x": 0, "z": 20.5},
        "wall": "#6B5236",
        "decor": decor, "monsters": [], "chests": [],
        "arena": {
            "startFlag": "force_running",
            "waves": [
                [spawn("wolf", -6, -8), spawn("wolf", 0, -12), spawn("wolf", 6, -8)],
                [spawn("goblin", -7, -10), spawn("goblin", 0, -13), spawn("goblin", 7, -10), spawn("wolf", 0, -6)],
                [spawn("skeleton_miner", -6, -11), spawn("skeleton_miner", 0, -13), spawn("skeleton_miner", 6, -11),
                 spawn("bat", -3, -6), spawn("bat", 3, -6)],
            ],
            "winSets": ["seal_force", "seal_force_intact", "force_answered"],
            "failSets": ["seal_force", "seal_force_cracked", "force_answered"],
            "winMessage": "La Fosse se tait. Le maître vous lance un sceau de cire : intact.",
            "failMessage": "L'épreuve est perdue. Le sceau qu'on vous garde est fêlé.",
        },
    }


# ------------------------------------------------------------------ Corvenne : la cour
def corvenne_cour():
    decor = []
    # le donjon, au nord, et ses deux tours
    decor += [box(0, -40, 44, 16, 8, STONE), box(-24, -38, 9, 22, 9, STONE_DARK), box(24, -38, 9, 22, 9, STONE_DARK),
              shape("cone", -24, -38, 11, 7, 11, RED, y=22), shape("cone", 24, -38, 11, 7, 11, RED, y=22)]
    for x in range(-18, 20, 6):
        decor.append(box(x, -36.2, 2.4, 1.4, 0.6, STONE_DARK, y=16, decal=True))   # creneaux
    for x in (-12, 12):
        decor += [box(x, -35.9, 1.2, 2.4, 0.2, "#1E1C22", y=7, decal=True), box(x, -35.9, 0.25, 6, 0.25, RED, y=9.6, decal=True)]
    # l'allee pavee, de la herse a la porte
    decor += [decal(0, 2, 7, 76, "#7E7A6E")]
    # la herse (derriere soi en arrivant) : deux piles et la grille levee
    decor += [box(-5, 43.5, 3, 9, 3, STONE_DARK), box(5, 43.5, 3, 9, 3, STONE_DARK), box(0, 44, 7, 1.2, 0.5, "#2A2A2E", y=7.6, decal=True)]
    # ecuries, puits, rateliers
    decor += [box(-32, 10, 14, 5, 10, WOOD), shape("gable", -32, 10, 15, 3, 11, WOOD_DARK, y=5),
              box(30, 14, 10, 4.5, 8, WOOD), shape("gable", 30, 14, 11, 2.6, 9, WOOD_DARK, y=4.5),
              shape("cylinder", 18, 26, 3.2, 1.2, 3.2, STONE_DARK), box(18, 26, 0.3, 3, 0.3, WOOD_DARK, y=1.2), box(18, 26, 3.4, 0.3, 0.3, WOOD_DARK, y=4.2, decal=True),
              box(-14, 24, 4, 1.6, 0.5, WOOD), box(-14, 24, 0.15, 2.4, 0.15, "#8E949B", y=1.6, decal=True)]
    # mannequins d'entrainement, braseros
    for (x, z) in [(-20, -8), (-24, -12), (-16, -14)]:
        decor += [box(x, z, 0.3, 2.2, 0.3, WOOD_DARK), box(x, z, 1.4, 0.3, 0.3, WOOD_DARK, y=1.5, decal=True)]
    for (x, z) in [(-6, -30), (6, -30), (-6, 30), (6, 30)]:
        decor += [box(x, z, 0.9, 1.1, 0.9, "#2A2A2E"), box(x, z, 0.6, 0.5, 0.6, "#E8862A", y=1.1, decal=True)]
    # la pierre de fondation, pres de la herse
    decor += [box(-9, 36, 2.4, 2.2, 0.6, "#8A867C", action="read", text="pierre_corvenne")]
    return {
        "ground": "#6E6A5E", "size": 96,
        "arrival": {"x": 0, "y": 2, "z": 38.0}, "exit": {"x": 9, "z": 40.0},
        "wall": "#55524C",
        "decor": decor,
        "monsters": [spawn("garrison_soldier", -9, 8, ["!flag:ending_chosen"]), spawn("garrison_soldier", 9, 8, ["!flag:ending_chosen"]),
                     spawn("garrison_soldier", -14, -14, ["!flag:ending_chosen"]), spawn("garrison_soldier", 14, -14, ["!flag:ending_chosen"]),
                     spawn("garrison_soldier", 0, -24, ["!flag:ending_chosen"])],
        "chests": [{"x": 34, "z": -26, "gold": 80, "items": ["greater_healing_potion", "greater_healing_potion"], "respawnDays": 14}],
        "doors": [{"x": 0, "z": -35.6, "to": "corvenne_salle", "label": "Entrer dans la grande salle", "color": "#4A3220"}],
    }


# ------------------------------------------------------------------ Corvenne : la grande salle
def corvenne_salle():
    decor = []
    for z in (-12, -2, 8):
        for x in (-13, 13):
            decor.append(shape("cylinder", x, z, 1.6, 9, 1.6, STONE))          # colonnes (visuel)
            decor.append(box(x, z, 1.4, 9, 1.4, STONE, decal=False))             # leur corps solide
    # le tapis, les tables le long des murs, le trone
    decor += [decal(0, 0, 5, 38, RED), decal(0, 0, 4.2, 38, "#8C2A2E")]
    decor += [box(-19, 4, 2.6, 1.0, 14, WOOD), box(19, 4, 2.6, 1.0, 14, WOOD),
              box(-16.8, 4, 0.8, 0.5, 12, WOOD_DARK), box(16.8, 4, 0.8, 0.5, 12, WOOD_DARK)]
    decor += [box(-9, -19.5, 4, 0.8, 3, STONE_DARK), box(-9, -20.2, 2.2, 3.4, 1.0, "#4A3220", y=0.8), box(-9, -19.4, 1.8, 0.3, 1.6, RED, y=0.8, decal=True)]
    for (x, z) in [(-21, -14), (21, -14), (-21, 14), (21, 14)]:
        decor += [box(x, z, 0.9, 1.1, 0.9, "#2A2A2E"), box(x, z, 0.6, 0.5, 0.6, "#E8862A", y=1.1, decal=True)]
    for x in (-18, -6, 6, 18):
        decor.append(box(x, -22.7, 2.2, 5, 0.2, RED if x in (-18, 18) else "#1F2C5A", y=3, decal=True))   # tentures
    return {
        "ground": "#4A3F38", "size": 46,
        "arrival": {"x": 0, "y": 2, "z": 18.0},
        "indoor": True, "background": "#0A0808", "wall": "#4E4A45", "ambient": 0.34,
        "decor": decor,
        "monsters": [spawn("captain_varn", 0, -10, ["!flag:varn_defeated"])],
        "chests": [],
        "doors": [{"x": 0, "z": 22.2, "to": "corvenne_cour", "spawn": {"x": 0, "y": 2, "z": -30.0},
                   "label": "Sortir dans la cour", "color": "#4A3220"},
                  {"x": 6, "z": -22.2, "to": "corvenne_crypte", "label": "Descendre dans la crypte",
                   "requires": ["flag:varn_defeated"], "lockedText": "La porte de la crypte est fermée à clé.", "color": "#2A2A2E"}],
    }


# ------------------------------------------------------------------ Corvenne : la crypte du Sablier
def corvenne_crypte():
    decor = []
    for (x, z) in [(-12, -10), (12, -10), (-12, 6), (12, 6)]:
        decor.append(box(x, z, 2.6, 9, 2.6, "#34323A"))
    # le Sablier : un socle (c'est lui qu'on vise), deux cones de verre plein de cendre, trois montants
    decor += [box(0, -14, 3.0, 0.7, 3.0, "#26242A", action="story", text="sablier"),
              shape("cone", 0, -14, 1.9, 1.5, 1.9, ASH, y=0.7, rot=[180, 0, 0]),
              shape("cone", 0, -14, 1.9, 1.5, 1.9, "#A8A8B2", y=2.2),
              shape("cylinder", 0, -14, 2.3, 0.2, 2.3, GOLD, y=3.7),
              shape("cylinder", 0, -14, 2.3, 0.15, 2.3, GOLD, y=0.7)]
    for (dx, dz) in [(1.0, 0.6), (-1.0, 0.6), (0, -1.1)]:
        decor.append(box(dx, -14 + dz, 0.14, 3.0, 0.14, GOLD, y=0.7, decal=True))
    # la cendre repandue autour, les tombes des seigneurs d'avant
    decor += [decal(0, -13, 7, 6, "#3C3A42"), decal(0.6, -12, 3.5, 2.6, "#4A4850")]
    for (x, z) in [(-15, -2), (-15, -14), (15, -2), (15, -14)]:
        decor += [box(x, z, 2.2, 1.0, 4.0, "#3E3C44"), box(x, z, 1.8, 0.25, 3.6, "#55535C", y=1.0, decal=True)]
    for (x, z) in [(-5, -17), (5, -17), (-7, 12), (7, 12)]:
        decor += [box(x, z, 0.9, 1.1, 0.9, "#2A2A2E"), box(x, z, 0.6, 0.5, 0.6, "#7FB0C8", y=1.1, decal=True)]
    return {
        "ground": "#2E2C33", "size": 40,
        "arrival": {"x": 0, "y": 2, "z": 15.0},
        "indoor": True, "background": "#020204", "wall": "#2A2830", "ambient": 0.26,
        "decor": decor,
        "monsters": [spawn("lord_aldric", 0, -6, ["!flag:aldric_yielded"])],
        "chests": [],
        "doors": [{"x": 0, "z": 19.2, "to": "corvenne_salle", "spawn": {"x": 6, "y": 2, "z": -17.0},
                   "label": "Remonter dans la grande salle", "color": "#2A2A2E"}],
    }


# ------------------------------------------------------------------ ajouts aux zones existantes
def add_cairn():
    zone = load("zone_foret.json")
    if any(p.get("text") == "cairn" for p in zone["decor"]):
        print("zone_foret.json : cairn deja present")
        return
    x, z = -62, -2
    stone, moss = "#7A7870", "#5E6A52"
    zone["decor"] += [
        box(x, z, 3.0, 1.0, 3.0, stone, action="story", text="cairn"),
        box(x + 0.1, z - 0.1, 2.2, 0.9, 2.2, "#8A887E", y=1.0, decal=True),
        box(x - 0.1, z + 0.1, 1.5, 0.8, 1.5, stone, y=1.9, decal=True),
        box(x, z, 0.8, 0.7, 0.8, "#96948A", y=2.7, decal=True),
        decal(x, z, 6, 6, moss),
        # des pierres plates qui quittent le chemin vers le cairn
        decal(-12, 20, 1.6, 1.2, stone), decal(-24, 14, 1.6, 1.2, stone), decal(-36, 8, 1.6, 1.2, stone), decal(-48, 3, 1.6, 1.2, stone),
    ]
    # pas de loups en plus : ceux de l'ouest (-30, 10) sont deja sur le passage
    save("zone_foret.json", zone)


def add_chief():
    zone = load("zone_ruines.json")
    if any(m["id"] == "goblin_chief" for m in zone["monsters"]):
        print("zone_ruines.json : chef deja present")
        return
    here = ["flag:done:prologue", "!flag:registre_found"]
    zone["monsters"] += [spawn("goblin_chief", 0, -58, here), spawn("goblin", -4, -55, here), spawn("goblin", 4, -55, here)]
    # son campement : un feu, une tente de peaux
    zone["decor"] += [box(0, -60, 0.9, 0.5, 0.9, "#2A2A2E"), box(0, -60, 0.6, 0.4, 0.6, "#E8862A", y=0.5, decal=True),
                      shape("cone", -6, -62, 4.5, 3.2, 4.5, "#7A5A3A"), shape("cone", 6, -62, 4.0, 2.8, 4.0, "#6B4E32")]
    save("zone_ruines.json", zone)


if __name__ == "__main__":
    save("zone_mine_galeries.json", mine_galeries())
    save("zone_mine_chambre.json", mine_chambre())
    save("zone_fosse.json", fosse())
    save("zone_corvenne_cour.json", corvenne_cour())
    save("zone_corvenne_salle.json", corvenne_salle())
    save("zone_corvenne_crypte.json", corvenne_crypte())
    add_cairn()
    add_chief()
