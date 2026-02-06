package com.lucas.guild.libgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

public class MainMenuScreen implements Screen {

    private final MainGame game;
    private Stage stage;
    private Skin skin;
    private BitmapFont font;

    // Tables pour les bouttons
    private Table mainTable;
    private Table difficultyTable;

    public MainMenuScreen(MainGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        // skin peut etre a modifier plus tard
        skin = new Skin();
        font = new BitmapFont();
        skin.add("default-font", font);

        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.font = skin.getFont("default-font");
        skin.add("default", textButtonStyle);

        // Table Menu P
        mainTable = new Table();
        mainTable.setFillParent(true);
        stage.addActor(mainTable);

        TextButton startButton = new TextButton("Start", skin);
        TextButton settingsButton = new TextButton("Settings", skin);
        TextButton creditsButton = new TextButton("Credits", skin);
        TextButton exitButton = new TextButton("Exit", skin);

        mainTable.add(startButton).pad(10).row();
        mainTable.add(settingsButton).pad(10).row();
        mainTable.add(creditsButton).pad(10).row();
        mainTable.add(exitButton).pad(10).row();

        // Table Difficulté
        difficultyTable = new Table();
        difficultyTable.setFillParent(true);
        difficultyTable.setVisible(false); // de base on met a faux pour ne pas l'afficher
        stage.addActor(difficultyTable);

        TextButton easyButton = new TextButton("Facile", skin);
        TextButton normalButton = new TextButton("Normale", skin);
        TextButton realisticButton = new TextButton("Realiste", skin);
        TextButton backButton = new TextButton("Retour", skin);

        difficultyTable.add(easyButton).pad(10).row();
        difficultyTable.add(normalButton).pad(10).row();
        difficultyTable.add(realisticButton).pad(10).row();
        difficultyTable.add(backButton).pad(20).row();

        // Clikeable Boutton logique

        // Bouton Start to Affiche le menu de difficulté
        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                mainTable.setVisible(false);
                difficultyTable.setVisible(true);
            }
        });

        // Boutons de difficulté real start a intergrer la save qui Lancent le jeu
        ClickListener startGameListener = new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // les difficulté ne change rien
                game.setScreen(new TownScreen(game)); // Changé pour aller à TownScreen
            }
        };
        easyButton.addListener(startGameListener);
        normalButton.addListener(startGameListener);
        realisticButton.addListener(startGameListener);

        // Bouton Retour qui Affiche le menu principal
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                difficultyTable.setVisible(false);
                mainTable.setVisible(true);
            }
        });
        
        // Boutons non implémentés et a faire plus tard les tables
        settingsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("MENU", "Settings cliqué (non implémenté)");
            }
        });
        creditsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("MENU", "Credits cliqué (non implémenté)");
            }
        });

        // Bouton Exit qui Ferme le jeu
        exitButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.exit();
            }
        });
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(Math.min(Gdx.graphics.getDeltaTime(), 1 / 30f));
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() {
        // la liberation des ressource quand in quitte
        dispose();
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void dispose() {
        stage.dispose();
        skin.dispose();
        font.dispose();
    }
}
