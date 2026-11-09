package com.lucas.guild.libgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;

/** Intérieur de la forge : le forgeron. */
public class ForgeScreen extends InteriorScreen {

    private static final String BLACKSMITH = "Blacksmith";

    public ForgeScreen(MainGame game, String difficulty) {
        super(game, difficulty, new Vector3(0, 2, -14), "la forge");
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

        addDialogButton("Forger un objet (10 Po)", () -> {
            Gdx.app.log("Forge", "Achat d'un objet...");
            toggleDialog(null);
        });

        if (player.hasLearnedSkill("Attaque Puissante")) {
            addDialogButton("Utiliser l'enclume", () -> {
                Gdx.app.log("Forge", "Utilisation de l'enclume...");
                toggleDialog(null);
            });
        }

        addLeaveButton();
    }
}
