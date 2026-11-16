package com.lucas.guild.libgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.lucas.guild.data.GameContent;

/** Intérieur de l'auberge « La Pinte qui Chante » : l'aubergiste, un lit à l'étage. */
public class InnScreen extends InteriorScreen {

    private static final String INNKEEPER = "Innkeeper";
    private static final String BED = "Bed";
    private static final String CELLAR = "Cellar";

    /** Prix d'une nuit en Po (les repas et boissons sont dans npcs.json). */
    private static final int BED_PRICE = 10;

    public InnScreen(MainGame game, String difficulty) {
        super(game, difficulty, "inn", "l'auberge");
    }

    @Override
    protected Color getBackgroundColor() {
        return new Color(0.3f, 0.2f, 0.1f, 1f);
    }

    @Override
    protected void buildWorld() {
        Color floor = Color.MAROON;
        Color wall = Color.TAN;

        buildRoom(20f, floor, wall);

        // Aubergiste (PNJ)
        addNpc("innkeeper", 0, 5f, INNKEEPER);

        // Trappe de la cave
        addBox(3f, 0.2f, 3f, new Color(0.23f, 0.14f, 0.07f, 1f), 6f, 0.6f, -4f, CELLAR);

        // Étage : deux planchers, le lit
        addBox(12f, 1f, 20f, floor, 4, 10f, 0, null);
        addBox(8f, 1f, 12f, floor, -6, 10f, 4, null);
        addBox(4f, 2f, 8f, Color.RED, 5, 11.5f, 5, BED);

        // Murs de l'étage
        addBox(1f, 10f, 20f, wall, 10f, 15f, 0, null);
        addBox(1f, 10f, 20f, wall, -10f, 15f, 0, null);
        addBox(20f, 10f, 1f, wall, 0, 15f, 10f, null);
        addBox(20f, 10f, 1f, wall, 0, 15f, -10f, null);

        // Toit
        addBox(20f, 1f, 20f, floor, 0, 20f, 0, null);

        // Escalier le long du mur ouest : vingt marches de 0,5 m (un saut monte de 0,7 m). Les douze premières
        // passent sous le plancher de l'étage ; les suivantes, plus courtes, s'arrêtent avant le mur de la porte.
        for (int j = 0; j < 20; j++) {
            float depth = j < 12 ? 1f : 0.8f;
            float lowEdge = j < 12 ? 9f - j : -3f - 0.8f * (j - 12);
            addBox(4f, 1f, depth, Color.BROWN, -8, 0.5f + 0.5f * j, lowEdge - depth / 2f, null);
        }
        // Palier : du haut de l'escalier au plancher de l'étage
        addBox(4f, 1f, 4f, floor, -4, 10f, -8f, null);
    }

    @Override
    protected String getNpcPrompt(Object userData) {
        if (INNKEEPER.equals(userData)) {
            return npcPrompt("innkeeper");
        }
        if (CELLAR.equals(userData)) {
            return "Descendre à la cave (E)";
        }
        if (BED.equals(userData)) {
            return "Dormir (" + BED_PRICE + " Po) (E)";
        }
        return null;
    }

    @Override
    protected boolean onNpcInteract(Object userData) {
        if (INNKEEPER.equals(userData)) {
            toggleDialog(INNKEEPER);
            return true;
        }
        if (CELLAR.equals(userData)) {
            game.setScreen(new CellarScreen(game, difficulty));
            return true;
        }
        if (BED.equals(userData)) {
            if (!player.spendGold(BED_PRICE)) {
                gameClock.postMessage("Pas assez d'or : une nuit coûte " + BED_PRICE + " Po.");
                return true;
            }
            Gdx.app.log("Auberge", "Le joueur va dormir");
            gameClock.sleepUntilNextMorning();
            player.train("vigor", 2); // une vie max plus haute doit être soignée aussi
            player.heal(player.getMaxHealth());
            return true;
        }
        return false;
    }

    @Override
    protected void buildDialog(String dialogType) {
        if (!INNKEEPER.equals(dialogType)) {
            return;
        }
        buildNpcDialog("innkeeper", INNKEEPER, GameContent.town(player.getTownId()).findByInterior("inn").name, null, null);
    }
}
