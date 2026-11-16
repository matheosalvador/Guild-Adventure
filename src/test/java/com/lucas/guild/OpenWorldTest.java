package com.lucas.guild;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.OpenWorldData;
import com.lucas.guild.data.ZoneData;
import com.lucas.guild.game.GameSession;
import com.lucas.guild.game.Respawns;
import com.lucas.guild.game.SaveManager;
import com.lucas.guild.game.Terrain;
import com.lucas.guild.game.WorldLayout;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.GameData;

/** Le prototype du monde ouvert, sans affichage : positions, route, campagne fabriquée, monstres, sauvegarde. */
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
        assertEquals("aethelgard", layout.placeAt(-95f, 95f));
        assertEquals("foret_loups", layout.placeAt(-900f, -1130f));
        assertNull(layout.placeAt(-450f, -500f), "en pleine campagne");
        assertEquals(0f, layout.distanceToPlace("aethelgard", 50f, 50f), 1e-3);
        assertEquals(50f, layout.distanceToPlace("aethelgard", 150f, 0f), 1e-3);
    }

    @Test
    void walkingTheRoadTakesAboutThreeHoursLikeTheMap() {
        OpenWorldData.RoadData road = GameContent.openWorld().roads.get(0);
        float meters = WorldLayout.length(road);
        // 8 m par seconde réelle, 60 s réelles par heure de jeu : une seconde = une minute de jeu
        float minutes = meters / 8f;
        assertTrue(minutes >= 165 && minutes <= 195, "aller : " + minutes + " minutes de jeu (attendu 3 h a 15 min pres)");
    }

    @Test
    void theRoadLeavesByTheGateAndEndsWhereTheForestRoadStarts() {
        OpenWorldData.RoadData road = GameContent.openWorld().roads.get(0);
        OpenWorldData.PointData first = road.points.get(0);
        OpenWorldData.PointData last = road.points.get(road.points.size() - 1);
        assertEquals("aethelgard", layout.placeAt(first.x, first.z));
        assertTrue(Math.abs(first.x) < 4f && first.z > 60f, "la route part de la porte sud (x 0, z 60)");
        ZoneData forest = GameContent.zone("foret_loups");
        float[] arrival = layout.toWorld("foret_loups", forest.arrival.x, forest.arrival.z);
        assertEquals(arrival[0], last.x, 0.5f);
        assertEquals(arrival[1], last.z, 0.5f);
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
        assertFalse(back.isInOpenWorld(), "le mode vient de l'ecran ouvert, pas de la sauvegarde");
        Files.deleteIfExists(Paths.get("save.json"));
    }
}
