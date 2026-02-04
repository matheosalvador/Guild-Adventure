package com.lucas.guild;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.lucas.guild.libgdx.MainGame;

public class GameLauncher {
    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Guild Adventure - LibGDX");
        config.setWindowedMode(800, 600);
        new Lwjgl3Application(new MainGame(), config);
    }
}
