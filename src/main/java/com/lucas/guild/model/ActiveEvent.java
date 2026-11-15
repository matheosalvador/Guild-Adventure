package com.lucas.guild.model;

/** Un événement du monde en cours dans un lieu : de {@code startDay} à {@code endDay} inclus. Sauvegardé avec le personnage. */
public class ActiveEvent {
    public String eventId;
    public String placeId;
    public int startDay;
    public int endDay;

    // Constructeur sans argument pour Gson
    public ActiveEvent() {
    }

    public ActiveEvent(String eventId, String placeId, int startDay, int endDay) {
        this.eventId = eventId;
        this.placeId = placeId;
        this.startDay = startDay;
        this.endDay = endDay;
    }
}
