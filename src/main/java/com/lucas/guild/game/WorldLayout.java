package com.lucas.guild.game;

import java.util.List;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.OpenWorldData;
import com.lucas.guild.data.WorldData;

/**
 * Où sont les choses dans le monde ouvert, en mètres, sans affichage : la position des lieux (celle de la carte,
 * multipliée par l'échelle), le passage entre les coordonnées d'un lieu et celles du monde, le lieu où se trouve un
 * point, la carte, les routes et les carreaux. Le nord (z négatif) est en haut de la carte, comme dans le monde.
 */
public final class WorldLayout {

    private final OpenWorldData data;
    private final WorldData world;

    public WorldLayout(OpenWorldData data, WorldData world) {
        this.data = data;
        this.world = world;
    }

    /** La disposition des fichiers du jeu (openworld.json et world.json). */
    public static WorldLayout standard() {
        return new WorldLayout(GameContent.openWorld(), GameContent.world());
    }

    public OpenWorldData data() {
        return data;
    }

    /** Ce lieu est-il posé dans le monde ouvert ? */
    public boolean contains(String placeId) {
        return data.place(placeId) != null;
    }

    /** Le centre du lieu dans le monde, en m. */
    public float[] origin(String placeId) {
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

    /** Le lieu dont le carré contient ce point du monde, ou null (en pleine campagne). */
    public String placeAt(float x, float z) {
        for (OpenWorldData.PlaceData p : data.places) {
            float[] o = origin(p.id);
            if (Math.abs(x - o[0]) <= p.half && Math.abs(z - o[1]) <= p.half) {
                return p.id;
            }
        }
        return null;
    }

    /** La distance entre ce point et le bord du carré du lieu (0 dedans). */
    public float distanceToPlace(String placeId, float x, float z) {
        OpenWorldData.PlaceData p = data.place(placeId);
        float[] o = origin(placeId);
        float dx = Math.max(0f, Math.abs(x - o[0]) - p.half);
        float dz = Math.max(0f, Math.abs(z - o[1]) - p.half);
        return (float) Math.hypot(dx, dz);
    }

    /** Ce point du monde sur la carte, en km. */
    public float[] toMap(float x, float z) {
        return new float[] {x / data.scale, z / data.scale};
    }

    /** La distance entre ce point et la route la plus proche (son axe), en m. */
    public float distanceToRoad(float x, float z) {
        float best = Float.MAX_VALUE;
        for (OpenWorldData.RoadData road : data.roads) {
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

    private static float distanceToSegment(float x, float z, OpenWorldData.PointData a, OpenWorldData.PointData b) {
        float dx = b.x - a.x;
        float dz = b.z - a.z;
        float len2 = dx * dx + dz * dz;
        float t = len2 == 0 ? 0 : Math.max(0f, Math.min(1f, ((x - a.x) * dx + (z - a.z) * dz) / len2));
        return (float) Math.hypot(x - (a.x + t * dx), z - (a.z + t * dz));
    }
}
