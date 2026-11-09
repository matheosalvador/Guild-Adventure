package com.lucas.guild.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Description d'une ville telle qu'écrite dans {@code town.json} : son nom et ses bâtiments.
 * Ne contient que des données, aucune dépendance à LibGDX.
 */
public class TownData {

    public String name;
    public List<BuildingData> buildings = new ArrayList<>();

    /** Un bâtiment de la ville. */
    public static class BuildingData {
        public String name;
        /** Type de bâtiment (« Forge », « Guilde », « Taverne »...). */
        public String type;
        /** Écran d'intérieur à ouvrir : « forge », « guild » ou « inn ». */
        public String interior;
        /** Position du centre du bâtiment, au sol. */
        public float x;
        public float z;
        /** Nom d'une couleur LibGDX (DARK_GRAY, BROWN, TAN...). */
        public String color;
        /** Où réapparaît le joueur en ville quand il sort du bâtiment. */
        public Point3 exit;
    }

    public static class Point3 {
        public float x;
        public float y;
        public float z;
    }

    /** @return le bâtiment dont l'intérieur est {@code interiorId}, ou null. */
    public BuildingData findByInterior(String interiorId) {
        for (BuildingData building : buildings) {
            if (interiorId.equals(building.interior)) {
                return building;
            }
        }
        return null;
    }

    /** Vérifie que tous les champs obligatoires sont présents. @throws IllegalStateException sinon. */
    void validate(String file) {
        if (name == null || name.isEmpty()) {
            throw new IllegalStateException(file + " : la ville n'a pas de nom");
        }
        if (buildings == null || buildings.isEmpty()) {
            throw new IllegalStateException(file + " : la ville n'a aucun batiment");
        }
        for (int i = 0; i < buildings.size(); i++) {
            BuildingData b = buildings.get(i);
            String where = file + " : batiment n°" + (i + 1) + (b.name != null ? " (" + b.name + ")" : "");
            if (b.name == null || b.name.isEmpty()) throw new IllegalStateException(where + " : \"name\" manquant");
            if (b.type == null) throw new IllegalStateException(where + " : \"type\" manquant");
            if (b.interior == null) throw new IllegalStateException(where + " : \"interior\" manquant");
            if (b.color == null) throw new IllegalStateException(where + " : \"color\" manquant");
            if (b.exit == null) throw new IllegalStateException(where + " : \"exit\" manquant");
        }
    }
}
