package com.lucas.guild.libgdx;

import com.badlogic.gdx.Screen;

/** Un bâtiment d'une ville (town*.json) visé dans le monde ouvert, avec l'intérieur qu'il ouvre. */
final class TownBuilding {

    final String name;
    final String interior;
    final String shop;

    TownBuilding(String name, String interior, String shop) {
        this.name = name;
        this.interior = interior;
        this.shop = shop;
    }

    /** L'écran d'intérieur du bâtiment (« forge », « guild », « inn » ou « store », voir town*.json). */
    Screen interiorScreen(MainGame game, String difficulty) {
        switch (interior) {
            case "forge":
                return new ForgeScreen(game, difficulty);
            case "guild":
                return new GuildScreen(game, difficulty);
            case "inn":
                return new InnScreen(game, difficulty);
            case "store":
                return new StoreScreen(game, difficulty, name, shop);
            default:
                throw new IllegalStateException("interieur inconnu : " + interior);
        }
    }
}
