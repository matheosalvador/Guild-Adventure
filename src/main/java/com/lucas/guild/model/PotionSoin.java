package com.lucas.guild.model;

public class PotionSoin implements Item {
    private String name = "Potion de Soin";
    private String description = "Restaure 25 points de vie.";
    private int soin = 25;

    // No-arg constructor for Gson deserialization
    public PotionSoin() {}

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void use(Entite user, Entite target) {
        if (user != null) {
            user.ajouterVie(soin);
            System.out.println(user.getName() + " utilise une " + name + " et récupère " + soin + " PV.");
        }
    }

    @Override
    public double getPoids() {
        return 0.5;
    }
}