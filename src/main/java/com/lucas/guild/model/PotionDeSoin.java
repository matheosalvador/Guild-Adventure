package com.lucas.guild.model;

public class PotionDeSoin extends Item {
    private int healingAmount;

    public PotionDeSoin(String name, int healingAmount) {
        super(name);
        this.healingAmount = healingAmount;
    }

    public void utiliser(Entite cible) {
        System.out.println(cible.getName() + " utilise " + this.name + " et récupère " + this.healingAmount + " PV !");
        cible.ajouterVie(this.healingAmount); // On a besoin de cette méthode dans Entite
    }
}
