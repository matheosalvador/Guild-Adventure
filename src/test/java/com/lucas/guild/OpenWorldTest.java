package com.lucas.guild;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.OpenWorldData;
import com.lucas.guild.game.GameSession;
import com.lucas.guild.game.Respawns;
import com.lucas.guild.game.SaveManager;
import com.lucas.guild.game.Terrain;
import com.lucas.guild.game.WorldLayout;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.GameData;

/** Le monde ouvert, sans affichage : positions, routes, campagne fabriquée, monstres, sauvegarde. */
class OpenWorldTest {

    private final WorldLayout layout = WorldLayout.standard();

    @Test
    void placesSitAtTheirMapPositionTimesTheScale() {
        float[] forest = layout.origin("foret_loups");
        assertEquals(-900f, forest[0], 1e-3);
        assertEquals(-1200f, forest[1], 1e-3);
        float[] w = layout.toWorld("foret_loups", 12f, 70f);
        float[] back = layout.toLocal("foret_loups", w[0], w[1]);
        assertEquals(12f, back[0], 1e-3);
        assertEquals(70f, back[1], 1e-3);
        float[] km = layout.toMap(-900f, -1200f);
        assertEquals(-9f, km[0], 1e-3);
        assertEquals(-12f, km[1], 1e-3);
    }

    @Test
    void thePlaceUnderTheFeet() {
        assertEquals("aethelgard", layout.placeAt(0f, 0f));
        assertEquals("aethelgard", layout.placeAt(-65f, 65f));
        assertEquals("foret_loups", layout.placeAt(-900f, -1130f));
        assertNull(layout.placeAt(-450f, -500f), "en pleine campagne");
        assertEquals(0f, layout.distanceToPlace("aethelgard", 50f, 50f), 1e-3);
        assertEquals(80f, layout.distanceToPlace("aethelgard", 150f, 0f), 1e-3);
    }

    @Test
    void everyLinkOfTheMapHasItsRoad() {
        int links = 0;
        for (List<String> link : GameContent.world().links) {
            boolean hidden = GameContent.world().place(link.get(0)).hidden || GameContent.world().place(link.get(1)).hidden;
            if (!hidden) {
                links++;
                assertNotNull(layout.road(link.get(0), link.get(1)), "pas de route " + link);
            }
        }
        assertEquals(links, layout.roads().size());
        assertTrue(links >= 10, "toutes les liaisons : " + links);
    }

    @Test
    void walkingARoadTakesAboutAsLongAsTheMapSays() {
        StringBuilder table = new StringBuilder();
        for (OpenWorldData.RoadData road : layout.roads()) {
            // 8 m par seconde reelle, 60 s reelles par heure de jeu : une seconde = une minute de jeu
            float minutes = WorldLayout.length(road) / 8f;
            double km = GameContent.world().distanceKm(road.from, road.to);
            double mapMinutes = km / GameContent.world().walkingKmh * 60.0;
            table.append(String.format(java.util.Locale.ROOT, "%s -> %s : %.0f min a pied, %.0f min sur la carte%n", road.from, road.to, minutes, mapMinutes));
            assertTrue(minutes <= mapMinutes * 1.4 + 20, "trop long : " + table);
            assertTrue(minutes >= mapMinutes * 0.8, "trop court : " + table);
        }
        System.out.print(table);
        float forest = WorldLayout.length(layout.road("aethelgard", "foret_loups")) / 8f;
        assertTrue(forest >= 165 && forest <= 200, "Aethelgard -> foret : " + forest + " minutes de jeu (environ 3 h)");
    }

    @Test
    void aRoadLeavesByTheGateAndArrivesAtTheOtherDoor() {
        OpenWorldData.RoadData road = layout.road("aethelgard", "foret_loups");
        OpenWorldData.PointData first = road.points.get(0);
        OpenWorldData.PointData last = road.points.get(road.points.size() - 1);
        assertEquals("aethelgard", layout.placeAt(first.x, first.z));
        assertTrue(Math.abs(first.x) < 4f && first.z > 60f, "la route part de la porte sud (x 0, z 60)");
        // elle s'arrete juste derriere l'ouverture de la haie (z 81), la ou commence le chemin de la foret (z 80)
        float[] pathStart = layout.toWorld("foret_loups", 0f, 80f);
        assertEquals(pathStart[0], last.x, 0.5f);
        assertEquals(pathStart[1], last.z, 0.5f);
        // dans l'autre sens, le meme trace a l'envers
        OpenWorldData.RoadData back = layout.road("foret_loups", "aethelgard");
        assertEquals(last.x, back.points.get(0).x, 1e-3);
        assertEquals(first.z, back.points.get(back.points.size() - 1).z, 1e-3);
    }

