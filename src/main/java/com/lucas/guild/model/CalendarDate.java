package com.lucas.guild.model;

/** Une date du calendrier du jeu (année, saison, jour, jour de la semaine). */
public final class CalendarDate {

    public final int year;
    public final int seasonIndex;
    public final String seasonName;
    /** Jour dans la saison, à partir de 1. */
    public final int dayOfSeason;
    public final int weekdayIndex;
    public final String weekdayName;

    CalendarDate(int year, int seasonIndex, String seasonName, int dayOfSeason, int weekdayIndex, String weekdayName) {
        this.year = year;
        this.seasonIndex = seasonIndex;
        this.seasonName = seasonName;
        this.dayOfSeason = dayOfSeason;
        this.weekdayIndex = weekdayIndex;
        this.weekdayName = weekdayName;
    }

    /** Ex. « Lundi 1 Printemps, An 1 ». */
    @Override
    public String toString() {
        return weekdayName + " " + dayOfSeason + " " + seasonName + ", An " + year;
    }
}
