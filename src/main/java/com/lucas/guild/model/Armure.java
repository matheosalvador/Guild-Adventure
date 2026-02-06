package com.lucas.guild.model;

public abstract class Armure implements Item {
    private String name;
    private int reductionDegats;

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
    public void use(Joueur joueur, Monstre monstre) {
        // L'utilisation d'une armure est gérée par l'équipement, pas par une utilisation directe
    }

    @Override
    public double getPoids() {
        return 5.0; // Poids par défaut pour une armure
    }

    @Override
    public String toString() {
        return name + " (Défense +" + reductionDegats + ")";
    }
}
