package com.lucas.guild.game;

import com.lucas.guild.model.Building;

public class Forge extends Building {

    public Forge(String name) {
        super(name, "Forge");
    }

    public void craftItem() {
        System.out.println("Vous forgez un objet...");
    }
}
