package com.lucas.guild.model;

public class PotionDeSoin implements Item {
    private String name;
    private int healingAmount;

    public PotionDeSoin(String name, int healingAmount) {
        this.name = name;
        this.healingAmount = healingAmount;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return "Restaure " + healingAmount + " points de vie.";
    }

    @Override
    public void use(Entite user, Entite target) {
        if (user != null) {
            user.ajouterVie(healingAmount);
            System.out.println(user.getName() + " utilise " + name + " et récupère " + healingAmount + " PV.");
        }
    }

    @Override
    public double getPoids() {
        return 0.5; // Poids par défaut pour une potion
    }
}
