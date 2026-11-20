package com.lucas.guild.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.OpenWorldData;
import com.lucas.guild.data.TownData;
import com.lucas.guild.data.WorldData;

/**
 * Où sont les choses dans le monde ouvert, en mètres, sans affichage : la position des lieux (celle de la carte,
 * multipliée par l'échelle), le passage entre les coordonnées d'un lieu et celles du monde, le lieu où se trouve un
 * point, la carte, les routes et les carreaux. Le nord (z négatif) est en haut de la carte, comme dans le monde.
 *
 * <p>Les routes sont tracées ici, une par liaison de {@code world.json} : de la porte d'un lieu (la porte de la
 * ville, l'ouverture de la haie d'une zone, la porte d'une entrée), quelques mètres droit devant, puis autour des
 * murs par les coins s'il le faut, jusqu'à la porte de l'autre lieu. Les points de passage de {@code openworld.json}
 * ({@code via}) s'insèrent au milieu.</p>
 */
public final class WorldLayout {

    /** Distance entre le bord d'un lieu et la route qui en fait le tour, en m. */
    public static final float ROAD_CLEARANCE = 5f;

    private final OpenWorldData data;
    private final WorldData world;
    private final List<OpenWorldData.RoadData> roads;

    public WorldLayout(OpenWorldData data, WorldData world) {
        this.data = data;
        this.world = world;
        this.roads = traceRoads();
    }

    /** La disposition des fichiers du jeu (openworld.json et world.json). */
    public static WorldLayout standard() {
        return new WorldLayout(GameContent.openWorld(), GameContent.world());
    }

    public OpenWorldData data() {
        return data;
    }

    /** Ce lieu est-il posé dans le monde ouvert (ville ou zone, pas une entrée) ? */
    public boolean contains(String placeId) {
        return data.place(placeId) != null;
    }

    /** Le centre du lieu (de l'entrée, du site) dans le monde, en m. */
    public float[] origin(String placeId) {
        OpenWorldData.SiteData site = data.site(placeId);
        if (site != null) {
            return new float[] {site.x, site.z};
        }
        WorldData.PlaceData place = world.place(placeId);
        return new float[] {place.x * data.scale, place.z * data.scale};
    }

    /** Un point d'un lieu (coordonnées de son fichier) dans le monde. */
    public float[] toWorld(String placeId, float x, float z) {
        float[] o = origin(placeId);
        return new float[] {o[0] + x, o[1] + z};
    }

    /** Un point du monde dans les coordonnées d'un lieu. */
    public float[] toLocal(String placeId, float x, float z) {
        float[] o = origin(placeId);
        return new float[] {x - o[0], z - o[1]};
    }

    /** Le lieu dont le carré contient ce point du monde, ou null (en pleine campagne, ou devant une entrée). */
    public String placeAt(float x, float z) {
        for (OpenWorldData.PlaceData p : data.places) {
            float[] o = origin(p.id);
            if (Math.abs(x - o[0]) <= p.half && Math.abs(z - o[1]) <= p.half) {
                return p.id;
            }
        }
        return null;
    }

    /** La distance entre ce point et le bord du carré du lieu ou de l'entrée (0 dedans). */
    public float distanceToPlace(String placeId, float x, float z) {
        float half = data.halfOf(placeId);
        float[] o = origin(placeId);
        float dx = Math.max(0f, Math.abs(x - o[0]) - half);
        float dz = Math.max(0f, Math.abs(z - o[1]) - half);
        return (float) Math.hypot(dx, dz);
    }

    /** Tous les identifiants posés dans le monde : lieux, entrées, puis sites. */
    public List<String> footprints() {
        List<String> ids = new ArrayList<>();
        for (OpenWorldData.PlaceData p : data.places) {
            ids.add(p.id);
        }
        for (OpenWorldData.PortalData p : data.portals) {
            ids.add(p.id);
        }
        for (OpenWorldData.SiteData s : data.sites) {
            ids.add(s.id);
        }
        return ids;
    }

    /** Le site dont le carré contient ce point du monde, ou null. */
    public OpenWorldData.SiteData siteAt(float x, float z) {
        for (OpenWorldData.SiteData s : data.sites) {
            if (Math.abs(x - s.x) <= s.half && Math.abs(z - s.z) <= s.half) {
                return s;
            }
        }
        return null;
    }

    /** Ce point du monde sur la carte, en km. */
    public float[] toMap(float x, float z) {
        return new float[] {x / data.scale, z / data.scale};
    }

    /** Les routes du monde, tracées (une par liaison de world.json). */
    public List<OpenWorldData.RoadData> roads() {
        return roads;
    }

