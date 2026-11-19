package com.lucas.guild;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.TownData;
import com.lucas.guild.data.WorldData;
import com.lucas.guild.game.RouteWalker;

/** Décor vivant : le parcours des villageois et le contenu des villes (toits, habitants). */
class VillagersTest {

    private static List<float[]> route(float... xz) {
        List<float[]> points = new ArrayList<>();
        for (int i = 0; i < xz.length; i += 2) {
            points.add(new float[] {xz[i], xz[i + 1]});
        }
        return points;
    }

    @Test
    void walkerGoesBackAndForth() {
        RouteWalker w = new RouteWalker(route(0, 0, 10, 0), 2f);
        assertEquals(0f, w.getX(), 1e-4);
        w.update(2.5f);                       // 5 m
        assertEquals(5f, w.getX(), 1e-3);
        w.update(2.5f);                       // 10 m en tout : au bout du parcours
        assertEquals(10f, w.getX(), 1e-3);
        w.update(1.25f);                      // il est reparti en sens inverse (2,5 m)
        assertEquals(7.5f, w.getX(), 1e-3);
        w.update(3.75f);                      // 10 m de retour : revenu au depart
        assertEquals(0f, w.getX(), 1e-3);
    }

    @Test
    void walkerFacesItsDirection() {
        RouteWalker east = new RouteWalker(route(0, 0, 10, 0), 1f);
        east.update(1f);
        assertEquals(-90f, east.getYaw(), 1e-3, "vers +X : -90 degres (0 = vers -Z)");
        RouteWalker north = new RouteWalker(route(0, 0, 0, -10), 1f);
        north.update(1f);
        assertEquals(0f, north.getYaw(), 1e-3, "vers -Z : 0");
        RouteWalker south = new RouteWalker(route(0, 0, 0, 10), 1f);
        south.update(1f);
        assertEquals(180f, Math.abs(south.getYaw()), 1e-3, "vers +Z : 180");
    }

    @Test
    void walkerFollowsALoopAndCatchesUpLongSteps() {
        RouteWalker w = new RouteWalker(route(0, 0, 10, 0, 10, 10, 0, 10), 1f);
        w.update(25f);                        // 25 m : 2 cotes et demi
        assertEquals(5f, w.getX(), 1e-3);
        assertEquals(10f, w.getZ(), 1e-3);
        w.update(1000f);                      // un tres grand pas ne plante pas et reste sur le parcours
        assertTrue(w.getX() >= 0f && w.getX() <= 10f && w.getZ() >= 0f && w.getZ() <= 10f);
    }

    @Test
    void walkerRejectsBadRoutes() {
        assertThrows(IllegalArgumentException.class, () -> new RouteWalker(route(0, 0), 1f));
        assertThrows(IllegalArgumentException.class, () -> new RouteWalker(route(0, 0, 1, 1), 0f));
    }

    @Test
    void everyTownHasVillagersWalkingOnOpenGround() {
        for (WorldData.PlaceData place : GameContent.world().places) {
            if (place.zoneFile != null) {
                continue;
            }
            TownData town = GameContent.town(place.id);
            assertTrue(town.villagers.size() >= 3, place.id + " : des habitants");
            for (TownData.VillagerData v : town.villagers) {
                assertTrue(GameContent.models().models.containsKey(v.model), place.id + " : modele " + v.model);
                for (TownData.RoutePoint p : v.route) {
                    for (TownData.BuildingData b : town.buildings) {
                        boolean inside = Math.abs(p.x - b.x) < 6.5f && Math.abs(p.z - b.z) < 6.5f;
                        assertFalse(inside, place.id + " : un point de parcours est dans " + b.name);
                    }
                }
            }
        }
    }

    private static void assertFalse(boolean value, String message) {
        org.junit.jupiter.api.Assertions.assertFalse(value, message);
    }

    @Test
    void housesHaveSlopedRoofsAndWindows() {
        for (WorldData.PlaceData place : GameContent.world().places) {
            if (place.zoneFile != null) {
                continue;
            }
            for (TownData.BuildingData b : GameContent.town(place.id).buildings) {
                assertTrue(b.parts.stream().anyMatch(p -> "gable".equals(p.shape)), place.id + " / " + b.name + " : un toit a deux pentes");
                assertTrue(b.parts.stream().filter(p -> p.decal && "#7FB3D9".equals(p.color)).count() >= 2, b.name + " : des fenetres");
            }
        }
    }
}
