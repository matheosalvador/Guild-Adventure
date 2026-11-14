package com.lucas.guild.libgdx;

import com.badlogic.gdx.graphics.Color;

/**
 * La cave de l'auberge : trois rats géants, à chaque descente. On y arrive par la trappe de
 * l'auberge, et la porte ramène à l'auberge. C'est la cible de la quête « Nettoyage de la cave ».
 */
public class CellarScreen extends InteriorScreen {

    private static final float SIZE = 16f;

    public CellarScreen(MainGame game, String difficulty) {
        super(game, difficulty, "inn", "la cave");
    }

    @Override
    protected Color getBackgroundColor() {
        return new Color(0.03f, 0.03f, 0.04f, 1f);
    }

    @Override
    protected float getAmbientLight() {
        return 0.35f;
    }

    @Override
    protected void buildWorld() {
        buildRoom(SIZE, Color.DARK_GRAY, new Color(0.35f, 0.33f, 0.3f, 1f));

        // Tonneaux et caisses le long des murs
        Color wood = new Color(0.4f, 0.25f, 0.12f, 1f);
        addBox(1.2f, 1.4f, 1.2f, wood, -6.5f, 1.2f, 4f, null);
        addBox(1.2f, 1.4f, 1.2f, wood, -6.5f, 1.2f, 6f, null);
        addBox(1.2f, 1.4f, 1.2f, wood, 6.5f, 1.2f, 1f, null);
        addBox(2f, 1.2f, 1.6f, new Color(0.5f, 0.35f, 0.2f, 1f), 5.5f, 1.1f, 6.5f, null);

        // Les rats : les murs les retiennent dans la pièce
        float half = SIZE / 2f - 1f;
        combat.setBounds(-half, half, -half, half);
        combat.spawn("giant_rat", -4f, 3f);
        combat.spawn("giant_rat", 4f, 5f);
        combat.spawn("giant_rat", 0f, 6.5f);
    }

    /** La pièce est plus petite que les autres : on apparaît à 2,5 m de la porte, pas dans le mur. */
    @Override
    protected com.badlogic.gdx.math.Vector3 getSpawnPoint() {
        return new com.badlogic.gdx.math.Vector3(0, 2, -5);
    }

    @Override
    protected String getDoorPrompt() {
        return "Remonter à l'auberge (E)";
    }

    @Override
    protected void leave() {
        game.setScreen(new InnScreen(game, difficulty));
    }

    @Override
    protected String getNpcPrompt(Object userData) {
        return null;
    }

    @Override
    protected boolean onNpcInteract(Object userData) {
        return false;
    }
}
