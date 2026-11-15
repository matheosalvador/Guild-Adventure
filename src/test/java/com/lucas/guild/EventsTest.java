package com.lucas.guild;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.GameGson;
import com.lucas.guild.game.GameSession;
import com.lucas.guild.game.Shop;
import com.lucas.guild.game.WorldEvents;
import com.lucas.guild.model.ActiveEvent;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Item;

/** Événements du monde : démarrage, durée, effet sur les prix des boutiques, sauvegarde. */
class EventsTest {

    /** Un tirage qui ne déclenche jamais d'événement (le tirage du jour vaut toujours 99 %). */
    private static final Random NEVER = new Random() {
        @Override
        public int nextInt(int bound) {
            return bound - 1;
        }
    };

    private static Adventurer newPlayer() {
        return GameSession.newGame("normale").getPlayer();
    }

    private static Item item(String id) {
        return GameContent.newItem(id);
    }

    @Test
    void eventDataIsValid() {
        assertTrue(GameContent.events().events.size() >= 6);
        assertNotNull(GameContent.events().event("fete_marche"));
        assertNull(GameContent.events().event("n_importe_quoi"));
    }

    @Test
    void firstUpdateOnlyRecordsTheDay() {
        Adventurer p = newPlayer();
        assertTrue(WorldEvents.update(p, 5, new Random(1)).isEmpty());
        assertEquals(5, p.getEventsDay());
        assertTrue(p.getActiveEvents().isEmpty());
    }

    @Test
    void marketFestivalLowersPricesThenEnds() {
        Adventurer p = newPlayer();
        Shop guild = new Shop("guild");
        int normal = guild.buyPrice(p, item("healing_potion"));
        assertEquals(10, normal);

        WorldEvents.update(p, 10, NEVER);
        String message = WorldEvents.start(p, "fete_marche", "aethelgard", 10);
        assertTrue(message.contains("Fête du marché") && message.contains(com.lucas.guild.model.GameCalendar.standard().dateOf(12).toString()), message);
        assertEquals(9, guild.buyPrice(p, item("healing_potion")), "-15 % : 8,5 arrondi a 9");
        assertEquals(13, new Shop("forge").buyPrice(p, item("dagger")), "la forge aussi, dans la meme ville : 15 x 85 % = 12,75 arrondi a 13");
        assertTrue(guild.eventLine(p).contains("Fête du marché"));

        WorldEvents.update(p, 12, NEVER);
        assertFalse(p.getActiveEvents().isEmpty(), "encore actif le dernier jour");
        WorldEvents.update(p, 13, NEVER);
        assertTrue(p.getActiveEvents().isEmpty(), "fini apres le jour 12");
        assertEquals(normal, guild.buyPrice(p, item("healing_potion")));
        assertNull(guild.eventLine(p));
    }

    @Test
    void shortageOnlyConcernsItsItems() {
        Adventurer p = newPlayer();
        WorldEvents.update(p, 3, NEVER);
        WorldEvents.start(p, "penurie_potions", "aethelgard", 3);
        assertEquals(14, new Shop("guild").buyPrice(p, item("healing_potion")), "+40 % sur les potions");
        assertEquals(20, new Shop("guild").buyPrice(p, item("pouch")), "le reste ne bouge pas");
    }

    @Test
    void caravanRaisesWhatTheShopPays() {
        Adventurer p = newPlayer();
        Shop forge = new Shop("forge");
        int before = forge.sellPrice(p, item("dagger"));
        assertEquals(7, before);
        WorldEvents.update(p, 3, NEVER);
        WorldEvents.start(p, "caravane", "aethelgard", 3);
        assertEquals(9, forge.sellPrice(p, item("dagger")), "+20 % sur le rachat");
        assertEquals(15, forge.buyPrice(p, item("dagger")), "le prix d'achat ne change pas");
    }

