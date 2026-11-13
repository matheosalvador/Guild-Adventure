package com.lucas.guild;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.TownData;
import com.lucas.guild.data.WorldData;
import com.lucas.guild.game.GameSession;
import com.lucas.guild.game.SaveManager;
import com.lucas.guild.game.Shop;
import com.lucas.guild.game.World;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.GameData;
import com.lucas.guild.model.Item;

class WorldTest {

    private static List<String> names(List<World.Route> routes) {
        return routes.stream().map(r -> r.place.id).collect(Collectors.toList());
    }

    @Test
    void worldData() {
        WorldData world = GameContent.world();
        assertEquals("aethelgard", world.startPlace);
        assertEquals(3, world.places.size());
        assertEquals(List.of("portbrume", "hautevigne"), world.neighbors("aethelgard").stream().map(p -> p.id).collect(Collectors.toList()));
        assertTrue(world.neighbors("portbrume").stream().anyMatch(p -> p.id.equals("aethelgard")), "les routes vont dans les deux sens");
        assertEquals(Math.hypot(30, 14), world.distanceKm("aethelgard", "portbrume"), 1e-6);
    }

    @Test
    void travelMinutesFollowDistanceAndSpeed() {
        assertEquals(360, World.travelMinutes(30, 5, 1.0), "30 km a 5 km/h = 6 h");
        assertEquals(720, World.travelMinutes(30, 5, 0.5), "moitie moins vite : deux fois plus long");
        assertEquals(5, World.travelMinutes(0.01, 5, 1.0), "au moins 5 minutes");
        assertEquals(395, World.travelMinutes(Math.hypot(30, 14), 5, 1.0), "arrondi aux 5 minutes");
        assertEquals("6 h 35", World.formatDuration(395));
        assertEquals("6 h", World.formatDuration(360));
        assertEquals("45 min", World.formatDuration(45));
    }

    @Test
    void travelMovesThePlayerAndTime() {
        GameSession s = GameSession.newGame("normale");
        Adventurer p = s.getPlayer();
        assertEquals("aethelgard", p.getTownId());
        assertEquals(List.of("aethelgard"), p.getVisitedPlaces());
        assertEquals(List.of("portbrume", "hautevigne"), names(World.routesFrom(p)));

        int start = s.getClock().getCurrentDay() * 1440 + s.getClock().getCurrentHour() * 60 + s.getClock().getCurrentMinute();
        String message = World.travel(s, "portbrume");
        assertNotNull(message);
        assertTrue(message.contains("Port-Brume") && message.contains("6 h 35"), message);
        int end = s.getClock().getCurrentDay() * 1440 + s.getClock().getCurrentHour() * 60 + s.getClock().getCurrentMinute();
        assertEquals(395, end - start);
        assertEquals("portbrume", p.getTownId());
        assertEquals(List.of("aethelgard", "portbrume"), p.getVisitedPlaces());
        assertEquals(List.of("aethelgard", "hautevigne"), names(World.routesFrom(p)));
    }

    @Test
    void noRouteNoTravel() {
        GameSession s = GameSession.newGame("normale");
        assertNull(World.travel(s, "aethelgard"), "on ne voyage pas vers l'endroit ou on est");
        assertNull(World.travel(s, "nulle-part"));
        assertEquals("aethelgard", s.getPlayer().getTownId());
    }

    @Test
    void heavyLoadSlowsTravel() {
        GameSession light = GameSession.newGame("normale");
        GameSession heavy = GameSession.newGame("normale");
        while (heavy.getPlayer().addItem(GameContent.newItem("healing_potion"))) { /* plein */ }
        int a = World.routesFrom(light.getPlayer()).get(0).minutes;
        int b = World.routesFrom(heavy.getPlayer()).get(0).minutes;
        assertTrue(b > a, "charge pleine : trajet plus long (" + a + " -> " + b + ")");
    }

    @Test
    void locationSurvivesSaveAndOldSavesStartAtTheStartPlace() throws Exception {
        GameSession s = GameSession.newGame("normale");
        World.travel(s, "hautevigne");
        SaveManager sm = new SaveManager();
        assertTrue(sm.saveGame(s.getPlayer(), s.getClock(), "normale", 0, 2, 52, "Town"));
        GameData data = sm.loadGame();
        Adventurer back = GameSession.fromSave(data).getPlayer();
        assertEquals("hautevigne", back.getTownId());
        assertEquals(List.of("aethelgard", "hautevigne"), back.getVisitedPlaces());
        Files.deleteIfExists(Paths.get("save.json"));

        GameData old = new com.google.gson.Gson().fromJson("{\"playerData\":{\"name\":\"Vieux\",\"health\":80,\"maxHealth\":100},\"currentDay\":42}", GameData.class);
        Adventurer o = GameSession.fromSave(old).getPlayer();
        assertEquals("aethelgard", o.getTownId());
        assertEquals(List.of("aethelgard"), o.getVisitedPlaces());

        GameData removed = new com.google.gson.Gson().fromJson("{\"playerData\":{\"name\":\"X\",\"health\":80,\"maxHealth\":100,\"townId\":\"ville-supprimee\"},\"currentDay\":42}", GameData.class);
        assertEquals("aethelgard", GameSession.fromSave(removed).getPlayer().getTownId(), "un lieu qui n'existe plus : retour au depart");
    }

    @Test
    void everyTownIsComplete() {
        for (WorldData.PlaceData place : GameContent.world().places) {
            TownData town = GameContent.town(place.id);
            assertTrue(town.buildings.stream().anyMatch(b -> "inn".equals(b.interior)), place.id + " : une auberge");
            assertNotNull(town.arrival, place.id + " : un point d'arrivee");
            assertTrue(town.decor.stream().anyMatch(p -> "gate".equals(p.action)), place.id + " : une porte vers la carte");
            for (TownData.BuildingData b : town.buildings) {
                if ("store".equals(b.interior)) {
                    assertNotNull(GameContent.shop(b.shop), place.id + " : boutique " + b.shop);
                }
                assertEquals(b.x, b.exit.x, 1e-6, "la sortie est devant la porte");
            }
        }
    }

    @Test
    void shopPricesDifferByTown() {
        Item potion = GameContent.newItem("healing_potion");   // 10 Po au catalogue
        Adventurer p = GameSession.newGame("normale").getPlayer();
        Shop guild = new Shop("guild");
        Shop port = new Shop("portbrume_store");
        Shop mine = new Shop("hautevigne_store");
        assertEquals(10, guild.buyPrice(p, potion));
        assertEquals(9, port.buyPrice(p, potion), "Port-Brume : 10 % moins cher");
        assertEquals(12, mine.buyPrice(p, potion), "Hautevigne : 15 % plus cher (11,5 arrondi)");
        assertEquals(5, guild.sellPrice(p, potion));
        assertEquals(4, port.sellPrice(p, potion), "9 Po x 55 %");
        assertEquals(5, mine.sellPrice(p, potion), "12 Po x 45 %");
        assertFalse(port.getStock().isEmpty());
        assertTrue(mine.getStock().stream().anyMatch(i -> "battle_axe".equals(i.getId())), "le village minier vend des haches");
    }

    @Test
    void buyingInOtherTownsShopWorks() {
        Adventurer p = GameSession.newGame("normale").getPlayer();
        Shop port = new Shop("portbrume_store");
        Item wanted = port.getStock().get(0);
        int gold = p.getGold();
        assertNull(port.buy(p, wanted));
        assertEquals(gold - port.buyPrice(p, wanted), p.getGold());
    }
}
