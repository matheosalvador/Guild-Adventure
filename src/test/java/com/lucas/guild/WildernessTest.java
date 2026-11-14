package com.lucas.guild;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.ZoneData;
import com.lucas.guild.game.Chests;
import com.lucas.guild.game.Combat;
import com.lucas.guild.game.DeathPenalty;
import com.lucas.guild.game.DebugMode;
import com.lucas.guild.game.GameSession;
import com.lucas.guild.game.GroundItems;
import com.lucas.guild.game.Monster;
import com.lucas.guild.game.World;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Item;

/** Zones sauvages : monstres et butin, objets au sol, coffres, embuscades, réveil après une défaite en route. */
class WildernessTest {

    @AfterEach
    void resetDebug() {
        DebugMode.setEnabled(false);
        DebugMode.setForceAmbush(false);
    }

    private static int minutesOf(GameSession s) {
        return s.getClock().getCurrentDay() * 1440 + s.getClock().getCurrentHour() * 60 + s.getClock().getCurrentMinute();
    }

    // ------------------------------------------------------------------ monstres et butin

    @Test
    void wolfAndGoblinExist() {
        assertEquals("Loup", GameContent.monster("wolf").name);
        assertEquals("Gobelin", GameContent.monster("goblin").name);
        assertTrue(GameContent.monster("wolf").health > GameContent.monster("giant_rat").health, "le loup est plus costaud que le rat");
    }

    @Test
    void monsterDropsRollByChance() {
        Monster wolf = Monster.spawn("wolf", 0, 0);
        int pelts = 0;
        Random random = new Random(1);
        for (int i = 0; i < 1000; i++) {
            for (Item item : Combat.rollDrops(wolf, random)) {
                if ("wolf_pelt".equals(item.getId())) {
                    pelts++;
                }
            }
        }
        assertTrue(pelts > 700 && pelts < 900, "environ 80 % de peaux : " + pelts);
        assertTrue(Combat.rollDrops(Monster.spawn("giant_rat", 0, 0), random).isEmpty(), "le rat ne laisse rien au sol");
    }

    @Test
    void droppedLootCanBeSold() {
        Item pelt = GameContent.newItem("wolf_pelt");
        assertNotNull(pelt);
        assertTrue(pelt.getPrix() > 0);
        assertEquals("Butin", pelt.getClass().getSimpleName());
    }

    // ------------------------------------------------------------------ objets au sol

    @Test
    void groundItemsDropPickupAndRange() {
        GroundItems ground = new GroundItems();
        Item potion = GameContent.newItem("healing_potion");
        GroundItems.Drop drop = ground.drop("zone", potion, 10f, 10f);

        assertEquals(1, ground.in("zone").size());
        assertEquals(0, ground.in("autre lieu").size(), "chaque lieu a ses objets");
        assertNull(ground.nearest("zone", 0f, 0f), "trop loin");
        assertEquals(drop, ground.nearest("zone", 11f, 10.5f));
        ground.remove("zone", drop);
        assertNull(ground.nearest("zone", 10f, 10f));
    }

    @Test
    void nearestGroundItemWins() {
        GroundItems ground = new GroundItems();
        ground.drop("z", GameContent.newItem("dagger"), 2f, 0f);
        GroundItems.Drop closer = ground.drop("z", GameContent.newItem("pouch"), 1f, 0f);
        assertEquals(closer, ground.nearest("z", 0f, 0f));
    }

    // ------------------------------------------------------------------ coffres

    @Test
    void chestGivesGoldAndItemsThenRefills() {
        GameSession s = GameSession.newGame("normale");
        Adventurer p = s.getPlayer();
        ZoneData.ChestData chest = new ZoneData.ChestData();
        chest.gold = 25;
        chest.items = List.of("healing_potion");
        chest.respawnDays = 7;
        String key = Chests.key("foret_loups", 0);

        int gold = p.getGold();
        int items = p.getInventoryItems().size();
        List<Item> overflow = new ArrayList<>();
        String message = Chests.open(p, key, chest, 10, overflow);
        assertNotNull(message);
        assertTrue(message.contains("25 Po"), message);
        assertEquals(gold + 25, p.getGold());
        assertEquals(items + 1, p.getInventoryItems().size());
        assertTrue(overflow.isEmpty());

        assertFalse(Chests.isFull(p, key, chest, 12));
        assertNull(Chests.open(p, key, chest, 12, overflow), "vide : rien de plus");
        assertEquals(gold + 25, p.getGold());
        assertTrue(Chests.isFull(p, key, chest, 17), "rempli de nouveau apres 7 jours");
    }

    @Test
    void heavyChestItemsFallToTheGround() {
        GameSession s = GameSession.newGame("realiste");
        Adventurer p = s.getPlayer();
        ZoneData.ChestData chest = new ZoneData.ChestData();
        chest.items = new ArrayList<>(List.of("plate_armor", "plate_armor"));
        List<Item> overflow = new ArrayList<>();
        // la charge de départ (25 kg) ne porte pas deux armures de plaques (15 kg chacune)
        String message = Chests.open(p, "z#0", chest, 1, overflow);
        assertNotNull(message);
        assertEquals(1, overflow.size(), "la deuxieme armure est trop lourde");
        assertTrue(message.contains("tombe au sol"), message);
    }

