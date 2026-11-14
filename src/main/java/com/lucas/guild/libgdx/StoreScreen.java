package com.lucas.guild.libgdx;

import com.badlogic.gdx.graphics.Color;

/** Intérieur d'une boutique de village : un commerçant, et la boutique de la ville (shops.json). */
public class StoreScreen extends InteriorScreen {

    private static final String SHOPKEEPER = "Shopkeeper";

    private final String storeName;
    private final String shopId;

    /**
     * @param storeName nom du bâtiment (« Comptoir du Port »)
     * @param shopId    la boutique dans shops.json
     */
    public StoreScreen(MainGame game, String difficulty, String storeName, String shopId) {
        super(game, difficulty, "store", "la boutique");
        this.storeName = storeName;
        this.shopId = shopId;
    }

    @Override
    protected Color getBackgroundColor() {
        return new Color(0.2f, 0.17f, 0.12f, 1f);
    }

    @Override
    protected void buildWorld() {
        buildRoom(20f, new Color(0.45f, 0.33f, 0.2f, 1f), new Color(0.75f, 0.65f, 0.5f, 1f));

        // Comptoir et étagères
        Color wood = new Color(0.4f, 0.26f, 0.13f, 1f);
        addBox(8f, 1f, 1.5f, wood, 0, 1.0f, 4f, null);   // plus bas que le regard : on voit le commerçant par-dessus
        addBox(6f, 4f, 1f, wood, -5f, 2.5f, 9f, null);
        addBox(6f, 4f, 1f, wood, 5f, 2.5f, 9f, null);

        // Commerçant (PNJ), derrière le comptoir
        addBox(1f, 2f, 1f, Color.GOLDENROD, 0, 1.5f, 6f, SHOPKEEPER);
    }

    @Override
    protected String getNpcPrompt(Object userData) {
        return SHOPKEEPER.equals(userData) ? "Parler au commerçant (E)" : null;
    }

    @Override
    protected boolean onNpcInteract(Object userData) {
        if (SHOPKEEPER.equals(userData)) {
            toggleDialog(SHOPKEEPER);
            return true;
        }
        return false;
    }

    @Override
    protected void buildDialog(String dialogType) {
        if (!SHOPKEEPER.equals(dialogType)) {
            return;
        }
        dialogTable.add("Bienvenue au " + storeName + " ! Je regarde ce qu'il vous faut ?").row();

        addDialogButton("Voir la marchandise", () -> showShop(shopId, () -> {
            dialogTable.clear();
            buildDialog(SHOPKEEPER);
        }));

        addLeaveButton();
    }
}
