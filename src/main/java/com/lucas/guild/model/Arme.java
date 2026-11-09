package com.lucas.guild.model;

public class Arme extends BaseItem {
    private String name;
    private int bonusDegats;

    // No-arg constructor for Gson deserialization
    public Arme() {}

    public Arme(String name, int bonusDegats) {
        this.name = name;
        this.bonusDegats = bonusDegats;
    }

    public int getBonusDegats() {
        return bonusDegats;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return "Une arme qui augmente les dégâts de " + bonusDegats;
    }

    @Override
    public void use(Entite user, Entite target) {
        // L'utilisation d'une arme est gérée par l'équipement, pas par une utilisation directe
    }

    @Override
    protected double defaultPoids() {
        return 2.0;
    }

    @Override
    public String toString() {
        return name + " (Dégâts +" + bonusDegats + ")";
    }
}