    @Test
    void noRoadCutsThroughAPlace() {
        for (OpenWorldData.RoadData road : layout.roads()) {
            List<OpenWorldData.PointData> pts = road.points;
            for (int i = 0; i + 1 < pts.size(); i++) {
                float[] p = {pts.get(i).x, pts.get(i).z};
                float[] q = {pts.get(i + 1).x, pts.get(i + 1).z};
                for (String id : layout.footprints()) {
                    boolean ownDoor = (i == 0 && id.equals(road.from)) || (i == pts.size() - 2 && id.equals(road.to));
                    if (ownDoor) {
                        continue;   // le premier et le dernier bout passent la porte
                    }
                    assertFalse(WorldLayout.crosses(layout.origin(id), GameContent.openWorld().halfOf(id) + 1f, p, q),
                            "la route " + road.from + " -> " + road.to + " traverse " + id + " (bout " + i + ")");
                }
            }
        }
    }

    @Test
    void sitesSitBesideTheRoadsNotOnThem() {
        OpenWorldData data = GameContent.openWorld();
        assertTrue(data.sites.size() >= 6, "des choses a croiser en chemin : " + data.sites.size());
        for (OpenWorldData.SiteData site : data.sites) {
            float clear = layout.distanceToRoad(site.x, site.z) - site.half;
            assertTrue(clear >= data.roadWidth / 2f, site.id + " : a " + clear + " m du bord de la route seulement");
            assertTrue(clear <= 80f, site.id + " : trop loin de toute route pour qu'on le croise (" + clear + " m)");
            assertNull(layout.placeAt(site.x, site.z), site.id + " : dans un lieu");
            assertEquals(site, layout.siteAt(site.x + site.half - 0.5f, site.z), site.id + " : le HUD le nomme quand on y est");
        }
    }

    @Test
    void thingsToGatherAreAlwaysAtTheSamePlaceAndOffTheRoad() {
        int found = 0;
        for (int tx = -9; tx <= 1; tx++) {
            for (int tz = -11; tz <= 1; tz++) {
                List<Terrain.Node> a = Terrain.gatherables(layout, tx, tz);
                List<Terrain.Node> b = Terrain.gatherables(layout, tx, tz);
                assertEquals(a.size(), b.size());
                for (int i = 0; i < a.size(); i++) {
                    assertEquals(a.get(i).key, b.get(i).key);
                    assertEquals(a.get(i).x, b.get(i).x, 0f);
                    assertTrue(Terrain.isFree(layout, a.get(i).x, a.get(i).z), "sur une route ou dans un lieu : " + a.get(i).key);
                }
                found += a.size();
            }
        }
        // 0,6 herbe et 0,25 caillou par carreau en moyenne, sur 132 carreaux (moins les routes et les lieux)
        assertTrue(found > 60 && found < 160, "choses a ramasser : " + found);
    }

    @Test
    void aGatheredThingGrowsBack() {
        GameSession s = GameSession.newGame("normale");
        Adventurer p = s.getPlayer();
        OpenWorldData.GatherData herb = GameContent.openWorld().gathering.get(0);
        assertTrue(com.lucas.guild.game.Gathering.isThere(p, "g:1:2:0:0", 10, herb.respawnDays));
        int before = p.getInventoryItems().size();
        String message = com.lucas.guild.game.Gathering.take(p, "g:1:2:0:0", herb, 10);
        assertEquals(before + 1, p.getInventoryItems().size(), message);
        assertTrue(message.contains("Herbe de soin"), message);
        assertFalse(com.lucas.guild.game.Gathering.isThere(p, "g:1:2:0:0", 10 + herb.respawnDays - 1, herb.respawnDays));
        assertTrue(com.lucas.guild.game.Gathering.isThere(p, "g:1:2:0:0", 10 + herb.respawnDays, herb.respawnDays), "repousse");
        assertTrue(com.lucas.guild.game.Gathering.isThere(p, "g:1:2:0:1", 10, herb.respawnDays), "les autres ne sont pas touchees");
    }

    @Test
    void tooHeavyToGatherLeavesTheThingThere() {
        GameSession s = GameSession.newGame("normale");
        Adventurer p = s.getPlayer();
        while (p.addItem(GameContent.newItem("iron_ore"))) { /* plein */ }
        OpenWorldData.GatherData ore = GameContent.openWorld().gathering.get(1);
        String message = com.lucas.guild.game.Gathering.take(p, "g:0:0:1:0", ore, 3);
        assertTrue(message.startsWith("Trop charg"), message);
        assertTrue(com.lucas.guild.game.Gathering.isThere(p, "g:0:0:1:0", 3, ore.respawnDays), "le caillou est toujours la");
    }

