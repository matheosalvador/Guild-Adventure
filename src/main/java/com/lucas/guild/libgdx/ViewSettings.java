package com.lucas.guild.libgdx;

import com.badlogic.gdx.Gdx;
import com.lucas.guild.game.Settings;

/**
 * Réglages d'affichage de la session de jeu. Les réglages enregistrés (menu Options) sont dans {@link Settings} ;
 * ici, ce qui change pendant la partie sans être enregistré.
 */
final class ViewSettings {

    /** Taille de la fenêtre hors plein écran (celle du lancement). */
    static final int WINDOW_WIDTH = 800;
    static final int WINDOW_HEIGHT = 600;

    /**
     * Vue à la 3e personne (la caméra recule derrière le joueur) au lieu de la 1re personne. Part de la vue choisie
     * dans les Options ; la touche V la bascule pour la session.
     */
    static boolean thirdPerson = Settings.current().thirdPerson;

    private ViewSettings() {
    }

    /** Passe en plein écran (sur l'écran actuel) ou revient à la fenêtre. */
    static void applyFullscreen(boolean fullscreen) {
        if (fullscreen) {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        } else {
            Gdx.graphics.setWindowedMode(WINDOW_WIDTH, WINDOW_HEIGHT);
        }
    }
}
