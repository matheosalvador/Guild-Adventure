package com.lucas.guild.libgdx;

import com.badlogic.gdx.graphics.Color;
import com.lucas.guild.data.GameContent;

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
        addNpc("blacksmith", 2f, 0, BLACKSMITH);
    }

    @Override
    protected String getNpcPrompt(Object userData) {
        return BLACKSMITH.equals(userData) ? npcPrompt("blacksmith") : null;
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
        buildNpcDialog("blacksmith", BLACKSMITH, GameContent.town(player.getTownId()).findByInterior("forge").name, null, null);
    }
}
