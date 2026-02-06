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
    public void use(Joueur joueur, Monstre monstre) {
        if (joueur != null) {
            joueur.ajouterVie(healingAmount);
            System.out.println(joueur.getName() + " utilise " + name + " et récupère " + healingAmount + " PV.");
        }
    }

    @Override
    public double getPoids() {
        return 0.5; // Poids par défaut pour une potion
    }

    /**
     * @param player
     */
    @Override
    public void utiliser(Adventurer player) {

    }
}
