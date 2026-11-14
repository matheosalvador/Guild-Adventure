package com.lucas.guild.model;

import java.util.Locale;

/**
 * Un sac : une fois porté, il augmente la charge que le personnage peut transporter
 * (son propre poids compte dans la charge).
 */
public class Sac extends BaseItem {

    private String name;
    /** Charge supplémentaire en kg quand le sac est porté. */
    private double capacityBonus;

    // No-arg constructor for Gson deserialization
    public Sac() {
    }

    public Sac(String name, double capacityBonus) {
        this.name = name;
        this.capacityBonus = capacityBonus;
    }

    public double getCapacityBonus() {
        return capacityBonus;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return String.format(Locale.FRANCE, "Un sac : +%.0f kg de charge quand il est porté.", capacityBonus);
    }

    @Override
    public void use(Entite user, Entite target) {
        // Un sac se porte (équipement), il ne s'utilise pas directement
    }

    @Override
    protected double defaultPoids() {
        return 1.0;
    }

    @Override
    public String toString() {
        return name + " (+" + capacityBonus + " kg)";
    }
}
