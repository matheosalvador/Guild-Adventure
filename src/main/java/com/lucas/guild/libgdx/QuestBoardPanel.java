package com.lucas.guild.libgdx;

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.lucas.guild.game.QuestLog;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Guild;
import com.lucas.guild.model.Quest;

/**
 * Un tableau de quêtes (celui de la guilde, ou le tableau d'annonces d'une ville) : la liste des
 * quêtes de la ville accessibles au rang du joueur, et le détail de chacune avec accepter / rendre.
 * Les règles sont dans {@link QuestLog} ; ce panneau affiche et relaie les clics.
 */
class QuestBoardPanel {

    private final Table table;
    private final Skin skin;
    private final Adventurer player;
    private final String townId;
    private final String title;
    private final Drawable background;
    private final Runnable onBack;
    private final Guild guild = new Guild();

    private boolean open = false;
    private int selected = 0;

    /**
     * @param townId la ville dont on affiche les quêtes
     * @param onBack ce que fait le bouton « Retour » (fermer le tableau, rouvrir le dialogue...)
     */
    QuestBoardPanel(Table table, Skin skin, Adventurer player, String townId, String title, Drawable background, Runnable onBack) {
        this.table = table;
        this.skin = skin;
        this.player = player;
        this.townId = townId;
        this.title = title;
        this.background = background;
        this.onBack = onBack;
    }

    boolean isOpen() {
        return open;
    }

    void toggle() {
        open = !open;
        table.setVisible(open);
        Gdx.input.setCursorCatched(!open);
        if (open) {
            selected = 0;
            drawBoard();
        }
    }

    private List<Quest> quests() {
        return guild.getQuestsFor(townId, player.getRank());
    }

    /** Flèches et Entrée dans la liste. @return true si la touche est consommée */
    boolean handleKey(int keycode) {
        if (!open) {
            return false;
        }
        List<Quest> quests = quests();
        if (keycode == Input.Keys.UP) {
            selected = Math.max(0, selected - 1);
            drawBoard();
            return true;
        }
        if (keycode == Input.Keys.DOWN) {
            selected = Math.min(quests.size() - 1, selected + 1);
            drawBoard();
            return true;
        }
        if (keycode == Input.Keys.ENTER) {
            if (!quests.isEmpty() && selected < quests.size()) {
                drawDetail(quests.get(selected), null);
            }
            return true;
        }
        return false;
    }

    private QuestLog log() {
        return new QuestLog(player);
    }

    private void drawBoard() {
        table.clear();
        table.setBackground(background);
        table.add(UiKit.label(skin, "--- " + title + " ---")).pad(10).row();
        List<Quest> quests = quests();
        if (quests.isEmpty()) {
            table.add(UiKit.label(skin, "Aucune quête disponible pour votre rang.")).row();
        } else {
            for (int i = 0; i < quests.size(); i++) {
                final Quest quest = quests.get(i);
                String mark = "";
                switch (log().status(quest)) {
                    case ACTIVE:
                        mark = quest.isPayOnDelivery() ? " [à livrer]" : log().hasProof(quest) ? " [à rendre]" : " [en cours]";
                        break;
                    case DONE:
                        mark = " [terminée]";
                        break;
                    default:
                        break;
                }
                String text = (i == selected ? "> " : "") + quest.getTitle() + " (" + quest.getGoldReward() + " Po)" + mark;
                table.add(UiKit.button(skin, text, () -> drawDetail(quest, null))).pad(5).row();
            }
        }
        table.add(UiKit.label(skin, "Rang : " + player.getRank() + " - quêtes terminées : " + player.getDoneQuests().size()
                + " (un rang de plus toutes les " + QuestLog.QUESTS_PER_RANK + ")")).pad(5).row();
        table.add(UiKit.button(skin, "Retour", onBack)).pad(10).row();
    }

    /** Le détail d'une quête : description, récompense, et selon l'état accepter ou rendre. */
    private void drawDetail(final Quest quest, String message) {
        table.clear();
        QuestLog log = log();
        QuestLog.Status status = log.status(quest);

        table.add(UiKit.label(skin, quest.getTitle())).pad(10).row();
        Label description = UiKit.label(skin, quest.getDescription());
        description.setWrap(true);
        table.add(description).width(500).pad(5).row();
        table.add(UiKit.label(skin, "Récompense : " + quest.getGoldReward() + " Po")).pad(3).row();
        table.add(UiKit.label(skin, quest.getObjectiveText())).pad(3).row();
        String state;
        if (status == QuestLog.Status.DONE) {
            state = "Quête terminée.";
        } else if (status == QuestLog.Status.ACTIVE) {
            state = quest.isPayOnDelivery() ? "Quête en cours : remettez-le à son destinataire."
                    : log.hasProof(quest) ? "Vous avez ce qu'il faut : vous pouvez rendre la quête." : "Quête en cours.";
        } else {
            state = "Pas encore acceptée.";
        }
        table.add(UiKit.label(skin, state)).pad(8).row();

        if (status == QuestLog.Status.AVAILABLE) {
            table.add(UiKit.button(skin, "Accepter la quête", () -> {
                String refusal = log().accept(quest);
                drawDetail(quest, refusal != null ? refusal
                        : quest.getGiveItem() != null ? "Quête acceptée. Vous recevez : " + quest.getGiveItem() + "." : "Quête acceptée.");
            })).pad(5).row();
        } else if (status == QuestLog.Status.ACTIVE && !quest.isPayOnDelivery()) {
            table.add(UiKit.button(skin, "Rendre la quête", () -> {
                String refusal = log().turnIn(quest);
                drawDetail(quest, refusal != null ? refusal : "Quête rendue : +" + quest.getGoldReward() + " Po !");
            })).pad(5).row();
        }
        if (message != null) {
            table.add(UiKit.message(skin, message)).pad(5).row();
        }
        table.add(UiKit.label(skin, "Or : " + player.getGold() + " Po")).pad(3).row();
        table.add(UiKit.button(skin, "Retour au tableau", this::drawBoard)).pad(10).row();
    }
}
