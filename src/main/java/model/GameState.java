package model;

/**
 * Représente l'état du jeu pour la sauvegarde.
 */
public class GameState {
    private String playerName;
    private int level;
    private int health;

    /**
     * Constructeur de l'état du jeu.
     * @param playerName Le nom du joueur.
     * @param level Le niveau du joueur.
     * @param health La santé du joueur.
     */
    public GameState(String playerName, int level, int health) {
        this.playerName = playerName;
        this.level = level;
        this.health = health;
    }

    // Getters & setters (ou Lombok si autorisé)
    /**
     * Récupère le nom du joueur.
     * @return Le nom.
     */
    public String getPlayerName() { return playerName; }

    /**
     * Récupère le niveau du joueur.
     * @return Le niveau.
     */
    public int getLevel() { return level; }

    /**
     * Récupère la santé du joueur.
     * @return La santé.
     */
    public int getHealth() { return health; }
}
