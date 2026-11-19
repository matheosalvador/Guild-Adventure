package com.lucas.guild.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Le prototype du monde ouvert, tel qu'écrit dans {@code openworld.json} : l'échelle entre la carte et le terrain,
 * les carreaux chargés autour du joueur, les lieux posés dans le monde et les routes qui les relient. Ne contient que
 * des données ; les règles sont dans {@code game/WorldLayout} et {@code game/Terrain}.
 * Voir {@code docs/CADRAGE-MONDE-OUVERT.md}.
 */
public class OpenWorldData {

    /** Mètres de terrain par kilomètre de carte (100 : le monde à 1:10, les durées de trajet restent vraies). */
    public float scale = 100f;
    /** Côté d'un carreau de campagne, en m. */
    public float tileSize = 128f;
    /** Carreaux affichés autour de celui du joueur (2 : un carré de 5 x 5). */
    public int loadRadius = 2;
    /** Carreaux dont les arbres sont solides (1 : un carré de 3 x 3) ; les autres sont seulement dessinés. */
    public int solidRadius = 1;
    /** Distance de vue en m : la brume cache ce qui est plus loin (et les carreaux qui apparaissent). */
    public float viewDistance = 270f;
    /** Un lieu (ville, zone) est construit quand le joueur est à moins de cette distance de son bord. */
    public float placeLoadDistance = 400f;
    public String ground;
    public String road;
    public float roadWidth = 5f;
    /** Arbres semés par carreau (moins là où passent une route ou un lieu). */
    public int treesPerTile = 16;
    /** Jours de jeu avant qu'un monstre tué revienne. */
    public int monsterRespawnDays = 3;
    public List<PlaceData> places = new ArrayList<>();
    public List<RoadData> roads = new ArrayList<>();

    /** Un lieu de world.json posé dans le monde, à la position de la carte multipliée par l'échelle. */
    public static class PlaceData {
        public String id;
        /** Demi-côté du carré occupé par le lieu (son sol), en m : rien n'y est semé. */
        public float half;
        /** Zone : l'ouverture dans la haie par où passe la route (x, z sur la haie, en coordonnées du lieu). */
        public EntranceData entrance;
    }

    public static class EntranceData {
        public float x;
        public float z;
        public float width;
    }

    /** Une route : une ligne brisée, en m dans le monde, d'un lieu à un autre. */
    public static class RoadData {
        public String from;
        public String to;
        public List<PointData> points = new ArrayList<>();
    }

    public static class PointData {
        public float x;
        public float z;
    }

    /** Le lieu {@code id}, ou null. */
    public PlaceData place(String id) {
        for (PlaceData p : places) {
            if (p.id.equals(id)) {
                return p;
            }
        }
        return null;
    }

    /** Vérifie le fichier ; {@code world} sert à vérifier les lieux. */
    void validate(String file, WorldData world) {
        if (scale <= 0) throw new IllegalStateException(file + " : \"scale\" doit etre > 0");
        if (tileSize < 32) throw new IllegalStateException(file + " : \"tileSize\" doit etre >= 32");
        if (loadRadius < 1 || loadRadius > 4) throw new IllegalStateException(file + " : \"loadRadius\" doit etre entre 1 et 4");
        if (solidRadius < 0 || solidRadius > loadRadius) throw new IllegalStateException(file + " : \"solidRadius\" doit etre entre 0 et loadRadius");
        if (viewDistance < 100 || viewDistance > 300) throw new IllegalStateException(file + " : \"viewDistance\" doit etre entre 100 et 300");
        if (placeLoadDistance < viewDistance) throw new IllegalStateException(file + " : \"placeLoadDistance\" doit etre >= viewDistance");
        if (ground == null || ground.isEmpty()) throw new IllegalStateException(file + " : \"ground\" manquant");
        if (road == null || road.isEmpty()) throw new IllegalStateException(file + " : \"road\" manquant");
        if (roadWidth <= 0) throw new IllegalStateException(file + " : \"roadWidth\" doit etre > 0");
        if (treesPerTile < 0) throw new IllegalStateException(file + " : \"treesPerTile\" doit etre >= 0");
        if (monsterRespawnDays < 1) throw new IllegalStateException(file + " : \"monsterRespawnDays\" doit etre >= 1");
        if (places == null) places = new ArrayList<>();
        if (roads == null) roads = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < places.size(); i++) {
            PlaceData p = places.get(i);
            String where = file + " : lieu n°" + (i + 1);
            if (p == null || p.id == null || p.id.isEmpty()) throw new IllegalStateException(where + " : \"id\" manquant");
            where = file + " : lieu \"" + p.id + "\"";
            if (world.place(p.id) == null) throw new IllegalStateException(where + " : inconnu (voir world.json)");
            if (!seen.add(p.id)) throw new IllegalStateException(where + " : en double");
            if (p.half <= 0) throw new IllegalStateException(where + " : \"half\" doit etre > 0");
            if (p.entrance != null && p.entrance.width <= 0) throw new IllegalStateException(where + " : \"entrance\" : \"width\" doit etre > 0");
        }
        for (int i = 0; i < roads.size(); i++) {
            RoadData r = roads.get(i);
            String where = file + " : route n°" + (i + 1);
            if (r == null) throw new IllegalStateException(where + " : vide");
            if (r.from == null || place(r.from) == null) throw new IllegalStateException(where + " : \"from\" doit etre un lieu de \"places\" (" + r.from + ")");
            if (r.to == null || place(r.to) == null) throw new IllegalStateException(where + " : \"to\" doit etre un lieu de \"places\" (" + r.to + ")");
            if (r.points == null || r.points.size() < 2) throw new IllegalStateException(where + " : \"points\" doit avoir au moins 2 points");
        }
    }
}
