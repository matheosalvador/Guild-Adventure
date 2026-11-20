package com.lucas.guild.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.lucas.guild.data.OpenWorldData;

/**
 * La campagne entre les lieux du monde ouvert, fabriquée à la demande, sans fichier : chaque carreau sème ses arbres
 * d'après sa seule position (le même carreau est toujours le même), jamais sur une route ni dans un lieu.
 */
public final class Terrain {

    /** Nombre de tailles d'arbres (les modèles sont partagés par taille). */
    public static final int SIZES = 3;
    /** Écart gardé libre de chaque côté du bord de la route, et autour d'un lieu, en m. */
    public static final float ROAD_MARGIN = 5f;
    public static final float PLACE_MARGIN = 6f;

    private Terrain() {
    }

    /** Un arbre : sa position dans le monde, sa forme (0 = rond, 1 = sapin) et sa taille (0 à SIZES - 1). */
    public static final class Tree {
        public final float x;
        public final float z;
        public final int kind;
        public final int size;

        Tree(float x, float z, int kind, int size) {
            this.x = x;
            this.z = z;
            this.kind = kind;
            this.size = size;
        }
    }

    /** Les arbres du carreau (tx, tz). */
    public static List<Tree> trees(WorldLayout layout, int tx, int tz) {
        OpenWorldData data = layout.data();
        Random random = GameRandom.forPlace(tx, tz);
        List<Tree> trees = new ArrayList<>();
        float x0 = tx * data.tileSize;
        float z0 = tz * data.tileSize;
        for (int i = 0; i < data.treesPerTile; i++) {
            // on tire toujours les quatre nombres : un arbre écarté ne décale pas les suivants
            float x = x0 + random.nextFloat() * data.tileSize;
            float z = z0 + random.nextFloat() * data.tileSize;
            int kind = random.nextInt(2);
            int size = random.nextInt(SIZES);
            if (isFree(layout, x, z)) {
                trees.add(new Tree(x, z, kind, size));
            }
        }
        return trees;
    }

    /** Une chose à ramasser (une herbe, un caillou) : sa position, sa sorte (rang dans « gathering ») et sa clé. */
    public static final class Node {
        public final float x;
        public final float z;
        public final int kind;
        /** Identifie la chose dans la sauvegarde (le jour où on l'a ramassée). */
        public final String key;

        Node(float x, float z, int kind, String key) {
            this.x = x;
            this.z = z;
            this.kind = kind;
            this.key = key;
        }
    }

    /**
     * Ce qu'on peut ramasser sur le carreau (tx, tz) : pour chaque sorte d'openworld.json (« gathering »), en moyenne
     * « perTile » choses, toujours aux mêmes endroits, jamais sur une route ni dans un lieu.
     */
    public static List<Node> gatherables(WorldLayout layout, int tx, int tz) {
        OpenWorldData data = layout.data();
        // une autre graine que les arbres du même carreau
        Random random = GameRandom.forPlace(tx * 7919L + 104729L, tz * 7907L - 1299709L);
        List<Node> nodes = new ArrayList<>();
        float x0 = tx * data.tileSize;
        float z0 = tz * data.tileSize;
        for (int k = 0; k < data.gathering.size(); k++) {
            float perTile = data.gathering.get(k).perTile;
            int count = (int) perTile + (random.nextFloat() < perTile - (int) perTile ? 1 : 0);
            for (int i = 0; i < count; i++) {
                float x = x0 + 4f + random.nextFloat() * (data.tileSize - 8f);
                float z = z0 + 4f + random.nextFloat() * (data.tileSize - 8f);
                if (isFree(layout, x, z)) {
                    nodes.add(new Node(x, z, k, "g:" + tx + ":" + tz + ":" + k + ":" + i));
                }
            }
        }
        return nodes;
    }

    /** Peut-on semer un arbre ici ? Ni sur la route, ni dans un lieu (ou trop près de son bord). */
    public static boolean isFree(WorldLayout layout, float x, float z) {
        if (layout.distanceToRoad(x, z) < layout.data().roadWidth / 2f + ROAD_MARGIN) {
            return false;
        }
        for (String id : layout.footprints()) {
            if (layout.distanceToPlace(id, x, z) < PLACE_MARGIN) {
                return false;
            }
        }
        return true;
    }
}
