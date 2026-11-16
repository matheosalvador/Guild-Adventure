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
    /** Les habitants qui se promènent dans les rues ; on leur parle s'ils ont des répliques ({@code talk}). */
    public List<VillagerData> villagers = new ArrayList<>();

    /** Un habitant : un modèle (models.json) qui suit un parcours en boucle à la vitesse donnée (m/s). */
    public static class VillagerData {
        public String model;
        /** Ses répliques : un habitant de npcs.json. Absent, il ne fait que passer. */
        public String talk;
        public float speed = 1.2f;
        public List<RoutePoint> route = new ArrayList<>();
    }

    public static class RoutePoint {
        public float x;
        public float z;
    }

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
        /** Forme : box (par defaut), cylinder, sphere, cone ou gable (toit a deux pentes, crete le long de X). Sauf box, visuel seulement. */
        public String shape = "box";
        /** Rotation en degres autour de x, y, z (facultatif ; visuel seulement). */
        public float[] rot;
        /** « water » : la surface ondule doucement. */
        public String anim;
        /**
         * Pour l'action « read » : l'identifiant du texte à lire dans lore.json (panneau, stèle, livre...).
         * Pour l'action « story » : le point d'histoire de story.json (« cairn »...) dont les dialogues se tiennent ici.
         * Pour l'action « walk » (monde ouvert, prototype) : le lieu où l'on part à pied (un lieu de openworld.json).
         */
        public String text;
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
            if (p.action != null && !java.util.Arrays.asList("gate", "read", "board", "story", "walk").contains(p.action)) {
                throw new IllegalStateException(at + " : \"action\" inconnue \"" + p.action + "\" (attendu : gate, read, board, story ou walk)");
            }
            if ("walk".equals(p.action) && (p.text == null || p.text.isEmpty())) throw new IllegalStateException(at + " : l'action \"walk\" demande un \"text\" (le lieu ou l'on part a pied, voir openworld.json)");
            if ("read".equals(p.action) && (p.text == null || p.text.isEmpty())) throw new IllegalStateException(at + " : l'action \"read\" demande un \"text\" (un identifiant de lore.json)");
            if ("story".equals(p.action) && (p.text == null || p.text.isEmpty())) throw new IllegalStateException(at + " : l'action \"story\" demande un \"text\" (un point de story.json)");
            if (p.w <= 0 || p.h <= 0 || p.d <= 0) throw new IllegalStateException(at + " : \"w\", \"h\" et \"d\" doivent etre > 0");
            if (p.shape == null || !java.util.Arrays.asList("box", "cylinder", "sphere", "cone", "gable").contains(p.shape)) {
                throw new IllegalStateException(at + " : \"shape\" inconnue \"" + p.shape + "\" (attendu : box, cylinder, sphere, cone ou gable)");
            }
            if (p.rot != null && p.rot.length != 3) throw new IllegalStateException(at + " : \"rot\" doit avoir 3 nombres [x, y, z]");
            if (p.anim != null && !"water".equals(p.anim)) throw new IllegalStateException(at + " : \"anim\" inconnue \"" + p.anim + "\" (attendu : water)");
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
        if (villagers == null) villagers = new ArrayList<>();
        for (int i = 0; i < villagers.size(); i++) {
            VillagerData v = villagers.get(i);
            String where = file + " : habitant n°" + (i + 1);
            if (v == null || v.model == null || v.model.isEmpty()) throw new IllegalStateException(where + " : \"model\" manquant");
            if (v.speed <= 0) throw new IllegalStateException(where + " : \"speed\" doit etre > 0");
            if (v.route == null || v.route.size() < 2) throw new IllegalStateException(where + " : \"route\" doit avoir au moins 2 points");
        }
    }
}
