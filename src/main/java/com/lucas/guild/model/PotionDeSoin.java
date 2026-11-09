package com.lucas.guild.model;

public class PotionDeSoin extends BaseItem {
    private String name;
    private int healingAmount;

    // No-arg constructor for Gson deserialization
    public PotionDeSoin() {
        this.name = "";
        this.healingAmount = 0;
    }

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
    public boolean estUtilisableHorsCombat() {
        return true;
    }

    @Override
    protected double defaultPoids() {
        return 0.5;
    }
}