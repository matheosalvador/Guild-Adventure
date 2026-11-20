package com.lucas.guild.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Le monde ouvert, tel qu'écrit dans {@code openworld.json} : l'échelle entre la carte et le terrain, les carreaux
 * chargés autour du joueur, les lieux posés dans le monde (villes et zones sauvages), les entrées des lieux fermés
 * (mine, fosse, château : on y entre comme dans une maison) et, au besoin, des points de passage pour les routes.
 * Les routes elles-mêmes sont tracées par le programme le long des liaisons de {@code world.json}.
 * Ne contient que des données ; les règles sont dans {@code game/WorldLayout} et {@code game/Terrain}.
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
    public List<PortalData> portals = new ArrayList<>();
    public List<RoadData> roads = new ArrayList<>();

    /** Un lieu de world.json posé dans le monde, à la position de la carte multipliée par l'échelle. */
    public static class PlaceData {
        public String id;
        /** Demi-côté du carré occupé par le lieu (son sol, ses murs ou sa haie), en m : rien n'y est semé. */
        public float half;
        /** Zone : l'ouverture dans la haie par où passe la route (x, z sur la haie, en coordonnées du lieu). */
        public EntranceData entrance;
    }

    public static class EntranceData {
        public float x;
        public float z;
        public float width;
    }

    /**
     * L'entrée d'un lieu fermé de world.json (mine, fosse, château) : un petit décor posé à la position du lieu, dont
     * la partie d'action « enter » ouvre le lieu, comme une porte de maison.
     */
    public static class PortalData {
        public String id;
        /** Demi-côté du carré occupé par l'entrée, en m. */
        public float half;
        /** Le décor, en coordonnées de l'entrée ; une partie au moins a l'action « enter ». */
        public List<TownData.PartData> decor = new ArrayList<>();
    }

    /**
     * Une route. Dans le fichier : seulement des points de passage ({@code via}) pour une liaison dont le tracé
     * automatique couperait un autre lieu. Les {@code points} sont calculés par {@code game/WorldLayout}.
     */
    public static class RoadData {
        public String from;
        public String to;
        /** Points de passage, en m dans le monde, de {@code from} vers {@code to}. */
        public List<PointData> via = new ArrayList<>();
        /** Le tracé complet, de la porte de {@code from} à celle de {@code to} (calculé, pas lu). */
        public transient List<PointData> points = new ArrayList<>();
    }

    public static class PointData {
        public float x;
        public float z;

        public PointData() {
        }

        public PointData(float x, float z) {
            this.x = x;
            this.z = z;
        }
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

    /** L'entrée {@code id}, ou null. */
    public PortalData portal(String id) {
        for (PortalData p : portals) {
            if (p.id.equals(id)) {
                return p;
            }
        }
        return null;
    }

    /** Ce lieu de world.json est-il dans le monde ouvert, en lieu ou en entrée ? */
    public boolean has(String id) {
        return place(id) != null || portal(id) != null;
    }

    /** Le demi-côté occupé par un lieu ou une entrée. */
    public float halfOf(String id) {
        PlaceData p = place(id);
        return p != null ? p.half : portal(id).half;
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
        if (portals == null) portals = new ArrayList<>();
        if (roads == null) roads = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < places.size(); i++) {
            PlaceData p = places.get(i);
            String where = file + " : lieu n°" + (i + 1);
            if (p == null || p.id == null || p.id.isEmpty()) throw new IllegalStateException(where + " : \"id\" manquant");
            where = file + " : lieu \"" + p.id + "\"";
            WorldData.PlaceData known = world.place(p.id);
            if (known == null) throw new IllegalStateException(where + " : inconnu (voir world.json)");
            if (!seen.add(p.id)) throw new IllegalStateException(where + " : en double");
            if (p.half <= 0) throw new IllegalStateException(where + " : \"half\" doit etre > 0");
            if (WorldData.isZone(known) && p.entrance == null) throw new IllegalStateException(where + " : une zone demande une \"entrance\" (l'ouverture de sa haie)");
            if (p.entrance != null && p.entrance.width <= 0) throw new IllegalStateException(where + " : \"entrance\" : \"width\" doit etre > 0");
        }
        for (int i = 0; i < portals.size(); i++) {
            PortalData p = portals.get(i);
            String where = file + " : entree n°" + (i + 1);
            if (p == null || p.id == null || p.id.isEmpty()) throw new IllegalStateException(where + " : \"id\" manquant");
            where = file + " : entree \"" + p.id + "\"";
            WorldData.PlaceData known = world.place(p.id);
            if (known == null) throw new IllegalStateException(where + " : inconnue (voir world.json)");
            if (!WorldData.isZone(known)) throw new IllegalStateException(where + " : une ville se pose dans \"places\", pas en entree");
            if (!seen.add(p.id)) throw new IllegalStateException(where + " : en double (deja un lieu ou une entree)");
            if (p.half <= 0) throw new IllegalStateException(where + " : \"half\" doit etre > 0");
            if (p.decor == null) p.decor = new ArrayList<>();
            TownData.validateParts(p.decor, where + " : \"decor\"");
            if (doorOf(p) == null) throw new IllegalStateException(where + " : aucune partie du \"decor\" n'a l'action \"enter\" (la porte)");
        }
        // tout le monde de la carte est dans le monde ouvert (sauf les étages cachés, atteints par une porte)
        for (WorldData.PlaceData q : world.places) {
            if (!q.hidden && !seen.contains(q.id)) {
                throw new IllegalStateException(file + " : le lieu \"" + q.id + "\" de world.json n'est ni dans \"places\" ni dans \"portals\"");
            }
        }
        for (int i = 0; i < roads.size(); i++) {
            RoadData r = roads.get(i);
            String where = file + " : route n°" + (i + 1);
            if (r == null) throw new IllegalStateException(where + " : vide");
            if (r.from == null || !has(r.from)) throw new IllegalStateException(where + " : \"from\" doit etre un lieu ou une entree (" + r.from + ")");
            if (r.to == null || !has(r.to)) throw new IllegalStateException(where + " : \"to\" doit etre un lieu ou une entree (" + r.to + ")");
            if (!isLink(world, r.from, r.to)) throw new IllegalStateException(where + " : " + r.from + " et " + r.to + " ne sont pas relies dans world.json (\"links\")");
            if (r.via == null) r.via = new ArrayList<>();
            r.points = new ArrayList<>();
        }
    }

    /** La porte d'une entrée : sa partie d'action « enter », ou null. */
    public static TownData.PartData doorOf(PortalData portal) {
        for (TownData.PartData part : portal.decor) {
            if ("enter".equals(part.action)) {
                return part;
            }
        }
        return null;
    }

    /** Les points de passage écrits pour la liaison {@code a} - {@code b}, dans le sens de a vers b (vide s'il n'y en a pas). */
    public List<PointData> viaFor(String a, String b) {
        for (RoadData r : roads) {
            if (r.from.equals(a) && r.to.equals(b)) {
                return r.via;
            }
            if (r.from.equals(b) && r.to.equals(a)) {
                List<PointData> reversed = new ArrayList<>(r.via);
                java.util.Collections.reverse(reversed);
                return reversed;
            }
        }
        return new ArrayList<>();
    }

    private static boolean isLink(WorldData world, String a, String b) {
        for (List<String> link : world.links) {
            if (link.size() == 2 && (link.get(0).equals(a) && link.get(1).equals(b) || link.get(0).equals(b) && link.get(1).equals(a))) {
                return true;
            }
        }
        return false;
    }
}
