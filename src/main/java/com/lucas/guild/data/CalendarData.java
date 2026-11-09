package com.lucas.guild.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Le calendrier tel qu'écrit dans {@code calendar.json}.
 * Ne contient que des données : le calcul des dates est dans {@code GameCalendar}.
 */
public class CalendarData {

    /** Secondes réelles pour une heure de jeu. */
    public float secondsPerGameHour;
    public int daysPerSeason;
    /** Heure du réveil après une nuit de sommeil. */
    public int wakeHour;
    public List<String> weekdays = new ArrayList<>();
    public List<SeasonData> seasons = new ArrayList<>();
    public NewGameData newGame;

    public static class SeasonData {
        public String name;
        /** Heures (0-24) de lever et de coucher du soleil, au milieu de la saison. */
        public float sunrise;
        public float sunset;
    }

    /** Situation de départ d'une nouvelle partie. */
    public static class NewGameData {
        public int day;
        public int hour;
        public int minute;
        public int startingAge;
    }

    /** Vérifie la cohérence des données. @throws IllegalStateException sinon. */
    void validate(String file) {
        if (secondsPerGameHour <= 0) throw new IllegalStateException(file + " : \"secondsPerGameHour\" doit etre > 0");
        if (daysPerSeason <= 0) throw new IllegalStateException(file + " : \"daysPerSeason\" doit etre > 0");
        if (wakeHour < 0 || wakeHour > 23) throw new IllegalStateException(file + " : \"wakeHour\" doit etre entre 0 et 23");
        if (weekdays == null || weekdays.isEmpty()) throw new IllegalStateException(file + " : \"weekdays\" est vide");
        if (seasons == null || seasons.isEmpty()) throw new IllegalStateException(file + " : \"seasons\" est vide");
        for (int i = 0; i < seasons.size(); i++) {
            SeasonData s = seasons.get(i);
            String where = file + " : saison n°" + (i + 1) + (s != null && s.name != null ? " (" + s.name + ")" : "");
            if (s == null || s.name == null || s.name.isEmpty()) throw new IllegalStateException(where + " : \"name\" manquant");
            if (s.sunrise < 0 || s.sunset > 24 || s.sunrise >= s.sunset) {
                throw new IllegalStateException(where + " : il faut 0 <= sunrise < sunset <= 24");
            }
        }
        if (newGame == null) throw new IllegalStateException(file + " : \"newGame\" manquant");
        if (newGame.day < 1) throw new IllegalStateException(file + " : newGame.day doit etre >= 1");
        if (newGame.hour < 0 || newGame.hour > 23) throw new IllegalStateException(file + " : newGame.hour doit etre entre 0 et 23");
        if (newGame.minute < 0 || newGame.minute > 59) throw new IllegalStateException(file + " : newGame.minute doit etre entre 0 et 59");
        if (newGame.startingAge < 0) throw new IllegalStateException(file + " : newGame.startingAge doit etre >= 0");
    }
}
