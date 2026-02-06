package com.lucas.guild.model;

public class PotionMix implements Item {
    private final String name = "Potion Mixte";
    private final String description = "Restaure 15 PV au joueur et inflige 15 dégâts au monstre.";
    private final int soin = 15;
    private final int degats = 15;

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
            System.out.println(user.getName() + " récupère " + soin + " PV.");
        }
        if (target != null) {
            target.perdreVie(degats);
            System.out.println(target.getName() + " subit " + degats + " dégâts.");
        }
        System.out.println("La fiole de " + name + " se brise.");
    }

    @Override
    public double getPoids() {
        return 1.0;
    }
}
