package com.lucas.guild.game;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import com.lucas.guild.model.GroundDrop;
import com.lucas.guild.model.Item;

/**
 * Les objets posés par terre (butin des monstres, objets jetés), par lieu. Ils sont rangés dans le personnage, donc
 * sauvegardés avec lui, et disparaissent au bout de {@link #LIFETIME_DAYS} jours de jeu (sinon la sauvegarde
 * grossirait sans fin dans un monde qu'on traverse et retraverse).
 */
public final class GroundItems {

    /** Distance max pour ramasser un objet, en m. */
    public static final float PICKUP_RANGE = 2.5f;
    /** Jours de jeu avant qu'un objet laissé au sol disparaisse. */
    public static final int LIFETIME_DAYS = 7;

    private final Supplier<List<GroundDrop>> store;
    private final IntSupplier today;

    /** Des objets au sol rangés dans {@code store} (la liste du personnage), datés par {@code today}. */
    public GroundItems(Supplier<List<GroundDrop>> store, IntSupplier today) {
        this.store = store;
        this.today = today;
    }

    /** Des objets au sol rien qu'en mémoire, au jour 0 (pour les tests). */
    public GroundItems() {
        List<GroundDrop> own = new ArrayList<>();
        this.store = () -> own;
        this.today = () -> 0;
    }

    /** Pose {@code item} au sol dans la scène {@code scene}, aujourd'hui. */
    public GroundDrop drop(String scene, Item item, float x, float z) {
        GroundDrop drop = new GroundDrop(scene, item, x, z, today.getAsInt());
        store.get().add(drop);
        return drop;
    }

    /** Les objets au sol de la scène (copie), après avoir retiré ceux qui y sont depuis trop longtemps. */
    public List<GroundDrop> in(String scene) {
        expire();
        List<GroundDrop> list = new ArrayList<>();
        for (GroundDrop d : store.get()) {
            if (scene.equals(d.scene)) {
                list.add(d);
            }
        }
        return list;
    }

    /** L'objet au sol le plus proche du point, à moins de {@link #PICKUP_RANGE}, ou null. */
    public GroundDrop nearest(String scene, float x, float z) {
        GroundDrop best = null;
        for (GroundDrop d : in(scene)) {
            if (d.distanceTo(x, z) <= PICKUP_RANGE && (best == null || d.distanceTo(x, z) < best.distanceTo(x, z))) {
                best = d;
            }
        }
        return best;
    }

    /** Retire l'objet du sol (il vient d'être ramassé). */
    public void remove(String scene, GroundDrop drop) {
        store.get().remove(drop);
    }

    /** Retire les objets posés il y a {@link #LIFETIME_DAYS} jours ou plus (et ceux d'une sauvegarde abîmée). */
    private void expire() {
        int now = today.getAsInt();
        for (Iterator<GroundDrop> it = store.get().iterator(); it.hasNext();) {
            GroundDrop d = it.next();
            if (d == null || d.item == null || d.scene == null || now - d.day >= LIFETIME_DAYS) {
                it.remove();
            }
        }
    }
}
