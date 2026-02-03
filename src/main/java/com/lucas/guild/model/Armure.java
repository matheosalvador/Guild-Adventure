package com.lucas.guild.model;

public class Armure extends Item {
    private int reductionDegats;

    public Armure(String name, int reductionDegats) {
        super(name);
        this.reductionDegats = reductionDegats;
    }

    public int getReductionDegats() {
        return reductionDegats;
    }

    @Override
    public String toString() {
        return name + " (Défense +" + reductionDegats + ")";
    }
}
