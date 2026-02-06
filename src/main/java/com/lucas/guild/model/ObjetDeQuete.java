package com.lucas.guild.model;

public class ObjetDeQuete implements Item {
    private String name;

    public ObjetDeQuete(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return "Un objet de quête.";
    }

    @Override
    public void use(Joueur joueur, Monstre monstre) {
        // Les objets de quête ne sont généralement pas "utilisables" de cette manière
    }

    @Override
    public double getPoids() {
        return 0.1; // Poids par défaut pour un objet de quête
    }

    /**
     * @param player
     */
    @Override
    public void utiliser(Adventurer player) {

    }
}
