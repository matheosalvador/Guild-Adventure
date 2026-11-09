package com.lucas.guild.libgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;

/**
 * Intérieur d'un bâtiment (forge, guilde, auberge) : une pièce avec une porte de sortie
 * qui ramène en ville, et un ou plusieurs PNJ.
 *
 * Cette classe gère ce qui est commun à tous les intérieurs : le point d'apparition devant
 * la porte, la porte « Sortir (E) » avec sauvegarde automatique, et le gabarit de pièce.
 * L'écran concret décrit seulement ses PNJ et ses dialogues.
 */
public abstract class InteriorScreen extends BaseWorldScreen {

    private final Vector3 townSpawn;
    private final String placeName;

    /**
     * @param townSpawn où réapparaît le joueur en ville quand il sort (devant la porte du bâtiment)
     * @param placeName nom du lieu pour le journal (« la forge », « la guilde »...)
     */
    protected InteriorScreen(MainGame game, String difficulty, Vector3 townSpawn, String placeName) {
        super(game, difficulty);
        this.townSpawn = townSpawn;
        this.placeName = placeName;
    }

    // ------------------------------------------------------------------ comportement commun

    @Override
    protected Vector3 getSpawnPoint() {
        // On arrive toujours devant la porte
        return new Vector3(0, 2, -8);
    }

    @Override
    protected final String getInteractionPrompt(Object userData) {
        String npcPrompt = getNpcPrompt(userData);
        if (npcPrompt != null) {
            return npcPrompt;
        }
        return "Door".equals(userData) ? "Sortir (E)" : null;
    }

    @Override
    protected final boolean onInteract(Object userData) {
        if (onNpcInteract(userData)) {
            return true;
        }
        if ("Door".equals(userData)) {
            leaveToTown();
            return true;
        }
        return false;
    }

    /** Texte à afficher quand on vise un PNJ ou un objet propre à ce lieu, sinon null. */
    protected abstract String getNpcPrompt(Object userData);

    /** Action de E sur un PNJ ou un objet propre à ce lieu. @return true si géré. */
    protected abstract boolean onNpcInteract(Object userData);

    /** Sauvegarde automatique puis retour en ville, devant la porte du bâtiment. */
    private void leaveToTown() {
        // On sauvegarde la position DANS LA VILLE (devant la porte), pas celle de l'intérieur.
        Vector3 spawn = new Vector3(townSpawn);
        saveManager.saveGame(player, gameClock, difficulty, spawn.x, spawn.y, spawn.z, "Town");
        Gdx.app.log("SAVE", "Sauvegarde automatique en quittant " + placeName);
        game.setScreen(new TownScreen(game, difficulty, spawn));
    }

    // ------------------------------------------------------------------ gabarit de pièce

    /**
     * Construit une pièce carrée : sol, trois murs et la porte de sortie (face au joueur à
     * l'apparition). Le quatrième mur, côté porte, est fermé par la porte elle-même.
     *
     * @param size côté de la pièce (20 pour la forge et l'auberge, 30 pour la guilde)
     */
    protected void buildRoom(float size, Color floorColor, Color wallColor) {
        float half = size / 2f;

        addBox(size, 1f, size, floorColor, 0, 0, 0, null);                    // sol
        addBox(1f, 10f, size, wallColor, half, 5f, 0, null);                  // mur est
        addBox(1f, 10f, size, wallColor, -half, 5f, 0, null);                 // mur ouest
        addBox(size, 10f, 1f, wallColor, 0, 5f, half, null);                  // mur du fond
        addBox(4f, 8f, 0.5f, Color.YELLOW, 0, 4.5f, -(half - 0.25f), "Door"); // porte
    }
}
