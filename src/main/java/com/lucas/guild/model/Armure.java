package com.lucas.guild.model;

public class Armure extends BaseItem {
    private String name;
    private int reductionDegats;

    // No-arg constructor for Gson deserialization
    public Armure() {}

    public Armure(String name, int reductionDegats) {
        this.name = name;
        this.reductionDegats = reductionDegats;
    }

    public int getReductionDegats() {
        return reductionDegats;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return "Une armure qui réduit les dégâts de " + reductionDegats;
    }

    @Override
    public void use(Entite user, Entite target) {
        // L'utilisation d'une armure est gérée par l'équipement, pas par une utilisation directe
    }

    @Override
    protected double defaultPoids() {
        return 5.0;
    }

    @Override
    public String toString() {
        return name + " (Défense +" + reductionDegats + ")";
    }
}
