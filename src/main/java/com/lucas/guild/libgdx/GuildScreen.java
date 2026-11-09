package com.lucas.guild.libgdx;

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.lucas.guild.model.Guild;
import com.lucas.guild.model.Quest;

/** Intérieur de la Guilde des Aventuriers : le maître de guilde et le tableau des quêtes. */
public class GuildScreen extends InteriorScreen {

    private static final String GUILD_MASTER = "GuildMaster";

    private Guild guild;
    private Table questTable;
    private boolean isQuestBoardOpen = false;
    private int selectedQuestIndex = 0;

    public GuildScreen(MainGame game, String difficulty) {
        super(game, difficulty, "guild", "la guilde");
    }

    @Override
    protected Color getBackgroundColor() {
        return new Color(0.2f, 0.15f, 0.1f, 1f);
    }

    @Override
    protected void buildWorld() {
        guild = new Guild();

        buildRoom(30f, Color.BROWN, Color.TAN);

        // Maître de guilde (PNJ)
        addBox(1f, 2f, 1f, Color.ROYAL, 0, 1.5f, 10f, GUILD_MASTER);
    }

    @Override
    protected String getNpcPrompt(Object userData) {
        return GUILD_MASTER.equals(userData) ? "Parler au Maître de Guilde (E)" : null;
    }

    @Override
    protected boolean onNpcInteract(Object userData) {
        if (GUILD_MASTER.equals(userData)) {
            toggleDialog(GUILD_MASTER);
            return true;
        }
        return false;
    }

    @Override
    protected void buildDialog(String dialogType) {
        if (!GUILD_MASTER.equals(dialogType)) {
            return;
        }
        dialogTable.add("Bienvenue à la guilde, aventurier. Que puis-je faire pour vous ?").row();

        addDialogButton("Voir les quêtes", () -> {
            toggleDialog(null);
            toggleQuestBoard();
        });

        addLeaveButton();
    }

    // ------------------------------------------------------------------ tableau des quêtes

    @Override
    protected void setupExtraUI() {
        questTable = new Table(skin);
        questTable.setFillParent(true);
        questTable.setVisible(false);
        stage.addActor(questTable);
    }

    @Override
    protected boolean isExtraOverlayOpen() {
        return isQuestBoardOpen;
    }

    @Override
    protected boolean closeExtraOverlay() {
        if (isQuestBoardOpen) {
            toggleQuestBoard();
            return true;
        }
        return false;
    }

    @Override
    protected boolean onOverlayKeyDown(int keycode) {
        if (!isQuestBoardOpen) {
            return false;
        }
        List<Quest> quests = guild.getQuestsForRank(player.getRank());
        if (keycode == Input.Keys.UP) {
            selectedQuestIndex = Math.max(0, selectedQuestIndex - 1);
            drawQuestBoard();
            return true;
        }
        if (keycode == Input.Keys.DOWN) {
            selectedQuestIndex = Math.min(quests.size() - 1, selectedQuestIndex + 1);
            drawQuestBoard();
            return true;
        }
        if (keycode == Input.Keys.ENTER) {
            if (!quests.isEmpty() && selectedQuestIndex < quests.size()) {
                Gdx.app.log("QUETE", quests.get(selectedQuestIndex).toString());
            }
            return true;
        }
        return false;
    }

    private void toggleQuestBoard() {
        isQuestBoardOpen = !isQuestBoardOpen;
        questTable.setVisible(isQuestBoardOpen);
        Gdx.input.setCursorCatched(!isQuestBoardOpen);
        if (isQuestBoardOpen) {
            selectedQuestIndex = 0;
            drawQuestBoard();
        }
    }

    private void drawQuestBoard() {
        questTable.clear();
        questTable.add("--- Tableau des Quêtes ---").row();
        List<Quest> quests = guild.getQuestsForRank(player.getRank());
        if (quests.isEmpty()) {
            questTable.add("Aucune quête disponible pour votre rang.").row();
        } else {
            for (int i = 0; i < quests.size(); i++) {
                final Quest quest = quests.get(i);
                String buttonText = (i == selectedQuestIndex) ? "> " + quest.getTitle() : quest.getTitle();
                TextButton questButton = new TextButton(buttonText, skin);
                questButton.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        Gdx.app.log("QUETE", quest.toString());
                    }
                });
                questTable.add(questButton).pad(5).row();
            }
        }
        TextButton backButton = new TextButton("Retour", skin);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                toggleQuestBoard();
                toggleDialog(GUILD_MASTER);
            }
        });
        questTable.add(backButton).pad(10).row();
    }
}
