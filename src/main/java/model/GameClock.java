package model;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class GameClock {

    private Timeline timeline;
    private int currentHour = 8; // Le jeu commence à 8h du matin
    private int currentDay = 1;

    private Adventurer player;

    public GameClock(Adventurer player) {
        this.player = player;
        setupClock();
    }

    private void setupClock() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> {
            passOneHour();
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
    }

    private void passOneHour() {
        currentHour++;
        System.out.println("Il est " + currentHour + "h, jour " + currentDay);

        player.updateVitalSigns();
        System.out.println(player); // Affiche l'état du joueur

        if (currentHour >= 24) {
            currentHour = 0;
            currentDay++;
            System.out.println("--- Un nouveau jour se lève ---");
        }
    }

    public void start() {
        timeline.play();
    }

    public void stop() {
        timeline.pause();
    }
}
