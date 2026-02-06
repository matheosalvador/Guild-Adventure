package com.lucas.guild.model;

public interface Item {
    String getName();
    String getDescription();
    void use(Entite user, Entite target);
    double getPoids();
}
