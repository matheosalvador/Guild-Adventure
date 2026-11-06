package com.lucas.guild.model;

public class GameClock {

    private int currentHour = 8;
    private int currentDay = 1;
    private Adventurer player;

    private float timeAccumulator = 0f;
    // 1 heure de jeu s'écoule toutes les 60 secondes de temps réel
    private static final float SECONDS_PER_GAME_HOUR = 60f;

    public GameClock(Adventurer player) {
        this.player = player;
    }
    
    public GameClock() {
        // Constructeur vide pour le chargement
    }

    /**
     * Met à jour l'horloge du jeu. Doit être appelé à chaque image.
     * @param deltaTime Le temps écoulé depuis la dernière image, en secondes.
     */
    public void update(float deltaTime) {
        if (player == null) {
            return; // Ne fait rien si aucun joueur n'est défini
        }

        timeAccumulator += deltaTime;
        if (timeAccumulator >= SECONDS_PER_GAME_HOUR) {
            timeAccumulator -= SECONDS_PER_GAME_HOUR;
            passOneHour();
        }
    }

    private void passOneHour() {
        currentHour++;
        System.out.println("Il est " + currentHour + "h, jour " + currentDay);

        player.updateVitalSigns();

        if (currentHour >= 24) {
            currentHour = 0;
            currentDay++;
            System.out.println("--- Un nouveau jour se lève ---");
        }
    }

    /**
     * Fait avancer le temps d'un certain nombre d'heures.
     * @param hours Le nombre d'heures à passer.
     */
    public void passTime(int hours) {
        for (int i = 0; i < hours; i++) {
            passOneHour();
        }
    }

    // --- Getters et Setters pour la sauvegarde/chargement ---

    public int getCurrentHour() {
        return currentHour;
    }

    public void setCurrentHour(int currentHour) {
        this.currentHour = currentHour;
    }

    public int getCurrentDay() {
        return currentDay;
    }

    public void setCurrentDay(int currentDay) {
        this.currentDay = currentDay;
    }

    public void setPlayer(Adventurer player) {
        this.player = player;
    }

    public void sleepUntilNextMorning() {
    }
}
