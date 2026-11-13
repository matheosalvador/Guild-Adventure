package com.lucas.guild.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Une zone sauvage telle qu'écrite dans un fichier {@code zone_*.json} : un carré de terrain avec
 * son décor, ses monstres et ses coffres. Ne contient que des données.
 */
public class ZoneData {

    public String ground;
    /** Côté du carré de terrain, en m. */
    public float size = 140f;
    /** Où le joueur apparaît en arrivant par la route. */
    public TownData.Point3 arrival;
    /** La borne de route : la viser (E) ouvre la carte du monde. */
    public TownData.Point3 exit;
    public List<TownData.PartData> decor = new ArrayList<>();
    public List<SpawnData> monsters = new ArrayList<>();
    public List<ChestData> chests = new ArrayList<>();

    /** Un monstre placé à la main. */
    public static class SpawnData {
        public String id;
        public float x;
        public float z;
    }

    /** Un coffre : de l'or et des objets, qui se remplit de nouveau après {@code respawnDays} jours. */
    public static class ChestData {
        public float x;
        public float z;
        public int gold;
        public List<String> items = new ArrayList<>();
        public int respawnDays = 7;
    }

    void validate(String file) {
        if (ground == null) throw new IllegalStateException(file + " : \"ground\" manquant");
        if (size < 20f) throw new IllegalStateException(file + " : \"size\" doit etre >= 20");
        if (arrival == null) throw new IllegalStateException(file + " : \"arrival\" manquant");
        if (exit == null) throw new IllegalStateException(file + " : \"exit\" (la borne de route) manquant");
        if (decor == null) decor = new ArrayList<>();
        if (monsters == null) monsters = new ArrayList<>();
        if (chests == null) chests = new ArrayList<>();
        TownData.validateParts(decor, file + " : \"decor\"");
        for (int i = 0; i < chests.size(); i++) {
            ChestData c = chests.get(i);
            String where = file + " : coffre n°" + (i + 1);
            if (c == null) throw new IllegalStateException(where + " : vide");
            if (c.gold < 0) throw new IllegalStateException(where + " : \"gold\" doit etre >= 0");
            if (c.respawnDays < 1) throw new IllegalStateException(where + " : \"respawnDays\" doit etre >= 1");
            if (c.items == null) c.items = new ArrayList<>();
        }
    }
}
