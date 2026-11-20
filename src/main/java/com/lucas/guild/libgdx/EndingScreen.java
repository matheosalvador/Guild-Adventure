package com.lucas.guild.libgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.lucas.guild.game.GameSession;
import com.lucas.guild.game.SaveManager;
import com.lucas.guild.game.Story;

/**
 * L'écran de fin : l'épilogue de la fin que le joueur a choisie, sur fond noir. « Continuer à jouer » sauvegarde et
 * ramène dans le monde, là où l'on était : l'histoire est finie, la partie continue.
 */
public class EndingScreen implements Screen {

    private final MainGame game;
    private final String difficulty;

    private Stage stage;
    private Skin skin;
    private BitmapFont font;

    public EndingScreen(MainGame game, String difficulty) {
        this.game = game;
        this.difficulty = difficulty;
    }

    @Override
    public void show() {
        Gdx.input.setCursorCatched(false);
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        font = new BitmapFont();
        skin = new Skin();
        skin.add("default-font", font);
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = font;
        skin.add("default", buttonStyle);
        skin.add("default", new Label.LabelStyle(font, Color.WHITE));

        final GameSession session = game.getSession();
        final Story story = new Story(session);
        final Story.Text text = story.nextText();

        Table table = new Table(skin);
        table.setFillParent(true);
        stage.addActor(table);
        if (text != null) {
            Label title = UiKit.label(skin, text.title);
            title.setColor(Color.GOLD);
            table.add(title).pad(20).row();
            Label body = UiKit.label(skin, text.body);
            body.setWrap(true);
            body.setAlignment(Align.center);
            table.add(body).width(Math.max(300f, Math.min(620f, Gdx.graphics.getWidth() - 120f))).pad(10).row();
        }
        table.add(UiKit.button(skin, "Continuer à jouer", new Runnable() {
            @Override
            public void run() {
                if (text != null) {
                    story.markShown(text);
                }
                // on rejoue là où l'on était : dans le lieu fermé (la crypte...), ou à l'arrivée du lieu dans le monde ouvert
                String here = session.getPlayer().getTownId();
                if (!com.lucas.guild.game.WorldLayout.standard().contains(here)) {
                    com.lucas.guild.data.TownData.Point3 a = com.lucas.guild.data.GameContent.arrival(here);
                    new SaveManager().saveGame(session.getPlayer(), session.getClock(), difficulty, a.x, a.y, a.z, "Zone");
                    game.setScreen(new ZoneScreen(game, difficulty, null));
                    return;
                }
                float[] w = MainGame.arrivalPoint(here);
                new SaveManager().saveGame(session.getPlayer(), session.getClock(), difficulty, w[0], 2f, w[1], OpenWorldScreen.SAVE_SCREEN);
                game.setScreen(game.arrivalIn(here, difficulty));
            }
        })).padTop(30).row();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.02f, 0.02f, 0.03f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(Math.min(delta, 1 / 30f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        if (stage != null) {
            stage.dispose();
            skin.dispose();
            stage = null;
        }
    }
}
