package com.lucas.guild.libgdx;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.utils.Array;
import com.lucas.guild.game.GroundItems;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Arme;
import com.lucas.guild.model.Armure;
import com.lucas.guild.model.Butin;
import com.lucas.guild.model.Item;
import com.lucas.guild.model.ObjetDeQuete;
import com.lucas.guild.model.Sac;

/**
 * Les objets posés au sol d'un écran (butin d'un monstre, objet jeté, trop-plein d'un coffre) : une petite boîte
 * colorée par objet. Les objets eux-mêmes sont dans {@link GroundItems} (la partie en cours), rangés par lieu.
 */
final class GroundItemViews {

    /** Comment l'écran pose une boîte seulement visible. */
    interface BoxFactory {
        ModelInstance box(float w, float h, float d, Color color, float x, float y, float z);
    }

    private final GroundItems ground;
    private final String scene;
    private final Array<ModelInstance> instances;
    private final BoxFactory boxes;
    private final Map<GroundItems.Drop, ModelInstance> views = new HashMap<>();

    /**
     * @param scene le lieu (voir {@code BaseWorldScreen.sceneKey()}) : les objets jetés ici sont retrouvés ici
     */
    GroundItemViews(GroundItems ground, String scene, Array<ModelInstance> instances, BoxFactory boxes) {
        this.ground = ground;
        this.scene = scene;
        this.instances = instances;
        this.boxes = boxes;
    }

    /** Montre les objets déjà au sol dans ce lieu (on y revient). */
    void showExisting() {
        for (GroundItems.Drop drop : ground.in(scene)) {
            show(drop);
        }
    }

    private static Color colorOf(Item item) {
        if (item instanceof ObjetDeQuete) return Color.GOLD;
        if (item instanceof Butin) return Color.TAN;
        if (item instanceof Arme) return Color.LIGHT_GRAY;
        if (item instanceof Armure || item instanceof Sac) return Color.BROWN;
        if (item.estUtilisableHorsCombat()) return Color.SCARLET;
        return Color.ORANGE;
    }

    private void show(GroundItems.Drop drop) {
        views.put(drop, boxes.box(0.5f, 0.3f, 0.5f, colorOf(drop.item), drop.x, 0.65f, drop.z));
    }

    /** Pose un objet au sol en (x, z). */
    void drop(Item item, float x, float z) {
        show(ground.drop(scene, item, x, z));
    }

    /** L'objet au sol le plus proche du point, à portée de main, ou null. */
    GroundItems.Drop nearest(float x, float z) {
        return ground.nearest(scene, x, z);
    }

    /**
     * Le joueur, en (x, z), ramasse l'objet au sol le plus proche.
     *
     * @return la phrase à afficher, ou null s'il n'y a rien à portée
     */
    String pickUpNearest(Adventurer player, float x, float z) {
        GroundItems.Drop drop = nearest(x, z);
        if (drop == null) {
            return null;
        }
        if (!player.addItem(drop.item)) {
            return "Trop lourd pour ramasser : " + drop.item.getName() + ".";
        }
        ground.remove(scene, drop);
        instances.removeValue(views.remove(drop), true);
        return "Vous ramassez : " + drop.item.getName() + ".";
    }

    void clear() {
        views.clear();
    }
}
