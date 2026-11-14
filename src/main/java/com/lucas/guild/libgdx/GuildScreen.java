package com.lucas.guild.libgdx;

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.lucas.guild.game.QuestLog;
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

        addDialogButton("Acheter du matériel", () -> showShop("guild", () -> {
            dialogTable.clear();
            buildDialog(GUILD_MASTER);
        }));

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
                drawQuestDetail(quests.get(selectedQuestIndex), null);
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
        questTable.setBackground(panelBackground());
        questTable.add(UiKit.label(skin, "--- Tableau des Quêtes ---")).pad(10).row();
        List<Quest> quests = guild.getQuestsForRank(player.getRank());
        if (quests.isEmpty()) {
            questTable.add(UiKit.label(skin, "Aucune quête disponible pour votre rang.")).row();
        } else {
            for (int i = 0; i < quests.size(); i++) {
                final Quest quest = quests.get(i);
                String mark = "";
                switch (questLog().status(quest)) {
                    case ACTIVE:
                        mark = questLog().hasProof(quest) ? " [à rendre]" : " [en cours]";
                        break;
                    case DONE:
                        mark = " [terminée]";
                        break;
                    default:
                        break;
                }
                String buttonText = (i == selectedQuestIndex ? "> " : "") + quest.getTitle() + " (" + quest.getGoldReward() + " Po)" + mark;
                questTable.add(UiKit.button(skin, buttonText, new Runnable() {
                    @Override
                    public void run() {
                        drawQuestDetail(quest, null);
                    }
                })).pad(5).row();
            }
        }
        questTable.add(UiKit.button(skin, "Retour", new Runnable() {
            @Override
            public void run() {
                toggleQuestBoard();
                toggleDialog(GUILD_MASTER);
            }
        })).pad(10).row();
    }

    /** Le détail d'une quête : description, récompense, et selon l'état accepter ou rendre. */
    private void drawQuestDetail(final Quest quest, String message) {
        questTable.clear();
        QuestLog log = questLog();
        QuestLog.Status status = log.status(quest);

        questTable.add(UiKit.label(skin, quest.getTitle())).pad(10).row();
        Label description = UiKit.label(skin, quest.getDescription());
        description.setWrap(true);
        questTable.add(description).width(500).pad(5).row();
        questTable.add(UiKit.label(skin, "Récompense : " + quest.getGoldReward() + " Po")).pad(3).row();
        questTable.add(UiKit.label(skin, "Preuve à rapporter : " + quest.getProofItem())).pad(3).row();
        String state = status == QuestLog.Status.DONE ? "Quête terminée."
                : status == QuestLog.Status.ACTIVE ? (log.hasProof(quest) ? "Vous avez la preuve : vous pouvez rendre la quête." : "Quête en cours.")
                : "Pas encore acceptée.";
        questTable.add(UiKit.label(skin, state)).pad(8).row();

        if (status == QuestLog.Status.AVAILABLE) {
            questTable.add(UiKit.button(skin, "Accepter la quête", new Runnable() {
                @Override
                public void run() {
                    String refusal = questLog().accept(quest);
                    drawQuestDetail(quest, refusal != null ? refusal
                            : quest.getGiveItem() != null ? "Quête acceptée. Vous recevez : " + quest.getGiveItem() + "." : "Quête acceptée.");
                }
            })).pad(5).row();
        } else if (status == QuestLog.Status.ACTIVE) {
            questTable.add(UiKit.button(skin, "Rendre la quête", new Runnable() {
                @Override
                public void run() {
                    String refusal = questLog().turnIn(quest);
                    drawQuestDetail(quest, refusal != null ? refusal : "Quête rendue : +" + quest.getGoldReward() + " Po !");
                }
            })).pad(5).row();
        }
        if (message != null) {
            questTable.add(UiKit.message(skin, message)).pad(5).row();
        }
        questTable.add(UiKit.label(skin, "Or : " + player.getGold() + " Po")).pad(3).row();
        questTable.add(UiKit.button(skin, "Retour au tableau", new Runnable() {
            @Override
            public void run() {
                drawQuestBoard();
            }
        })).pad(10).row();
    }

    private QuestLog questLog() {
        return new QuestLog(player);
    }

    private com.badlogic.gdx.scenes.scene2d.utils.Drawable panelBackground() {
        return dialogTable.getBackground();
    }
}
