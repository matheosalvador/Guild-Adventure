package com.lucas.guild.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Le monde tel qu'écrit dans {@code world.json} : les lieux (villes pour l'instant), leur position
 * sur la carte du monde, et les routes qui les relient.
 *
 * <p>Les positions sont des coordonnées réelles (en km) : la durée d'un trajet se déduit de la
 * distance, elle n'est pas écrite à la main. Le jour où le monde sera ouvert, ces mêmes
 * coordonnées pourront servir de positions dans un monde continu.</p>
 */
public class WorldData {

    /** Le lieu où commence une nouvelle partie. */
    public String startPlace;
    /** Vitesse de marche sur les routes, en km/h. */
    public float walkingKmh = 5f;
    public List<PlaceData> places = new ArrayList<>();
    /** Les routes : des paires d'identifiants de lieux, dans les deux sens. */
    public List<List<String>> links = new ArrayList<>();
    /** Les embuscades sur les routes entre villes (optionnel) : où elles se jouent et qui attaque. */
    public AmbushData ambush;

    /** Une embuscade : un décor de route (zoneFile) et les monstres qui attaquent, entre {@code min} et {@code max}. */
    public static class AmbushData {
        public String zoneFile;
        public List<String> monsters = new ArrayList<>();
        public int min = 1;
        public int max = 2;
    }

    public static class PlaceData {
        public String id;
        public String name;
        /** Fichier de la ville dans assets/data (town.json, town_portbrume.json...). Un lieu a une ville OU une zone. */
        public String townFile;
        /** Fichier de la zone sauvage dans assets/data (zone_foret.json...) quand le lieu n'est pas une ville. */
        public String zoneFile;
        /** Position sur la carte du monde, en km. */
        public float x;
        public float z;
    }

    public static boolean isZone(PlaceData place) {
        return place != null && place.zoneFile != null;
    }

    public PlaceData place(String id) {
        for (PlaceData p : places) {
            if (p.id.equals(id)) {
                return p;
            }
        }
        return null;
    }

    /** Les lieux reliés par une route à {@code id}, dans l'ordre du fichier. */
    public List<PlaceData> neighbors(String id) {
        List<PlaceData> result = new ArrayList<>();
        for (List<String> link : links) {
            if (link.get(0).equals(id) && place(link.get(1)) != null) {
                result.add(place(link.get(1)));
            } else if (link.get(1).equals(id) && place(link.get(0)) != null) {
                result.add(place(link.get(0)));
            }
        }
        return result;
    }

    /** Distance à vol d'oiseau entre deux lieux, en km. */
    public double distanceKm(String from, String to) {
        PlaceData a = place(from);
        PlaceData b = place(to);
        return Math.hypot(a.x - b.x, a.z - b.z);
    }

    void validate(String file) {
        if (places == null || places.isEmpty()) {
            throw new IllegalStateException(file + " : aucun lieu");
        }
        if (walkingKmh <= 0) {
            throw new IllegalStateException(file + " : \"walkingKmh\" doit etre > 0");
        }
        List<String> seen = new ArrayList<>();
        for (int i = 0; i < places.size(); i++) {
            PlaceData p = places.get(i);
            String where = file + " : lieu n°" + (i + 1) + (p != null && p.id != null ? " (" + p.id + ")" : "");
            if (p == null || p.id == null || p.id.isEmpty()) throw new IllegalStateException(where + " : \"id\" manquant");
            if (seen.contains(p.id)) throw new IllegalStateException(where + " : \"id\" en double");
            seen.add(p.id);
            if (p.name == null || p.name.isEmpty()) throw new IllegalStateException(where + " : \"name\" manquant");
            boolean hasTown = p.townFile != null && !p.townFile.isEmpty();
            boolean hasZone = p.zoneFile != null && !p.zoneFile.isEmpty();
            if (!hasTown && !hasZone) throw new IllegalStateException(where + " : \"townFile\" (ville) ou \"zoneFile\" (zone sauvage) manquant");
            if (hasTown && hasZone) throw new IllegalStateException(where + " : un lieu ne peut pas avoir a la fois \"townFile\" et \"zoneFile\"");
        }
        if (startPlace == null || !seen.contains(startPlace)) {
            throw new IllegalStateException(file + " : \"startPlace\" doit etre l'identifiant d'un lieu (" + startPlace + ")");
        }
        if (ambush != null) {
            if (ambush.zoneFile == null || ambush.zoneFile.isEmpty()) throw new IllegalStateException(file + " : \"ambush\" : \"zoneFile\" manquant");
            if (ambush.monsters == null || ambush.monsters.isEmpty()) throw new IllegalStateException(file + " : \"ambush\" : \"monsters\" vide");
            if (ambush.min < 1 || ambush.max < ambush.min) throw new IllegalStateException(file + " : \"ambush\" : \"min\" >= 1 et \"max\" >= \"min\" attendus");
        }
        if (links == null) {
            links = new ArrayList<>();
        }
        for (List<String> link : links) {
            if (link == null || link.size() != 2) {
                throw new IllegalStateException(file + " : chaque route doit etre une paire [lieu, lieu]");
            }
            for (String end : link) {
                if (!seen.contains(end)) throw new IllegalStateException(file + " : route vers un lieu inconnu \"" + end + "\"");
            }
            if (link.get(0).equals(link.get(1))) {
                throw new IllegalStateException(file + " : une route ne peut pas relier un lieu a lui-meme (" + link.get(0) + ")");
            }
            if (distanceKm(link.get(0), link.get(1)) <= 0) {
                throw new IllegalStateException(file + " : la route " + link + " relie deux lieux a la meme position");
            }
        }
    }
}
