package com.lucas.guild.model;

/** Un objet de butin (peau, oreille, pièce de ferraille...) : ne sert à rien d'autre qu'à être revendu. */
public class Butin extends BaseItem {
    private String name;
    private String description;

    // Constructeur sans argument pour Gson
    public Butin() {
        this.name = "";
    }

    public Butin(String name, String description) {
        this.name = name;
        this.description = description;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description != null ? description : "À revendre en boutique.";
    }

    @Override
    public void use(Entite user, Entite target) {
        // rien à faire : un butin se revend
    }

    @Override
    protected double defaultPoids() {
        return 0.5;
    }
}
