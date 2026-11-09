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

    public GameSession getSession() {
        if (session == null) {
            // Ne devrait pas arriver : on passe toujours par le menu. Évite un plantage.
            Gdx.app.error("SESSION", "Aucune partie en cours, creation d'une partie par defaut");
            session = GameSession.newGame("normale");
        }
        return session;
    }
}
