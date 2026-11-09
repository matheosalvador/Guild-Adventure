package com.lucas.guild.libgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.lucas.guild.game.Forge;
import com.lucas.guild.game.GameSession;
import com.lucas.guild.model.Building;

/** La ville : le sol et les trois bâtiments qu'on peut entrer avec E. */
public class TownScreen extends BaseWorldScreen {

    private static final String FORGE = "La Forge du Nain Grincheux";
    private static final String GUILD = "Guilde des Aventuriers";
    private static final String INN = "La Pinte qui Chante";

    private final Vector3 spawnPoint;

    public TownScreen(MainGame game, String difficulty) {
        this(game, difficulty, new Vector3(0, 2, 0));
    }

    public TownScreen(MainGame game, String difficulty, Vector3 spawnPoint) {
        super(game, difficulty);
        this.spawnPoint = spawnPoint;
    }

    @Override
    protected Vector3 getSpawnPoint() {
        return spawnPoint;
    }

    @Override
    protected Color getBackgroundColor() {
        return new Color(0.5f, 0.8f, 1f, 1f);
    }

    @Override
    protected float getAmbientLight() {
        return 0.4f;
    }

    @Override
    protected float getInteractionRange() {
        return 10f;
    }

    // ------------------------------------------------------------------ décor

    @Override
    protected void buildWorld() {
        addBox(200f, 1f, 200f, Color.GRAY, 0, 0, 0, null); // sol

        addBuilding(new Forge(FORGE), 0, -20, Color.DARK_GRAY);
        addBuilding(new Building(GUILD, "Guilde"), -20, 0, Color.BROWN);
        addBuilding(new Building(INN, "Taverne"), 20, 0, Color.TAN);
    }

    private void addBuilding(Building building, float x, float z, Color color) {
        addBox(10f, 10f, 10f, color, x, 5.5f, z, building).userData = building;
    }

    // ------------------------------------------------------------------ interactions

    @Override
    protected String getInteractionPrompt(Object userData) {
        if (userData instanceof Building) {
            return "Entrer dans: " + ((Building) userData).getName() + " (E)";
        }
        return null;
    }

    @Override
    protected boolean onInteract(Object userData) {
        if (!(userData instanceof Building)) {
            return false;
        }
        String name = ((Building) userData).getName();
        if (name.equals(FORGE)) {
            game.setScreen(new ForgeScreen(game, difficulty));
            return true;
        } else if (name.equals(GUILD)) {
            game.setScreen(new GuildScreen(game, difficulty));
            return true;
        } else if (name.equals(INN)) {
            game.setScreen(new InnScreen(game, difficulty));
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ sauvegarde rapide

    @Override
    protected boolean onGlobalKeyDown(int keycode) {
        // Quick Save (F5) — toujours disponible, même en pause
        if (keycode == Input.Keys.F5) {
            Vector3 playerPos = playerController.getPlayerBody().getCenterOfMassPosition();
            boolean saved = saveManager.saveGame(player, gameClock, difficulty, playerPos.x, playerPos.y, playerPos.z, "Town");
            if (saved) {
                Gdx.app.log("SAVE", "Partie sauvegardée ! Niveau: " + player.getLevel() + ", XP: " + player.getExperience() + ", Items: " + player.getInventoryItems().size());
            } else {
                Gdx.app.error("SAVE", "ÉCHEC de la sauvegarde (voir la console)");
            }
            return true;
        }

        // Quick Load (F9) — recharge la sauvegarde disque dans la session puis relance la ville
        if (keycode == Input.Keys.F9) {
            if (game.loadSavedGame()) {
                GameSession loaded = game.getSession();
                Vector3 pos = loaded.hasSavedPosition()
                        ? new Vector3(loaded.getSavedX(), loaded.getSavedY(), loaded.getSavedZ())
                        : new Vector3(0, 2, 0);
                Gdx.app.log("SAVE", "Sauvegarde chargée : " + loaded.getPlayer().getName());
                game.setScreen(new TownScreen(game, loaded.getDifficulty(), pos));
            } else {
                Gdx.app.error("SAVE", "Aucune sauvegarde utilisable à charger");
            }
            return true;
        }
        return false;
    }
}