    /** La route entre deux lieux (dans le sens de {@code from} vers {@code to}), ou null s'ils ne sont pas reliés. */
    public OpenWorldData.RoadData road(String from, String to) {
        for (OpenWorldData.RoadData r : roads) {
            if (r.from.equals(from) && r.to.equals(to)) {
                return r;
            }
            if (r.from.equals(to) && r.to.equals(from)) {
                OpenWorldData.RoadData back = new OpenWorldData.RoadData();
                back.from = from;
                back.to = to;
                back.points = new ArrayList<>(r.points);
                Collections.reverse(back.points);
                return back;
            }
        }
        return null;
    }

    /** La distance entre ce point et la route la plus proche (son axe), en m. */
    public float distanceToRoad(float x, float z) {
        float best = Float.MAX_VALUE;
        for (OpenWorldData.RoadData road : roads) {
            List<OpenWorldData.PointData> pts = road.points;
            for (int i = 0; i + 1 < pts.size(); i++) {
                best = Math.min(best, distanceToSegment(x, z, pts.get(i), pts.get(i + 1)));
            }
        }
        return best;
    }

    /** La longueur d'une route, en m. */
    public static float length(OpenWorldData.RoadData road) {
        float total = 0f;
        for (int i = 0; i + 1 < road.points.size(); i++) {
            OpenWorldData.PointData a = road.points.get(i);
            OpenWorldData.PointData b = road.points.get(i + 1);
            total += (float) Math.hypot(b.x - a.x, b.z - a.z);
        }
        return total;
    }

    /** Le numéro de carreau d'une coordonnée (x ou z) du monde. */
    public int tileOf(float v) {
        return (int) Math.floor(v / data.tileSize);
    }

    /** Où l'on arrive dans le monde en sortant d'un lieu fermé par son entrée : devant sa porte, en coordonnées du monde. */
    public float[] portalExit(String portalId) {
        Door door = door(portalId);
        return new float[] {door.inner[0], door.inner[1]};
    }

