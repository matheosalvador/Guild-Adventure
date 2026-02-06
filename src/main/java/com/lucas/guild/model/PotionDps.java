package com.lucas.guild.model;

public class PotionDps implements Item {
    private final String name = "Potion de Dégâts";
    private final String description = "Inflige 20 points de dégâts au monstre.";
    private final int degats = 20;

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
        if (target != null) {
            target.perdreVie(degats);
            System.out.println("Vous lancez une " + name + ". " + target.getName() + " subit " + degats + " dégâts !");
        }
    }

    @Override
    public double getPoids() {
        return 0.7;
    }
}
