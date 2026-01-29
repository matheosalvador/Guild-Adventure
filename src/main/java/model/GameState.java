package model;

public class GameState {
    private String playerName;
    private int level;
    private int health;

    public GameState(String playerName, int level, int health) {
        this.playerName = playerName;
        this.level = level;
        this.health = health;
    }

    // Getters & setters (ou Lombok si autorisé)
    public String getPlayerName() { return playerName; }
    public int getLevel() { return level; }
    public int getHealth() { return health; }
}
