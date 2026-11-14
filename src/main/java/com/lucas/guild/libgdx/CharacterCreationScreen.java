package com.lucas.guild.libgdx;

import java.util.List;
import java.util.Locale;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.ProfileData;
import com.lucas.guild.data.SkillData;
import com.lucas.guild.game.GameSession;
import com.lucas.guild.game.ProfileSummary;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Item;

/**
 * La création du personnage, après le choix de la difficulté, en trois étapes :
 * le nom, la classe, l'origine (avec le résumé du départ). Chaque choix montre ses avantages
 * et ses inconvénients, calculés à partir des données (classes.json, backgrounds.json).
 */
public class CharacterCreationScreen implements Screen {

    private static final int NAME_MAX_LENGTH = 20;

    private final MainGame game;
    private final String difficulty;

    private Stage stage;
    private Skin skin;
    private BitmapFont font;
    private Texture whiteTexture;
    private Table page;
    private TextField nameField;

    private List<ProfileData> classes;
    private List<ProfileData> backgrounds;
    private ProfileData chosenClass;
    private ProfileData chosenBackground;
    private String name = "Aventurier";
    private int step = 0;

    public CharacterCreationScreen(MainGame game, String difficulty) {
        this.game = game;
        this.difficulty = difficulty;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        font = new BitmapFont();
        skin = new Skin();
        skin.add("default-font", font);
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = font;
        skin.add("default", buttonStyle);
        skin.add("default", new Label.LabelStyle(font, Color.WHITE));

        // Le champ de texte a besoin de dessins (curseur, sélection, fond) : de simples rectangles de couleur
        Pixmap pixel = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixel.setColor(Color.WHITE);
        pixel.fill();
        whiteTexture = new Texture(pixel);
        pixel.dispose();
        TextureRegionDrawable white = new TextureRegionDrawable(new TextureRegion(whiteTexture));
        TextField.TextFieldStyle fieldStyle = new TextField.TextFieldStyle();
        fieldStyle.font = font;
        fieldStyle.fontColor = Color.WHITE;
        fieldStyle.messageFontColor = Color.GRAY;
        fieldStyle.cursor = white.tint(Color.WHITE);
        fieldStyle.cursor.setMinWidth(2);
        fieldStyle.selection = white.tint(new Color(0.25f, 0.4f, 0.8f, 1f));
        fieldStyle.background = white.tint(new Color(0.1f, 0.1f, 0.12f, 1f));
        fieldStyle.background.setLeftWidth(8);
        fieldStyle.background.setRightWidth(8);
        fieldStyle.background.setTopHeight(6);
        fieldStyle.background.setBottomHeight(6);
        skin.add("default", fieldStyle);

        classes = GameContent.characterClasses();
        backgrounds = GameContent.backgrounds();

        page = new Table(skin);
        page.setFillParent(true);
        stage.addActor(page);

        showStep();
    }

    // ------------------------------------------------------------------ les étapes

    private void showStep() {
        page.clear();
        page.top().padTop(20);
        page.add(UiKit.label(skin, "Création du personnage - étape " + (step + 1) + " / 3")).colspan(2).pad(10).row();
        switch (step) {
            case 0:
                nameStep();
                break;
            case 1:
                choiceStep("Choisissez votre classe", classes, chosenClass, new ProfileListener() {
                    @Override
                    public void picked(ProfileData profile) {
                        chosenClass = profile;
                        showStep();
                    }
                }, null);
                break;
            default:
                choiceStep("Choisissez votre origine", backgrounds, chosenBackground, new ProfileListener() {
                    @Override
                    public void picked(ProfileData profile) {
                        chosenBackground = profile;
                        showStep();
                    }
                }, chosenBackground != null ? startSummary() : null);
                break;
        }
    }

    private void nameStep() {
        page.add(UiKit.label(skin, "Comment vous appelez-vous ?")).colspan(2).pad(10).row();

        nameField = new TextField(name, skin);
        nameField.setMaxLength(NAME_MAX_LENGTH);
        nameField.setMessageText("Votre nom");
        page.add(nameField).colspan(2).width(300).pad(10).row();
        stage.setKeyboardFocus(nameField);
        nameField.setCursorPosition(nameField.getText().length());

        final Label message = UiKit.message(skin, "");
        page.add(message).colspan(2).pad(5).row();

        Table buttons = new Table(skin);
        buttons.add(UiKit.button(skin, "Retour", new Runnable() {
            @Override
            public void run() {
                game.setScreen(new MainMenuScreen(game));
            }
        })).pad(10);
        buttons.add(UiKit.button(skin, "Suivant", new Runnable() {
            @Override
            public void run() {
                String typed = nameField.getText().trim();
                if (typed.isEmpty()) {
                    message.setText("Entrez un nom.");
                    return;
                }
                name = typed;
                step = 1;
                showStep();
            }
        })).pad(10);
        page.add(buttons).colspan(2).row();
    }

