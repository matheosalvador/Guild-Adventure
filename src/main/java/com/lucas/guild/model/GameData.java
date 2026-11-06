package com.lucas.guild.model;

/**
 * Conteneur de données pour la sauvegarde/chargement de partie.
 * Stocke toutes les informations nécessaires pour restaurer l'état du jeu.
 */
public class GameData {

    public Adventurer playerData;
    public int currentHour;
    public int currentDay;
    public String difficulty;
    public float playerX;
    public float playerY;
    public float playerZ;
    public String currentScreen;

    // Constructeur pour la sauvegarde
    public GameData(Adventurer player, GameClock clock) {
        this.playerData = player;
        this.currentHour = clock.getCurrentHour();
        this.currentDay = clock.getCurrentDay();
    }

    /**
     * Constructeur complet pour la sauvegarde avec position et difficulté.
     * @param player Le joueur à sauvegarder.
     * @param clock L'horloge du jeu.
     * @param difficulty La difficulté de la partie (facile, normale, realiste).
     * @param playerX Position X du joueur.
     * @param playerY Position Y du joueur.
     * @param playerZ Position Z du joueur.
     * @param currentScreen L'écran actuel (Town, Forge, Guild, Inn).
     */
    public GameData(Adventurer player, GameClock clock, String difficulty,
                    float playerX, float playerY, float playerZ, String currentScreen) {
        this.playerData = player;
        this.currentHour = clock.getCurrentHour();
        this.currentDay = clock.getCurrentDay();
        this.difficulty = difficulty;
        this.playerX = playerX;
        this.playerY = playerY;
        this.playerZ = playerZ;
        this.currentScreen = currentScreen;
    }

    // Constructeur par défaut requis par Gson
    public GameData() {}
}