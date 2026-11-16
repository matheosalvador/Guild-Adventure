package com.lucas.guild.libgdx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.lucas.guild.game.Settings;

/**
 * Le menu Options (depuis le menu principal ou la pause) : sensibilité de la souris, vue au lancement, ombres, plein
 * écran. Chaque changement s'applique tout de suite et s'enregistre dans {@code settings.json}.
 */
final class OptionsPanel {

    private final Table table;
    private final Skin skin;
    private final Runnable onBack;
    /** Le dernier enregistrement a-t-il échoué ? (le réglage vaut quand même pour la session) */
    private boolean saveFailed;

    /** @param onBack le bouton « Retour » */
    OptionsPanel(Table table, Skin skin, Runnable onBack) {
        this.table = table;
        this.skin = skin;
        this.onBack = onBack;
    }

    void rebuild() {
        table.clear();
        final Settings s = Settings.current();
        Label title = UiKit.label(skin, "Options");
        title.setColor(Color.GOLD);
        table.add(title).colspan(3).pad(10).row();

        table.add(UiKit.button(skin, "  -  ", () -> changed(() -> s.changeSensitivity(-1)))).pad(10);
        table.add(UiKit.label(skin, "Sensibilité de la souris : " + s.sensitivityLabel())).pad(10);
        table.add(UiKit.button(skin, "  +  ", () -> changed(() -> s.changeSensitivity(1)))).pad(10).row();

        table.add(UiKit.button(skin, "Vue au lancement : " + (s.thirdPerson ? "3e personne" : "1re personne"), () -> changed(() -> {
            s.thirdPerson = !s.thirdPerson;
            ViewSettings.thirdPerson = s.thirdPerson;   // choisie maintenant : elle vaut aussi tout de suite
        }))).colspan(3).pad(10).row();
        table.add(UiKit.button(skin, "Ombres : " + (s.shadows ? "oui" : "non"), () -> changed(() -> s.shadows = !s.shadows)))
                .colspan(3).pad(10).row();
        table.add(UiKit.button(skin, "Plein écran : " + (s.fullscreen ? "oui" : "non"), () -> changed(() -> {
            s.fullscreen = !s.fullscreen;
            ViewSettings.applyFullscreen(s.fullscreen);
        }))).colspan(3).pad(10).row();

        if (saveFailed) {
            table.add(UiKit.message(skin, "Réglages non enregistrés (voir la console) : ils valent pour cette session.")).colspan(3).pad(5).row();
        }
        table.add(UiKit.button(skin, "Retour", onBack)).colspan(3).pad(20).row();
    }

    private void changed(Runnable change) {
        change.run();
        saveFailed = !Settings.current().save();
        rebuild();
    }
}