    private interface ProfileListener {
        void picked(ProfileData profile);
    }

    /**
     * Une étape de choix : la liste des options à gauche, le détail de l'option choisie à droite.
     *
     * @param summary résumé du départ à afficher en bas (dernière étape), ou null
     */
    private void choiceStep(String title, List<ProfileData> options, ProfileData chosen,
                            final ProfileListener listener, String summary) {
        page.add(UiKit.label(skin, title)).colspan(2).pad(5).row();

        Table list = new Table(skin);
        list.top();
        for (final ProfileData option : options) {
            TextButton button = UiKit.button(skin, option.label, new Runnable() {
                @Override
                public void run() {
                    listener.picked(option);
                }
            });
            if (option == chosen) {
                button.setColor(Color.YELLOW);
            }
            list.add(button).width(180).pad(4).row();
        }

        Table detail = new Table(skin);
        detail.top().left();
        if (chosen == null) {
            detail.add(UiKit.label(skin, "Cliquez sur une option pour voir ses avantages et ses inconvénients.")).left().row();
        } else {
            detail.add(UiKit.label(skin, chosen.label)).left().padBottom(4).row();
            Label description = UiKit.label(skin, chosen.description);
            description.setWrap(true);
            detail.add(description).width(430).left().padBottom(8).row();
            // les lignes sont écrites sans signe : le « + » ou le « - » vient d'ici (et la couleur le redouble)
            for (String line : ProfileSummary.advantages(chosen)) {
                Label label = UiKit.label(skin, "+ " + line);
                label.setColor(Color.GREEN);
                detail.add(label).left().row();
            }
            for (String line : ProfileSummary.drawbacks(chosen)) {
                Label label = UiKit.label(skin, "- " + line);
                label.setColor(Color.SALMON);
                detail.add(label).left().row();
            }
        }

        page.add(list).top().pad(10);
        page.add(detail).top().left().width(450).pad(10).row();

        if (summary != null) {
            Label label = UiKit.label(skin, summary);
            label.setColor(Color.LIGHT_GRAY);
            label.setWrap(true);
            page.add(label).colspan(2).width(700).pad(10).row();
        }

        final Label message = UiKit.message(skin, "");
        page.add(message).colspan(2).pad(3).row();

        Table buttons = new Table(skin);
        buttons.add(UiKit.button(skin, "Précédent", new Runnable() {
            @Override
            public void run() {
                step--;
                showStep();
            }
        })).pad(10);
        if (step == 1) {
            buttons.add(UiKit.button(skin, "Suivant", new Runnable() {
                @Override
                public void run() {
                    if (chosenClass == null) {
                        message.setText("Choisissez une classe.");
                        return;
                    }
                    step = 2;
                    showStep();
                }
            })).pad(10);
        } else {
            buttons.add(UiKit.button(skin, "Commencer l'aventure", new Runnable() {
                @Override
                public void run() {
                    if (chosenBackground == null) {
                        message.setText("Choisissez une origine.");
                        return;
                    }
                    start();
                }
            })).pad(10);
        }
        page.add(buttons).colspan(2).row();
    }

    /** Ce que donnera vraiment le personnage : on construit la partie de départ (en mémoire seulement) et on la lit. */
    private String startSummary() {
        GameSession preview = GameSession.newGame(difficulty, name, chosenClass.id, chosenBackground.id);
        Adventurer player = preview.getPlayer();

        StringBuilder items = new StringBuilder();
        for (Item item : player.getInventoryItems()) {
            items.append(items.length() > 0 ? ", " : "").append(item.getName());
        }
        if (player.getInventaire().getWeapon() != null) {
            items.append(items.length() > 0 ? ", " : "").append(player.getInventaire().getWeapon().getName());
        }
        if (player.getInventaire().getBag() != null) {
            items.append(items.length() > 0 ? ", " : "").append(player.getInventaire().getBag().getName());
        }
        StringBuilder skills = new StringBuilder();
        for (SkillData def : player.getSkills().definitions()) {
            if (player.getSkills().getLevel(def.id) > 0) {
                skills.append(skills.length() > 0 ? ", " : "").append(def.label).append(" niv. ").append(player.getSkills().getLevel(def.id));
            }
        }
        return String.format(Locale.FRANCE, "%s : %d PV, %.1f kg de charge, %d Po. Objets : %s. Compétences : %s.",
                player.getName(), player.getMaxHealth(), player.getInventaire().getPoidsMax(), player.getGold(),
                items.length() > 0 ? items : "aucun", skills.length() > 0 ? skills : "aucune");
    }

    private void start() {
        game.startNewGame(difficulty, name, chosenClass.id, chosenBackground.id);
        game.setScreen(new TownScreen(game, difficulty));
    }

    // ------------------------------------------------------------------ Screen

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
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
            font.dispose();
            whiteTexture.dispose();
            stage = null;
        }
    }
}
