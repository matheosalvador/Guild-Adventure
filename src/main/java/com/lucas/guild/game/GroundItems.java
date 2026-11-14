package com.lucas.guild.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.lucas.guild.model.Item;

/**
 * Les objets posés par terre (butin des monstres, objets jetés), par lieu. Gardés en mémoire pour
 * la partie en cours : ils ne sont pas sauvegardés (un objet jeté puis oublié disparaît à la fermeture du jeu).
 */
public final class GroundItems {

    /** Un objet au sol, à une position (x, z) du lieu. */
    public static final class Drop {
        public final Item item;
        public final float x;
        public final float z;

        Drop(Item item, float x, float z) {
            this.item = item;
            this.x = x;
            this.z = z;
        }

        public float distanceTo(float px, float pz) {
            return (float) Math.hypot(px - x, pz - z);
        }
    }

    /** Distance max pour ramasser un objet, en m. */
    public static final float PICKUP_RANGE = 2.5f;

    private final Map<String, List<Drop>> byScene = new HashMap<>();

    /** Pose {@code item} au sol dans la scène {@code scene}. */
    public Drop drop(String scene, Item item, float x, float z) {
        Drop drop = new Drop(item, x, z);
        byScene.computeIfAbsent(scene, k -> new ArrayList<>()).add(drop);
        return drop;
    }

    /** Les objets au sol de la scène (copie). */
    public List<Drop> in(String scene) {
        List<Drop> list = byScene.get(scene);
        return list == null ? new ArrayList<Drop>() : new ArrayList<>(list);
    }

    /** L'objet au sol le plus proche du point, à moins de {@link #PICKUP_RANGE}, ou null. */
    public Drop nearest(String scene, float x, float z) {
        Drop best = null;
        for (Drop d : in(scene)) {
            if (d.distanceTo(x, z) <= PICKUP_RANGE && (best == null || d.distanceTo(x, z) < best.distanceTo(x, z))) {
                best = d;
            }
        }
        return best;
    }

    /** Retire l'objet du sol (il vient d'être ramassé). */
    public void remove(String scene, Drop drop) {
        List<Drop> list = byScene.get(scene);
        if (list != null) {
            list.remove(drop);
        }
    }
}