    @Test
    void openedChestsSurviveSaving() {
        GameSession s = GameSession.newGame("normale");
        s.getPlayer().getOpenedChests().put("foret_loups#1", 42);
        com.google.gson.Gson gson = com.lucas.guild.data.GameGson.create();
        Adventurer back = gson.fromJson(gson.toJson(s.getPlayer()), Adventurer.class);
        assertEquals(42, back.getOpenedChests().get("foret_loups#1"));
        Adventurer old = gson.fromJson("{\"name\":\"X\",\"health\":50,\"maxHealth\":100}", Adventurer.class);
        assertTrue(old.getOpenedChests().isEmpty(), "ancienne sauvegarde : aucun coffre ouvert, sans erreur");
    }

    // ------------------------------------------------------------------ zones dans le monde

    @Test
    void zonesAreOnTheMap() {
        GameSession s = GameSession.newGame("normale");
        Adventurer p = s.getPlayer();
        assertTrue(GameContent.world().place("foret_loups").zoneFile != null);
        String message = World.travel(s, "foret_loups");
        assertNotNull(message);
        assertEquals("foret_loups", p.getTownId());
        assertEquals("aethelgard", p.getHomeTownId(), "une zone n'est pas une ville : on se souvient de la derniere ville");
        assertNotNull(GameContent.zone("foret_loups"));
        assertEquals(GameContent.zone("foret_loups").arrival.z, GameContent.arrival("foret_loups").z);
    }

    @Test
    void everyZoneIsComplete() {
        for (String id : List.of("foret_loups", "ruines_gobelines")) {
            ZoneData zone = GameContent.zone(id);
            assertFalse(zone.monsters.isEmpty(), id + " : des monstres");
            assertFalse(zone.chests.isEmpty(), id + " : des coffres");
            assertNotNull(zone.exit, id + " : une borne de route");
        }
        assertNotNull(GameContent.ambushZone(), "le decor des embuscades");
    }

    // ------------------------------------------------------------------ embuscades

    @Test
    void noAmbushOnEasyAndAlwaysWhenForced() {
        GameSession easy = GameSession.newGame("facile");
        Random random = new Random(7);
        for (int i = 0; i < 200; i++) {
            assertFalse(World.shouldAmbush(easy, "portbrume", random), "pas d'embuscade en facile");
        }
        DebugMode.setEnabled(true);
        DebugMode.setForceAmbush(true);
        assertTrue(World.shouldAmbush(easy, "portbrume", random), "debug : embuscade forcee");
        assertFalse(World.shouldAmbush(easy, "foret_loups", random), "jamais en allant dans une zone sauvage");
    }

    @Test
    void ambushChanceFollowsDifficulty() {
        Random random = new Random(3);
        for (String difficulty : new String[] {"normale", "realiste"}) {
            GameSession s = GameSession.newGame(difficulty);
            int hits = 0;
            for (int i = 0; i < 1000; i++) {
                if (World.shouldAmbush(s, "portbrume", random)) {
                    hits++;
                }
            }
            int expected = GameContent.ambushPercent(difficulty) * 10;
            assertTrue(Math.abs(hits - expected) < 80, difficulty + " : " + hits + " embuscades sur 1000 (attendu " + expected + ")");
        }
        assertTrue(GameContent.ambushPercent("realiste") > GameContent.ambushPercent("normale"));
    }

    @Test
    void ambushedTravelSplitsTheTime() {
        GameSession s = GameSession.newGame("normale");
        int start = minutesOf(s);
        String warning = World.startAmbushedTravel(s, "portbrume");
        assertNotNull(warning);
        assertEquals("aethelgard", s.getPlayer().getTownId(), "pas encore arrive");
        assertEquals("portbrume", s.getPendingDestination());
        int before = minutesOf(s) - start;
        assertTrue(before > 0 && before < 395, "une partie du trajet seulement : " + before);

        String arrival = World.finishTravel(s);
        assertNotNull(arrival);
        assertEquals(395, minutesOf(s) - start, "la duree totale ne change pas");
        assertEquals("portbrume", s.getPlayer().getTownId());
        assertTrue(s.getPlayer().getVisitedPlaces().contains("portbrume"));
        assertNull(s.getPendingDestination());
        assertNull(World.finishTravel(s), "plus de voyage en cours");
    }

    // ------------------------------------------------------------------ défaite en route

    @Test
    void deathOnAmbushWakesUpInTheTownWhereTheTripBegan() {
        GameSession s = GameSession.newGame("normale");
        World.startAmbushedTravel(s, "portbrume");
        DeathPenalty.apply(s);
        assertEquals("aethelgard", s.getPlayer().getTownId(), "reveil a l'auberge de la ville de depart");
        assertNull(s.getPendingDestination(), "le voyage est abandonne");
    }

    @Test
    void deathInAWildZoneWakesUpInTheLastTown() {
        GameSession s = GameSession.newGame("normale");
        World.travel(s, "portbrume");
        World.travel(s, "ruines_gobelines");
        assertEquals("ruines_gobelines", s.getPlayer().getTownId());
        DeathPenalty.apply(s);
        assertEquals("portbrume", s.getPlayer().getTownId(), "la derniere ville visitee");
    }
}
