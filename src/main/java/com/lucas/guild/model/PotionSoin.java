package com.lucas.guild.model;

public class PotionSoin implements Item {
    private final String name = "Potion de Soin";
    private final String description = "Restaure 25 points de vie.";
    private final int soin = 25;

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
