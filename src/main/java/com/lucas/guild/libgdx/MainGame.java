package com.lucas.guild.libgdx;

import com.badlogic.gdx.Game;

public class MainGame extends Game {

    @Override
    public void create() {
        this.setScreen(new MainMenuScreen(this));
    }

    @Override
    public void render() {
        super.render(); // Important: appelle la méthode render() de l'écran actuel
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
