package com.lucas.guild.libgdx;

import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.lucas.guild.model.GameClock;

/**
 * Les messages affichés quelques secondes en haut de l'écran (saison, anniversaire, réveil, refus...). Ils viennent
 * de la file de l'horloge ({@link GameClock#pollMessage()}) et passent l'un après l'autre.
 */
final class Toasts {

    private static final float SECONDS = 5f;
    private static final float MIN_SECONDS = 2f;   // durée minimale quand un autre message attend

    private final GlyphLayout layout = new GlyphLayout();
    private String toast;
    private float timer;

    /** Affiche le message en cours (ou le suivant) ; à appeler à chaque image, lot ouvert. */
    void draw(float delta, GameClock clock, SpriteBatch batch, BitmapFont font, float w, float h) {
        if (toast != null) {
            timer -= delta;
            // Si d'autres messages attendent, on ne garde pas celui-ci les 5 s complètes : un refus
            // ("Pas assez d'or") doit apparaître vite, pas après le message précédent.
            boolean shownLongEnough = timer <= SECONDS - MIN_SECONDS;
            if (timer <= 0f || (shownLongEnough && clock.hasPendingMessage())) {
                toast = null;
            }
        }
        if (toast == null) {
            toast = clock.pollMessage(); // un message arrivé est affiché tout de suite
            timer = SECONDS;
        }
        if (toast != null) {
            layout.setText(font, toast);
            font.draw(batch, layout, w / 2f - layout.width / 2f, h - 20);
        }
    }
}
