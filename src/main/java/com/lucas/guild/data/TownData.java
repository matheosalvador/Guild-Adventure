package com.lucas.guild.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Description d'une ville telle qu'écrite dans {@code town.json} : son nom et ses bâtiments.
 * Ne contient que des données, aucune dépendance à LibGDX.
 */
public class TownData {

    public String name;
    /** Couleur du sol : nom LibGDX (GREEN, GRAY...) ou code « #RRGGBB ». Gris si absente. */
    public String ground;
    public List<BuildingData> buildings = new ArrayList<>();
    /** Où le joueur apparaît quand il arrive par la route (devant la porte de la ville). */
    public Point3 arrival;
    /** Décor de la ville (rues, maisons, arbres, murs...), en coordonnées absolues. */
    public List<PartData> decor = new ArrayList<>();

    /**
     * Une boîte du décor. {@code x} et {@code z} sont le centre (relatif au bâtiment pour les
     * {@code parts} d'un bâtiment) ; {@code y} est la hauteur du bas de la boîte au-dessus du sol.
     */
    public static class PartData {
        public float x;
        public float z;
        public float y;
        public float w;
        public float h;
        public float d;
        /** Nom de couleur LibGDX ou « #RRGGBB ». */
        public String color;
        /** Visuel seulement : on marche dessus sans la heurter (rues, eau...). */
        public boolean decal;
        /** Action quand on vise cette partie (E) : « gate » = la porte de la ville, qui mène à la carte du monde. */
        public String action;
    }

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
        /** Boutique (shops.json) d'un bâtiment de type « store ». */
        public String shop;
        /** Détails du bâtiment (toit, porte, enseigne...), positions relatives à son centre. */
        public List<PartData> parts = new ArrayList<>();
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

    static void validateParts(List<PartData> parts, String where) {
        if (parts == null) {
            return;
        }
        for (int i = 0; i < parts.size(); i++) {
            PartData p = parts.get(i);
            String at = where + " n°" + (i + 1);
            if (p == null) throw new IllegalStateException(at + " : vide");
            if (p.color == null) throw new IllegalStateException(at + " : \"color\" manquant");
            if (p.action != null && !"gate".equals(p.action)) throw new IllegalStateException(at + " : \"action\" inconnue \"" + p.action + "\" (attendu : gate)");
            if (p.w <= 0 || p.h <= 0 || p.d <= 0) throw new IllegalStateException(at + " : \"w\", \"h\" et \"d\" doivent etre > 0");
        }
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
            if ("store".equals(b.interior) && (b.shop == null || b.shop.isEmpty())) {
                throw new IllegalStateException(where + " : un batiment \"store\" doit avoir une \"shop\" (voir shops.json)");
            }
            validateParts(b.parts, where + " : \"parts\"");
        }
        validateParts(decor, file + " : \"decor\"");
    }
}
