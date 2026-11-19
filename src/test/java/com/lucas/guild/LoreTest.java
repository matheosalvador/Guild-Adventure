package com.lucas.guild;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.GameGson;
import com.lucas.guild.data.LoreData;
import com.lucas.guild.data.TownData;
import com.lucas.guild.data.WorldData;
import com.lucas.guild.game.GameSession;
import com.lucas.guild.game.Lore;
import com.lucas.guild.model.Adventurer;

/** Lore : rumeurs de l'aubergiste, textes à lire, journal (ce que le personnage sait, sauvegardé). */
class LoreTest {

    @Test
    void everyTownHasRumorsAndEveryPlaceHasASignOrStele() {
        LoreData lore = GameContent.lore();
        for (WorldData.PlaceData place : GameContent.world().places) {
            if (place.zoneFile == null) {
                assertTrue(lore.rumors.stream().anyMatch(r -> place.id.equals(r.town)), place.id + " : des rumeurs");
                assertTrue(GameContent.town(place.id).decor.stream().anyMatch(p -> "read".equals(p.action)), place.id + " : un panneau");
            } else if (!place.hidden) {   // un etage cache de donjon n'a pas forcement son panneau
                assertTrue(GameContent.zone(place.id).decor.stream().anyMatch(p -> "read".equals(p.action)), place.id + " : un panneau ou une stele");
            }
        }
    }

    @Test
    void innkeeperTellsTheTownsRumorsInOrderThenRunsOut() {
        Adventurer p = GameSession.newGame("normale").getPlayer();
        int tellable = 0;
        LoreData.RumorData first = Lore.nextRumor(p, "aethelgard");
        assertNotNull(first);
        assertTrue(first.town == null || first.town.equals("aethelgard"));
        LoreData.RumorData rumor;
        while ((rumor = Lore.nextRumor(p, "aethelgard")) != null) {
            assertTrue(rumor.town == null || "aethelgard".equals(rumor.town), "une rumeur d'une autre ville : " + rumor.id);
            assertTrue(Lore.learn(p, Lore.rumorKey(rumor.id)));
            tellable++;
            assertTrue(tellable < 50, "boucle infinie");
        }
        assertTrue(tellable >= 5, "au moins 5 rumeurs a Aethelgard : " + tellable);
        assertNull(Lore.nextRumor(p, "aethelgard"), "plus rien a raconter");
        assertNotNull(Lore.nextRumor(p, "portbrume"), "Port-Brume a les siennes");
    }

    @Test
    void learningIsNotedOnce() {
        Adventurer p = GameSession.newGame("normale").getPlayer();
        assertTrue(Lore.learn(p, Lore.textKey("panneau_foret")));
        assertFalse(Lore.learn(p, Lore.textKey("panneau_foret")), "deja lu");
        assertEquals(1, Lore.readTexts(p).size());
        assertEquals("Panneau de la forêt", Lore.readTexts(p).get(0).title);
        assertTrue(Lore.heardRumors(p).isEmpty());
    }

    @Test
    void loreSurvivesSavingAndOldSavesHaveNone() {
        Adventurer p = GameSession.newGame("normale").getPlayer();
        Lore.learn(p, Lore.rumorKey("aeth_rats"));
        Lore.learn(p, Lore.textKey("stele_ruines"));
        com.google.gson.Gson gson = GameGson.create();
        Adventurer back = gson.fromJson(gson.toJson(p), Adventurer.class);
        assertEquals(1, Lore.heardRumors(back).size());
        assertEquals(1, Lore.readTexts(back).size());

        Adventurer old = gson.fromJson("{\"name\":\"X\",\"health\":50,\"maxHealth\":100}", Adventurer.class);
        assertTrue(old.getKnownLore().isEmpty(), "ancienne sauvegarde : rien appris, sans erreur");
    }

    @Test
    void readablePartsPointToExistingTexts() {
        LoreData lore = GameContent.lore();
        for (WorldData.PlaceData place : GameContent.world().places) {
            java.util.List<TownData.PartData> parts = place.zoneFile != null
                    ? GameContent.zone(place.id).decor : GameContent.town(place.id).decor;
            for (TownData.PartData part : parts) {
                if ("read".equals(part.action)) {
                    assertTrue(lore.texts.containsKey(part.text), place.id + " : texte " + part.text);
                }
            }
        }
    }
}
