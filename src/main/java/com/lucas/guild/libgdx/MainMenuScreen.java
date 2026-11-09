package com.lucas.guild.libgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.lucas.guild.game.GameSession;
import com.lucas.guild.game.SaveManager;

public class MainMenuScreen implements Screen {

    private final MainGame game;
    private Stage stage;
    private Skin skin;
    private BitmapFont font;

    // Tables pour les bouttons
    private Table mainTable;
    private Table startTable;
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
        skin.add("default", new Label.LabelStyle(font, Color.WHITE));

        boolean hasSave = new SaveManager().hasSave();

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

        // Table Nouvelle partie / Continuer
        startTable = new Table();
        startTable.setFillParent(true);
        startTable.setVisible(false);
        stage.addActor(startTable);

        TextButton newGameButton = new TextButton("Nouvelle partie", skin);
        TextButton continueButton = new TextButton("Continuer", skin);
        TextButton startBackButton = new TextButton("Retour", skin);
        final Label startMessage = new Label("", skin);
        startMessage.setColor(Color.SALMON);

        if (!hasSave) {
            // Pas de sauvegarde : le bouton est grisé et non cliquable
            continueButton.setColor(Color.GRAY);
            continueButton.setTouchable(Touchable.disabled);
        }

        startTable.add(newGameButton).pad(10).row();
        startTable.add(continueButton).pad(10).row();
        startTable.add(startMessage).pad(5).row();
        startTable.add(startBackButton).pad(20).row();

        // Table Difficulté
        difficultyTable = new Table();
        difficultyTable.setFillParent(true);
        difficultyTable.setVisible(false); // de base on met a faux pour ne pas l'afficher
        stage.addActor(difficultyTable);

        TextButton easyButton = new TextButton("Facile", skin);
        TextButton normalButton = new TextButton("Normale", skin);
        TextButton realisticButton = new TextButton("Realiste", skin);
        TextButton backButton = new TextButton("Retour", skin);

        if (hasSave) {
            Label warning = new Label("Une sauvegarde existe : elle sera remplacee a la premiere sauvegarde.", skin);
            warning.setColor(Color.ORANGE);
            difficultyTable.add(warning).pad(10).row();
        }
        difficultyTable.add(easyButton).pad(10).row();
        difficultyTable.add(normalButton).pad(10).row();
        difficultyTable.add(realisticButton).pad(10).row();
        difficultyTable.add(backButton).pad(20).row();

        // Clikeable Boutton logique

        // Bouton Start : affiche le choix Nouvelle partie / Continuer
        startButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                startMessage.setText("");
                mainTable.setVisible(false);
                startTable.setVisible(true);
            }
        });

        // Nouvelle partie : affiche le choix de difficulté
        newGameButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                startTable.setVisible(false);
                difficultyTable.setVisible(true);
            }
        });

        // Continuer : charge la sauvegarde disque puis reprend la ville à la position sauvegardée
        continueButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                if (game.loadSavedGame()) {
                    GameSession session = game.getSession();
                    Vector3 spawn = session.hasSavedPosition()
                            ? new Vector3(session.getSavedX(), session.getSavedY(), session.getSavedZ())
                            : new Vector3(0, 2, 0);
                    game.setScreen(new TownScreen(game, session.getDifficulty(), spawn));
                } else {
                    startMessage.setText("Sauvegarde illisible ou introuvable (voir la console).");
                }
            }
        });

        startBackButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                startTable.setVisible(false);
                mainTable.setVisible(true);
            }
        });

        // Boutons de difficulté : démarrent une partie NEUVE (aucune sauvegarde n'est lue)
        easyButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                startNewGame("facile");
            }
        });
        normalButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                startNewGame("normale");
            }
        });
        realisticButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                startNewGame("realiste");
            }
        });

        // Bouton Retour : revient au choix Nouvelle partie / Continuer
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                difficultyTable.setVisible(false);
                startTable.setVisible(true);
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

    /** La difficulté est choisie : on passe à la création du personnage (la partie démarre à la fin de celle-ci). */
    private void startNewGame(String difficulty) {
        game.setScreen(new CharacterCreationScreen(game, difficulty));
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