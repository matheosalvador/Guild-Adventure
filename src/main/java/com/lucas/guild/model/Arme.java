package com.lucas.guild.model;

public class Arme extends Item {
    private int bonusDegats;

    public Arme(String name, int bonusDegats) {
        super(name);
        this.bonusDegats = bonusDegats;
    }

    public int getBonusDegats() {
        return bonusDegats;
    }

    @Override
    public String toString() {
        return name + " (Dégâts +" + bonusDegats + ")";
    }
}
