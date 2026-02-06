package com.lucas.guild.model;

public interface Item {
    String getName();
    String getDescription();
    void use(Joueur joueur, Monstre monstre);
    double getPoids();

    void utiliser(Adventurer player);
}