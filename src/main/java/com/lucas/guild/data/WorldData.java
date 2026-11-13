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

    public static class PlaceData {
        public String id;
        public String name;
        /** Fichier de la ville dans assets/data (town.json, town_portbrume.json...). */
        public String townFile;
        /** Position sur la carte du monde, en km. */
        public float x;
        public float z;
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
            if (p.townFile == null || p.townFile.isEmpty()) throw new IllegalStateException(where + " : \"townFile\" manquant");
        }
        if (startPlace == null || !seen.contains(startPlace)) {
            throw new IllegalStateException(file + " : \"startPlace\" doit etre l'identifiant d'un lieu (" + startPlace + ")");
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
