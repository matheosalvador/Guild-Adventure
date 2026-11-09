package com.lucas.guild.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.lucas.guild.data.DifficultyData;
import com.lucas.guild.data.GameContent;

/**
 * L'inventaire du personnage : les objets qu'il transporte, son équipement (arme, armure, sac)
 * et les règles de charge.
 *
 * <ul>
 *   <li><b>Poids :</b> tout ce qui est porté compte, équipement et sac compris.</li>
 *   <li><b>Capacité :</b> (poids de base selon la difficulté + capacité du sac porté) × effet de l'âge.
 *       On ne peut pas ramasser ou acheter un objet qui dépasserait la capacité.</li>
 *   <li><b>Encombrement :</b> au-delà d'une part de la capacité, la vitesse de déplacement baisse
 *       progressivement jusqu'à un minimum à pleine charge.</li>
 * </ul>
 */
public class Inventaire {

    /** Un emplacement d'équipement. */
    public enum Slot {
        WEAPON("Arme"), ARMOR("Armure"), BAG("Sac");

        private final String label;

        Slot(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private List<Item> items = new ArrayList<>();
    private Arme weapon;
    private Armure armor;
    private Sac bag;
    private double poidsMax = 25.0; // Poids de base porté sans sac (selon la difficulté)

    // Anciens formats de sauvegarde (une liste par type de potion) : lus puis recopiés dans items
    private List<PotionSoin> potionsSoin;
    private List<PotionDps> potionsDps;
    private List<PotionMix> potionsMix;

    // Recalculés à chaque chargement (âge, difficulté) : pas sauvegardés
    private transient double capacityFactor = 1.0;
    private transient double encumbranceStart = 0.5;
    private transient double minSpeedFactor = 1.0;

    public Inventaire() {
    }

    // ------------------------------------------------------------------ réglages

    /** Applique la difficulté : poids de base et sensibilité à l'encombrement (assets/data/difficulties.json). */
    public void applyDifficulty(String difficulte) {
        DifficultyData data = GameContent.difficulty(difficulte);
        if (data != null) {
            this.poidsMax = data.maxWeight;
            this.encumbranceStart = data.encumbranceStart;
            this.minSpeedFactor = data.minSpeedPercent / 100.0;
        }
    }

    /** Effet de l'âge sur la charge portable (1.0 = aucun effet). */
    public double getCapacityFactor() {
        return capacityFactor;
    }

    /** Applique l'effet de l'âge sur la charge portable (1.0 = aucun effet). */
    public void setCapacityFactor(double capacityFactor) {
        this.capacityFactor = capacityFactor;
    }

    /** Recopie les anciennes listes de potions dans la liste unique. À appeler après un chargement. */
    public void migrateLegacy() {
        if (items == null) {
            items = new ArrayList<>();
        }
        if (potionsSoin != null) items.addAll(potionsSoin);
        if (potionsDps != null) items.addAll(potionsDps);
        if (potionsMix != null) items.addAll(potionsMix);
        potionsSoin = null;
        potionsDps = null;
        potionsMix = null;
    }

    // ------------------------------------------------------------------ poids et capacité

    /** Poids de tout ce qui est porté : objets, équipement et sac. */
    public double getPoidsActuel() {
        double total = 0;
        for (Item item : items) {
            total += item.getPoids();
        }
        if (weapon != null) total += weapon.getPoids();
        if (armor != null) total += armor.getPoids();
        if (bag != null) total += bag.getPoids();
        return total;
    }

    /** Poids porté sans sac, selon la difficulté. */
    public double getPoidsDeBase() {
        return poidsMax;
    }

    /** Charge supplémentaire apportée par le sac porté (0 sans sac). */
    public double getCapaciteSac() {
        return bag != null ? bag.getCapacityBonus() : 0;
    }

    /** Poids maximum réellement portable : (base + sac) réduit par l'âge. */
    public double getPoidsMax() {
        return (poidsMax + getCapaciteSac()) * capacityFactor;
    }

    /** Peut-on porter cet objet en plus sans dépasser la capacité ? */
    public boolean peutPorter(Item item) {
        return getPoidsActuel() + item.getPoids() <= getPoidsMax() + 1e-9;
    }

    /** Poids qu'il faudrait retirer pour pouvoir porter cet objet (0 s'il passe déjà). */
    public double poidsManquantPour(Item item) {
        return Math.max(0, getPoidsActuel() + item.getPoids() - getPoidsMax());
    }

    /**
     * Multiplicateur de vitesse dû à la charge : 1.0 tant qu'on porte moins de la part
     * {@code encumbranceStart} de la capacité, puis décroît linéairement jusqu'au minimum à pleine charge.
     */
    public double getEncumbranceSpeedFactor() {
        double max = getPoidsMax();
        if (max <= 0) {
            return minSpeedFactor;
        }
        double ratio = getPoidsActuel() / max;
        if (ratio <= encumbranceStart) {
            return 1.0;
        }
        double progress = Math.min(1.0, (ratio - encumbranceStart) / (1.0 - encumbranceStart));
        return 1.0 - progress * (1.0 - minSpeedFactor);
    }

    // ------------------------------------------------------------------ objets

    /** @return false (et rien n'est ajouté) si l'objet dépasserait la capacité. */
    public boolean ajouterItem(Item item) {
        if (!peutPorter(item)) {
            return false;
        }
        items.add(item);
        return true;
    }

    public void retirerItem(Item item) {
        items.remove(item);
    }

    /** Les objets transportés (hors équipement porté). Liste en lecture seule. */
    public List<Item> getItems() {
        return Collections.unmodifiableList(items);
    }

    public boolean estVide() {
        return items.isEmpty();
    }

    // ------------------------------------------------------------------ équipement

    public Arme getWeapon() {
        return weapon;
    }

    public Armure getArmor() {
        return armor;
    }

    public Sac getBag() {
        return bag;
    }

    /**
     * Équipe un objet transporté (arme, armure ou sac). L'objet qui occupait l'emplacement retourne
     * dans l'inventaire.
     *
     * @return null si c'est fait, sinon la raison du refus
     */
    public String equip(Item item) {
        if (!items.contains(item)) {
            return "Cet objet n'est pas dans votre inventaire.";
        }
        if (item instanceof Arme) {
            items.remove(item);
            if (weapon != null) items.add(weapon);
            weapon = (Arme) item;
            return null;
        }
        if (item instanceof Armure) {
            items.remove(item);
            if (armor != null) items.add(armor);
            armor = (Armure) item;
            return null;
        }
        if (item instanceof Sac) {
            Sac old = bag;
            items.remove(item);
            if (old != null) items.add(old); // l'ancien sac reste porté, donc compté dans le poids
            bag = (Sac) item;
            // Un sac plus petit peut ne plus suffire pour tout ce qu'on porte
            if (getPoidsActuel() > getPoidsMax() + 1e-9) {
                bag = old;
                if (old != null) items.remove(old);
                items.add(item);
                return "Trop lourd pour ce sac : videz d'abord de quoi porter moins.";
            }
            return null;
        }
        return "Cet objet ne s'équipe pas.";
    }

    /**
     * Retire un équipement et le remet dans l'inventaire.
     *
     * @return null si c'est fait, sinon la raison du refus (ex. sac nécessaire pour porter la charge)
     */
    public String unequip(Slot slot) {
        switch (slot) {
            case WEAPON:
                if (weapon == null) return "Aucune arme équipée.";
                items.add(weapon);
                weapon = null;
                return null;
            case ARMOR:
                if (armor == null) return "Aucune armure équipée.";
                items.add(armor);
                armor = null;
                return null;
            case BAG:
                if (bag == null) return "Aucun sac porté.";
                // Sans le sac, la capacité baisse : on ne peut le poser que si le reste se porte encore
                if (getPoidsActuel() > poidsMax * capacityFactor + 1e-9) {
                    return "Trop lourd : videz d'abord votre sac avant de le poser.";
                }
                items.add(bag);
                bag = null;
                return null;
            default:
                return "Emplacement inconnu.";
        }
    }
}
