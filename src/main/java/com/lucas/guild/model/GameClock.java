package com.lucas.guild.model;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * L'horloge du jeu : jour, heure et minute, avancée en temps réel ou par le sommeil.
 *
 * <ul>
 *   <li>Le jour 1 est le premier jour de la partie ; la date (saison, année...) se déduit du numéro
 *       de jour par {@link GameCalendar}.</li>
 *   <li>Chaque heure écoulée met à jour les constantes vitales du joueur ; chaque nouveau jour
 *       fait vieillir le personnage et signale les saisons, années et anniversaires.</li>
 *   <li>Ces événements sont déposés dans une file de messages ({@link #pollMessage()}) que l'écran
 *       affiche à l'écran du joueur.</li>
 * </ul>
 */
public class GameClock {

    private int currentDay = 1;
    private int currentHour = 8;
    private int currentMinute = 0;
    private Adventurer player;

    /** Fraction de minute de jeu déjà écoulée (temps réel qui n'a pas encore fait une minute). */
    private float minuteFraction = 0f;
    private final Deque<String> messages = new ArrayDeque<>();

    public GameClock(Adventurer player) {
        this.player = player;
    }

    public GameClock() {
        // Constructeur vide pour le chargement
    }

    // ------------------------------------------------------------------ avancer le temps

    /**
     * Fait avancer le temps réel. À appeler à chaque image.
     *
     * @param deltaTime temps réel écoulé depuis la dernière image, en secondes
     */
    public void update(float deltaTime) {
        float minutesPerSecond = 60f / GameCalendar.standard().secondsPerGameHour();
        minuteFraction += deltaTime * minutesPerSecond;
        while (minuteFraction >= 1f) {
            minuteFraction -= 1f;
            stepMinute();
        }
    }

    /** Fait avancer le temps d'un certain nombre d'heures (voyage, attente...). */
    public void passTime(int hours) {
        advanceMinutes(hours * 60);
    }

    /** Fait avancer le temps d'un certain nombre de minutes de jeu. */
    public void advanceMinutes(int minutes) {
        for (int i = 0; i < minutes; i++) {
            stepMinute();
        }
    }

    /** Dort jusqu'à l'heure du réveil (voir {@code wakeHour} dans calendar.json), le lendemain si besoin. */
    public void sleepUntilNextMorning() {
        int wakeMinutes = GameCalendar.standard().wakeHour() * 60;
        int nowMinutes = currentHour * 60 + currentMinute;
        int toSleep = nowMinutes < wakeMinutes ? wakeMinutes - nowMinutes : 24 * 60 - nowMinutes + wakeMinutes;
        advanceMinutes(toSleep);
        minuteFraction = 0f;
        postMessage("Vous vous réveillez, il est " + formatTime() + ".");
    }

    private void stepMinute() {
        currentMinute++;
        if (currentMinute < 60) {
            return;
        }
        currentMinute = 0;
        currentHour++;
        if (player != null) {
            player.updateVitalSigns();
        }
        if (currentHour >= 24) {
            currentHour = 0;
            currentDay++;
            onNewDay();
        }
    }

    private void onNewDay() {
        CalendarDate date = GameCalendar.standard().dateOf(currentDay);
        if (date.dayOfSeason == 1) {
            if (date.seasonIndex == 0) {
                postMessage("Une nouvelle année commence : An " + date.year + ", " + date.seasonName + ".");
            } else {
                postMessage("Une nouvelle saison commence : " + date.seasonName + ".");
            }
        }
        if (player != null) {
            if (player.isBirthday(currentDay)) {
                postMessage("Joyeux anniversaire ! Vous avez " + player.getAge(currentDay) + " ans.");
            }
            String aging = player.refreshAging(currentDay);
            if (aging != null) {
                postMessage(aging);
            }
        }
    }

    // ------------------------------------------------------------------ lecture

    public int getCurrentHour() {
        return currentHour;
    }

    public int getCurrentMinute() {
        return currentMinute;
    }

    public int getCurrentDay() {
        return currentDay;
    }

    /** Heure de la journée en heures décimales (14h30 = 14,5), minutes écoulées comprises. */
    public double getTimeOfDay() {
        return currentHour + (currentMinute + minuteFraction) / 60.0;
    }

    public CalendarDate getDate() {
        return GameCalendar.standard().dateOf(currentDay);
    }

    public double getSunrise() {
        return GameCalendar.standard().sunrise(currentDay);
    }

    public double getSunset() {
        return GameCalendar.standard().sunset(currentDay);
    }

    /** « 08:05 ». */
    public String formatTime() {
        return String.format("%02d:%02d", currentHour, currentMinute);
    }

    /** Prochain message d'événement (saison, anniversaire, réveil...), ou null s'il n'y en a pas. */
    public String pollMessage() {
        return messages.poll();
    }

    /** D'autres messages attendent-ils d'être affichés ? */
    public boolean hasPendingMessage() {
        return !messages.isEmpty();
    }

    /** Ajoute un message à afficher au joueur (événement, refus...). */
    public void postMessage(String message) {
        messages.add(message);
    }

    // ------------------------------------------------------------------ pour la sauvegarde / chargement

    public void setCurrentHour(int currentHour) {
        this.currentHour = Math.max(0, Math.min(23, currentHour));
    }

    public void setCurrentMinute(int currentMinute) {
        this.currentMinute = Math.max(0, Math.min(59, currentMinute));
    }

    public void setCurrentDay(int currentDay) {
        this.currentDay = currentDay;
    }

    public void setPlayer(Adventurer player) {
        this.player = player;
    }
}