    @Test
    void fastTravelGoesOnlyToPlacesAlreadyReached() {
        GameSession s = GameSession.newGame("normale");
        s.setWorldPosition(-450f, -560f);   // en chemin, entre Aethelgard et la foret
        java.util.Set<String> offered = new java.util.HashSet<>();
        for (com.lucas.guild.game.World.Route r : com.lucas.guild.game.World.fastTravelRoutes(s, layout)) {
            offered.add(r.place.id);
        }
        assertEquals(java.util.Collections.singleton("aethelgard"), offered, "au depart, seule la ville de depart est atteinte");
        assertNull(com.lucas.guild.game.World.fastTravel(s, layout, "portbrume"), "Port-Brume n'a jamais ete atteint");

        s.getPlayer().markVisited("foret_loups");
        int before = s.getClock().getCurrentDay() * 1440 + s.getClock().getCurrentHour() * 60 + s.getClock().getCurrentMinute();
        String message = com.lucas.guild.game.World.fastTravel(s, layout, "foret_loups");
        assertNotNull(message);
        int after = s.getClock().getCurrentDay() * 1440 + s.getClock().getCurrentHour() * 60 + s.getClock().getCurrentMinute();
        // de (-4,5 ; -5,6) km a (-9 ; -12) km : 7,8 km, environ 1 h 35 a 5 km/h
        assertTrue(after - before >= 80 && after - before <= 110, "duree depuis la position : " + (after - before) + " min");
        assertEquals("foret_loups", s.getPlayer().getTownId());

        s.setWorldPosition(0f, 0f);   // dans Aethelgard : on ne la propose pas
        for (com.lucas.guild.game.World.Route r : com.lucas.guild.game.World.fastTravelRoutes(s, layout)) {
            assertFalse("aethelgard".equals(r.place.id), "le lieu ou l'on est n'est pas propose");
        }
    }

    @Test
    void aHiddenFloorLeavesByTheEntranceOfItsPlace() {
        assertEquals("mine_galeries", layout.portalFor("mine_galeries"));
        assertEquals("mine_galeries", layout.portalFor("mine_chambre"));
        assertEquals("corvenne_cour", layout.portalFor("corvenne_crypte"));
        assertNull(layout.portalFor("aethelgard"));
        float[] exit = layout.portalExit("fosse_debardeurs");
        assertTrue(layout.distanceToPlace("fosse_debardeurs", exit[0], exit[1]) == 0f, "on ressort devant la porte, dans le carre de l'entree");
    }

    @Test
    void theCountrysideIsAlwaysTheSameAtTheSamePlace() {
        List<Terrain.Tree> a = Terrain.trees(layout, -3, -5);
        List<Terrain.Tree> b = Terrain.trees(layout, -3, -5);
        assertEquals(a.size(), b.size());
        for (int i = 0; i < a.size(); i++) {
            assertEquals(a.get(i).x, b.get(i).x, 0f);
            assertEquals(a.get(i).z, b.get(i).z, 0f);
            assertEquals(a.get(i).kind, b.get(i).kind);
        }
        assertEquals(GameContent.openWorld().treesPerTile, Terrain.trees(layout, 30, 30).size(), "loin de tout : tous les arbres");
    }

    @Test
    void noTreeOnTheRoadNorInAPlace() {
        float clear = GameContent.openWorld().roadWidth / 2f + Terrain.ROAD_MARGIN;
        int checked = 0;
        for (int tx = -9; tx <= 1; tx++) {
            for (int tz = -11; tz <= 1; tz++) {
                for (Terrain.Tree t : Terrain.trees(layout, tx, tz)) {
                    assertTrue(layout.distanceToRoad(t.x, t.z) >= clear, "arbre sur la route en " + t.x + ", " + t.z);
                    assertNull(layout.placeAt(t.x, t.z), "arbre dans un lieu en " + t.x + ", " + t.z);
                    checked++;
                }
            }
        }
        assertTrue(checked > 500, "assez d'arbres verifies : " + checked);
    }

    @Test
    void aKilledMonsterComesBackAfterThreeDays() {
        Adventurer p = GameSession.newGame("normale").getPlayer();
        int days = GameContent.openWorld().monsterRespawnDays;
        String key = Respawns.key("foret_loups", 2);
        assertTrue(Respawns.isAlive(p, key, 10, days));
        Respawns.killed(p, key, 10);
        assertFalse(Respawns.isAlive(p, key, 10, days));
        assertFalse(Respawns.isAlive(p, key, 10 + days - 1, days));
        assertTrue(Respawns.isAlive(p, key, 10 + days, days));
        assertTrue(Respawns.isAlive(p, Respawns.key("foret_loups", 3), 10, days), "les autres ne sont pas touches");
    }

    @Test
    void savedOnTheRoadAndReadBack() throws Exception {
        Files.deleteIfExists(Paths.get("save.json"));
        GameSession s = GameSession.newGame("normale");
        s.getClock().setCurrentHour(14);
        s.getClock().setCurrentMinute(37);
        Respawns.killed(s.getPlayer(), Respawns.key("foret_loups", 1), 4);
        assertTrue(new SaveManager().saveGame(s.getPlayer(), s.getClock(), "normale", -480.5f, 1.6f, -560.25f, "World"));
        GameData data = new SaveManager().loadGame();
        GameSession back = GameSession.fromSave(data);
        assertEquals("World", back.getSavedScreen());
        assertEquals(-480.5f, back.getSavedX(), 1e-3);
        assertEquals(-560.25f, back.getSavedZ(), 1e-3);
        assertEquals(14, back.getClock().getCurrentHour());
        assertEquals(37, back.getClock().getCurrentMinute());
        assertEquals(Integer.valueOf(4), back.getPlayer().getMonsterKills().get("foret_loups#1"));
        Files.deleteIfExists(Paths.get("save.json"));
    }
}
