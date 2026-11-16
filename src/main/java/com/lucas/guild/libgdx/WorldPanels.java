package com.lucas.guild.libgdx;

import java.util.function.Consumer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Disposable;
import com.lucas.guild.game.GameSession;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Item;

/**
 * Les menus communs à tous les écrans du monde : la fenêtre de dialogue, l'inventaire (I), le personnage (K),
 * le journal (J), le panneau de debug (F10) et la pause (Échap). Cette classe les construit et les remplit ;
 * l'écran décide quand ils s'ouvrent (un seul à la fois).
 */
final class WorldPanels implements Disposable {

    final Table dialog;
    final Table inventory;
    final Table skills;
    final Table journal;
    final Table debug;
    final Table pause;

    private final InventoryPanel inventoryPanel;
    private final SkillsPanel skillsPanel;
    private final JournalPanel journalPanel;
    private final DebugPanel debugPanel;
    private final Texture shadeTexture;

    /**
     * @param onDrop     ce que devient un objet jeté depuis l'inventaire (le poser devant le joueur)
     * @param onContinue le bouton « Continuer » de la pause
     */
    WorldPanels(Stage stage, Skin skin, final MainGame game, BaseWorldScreen screen, String difficulty,
                Consumer<Item> onDrop, final Runnable onContinue) {
        GameSession session = game.getSession();
        Adventurer player = session.getPlayer();

        dialog = addTable(stage, skin);
        inventory = addTable(stage, skin);
        inventoryPanel = new InventoryPanel(inventory, skin, player);
        inventoryPanel.setDropHandler(onDrop);
        skills = addTable(stage, skin);

        // Fond sombre translucide derrière les panneaux : le texte blanc reste lisible sur le ciel clair
        Pixmap shade = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        shade.setColor(0f, 0f, 0f, 0.7f);
        shade.fill();
        shadeTexture = new Texture(shade);
        shade.dispose();
        Drawable background = new TextureRegionDrawable(new TextureRegion(shadeTexture));
        dialog.setBackground(background);
        inventory.setBackground(background);
        skills.setBackground(background);
        skillsPanel = new SkillsPanel(skills, skin, player, session.getClock());

        journal = addTable(stage, skin);
        journal.setBackground(background);
        journalPanel = new JournalPanel(journal, skin, player);

        debug = addTable(stage, skin);
        debug.setBackground(background);
        debugPanel = new DebugPanel(debug, skin, game, screen, session, difficulty);

        // Menu pause
        pause = addTable(stage, skin);
        TextButton continueButton = new TextButton("Continuer", skin);
        TextButton menuButton = new TextButton("Retour au Menu", skin);
        final TextButton shadowsButton = new TextButton(shadowsLabel(), skin);
        shadowsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                ViewSettings.shadows = !ViewSettings.shadows;
                shadowsButton.setText(shadowsLabel());
            }
        });
        pause.add(continueButton).pad(10).row();
        pause.add(shadowsButton).pad(10).row();
        pause.add(menuButton).pad(10).row();
        continueButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                onContinue.run();
            }
        });
        menuButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen(game));
            }
        });
    }

    private static Table addTable(Stage stage, Skin skin) {
        Table table = new Table(skin);
        table.setFillParent(true);
        table.setVisible(false);
        stage.addActor(table);
        return table;
    }

    private static String shadowsLabel() {
        return "Ombres : " + (ViewSettings.shadows ? "oui" : "non");
    }

    /** Montre ou cache un menu ; la souris est libre tant qu'un menu est ouvert, capturée pour regarder sinon. */
    void show(Table table, boolean open) {
        table.setVisible(open);
        Gdx.input.setCursorCatched(!open);
    }

    void refreshInventory() {
        inventoryPanel.rebuild(null);
    }

    void refreshSkills() {
        skillsPanel.rebuild();
    }

    void refreshJournal() {
        journalPanel.rebuild();
    }

    void refreshDebug() {
        debugPanel.rebuild(null);
    }

    @Override
    public void dispose() {
        shadeTexture.dispose();
    }
}
