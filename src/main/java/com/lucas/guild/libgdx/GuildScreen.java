package com.lucas.guild.libgdx;


import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

/** Intérieur de la Guilde des Aventuriers : le maître de guilde et le tableau des quêtes. */
public class GuildScreen extends InteriorScreen {

    private static final String GUILD_MASTER = "GuildMaster";

    private Table questTable;
    private QuestBoardPanel questBoard;

    public GuildScreen(MainGame game, String difficulty) {
        super(game, difficulty, "guild", "la guilde");
    }

    @Override
    protected Color getBackgroundColor() {
        return new Color(0.2f, 0.15f, 0.1f, 1f);
    }

    @Override
    protected void buildWorld() {
        buildRoom(30f, Color.BROWN, Color.TAN);

        // Maître de guilde (PNJ)
        addNpc("guildmaster", 0, 10f, GUILD_MASTER);
    }

    @Override
    protected String getNpcPrompt(Object userData) {
        return GUILD_MASTER.equals(userData) ? npcPrompt("guildmaster") : null;
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
        buildNpcDialog("guildmaster", GUILD_MASTER, "la guilde", null, () -> questBoard.toggle());
    }

    // ------------------------------------------------------------------ tableau des quêtes

    @Override
    protected void setupExtraUI() {
        questTable = new Table(skin);
        questTable.setFillParent(true);
        questTable.setVisible(false);
        stage.addActor(questTable);
        questBoard = new QuestBoardPanel(questTable, skin, player, player.getTownId(), "Tableau des Quêtes", dialogTable.getBackground(), () -> {
            questBoard.toggle();
            toggleDialog(GUILD_MASTER);
        });
    }

    @Override
    protected boolean isExtraOverlayOpen() {
        return questBoard != null && questBoard.isOpen();
    }

    @Override
    protected boolean closeExtraOverlay() {
        if (isExtraOverlayOpen()) {
            questBoard.toggle();
            return true;
        }
        return false;
    }

    @Override
    protected boolean onOverlayKeyDown(int keycode) {
        return questBoard != null && questBoard.handleKey(keycode);
    }
}
