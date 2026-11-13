package com.lucas.guild.libgdx;

import com.badlogic.gdx.graphics.Color;
import com.lucas.guild.data.GameContent;
import com.lucas.guild.game.QuestLog;

/** Intérieur de la forge : le forgeron. */
public class ForgeScreen extends InteriorScreen {

    private static final String BLACKSMITH = "Blacksmith";

    public ForgeScreen(MainGame game, String difficulty) {
        super(game, difficulty, "forge", "la forge");
    }

    @Override
    protected Color getBackgroundColor() {
        return new Color(0.1f, 0.1f, 0.1f, 1f);
    }

    @Override
    protected void buildWorld() {
        buildRoom(20f, Color.DARK_GRAY, Color.GRAY);

        // Forgeron (PNJ)
        addBox(1f, 4f, 1f, Color.GREEN, 2, 2.5f, 0, BLACKSMITH);
    }

    @Override
    protected String getNpcPrompt(Object userData) {
        return BLACKSMITH.equals(userData) ? "Parler au forgeron (E)" : null;
    }

    @Override
    protected boolean onNpcInteract(Object userData) {
        if (BLACKSMITH.equals(userData)) {
            toggleDialog(BLACKSMITH);
            return true;
        }
        return false;
    }

    @Override
    protected void buildDialog(String dialogType) {
        if (!BLACKSMITH.equals(dialogType)) {
            return;
        }
        dialogTable.add("Que puis-je faire pour vous ?").row();

        final QuestLog questLog = new QuestLog(player);
        if (questLog.canDeliver("Forgeron", GameContent.quests())) {
            addDialogButton("Remettre la lettre", () -> {
                String message = questLog.deliver("Forgeron", GameContent.quests());
                toggleDialog(null);
                gameClock.postMessage(message);
            });
        }

        addDialogButton("Armes et armures", () -> showShop("forge", () -> {
            dialogTable.clear();
            buildDialog(BLACKSMITH);
        }));

        addLeaveButton();
    }
}
