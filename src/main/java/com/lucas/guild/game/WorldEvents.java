package com.lucas.guild.game;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.lucas.guild.data.EventsData;
import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.WorldData;
import com.lucas.guild.model.ActiveEvent;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Item;

/**
 * Les événements du monde (events.json) : chaque jour, un événement peut démarrer dans une ville
 * (fête, pénurie, caravane...) et joue sur les prix de ses boutiques pendant quelques jours.
 * L'état est gardé dans le personnage, donc sauvegardé. Sans dépendance à l'interface.
 */
public final class WorldEvents {

    /** On ne rattrape pas plus de jours que ça après un très long sommeil ou voyage. */
    private static final int MAX_CATCH_UP_DAYS = 60;

    private WorldEvents() {
    }

    /**
     * Fait avancer les événements jusqu'à {@code today} : ceux qui sont finis disparaissent, de nouveaux
     * peuvent démarrer (un tirage par jour écoulé).
     *
     * @return les événements qui viennent de démarrer
     */
    public static List<ActiveEvent> update(Adventurer player, int today, Random random) {
        List<ActiveEvent> started = new ArrayList<>();
        int last = player.getEventsDay();
        if (last <= 0) {
            player.setEventsDay(today);   // premiere fois : on part d'aujourd'hui, sans rattrapage
            return started;
        }
        if (today <= last) {
            return started;
        }
        EventsData data = GameContent.events();
        WorldData world = GameContent.world();
        for (int day = Math.max(last + 1, today - MAX_CATCH_UP_DAYS + 1); day <= today; day++) {
            final int d = day;
            Iterator<ActiveEvent> it = player.getActiveEvents().iterator();
            while (it.hasNext()) {
                ActiveEvent e = it.next();
                if (e.endDay < d || data.event(e.eventId) == null) {
                    it.remove();   // fini, ou retire des donnees
                }
            }
            if (!data.events.isEmpty() && random.nextInt(100) < data.dailyChance) {
                ActiveEvent event = draw(player, data, world, d, random);
                if (event != null) {
                    player.getActiveEvents().add(event);
                    started.add(event);
                }
            }
        }
        // les événements démarrés puis finis dans le rattrapage ne comptent plus
        started.removeIf(e -> !player.getActiveEvents().contains(e));
        player.setEventsDay(today);
        return started;
    }

    /** Tire une ville sans événement et un événement qui peut s'y dérouler, ou null. */
    private static ActiveEvent draw(Adventurer player, EventsData data, WorldData world, int day, Random random) {
        List<String> towns = new ArrayList<>();
        for (WorldData.PlaceData place : world.places) {
            if (place.zoneFile == null && at(player, place.id).isEmpty()) {
                towns.add(place.id);
            }
        }
        if (towns.isEmpty()) {
            return null;
        }
        String town = towns.get(random.nextInt(towns.size()));
        List<EventsData.EventData> possible = new ArrayList<>();
        for (EventsData.EventData e : data.events) {
            if (e.towns == null || e.towns.contains(town)) {
                possible.add(e);
            }
        }
        if (possible.isEmpty()) {
            return null;
        }
        EventsData.EventData chosen = possible.get(random.nextInt(possible.size()));
        return new ActiveEvent(chosen.id, town, day, day + chosen.days - 1);
    }

    /** Démarre l'événement {@code eventId} dans {@code placeId} (debug). @return la phrase décrivant ce qui s'est passé */
    public static String start(Adventurer player, String eventId, String placeId, int today) {
        EventsData.EventData def = GameContent.events().event(eventId);
        if (def == null) {
            return "Evenement inconnu : " + eventId + ".";
        }
        WorldData.PlaceData place = GameContent.world().place(placeId);
        if (place == null || place.zoneFile != null) {
            return "Un événement a lieu dans une ville, pas ici.";
        }
        player.getActiveEvents().removeIf(e -> e.placeId.equals(placeId));
        player.getActiveEvents().add(new ActiveEvent(eventId, placeId, today, today + def.days - 1));
        if (player.getEventsDay() <= 0) {
            player.setEventsDay(today);
        }
        return def.name + " à " + place.name + " (jusqu'au " + untilDate(today + def.days - 1) + ").";
    }

    /** Les événements en cours dans un lieu. */
    public static List<ActiveEvent> at(Adventurer player, String placeId) {
        List<ActiveEvent> result = new ArrayList<>();
        for (ActiveEvent e : player.getActiveEvents()) {
            if (e.placeId.equals(placeId)) {
                result.add(e);
            }
        }
        return result;
    }

    private static boolean concerns(EventsData.EventData def, Item item) {
        return def.items == null || def.items.contains(item.getId());
    }

    /** Prix d'achat dans la ville, en % du prix normal (100 = pas d'événement). */
    public static int buyPercent(Adventurer player, String townId, Item item) {
        double factor = 1.0;
        for (ActiveEvent e : at(player, townId)) {
            EventsData.EventData def = GameContent.events().event(e.eventId);
            if (def != null && concerns(def, item)) {
                factor *= (100 + def.buyChangePercent) / 100.0;
            }
        }
        return (int) Math.round(factor * 100);
    }

    /** Prix auquel la boutique de la ville rachète, en % du prix normal (100 = pas d'événement). */
    public static int sellPercent(Adventurer player, String townId, Item item) {
        double factor = 1.0;
        for (ActiveEvent e : at(player, townId)) {
            EventsData.EventData def = GameContent.events().event(e.eventId);
            if (def != null && concerns(def, item)) {
                factor *= (100 + def.sellChangePercent) / 100.0;
            }
        }
        return (int) Math.round(factor * 100);
    }

    /** « Fête du marché (jusqu'au jour 12) : tout coûte 15 % de moins. », ou null s'il n'y a rien à signaler dans cette ville. */
    public static String describeAt(Adventurer player, String placeId) {
        List<String> parts = new ArrayList<>();
        for (ActiveEvent e : at(player, placeId)) {
            parts.add(describe(e));
        }
        return parts.isEmpty() ? null : String.join(" ", parts);
    }

    /** « Fête du marché (jusqu'au jour 12) : prix d'achat -15 %. » */
    public static String describe(ActiveEvent e) {
        EventsData.EventData def = GameContent.events().event(e.eventId);
        if (def == null) {
            return e.eventId;
        }
        StringBuilder effects = new StringBuilder();
        if (def.buyChangePercent != 0) {
            effects.append("prix d'achat ").append(signed(def.buyChangePercent));
        }
        if (def.sellChangePercent != 0) {
            if (effects.length() > 0) effects.append(", ");
            effects.append("rachat ").append(signed(def.sellChangePercent));
        }
        String scope = def.items != null ? " sur certains objets" : "";
        return def.name + " (jusqu'au " + untilDate(e.endDay) + ") : " + effects + scope + ".";
    }

    /** « Mercredi 12 Printemps, An 1 » : la date du calendrier du jeu, que le joueur voit sur son écran. */
    private static String untilDate(int day) {
        return com.lucas.guild.model.GameCalendar.standard().dateOf(day).toString();
    }

    private static String signed(int percent) {
        return (percent > 0 ? "+" : "") + percent + " %";
    }
}
