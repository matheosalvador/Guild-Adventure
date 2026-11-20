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
        if (com.lucas.guild.game.Settings.current().fullscreen) {
            ViewSettings.applyFullscreen(true);   // le plein écran choisi dans les Options (settings.json)
        }
        this.setScreen(new MainMenuScreen(this));
    }

    @Override
    public void render() {
        super.render(); // Important: appelle la méthode render() de l'écran actuel
    }

    @Override
    public void dispose() {
        super.dispose();
        CharacterModel.disposeShared();
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
     * L'écran où reprendre la partie en cours : le monde ouvert, à la position sauvegardée (ou à l'arrivée du lieu
     * s'il n'y en a pas), ou le lieu fermé (mine, château...) où l'on se trouvait. Une sauvegarde d'avant le monde
     * ouvert (position dans une ville ou une zone) est convertie en position du monde.
     */
    public com.badlogic.gdx.Screen resumeScreen() {
        GameSession s = getSession();
        com.lucas.guild.model.Adventurer player = s.getPlayer();
        com.lucas.guild.game.WorldLayout layout = com.lucas.guild.game.WorldLayout.standard();
        String here = player.getTownId();
        com.badlogic.gdx.math.Vector3 pos = s.hasSavedPosition()
                ? new com.badlogic.gdx.math.Vector3(s.getSavedX(), s.getSavedY(), s.getSavedZ())
                : null;
        if (pos != null && OpenWorldScreen.SAVE_SCREEN.equals(s.getSavedScreen())) {
            return new OpenWorldScreen(this, s.getDifficulty(), pos);   // la position est déjà celle du monde
        }
        if (!layout.contains(here)) {
            // un lieu fermé (mine, fosse, château, ou un de leurs étages) : on y reprend, on en sortira par sa porte
            return new ZoneScreen(this, s.getDifficulty(), pos);
        }
        if (pos == null) {
            return arrivalIn(here, s.getDifficulty());
        }
        float[] w = layout.toWorld(here, pos.x, pos.z);
        return new OpenWorldScreen(this, s.getDifficulty(), new com.badlogic.gdx.math.Vector3(w[0], pos.y, w[1]));
    }

    /** Le monde ouvert, à l'arrivée du lieu {@code placeId} (pour un lieu fermé : devant sa porte). */
    public OpenWorldScreen arrivalIn(String placeId, String difficulty) {
        float[] w = arrivalPoint(placeId);
        return new OpenWorldScreen(this, difficulty, new com.badlogic.gdx.math.Vector3(w[0], 2f, w[1]));
    }

    /** L'arrivée du lieu {@code placeId} dans le monde ouvert, en coordonnées du monde. */
    public static float[] arrivalPoint(String placeId) {
        com.lucas.guild.game.WorldLayout layout = com.lucas.guild.game.WorldLayout.standard();
        if (!layout.contains(placeId)) {
            return layout.portalExit(layout.portalFor(placeId));
        }
        com.lucas.guild.data.TownData.Point3 a = com.lucas.guild.data.GameContent.arrival(placeId);
        return layout.toWorld(placeId, a.x, a.z);
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