    @Test
    void eventsOnlyAffectTheirTown() {
        Adventurer p = newPlayer();
        WorldEvents.update(p, 3, NEVER);
        WorldEvents.start(p, "mauvaise_recolte", "portbrume", 3);
        assertEquals(10, new Shop("guild").buyPrice(p, item("healing_potion")), "Aethelgard n'est pas touchee");
        p.setTownId("portbrume");
        int expected = (int) Math.round(Math.round(10 * 90 / 100.0) * 112 / 100.0);
        assertEquals(expected, new Shop("portbrume_store").buyPrice(p, item("healing_potion")));
        assertTrue(WorldEvents.describeAt(p, "portbrume").contains("Mauvaise récolte"));
        assertNull(WorldEvents.describeAt(p, "hautevigne"));
    }

    @Test
    void oneEventPerTownAndTownRestrictionsHold() {
        Adventurer p = newPlayer();
        WorldEvents.update(p, 1, NEVER);
        Random random = new Random(42);
        int started = 0;
        for (int day = 2; day < 600; day++) {
            for (ActiveEvent e : WorldEvents.update(p, day, random)) {
                started++;
                if ("filon_retrouve".equals(e.eventId)) {
                    assertEquals("hautevigne", e.placeId, "filon_retrouve n'a lieu qu'a Hautevigne");
                }
            }
            for (String town : List.of("aethelgard", "portbrume", "hautevigne")) {
                assertTrue(WorldEvents.at(p, town).size() <= 1, "un seul evenement par ville");
            }
            assertTrue(WorldEvents.at(p, "foret_loups").isEmpty(), "pas d'evenement dans une zone sauvage");
        }
        assertTrue(started > 30 && started < 200, "environ 15 % des jours : " + started + " sur 600");
    }

    @Test
    void longAbsenceIsCaughtUpWithoutFuss() {
        Adventurer p = newPlayer();
        WorldEvents.update(p, 1, NEVER);
        WorldEvents.update(p, 5000, new Random(7));
        assertEquals(5000, p.getEventsDay());
        for (ActiveEvent e : p.getActiveEvents()) {
            assertTrue(e.endDay >= 5000, "pas d'evenement perime : " + e.eventId);
        }
    }

    @Test
    void cannotStartAnEventInAWildZone() {
        Adventurer p = newPlayer();
        String message = WorldEvents.start(p, "caravane", "foret_loups", 2);
        assertTrue(message.contains("ville"), message);
        assertTrue(p.getActiveEvents().isEmpty());
        assertTrue(WorldEvents.start(p, "nimporte", "aethelgard", 2).contains("inconnu"));
    }

    @Test
    void eventsSurviveSavingAndOldSavesHaveNone() {
        Adventurer p = newPlayer();
        WorldEvents.update(p, 8, NEVER);
        WorldEvents.start(p, "caravane", "portbrume", 8);
        com.google.gson.Gson gson = GameGson.create();
        Adventurer back = gson.fromJson(gson.toJson(p), Adventurer.class);
        assertEquals(1, back.getActiveEvents().size());
        assertEquals("caravane", back.getActiveEvents().get(0).eventId);
        assertEquals(9, back.getActiveEvents().get(0).endDay);
        assertEquals(8, back.getEventsDay());

        Adventurer old = gson.fromJson("{\"name\":\"X\",\"health\":50,\"maxHealth\":100}", Adventurer.class);
        assertTrue(old.getActiveEvents().isEmpty(), "ancienne sauvegarde : aucun evenement, sans erreur");
        assertEquals(0, old.getEventsDay());
    }

    @Test
    void debugActionsStartAndClearEvents() {
        GameSession s = GameSession.newGame("normale");
        com.lucas.guild.game.DebugActions actions = new com.lucas.guild.game.DebugActions(s);
        assertTrue(actions.startEvent("fete_marche").contains("Aethelgard"));
        assertEquals(1, s.getPlayer().getActiveEvents().size());
        assertTrue(actions.clearEvents().contains("1"));
        assertTrue(s.getPlayer().getActiveEvents().isEmpty());
    }
}
