package com.lucas.guild.libgdx;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class DesktopLauncher {
    public static void main(String[] arg) {
        com.lucas.guild.game.DebugMode.enableFromArgs(arg);
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setTitle("Guild Adventure 3D");
        config.setWindowedMode(800, 600);
        config.useVsync(true);
        // On lance maintenant la classe MainGame
        new Lwjgl3Application(new MainGame(), config);
    }
}
