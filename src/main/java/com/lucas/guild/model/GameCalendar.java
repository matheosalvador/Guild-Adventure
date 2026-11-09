package com.lucas.guild.model;

import java.util.List;

import com.lucas.guild.data.CalendarData;
import com.lucas.guild.data.GameContent;

/**
 * Le calendrier du jeu : transforme un numéro de jour (le jour 1 est le premier jour de la partie)
 * en date, et donne les heures de lever et de coucher du soleil.
 *
 * Les durées et les noms viennent de {@code assets/data/calendar.json}. Les numéros de jour
 * peuvent être négatifs ou nuls pour les dates avant le début de la partie (naissance du personnage).
 */
public final class GameCalendar {

    private static GameCalendar standard;

    private final CalendarData data;
    private final int daysPerYear;

    GameCalendar(CalendarData data) {
        this.data = data;
        this.daysPerYear = data.daysPerSeason * data.seasons.size();
    }

    /** Le calendrier du jeu, lu une seule fois depuis calendar.json. */
    public static synchronized GameCalendar standard() {
        if (standard == null) {
            standard = new GameCalendar(GameContent.calendar());
        }
        return standard;
    }

    /** Construit un calendrier depuis des données (pour les tests). */
    public static GameCalendar of(CalendarData data) {
        return new GameCalendar(data);
    }

    public int daysPerYear() {
        return daysPerYear;
    }

    public int daysPerSeason() {
        return data.daysPerSeason;
    }

    public float secondsPerGameHour() {
        return data.secondsPerGameHour;
    }

    public int wakeHour() {
        return data.wakeHour;
    }

    public CalendarData.NewGameData newGame() {
        return data.newGame;
    }

    /** La date du jour numéro {@code day}. */
    public CalendarDate dateOf(int day) {
        int index = day - 1;
        int year = Math.floorDiv(index, daysPerYear) + 1;
        int dayOfYear = Math.floorMod(index, daysPerYear);
        int season = dayOfYear / data.daysPerSeason;
        int dayOfSeason = dayOfYear % data.daysPerSeason + 1;
        int weekday = Math.floorMod(index, data.weekdays.size());
        return new CalendarDate(year, season, data.seasons.get(season).name, dayOfSeason, weekday, data.weekdays.get(weekday));
    }

    /** Heure (0-24) du lever du soleil ce jour-là, adoucie d'une saison à l'autre. */
    public double sunrise(int day) {
        return sunTime(day, true);
    }

    /** Heure (0-24) du coucher du soleil ce jour-là, adoucie d'une saison à l'autre. */
    public double sunset(int day) {
        return sunTime(day, false);
    }

    /**
     * Les valeurs de calendar.json valent au milieu de chaque saison ; entre deux milieux on
     * interpole, pour que les jours rallongent ou raccourcissent progressivement.
     */
    private double sunTime(int day, boolean sunrise) {
        List<CalendarData.SeasonData> seasons = data.seasons;
        int n = seasons.size();
        double dps = data.daysPerSeason;
        double half = (dps - 1) / 2.0;
        double pos = Math.floorMod(day - 1, daysPerYear);

        int k = (int) Math.floor((pos - half) / dps);   // saison dont le milieu précède pos
        double t = (pos - (k * dps + half)) / dps;       // 0..1 jusqu'au milieu de la suivante
        CalendarData.SeasonData a = seasons.get(Math.floorMod(k, n));
        CalendarData.SeasonData b = seasons.get(Math.floorMod(k + 1, n));
        double va = sunrise ? a.sunrise : a.sunset;
        double vb = sunrise ? b.sunrise : b.sunset;
        return va + (vb - va) * t;
    }
}
