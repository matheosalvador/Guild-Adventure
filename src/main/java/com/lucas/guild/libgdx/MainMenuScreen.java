package com.lucas.guild.libgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class MainMenuScreen implements Screen {

    private final MainGame game; // Référence à la classe principale du jeu
    private Stage stage;
    private Skin skin;
    private SpriteBatch spriteBatch;
    private BitmapFont font;

    public MainMenuScreen(MainGame game) {
        this.game = game;
        spriteBatch = new SpriteBatch();
        font = new BitmapFont();
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage); // Le stage gère les entrées

        // Création d'un skin simple pour les boutons (nécessaire pour TextButton)
        skin = new Skin();
        skin.add("default-font", font);
        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.font = skin.getFont("default-font");
        skin.add("default", textButtonStyle);

        // Bouton "Start Game"
        TextButton startGameButton = new TextButton("Start Game", skin);
        startGameButton.setWidth(200);
        startGameButton.setHeight(50);
        startGameButton.setPosition(Gdx.graphics.getWidth() / 2 - startGameButton.getWidth() / 2, Gdx.graphics.getHeight() / 2);
        startGameButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new GameScreen(game)); // Changer pour l'écran de jeu
                dispose(); // Libérer les ressources de cet écran
            }
        });
        stage.addActor(startGameButton);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1); // Couleur de fond du menu
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();

        spriteBatch.begin();
        font.draw(spriteBatch, "Main Menu", Gdx.graphics.getWidth() / 2 - 30, Gdx.graphics.getHeight() - 50);
        spriteBatch.end();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        spriteBatch.dispose();
        font.dispose();
    }
}
