package com.lucas.guild.libgdx;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.lucas.guild.data.GameContent;
import com.lucas.guild.game.GameSession;
import com.lucas.guild.game.SaveManager;
import com.lucas.guild.model.GameData;

public class MainGame extends Game {

    /** Partie en cours, partagée par tous les écrans. */
    private GameSession session;

    @Override
    public void create() {
        GameContent.validateAll(); // échoue tout de suite, avec le nom du fichier, si un JSON est cassé
        this.setScreen(new MainMenuScreen(this));
    }

    @Override
    public void render() {
        super.render(); // Important: appelle la méthode render() de l'écran actuel
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    /** Démarre une partie neuve (n'écrit ni ne lit aucune sauvegarde). */
    public void startNewGame(String difficulty) {
        session = GameSession.newGame(difficulty);
    }

    /** Démarre une partie neuve avec le personnage créé par le joueur. */
    public void startNewGame(String difficulty, String name, String classId, String backgroundId) {
        session = GameSession.newGame(difficulty, name, classId, backgroundId);
    }

    /**
     * Charge la sauvegarde disque dans la session.
     *
     * @return false s'il n'y a pas de sauvegarde exploitable (la session en cours reste inchangée).
     */
    public boolean loadSavedGame() {
        GameData data = new SaveManager().loadGame();
        if (data == null) {
            return false;
        }
        session = GameSession.fromSave(data);
        return true;
    }

    /**
     * L'écran où reprendre la partie en cours : la ville ou la zone sauvage où se trouve le joueur,
     * à la position sauvegardée (ou à l'arrivée du lieu s'il n'y en a pas).
     */
    public com.badlogic.gdx.Screen resumeScreen() {
        GameSession s = getSession();
        com.lucas.guild.model.Adventurer player = s.getPlayer();
        com.lucas.guild.data.WorldData.PlaceData place = com.lucas.guild.data.GameContent.world().place(player.getTownId());
        com.badlogic.gdx.math.Vector3 pos = s.hasSavedPosition()
                ? new com.badlogic.gdx.math.Vector3(s.getSavedX(), s.getSavedY(), s.getSavedZ())
                : null;
        if (place != null && place.zoneFile != null) {
            return new ZoneScreen(this, s.getDifficulty(), pos);
        }
        return new TownScreen(this, s.getDifficulty(), pos != null ? pos : new com.badlogic.gdx.math.Vector3(0, 2, 0));
    }

    /** L'écran d'arrivée dans le lieu {@code placeId} (devant la porte de la ville, ou au bord de la route d'une zone). */
    public com.badlogic.gdx.Screen screenAt(String placeId, String difficulty) {
        com.lucas.guild.data.WorldData.PlaceData place = com.lucas.guild.data.GameContent.world().place(placeId);
        if (place != null && place.zoneFile != null) {
            return new ZoneScreen(this, difficulty, null);
        }
        com.lucas.guild.data.TownData.Point3 a = com.lucas.guild.data.GameContent.arrival(placeId);
        return new TownScreen(this, difficulty, a != null ? new com.badlogic.gdx.math.Vector3(a.x, a.y, a.z) : new com.badlogic.gdx.math.Vector3(0, 2, 50));
    }

    public GameSession getSession() {
        if (session == null) {
            // Ne devrait pas arriver : on passe toujours par le menu. Évite un plantage.
            Gdx.app.error("SESSION", "Aucune partie en cours, creation d'une partie par defaut");
            session = GameSession.newGame("normale");
        }
        return session;
    }
}
