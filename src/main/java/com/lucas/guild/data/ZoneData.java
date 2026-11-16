package com.lucas.guild.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Une zone telle qu'écrite dans un fichier {@code zone_*.json} : un carré de terrain avec son décor, ses monstres
 * et ses coffres. C'est une zone sauvage en plein air, ou (avec {@code indoor}) un lieu fermé : une galerie de mine,
 * une salle de château. Ne contient que des données.
 */
public class ZoneData {

    public String ground;
    /** Côté du carré de terrain, en m. */
    public float size = 140f;
    /** Où le joueur apparaît en arrivant par la route. */
    public TownData.Point3 arrival;
    /** La borne de route : la viser (E) ouvre la carte du monde. Facultative quand la zone a des portes. */
    public TownData.Point3 exit;
    public List<TownData.PartData> decor = new ArrayList<>();
    public List<SpawnData> monsters = new ArrayList<>();
    public List<ChestData> chests = new ArrayList<>();

    /** Un lieu fermé : pas de ciel ni d'ombres portées, des murs à la place des haies, une lumière fixe. */
    public boolean indoor;
    /** Lieu fermé : couleur du fond (ce qu'on voit au-dessus des murs). */
    public String background = "#050508";
    /** Couleur du tour de la zone : absent = une haie dehors, des murs gris dans un lieu fermé. */
    public String wall;
    /** Intensité de la lumière ambiante (0 = celle par défaut : 0,4 dehors, 0,3 dans un lieu fermé). */
    public float ambient;
    /** Les portes vers d'autres zones (un autre étage, une autre salle). */
    public List<DoorData> doors = new ArrayList<>();
    /** Une arène : des vagues de monstres déclenchées par l'histoire (une épreuve). */
    public ArenaData arena;

    /** Un monstre placé à la main. */
    public static class SpawnData {
        public String id;
        public float x;
        public float z;
        /** Conditions d'histoire (voir StoryData) pour qu'il soit là ; absent = toujours. */
        public List<String> requires = new ArrayList<>();
    }

    /** Un coffre : de l'or et des objets, qui se remplit de nouveau après {@code respawnDays} jours. */
    public static class ChestData {
        public float x;
        public float z;
        public int gold;
        public List<String> items = new ArrayList<>();
        public int respawnDays = 7;
    }

    /** Une porte : la viser (E) mène à la zone {@code to} (un lieu de world.json), si l'histoire le permet. */
    public static class DoorData {
        public float x;
        public float z;
        /** Le lieu (world.json) où mène la porte. */
        public String to;
        /** Où l'on apparaît de l'autre côté ; absent = à l'arrivée de la zone. */
        public TownData.Point3 spawn;
        /** Le texte affiché quand on vise la porte (« Descendre dans la chambre »). */
        public String label;
        public List<String> requires = new ArrayList<>();
        /** Le texte affiché tant que la porte est fermée. */
        public String lockedText;
        public String color = "#5A4632";
    }

    /** Une arène : quand l'histoire pose {@code startFlag}, les vagues se suivent ; toutes vaincues, c'est gagné. */
    public static class ArenaData {
        public String startFlag;
        public List<List<SpawnData>> waves = new ArrayList<>();
        /** Drapeaux posés en cas de victoire, et en cas d'échec (mort, ou sortie avant la fin). */
        public List<String> winSets = new ArrayList<>();
        public List<String> failSets = new ArrayList<>();
        public String winMessage;
        public String failMessage;
    }

    void validate(String file) {
        if (ground == null) throw new IllegalStateException(file + " : \"ground\" manquant");
        if (size < 20f) throw new IllegalStateException(file + " : \"size\" doit etre >= 20");
        if (arrival == null) throw new IllegalStateException(file + " : \"arrival\" manquant");
        if (doors == null) doors = new ArrayList<>();
        if (exit == null && doors.isEmpty()) throw new IllegalStateException(file + " : \"exit\" (la borne de route) manquant");
        if (ambient < 0 || ambient > 1) throw new IllegalStateException(file + " : \"ambient\" doit etre entre 0 et 1");
        if (decor == null) decor = new ArrayList<>();
        if (monsters == null) monsters = new ArrayList<>();
        if (chests == null) chests = new ArrayList<>();
        TownData.validateParts(decor, file + " : \"decor\"");
        for (SpawnData m : monsters) {
            if (m != null) {
                if (m.requires == null) m.requires = new ArrayList<>();
                StoryData.validateConditions(m.requires, file + " : monstre \"" + m.id + "\" : \"requires\"");
            }
        }
        for (int i = 0; i < chests.size(); i++) {
            ChestData c = chests.get(i);
            String where = file + " : coffre n°" + (i + 1);
            if (c == null) throw new IllegalStateException(where + " : vide");
            if (c.gold < 0) throw new IllegalStateException(where + " : \"gold\" doit etre >= 0");
            if (c.respawnDays < 1) throw new IllegalStateException(where + " : \"respawnDays\" doit etre >= 1");
            if (c.items == null) c.items = new ArrayList<>();
        }
        for (int i = 0; i < doors.size(); i++) {
            DoorData d = doors.get(i);
            String where = file + " : porte n°" + (i + 1);
            if (d == null) throw new IllegalStateException(where + " : vide");
            if (d.to == null || d.to.isEmpty()) throw new IllegalStateException(where + " : \"to\" manquant (le lieu ou elle mene)");
            if (d.label == null || d.label.isEmpty()) throw new IllegalStateException(where + " : \"label\" manquant");
            if (d.requires == null) d.requires = new ArrayList<>();
            StoryData.validateConditions(d.requires, where + " : \"requires\"");
            if (!d.requires.isEmpty() && (d.lockedText == null || d.lockedText.isEmpty())) {
                throw new IllegalStateException(where + " : une porte a conditions demande un \"lockedText\" (la raison affichee)");
            }
        }
        if (arena != null) {
            String where = file + " : \"arena\"";
            if (arena.startFlag == null || arena.startFlag.isEmpty()) throw new IllegalStateException(where + " : \"startFlag\" manquant");
            if (arena.waves == null || arena.waves.isEmpty()) throw new IllegalStateException(where + " : aucune vague (\"waves\")");
            for (int i = 0; i < arena.waves.size(); i++) {
                if (arena.waves.get(i) == null || arena.waves.get(i).isEmpty()) throw new IllegalStateException(where + " : la vague n°" + (i + 1) + " est vide");
            }
            if (arena.winSets == null || arena.winSets.isEmpty()) throw new IllegalStateException(where + " : \"winSets\" manquant");
            if (arena.failSets == null || arena.failSets.isEmpty()) throw new IllegalStateException(where + " : \"failSets\" manquant");
        }
    }
}
