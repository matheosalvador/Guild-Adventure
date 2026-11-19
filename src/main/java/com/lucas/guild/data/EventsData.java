package com.lucas.guild.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Les événements du monde tels qu'écrits dans {@code events.json} : fête du marché, pénurie, caravane...
 * Chacun joue sur les prix d'une ville pendant quelques jours. Ne contient que des données.
 */
public class EventsData {

    /** Chance, en %, qu'un nouvel événement démarre chaque jour (dans une ville qui n'en a pas déjà un). */
    public int dailyChance = 15;
    public List<EventData> events = new ArrayList<>();

    public static class EventData {
        public String id;
        public String name;
        public String description;
        /** Durée, en jours (le jour du début compte). */
        public int days = 3;
        /** Variation, en %, des prix d'ACHAT dans la ville (-15 = 15 % moins cher, +40 = 40 % plus cher). */
        public int buyChangePercent;
        /** Variation, en %, du prix auquel la boutique RACHÈTE (+20 = elle paie 20 % de plus). */
        public int sellChangePercent;
        /** Objets concernés (identifiants de items.json) ; absent = tous. */
        public List<String> items;
        /** Villes où l'événement peut avoir lieu (identifiants de world.json) ; absent = n'importe laquelle. */
        public List<String> towns;
    }

    public EventData event(String id) {
        for (EventData e : events) {
            if (e.id.equals(id)) {
                return e;
            }
        }
        return null;
    }

    void validate(String file) {
        if (events == null) events = new ArrayList<>();
        if (dailyChance < 0 || dailyChance > 100) throw new IllegalStateException(file + " : \"dailyChance\" doit etre entre 0 et 100");
        List<String> seen = new ArrayList<>();
        for (int i = 0; i < events.size(); i++) {
            EventData e = events.get(i);
            String where = file + " : evenement n°" + (i + 1) + (e != null && e.id != null ? " (" + e.id + ")" : "");
            if (e == null || e.id == null || e.id.isEmpty()) throw new IllegalStateException(where + " : \"id\" manquant");
            if (seen.contains(e.id)) throw new IllegalStateException(where + " : \"id\" en double");
            seen.add(e.id);
            if (e.name == null || e.name.isEmpty()) throw new IllegalStateException(where + " : \"name\" manquant");
            if (e.days < 1) throw new IllegalStateException(where + " : \"days\" doit etre >= 1");
            if (e.buyChangePercent <= -100 || e.sellChangePercent <= -100) throw new IllegalStateException(where + " : une variation doit etre > -100");
            if (e.buyChangePercent == 0 && e.sellChangePercent == 0) throw new IllegalStateException(where + " : aucun effet (\"buyChangePercent\" ou \"sellChangePercent\" attendu)");
            if (e.items != null && e.items.isEmpty()) throw new IllegalStateException(where + " : \"items\" est vide (l'enlever pour viser tous les objets)");
            if (e.towns != null && e.towns.isEmpty()) throw new IllegalStateException(where + " : \"towns\" est vide (l'enlever pour viser toutes les villes)");
        }
    }
}
