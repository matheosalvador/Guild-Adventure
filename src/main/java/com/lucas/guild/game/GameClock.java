package com.lucas.guild.game;

import com.lucas.guild.model.Adventurer;
// utilsé actuellement a voire pour le temps IG par rapport temps IRL
public class GameClock {

    private int currentHour = 8;
    private int currentDay = 1;
    private Adventurer player;

    public GameClock(Adventurer player) {
        this.player = player;
    }
    
    // Constructeur par défaut pour Gson
    public GameClock() {}

    public void passTime(int hours) {
        for (int i = 0; i < hours; i++) {
            passOneHour();
        }
    }

    public void sleepUntilNextMorning() {
        int hoursToSleep = (currentHour < 8) ? (8 - currentHour) : ((24 - currentHour) + 8);
        System.out.println("Le soleil se couche... et se lève à nouveau.");
        passTime(hoursToSleep);
    }

    private void passOneHour() {
        currentHour++;
        if (currentHour >= 24) {
            currentHour = 0;
            currentDay++;
            System.out.println("--- Un nouveau jour se lève ---");
        }
        if (player != null) player.updateVitalSigns();
    }

    // Getters et Setters pour la sauvegarde/chargement
    public int getCurrentHour() { return currentHour; }
    public int getCurrentDay() { return currentDay; }
    public void setPlayer(Adventurer player) { this.player = player; }
    public void setCurrentHour(int hour) { this.currentHour = hour; }
    public void setCurrentDay(int day) { this.currentDay = day; }

    public String getCurrentTime() {
        return "Jour " + currentDay + ", " + currentHour + "h00";
    }
}