    /** L'entrée posée dans le monde pour ce lieu fermé : lui-même, ou celle à la même position (un étage caché). */
    public String portalFor(String placeId) {
        if (data.portal(placeId) != null) {
            return placeId;
        }
        WorldData.PlaceData place = world.place(placeId);
        if (place == null) {
            return null;
        }
        for (OpenWorldData.PortalData p : data.portals) {
            WorldData.PlaceData q = world.place(p.id);
            if (q.x == place.x && q.z == place.z) {
                return p.id;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ tracé des routes

    /** La porte d'un lieu : le point juste devant, et le point de départ de la route, à l'écart des murs. */
    private static final class Door {
        /** Juste devant la porte (dans le monde). */
        final float[] inner;
        /** Droit devant, au-delà du bord du lieu (dans le monde). */
        final float[] out;

        Door(float[] inner, float[] out) {
            this.inner = inner;
            this.out = out;
        }
    }

    private Door door(String id) {
        float[] o = origin(id);
        float ext = data.halfOf(id) + ROAD_CLEARANCE;
        float gx;
        float gz;
        float ix;
        float iz;
        OpenWorldData.PlaceData place = data.place(id);
        if (place != null && place.entrance != null) {
            // une zone : la route s'arrête juste derrière l'ouverture de la haie, où commence le chemin de la zone
            gx = place.entrance.x;
            gz = place.entrance.z;
            float[] dir = outward(gx, gz);
            ix = gx - dir[0];
            iz = gz - dir[1];
        } else {
            TownData.PartData gate = place != null ? gateOf(GameContent.town(id), id) : OpenWorldData.doorOf(data.portal(id));
            gx = gate.x;
            gz = gate.z;
            float[] dir = outward(gx, gz);
            ix = gx + dir[0] * 2f;
            iz = gz + dir[1] * 2f;
        }
        float[] dir = outward(gx, gz);
        float ox = dir[0] != 0 ? dir[0] * ext : gx;
        float oz = dir[1] != 0 ? dir[1] * ext : gz;
        return new Door(new float[] {o[0] + ix, o[1] + iz}, new float[] {o[0] + ox, o[1] + oz});
    }

    private static TownData.PartData gateOf(TownData town, String id) {
        for (TownData.PartData part : town.decor) {
            if ("gate".equals(part.action)) {
                return part;
            }
        }
        throw new IllegalStateException("ville \"" + id + "\" : pas de porte (action \"gate\") pour la route du monde ouvert");
    }

    /** Le côté du lieu où se trouve ce point : (±1, 0) ou (0, ±1). */
    private static float[] outward(float x, float z) {
        if (Math.abs(z) >= Math.abs(x)) {
            return new float[] {0f, Math.signum(z) != 0 ? Math.signum(z) : 1f};
        }
        return new float[] {Math.signum(x), 0f};
    }

    private List<OpenWorldData.RoadData> traceRoads() {
        List<OpenWorldData.RoadData> traced = new ArrayList<>();
        for (List<String> link : world.links) {
            String a = link.get(0);
            String b = link.get(1);
            if (!data.has(a) || !data.has(b)) {
                continue;   // un étage caché : on y va par une porte, pas par la route
            }
            OpenWorldData.RoadData road = new OpenWorldData.RoadData();
            road.from = a;
            road.to = b;
            road.points = trace(a, b, data.viaFor(a, b));
            traced.add(road);
        }
        return traced;
    }

    private List<OpenWorldData.PointData> trace(String a, String b, List<OpenWorldData.PointData> via) {
        Door da = door(a);
        Door db = door(b);
        float[] towardB = via.isEmpty() ? db.out : new float[] {via.get(0).x, via.get(0).z};
        List<float[]> aSide = around(a, da.out, towardB);
        float[] lastA = aSide.isEmpty() ? da.out : aSide.get(aSide.size() - 1);
        float[] towardA = via.isEmpty() ? lastA : new float[] {via.get(via.size() - 1).x, via.get(via.size() - 1).z};
        List<float[]> bSide = around(b, db.out, towardA);
        Collections.reverse(bSide);

        List<OpenWorldData.PointData> points = new ArrayList<>();
        add(points, da.inner);
        add(points, da.out);
        for (float[] p : aSide) {
            add(points, p);
        }
        for (OpenWorldData.PointData p : via) {
            add(points, new float[] {p.x, p.z});
        }
        for (float[] p : bSide) {
            add(points, p);
        }
        add(points, db.out);
        add(points, db.inner);
        return points;
    }

    private static void add(List<OpenWorldData.PointData> points, float[] p) {
        points.add(new OpenWorldData.PointData(p[0], p[1]));
    }

    /**
     * Les coins par où passer pour aller de {@code start} (sur le tour du lieu) vers {@code target} sans traverser le
     * lieu : aucun si la ligne droite est libre, sinon le plus court des deux sens.
     */
    private List<float[]> around(String id, float[] start, float[] target) {
        float[] o = origin(id);
        float e = data.halfOf(id) + ROAD_CLEARANCE;
        // les coins du tour, dans l'ordre : (+e,+e), (-e,+e), (-e,-e), (+e,-e)
        float[][] corners = {{o[0] + e, o[1] + e}, {o[0] - e, o[1] + e}, {o[0] - e, o[1] - e}, {o[0] + e, o[1] - e}};
        int side = sideOf(o, e, start);   // le côté k va du coin k au coin k + 1
        List<float[]> best = null;
        float bestLength = Float.MAX_VALUE;
        for (int sense = 0; sense < 2; sense++) {
            List<float[]> path = new ArrayList<>();
            float[] cur = start;
            for (int i = 0; i < 4 && crosses(o, e, cur, target); i++) {
                int k = sense == 0 ? (side + 1 + i) % 4 : ((side - i) % 4 + 4) % 4;
                cur = corners[k];
                path.add(cur);
            }
            float length = 0f;
            float[] prev = start;
            for (float[] p : path) {
                length += (float) Math.hypot(p[0] - prev[0], p[1] - prev[1]);
                prev = p;
            }
            length += (float) Math.hypot(target[0] - prev[0], target[1] - prev[1]);
            if (length < bestLength) {
                bestLength = length;
                best = path;
            }
        }
        return best;
    }

    private static int sideOf(float[] o, float e, float[] p) {
        float dx = p[0] - o[0];
        float dz = p[1] - o[1];
        if (Math.abs(dz) >= Math.abs(dx)) {
            return dz > 0 ? 0 : 2;
        }
        return dx < 0 ? 1 : 3;
    }

    /** Le segment p-q passe-t-il à l'intérieur du carré de centre o et de demi-côté e (son bord ne compte pas) ? */
    public static boolean crosses(float[] o, float e, float[] p, float[] q) {
        float inner = e - 0.5f;
        float t0 = 0f;
        float t1 = 1f;
        float[] d = {q[0] - p[0], q[1] - p[1]};
        float[] lo = {o[0] - inner, o[1] - inner};
        float[] hi = {o[0] + inner, o[1] + inner};
        for (int axis = 0; axis < 2; axis++) {
            if (Math.abs(d[axis]) < 1e-6f) {
                if (p[axis] <= lo[axis] || p[axis] >= hi[axis]) {
                    return false;
                }
                continue;
            }
            float ta = (lo[axis] - p[axis]) / d[axis];
            float tb = (hi[axis] - p[axis]) / d[axis];
            t0 = Math.max(t0, Math.min(ta, tb));
            t1 = Math.min(t1, Math.max(ta, tb));
            if (t0 >= t1) {
                return false;
            }
        }
        return true;
    }

    private static float distanceToSegment(float x, float z, OpenWorldData.PointData a, OpenWorldData.PointData b) {
        float dx = b.x - a.x;
        float dz = b.z - a.z;
        float len2 = dx * dx + dz * dz;
        float t = len2 == 0 ? 0 : Math.max(0f, Math.min(1f, ((x - a.x) * dx + (z - a.z) * dz) / len2));
        return (float) Math.hypot(x - (a.x + t * dx), z - (a.z + t * dz));
    }
}
