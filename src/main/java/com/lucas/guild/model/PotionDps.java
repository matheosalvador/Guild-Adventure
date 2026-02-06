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
    public void use(Joueur joueur, Monstre monstre) {
        if (monstre != null) {
            monstre.perdreVie(degats);
            System.out.println("Vous lancez une " + name + ". " + monstre.getName() + " subit " + degats + " dégâts !");
        }
    }

    @Override
    public double getPoids() {
        return 0.7;
    }

    /**
     * @param player
     */
    @Override
    public void utiliser(Adventurer player) {

    }
}