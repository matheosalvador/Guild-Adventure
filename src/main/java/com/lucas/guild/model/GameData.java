package com.lucas.guild.model;

import com.lucas.guild.game.GameClock;

public class GameData {

    public Adventurer playerData;
    public int currentHour;
    public int currentDay;

    // Constructeur pour la sauvegarde
    public GameData(Adventurer player, GameClock clock) {
        this.playerData = player;
        this.currentHour = clock.getCurrentHour();
        this.currentDay = clock.getCurrentDay();
    }
    
    // Constructeur par défaut requis par Gson
    public GameData() {}
}
