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
import com.lucas.guild.game.Combat;
import com.lucas.guild.game.GameSession;
import com.lucas.guild.game.Monster;
import com.lucas.guild.game.QuestLog;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Guild;
import com.lucas.guild.model.ObjetDeQuete;
import com.lucas.guild.model.Quest;

/** Quêtes entre villes : tableaux par ville, livraisons payées à la remise, collectes, rang qui monte. */
class QuestBoardsTest {

    private static Quest quest(String title) {
        return GameContent.quests().stream().filter(q -> q.getTitle().equals(title)).findFirst().get();
    }

    private static List<String> titles(List<Quest> quests) {
        return quests.stream().map(Quest::getTitle).collect(java.util.stream.Collectors.toList());
    }

    private static boolean has(Adventurer p, String name) {
        return p.getInventoryItems().stream().anyMatch(i -> i instanceof ObjetDeQuete && i.getName().equals(name));
    }

    private static long count(Adventurer p, String itemId) {
        return p.getInventoryItems().stream().filter(i -> itemId.equals(i.getId())).count();
    }

    @Test
    void everyTownHasItsOwnBoard() {
        Guild guild = new Guild();
        List<Quest> aethelgard = guild.getQuestsFor("aethelgard", 9);
        List<Quest> portbrume = guild.getQuestsFor("portbrume", 9);
        List<Quest> hautevigne = guild.getQuestsFor("hautevigne", 9);
        assertTrue(aethelgard.size() >= 5, "la guilde : les 3 quetes d'origine et 2 livraisons");
        assertTrue(portbrume.size() >= 2 && hautevigne.size() >= 2);
        for (Quest q : portbrume) assertEquals("portbrume", q.getTown());
        for (Quest q : aethelgard) assertEquals("aethelgard", q.getTown(), "une quete n'est que sur un tableau");
        assertEquals(GameContent.quests().size(), aethelgard.size() + portbrume.size() + hautevigne.size());
        // un personnage de rang 0 ne voit pas la quete de rang 1
        assertFalse(titles(guild.getQuestsFor("aethelgard", 0)).contains("Chasse aux gobelins"));
        assertTrue(titles(guild.getQuestsFor("aethelgard", 1)).contains("Chasse aux gobelins"));
    }

    @Test
    void parcelDeliveredToAnotherTownIsPaidOnTheSpot() {
        Adventurer p = GameSession.newGame("normale").getPlayer();
        QuestLog log = new QuestLog(p);
        Quest parcel = quest("Colis pour Port-Brume");
        List<Quest> all = GameContent.quests();

        assertNull(log.accept(parcel));
        assertTrue(has(p, "Colis scellé"));
        assertFalse(log.canDeliver("La Voile Rouge", all), "mauvais destinataire");
        assertFalse(log.hasProof(parcel), "pas de preuve a rapporter : la quete est payee a la remise");

        int gold = p.getGold();
        assertTrue(log.canDeliver("Comptoir du Port", all));
        String message = log.deliver("Comptoir du Port", all);
        assertTrue(message.contains("45 Po"), message);
        assertEquals(gold + 45, p.getGold());
        assertFalse(has(p, "Colis scellé"));
        assertEquals(QuestLog.Status.DONE, log.status(parcel));
        assertFalse(p.getActiveQuests().contains(parcel.getTitle()));
        assertFalse(log.canDeliver("Comptoir du Port", all), "deja livre");
    }

    @Test
    void everyDeliveryHasARealRecipient() {
        List<String> recipients = GameContent.deliveryRecipients();
        assertTrue(recipients.contains("Forgeron") && recipients.contains("La Voile Rouge") && recipients.contains("Le Filon d'Argent")
                && recipients.contains("La Pinte qui Chante") && recipients.contains("Comptoir du Port"), recipients.toString());
        for (Quest q : GameContent.quests()) {
            if (q.getType() == com.lucas.guild.model.QuestType.DELIVERY) {
                assertTrue(recipients.contains(q.getTarget()), q.getTitle() + " -> " + q.getTarget());
            }
        }
    }

    @Test
    void collectQuestNeedsTheRightItemsAndTakesOnlyWhatItAsks() {
        Adventurer p = GameSession.newGame("facile").getPlayer();
        QuestLog log = new QuestLog(p);
        Quest trophies = quest("Trophées de gobelins");
        assertNull(log.accept(trophies));
        assertEquals("Rapporter 4 x Oreille de gobelin", trophies.getObjectiveText());

        assertFalse(log.hasProof(trophies));
        assertTrue(log.turnIn(trophies).contains("0"), "aucun objet");

        for (int i = 0; i < 3; i++) p.addItem(GameContent.newItem("goblin_ear"));
        p.addItem(GameContent.newItem("wolf_fang"));
        assertFalse(log.hasProof(trophies), "3 sur 4 ; un croc ne compte pas");
        assertTrue(log.turnIn(trophies).contains("3"));
        assertEquals(QuestLog.Status.ACTIVE, log.status(trophies));

        p.addItem(GameContent.newItem("goblin_ear"));
        p.addItem(GameContent.newItem("goblin_ear"));
        assertTrue(log.hasProof(trophies));
        int gold = p.getGold();
        assertNull(log.turnIn(trophies));
        assertEquals(gold + 70, p.getGold());
        assertEquals(1, count(p, "goblin_ear"), "5 oreilles - 4 rendues = 1 gardee");
        assertEquals(1, count(p, "wolf_fang"));
        assertEquals(QuestLog.Status.DONE, log.status(trophies));
    }

    /** Le rang ne vient plus d'un simple compte de quetes : il faut aussi l'etape de l'histoire (story.json). */
    @Test
    void rankNeedsTheStoryAsWellAsQuests() {
        GameSession s = GameSession.newGame("facile");
        Adventurer p = s.getPlayer();
        QuestLog log = new QuestLog(p);
        Quest hunt = quest("Chasse aux gobelins");
        assertNotNull(log.accept(hunt), "rang F : refuse");

        for (String title : new String[] {"Colis pour Port-Brume", "Pli pour Hautevigne"}) {
            Quest q = quest(title);
            assertNull(log.accept(q));
            assertNotNull(log.deliver(q.getTarget(), GameContent.quests()));
        }
        Quest rats = quest("Nettoyage de la cave");
        assertNull(log.accept(rats));
        p.addItem(new ObjetDeQuete("Queue de rat"));
        assertNull(log.turnIn(rats));
        assertEquals(3, p.getDoneQuests().size());
        assertEquals(0, p.getRank(), "3 quetes terminees ne suffisent plus : le rang E demande le prologue");

        // le prologue : les deux quetes de depart, la plaque trouvee dans la cave et montree au maitre de guilde
        Quest letter = quest("Livraison express");
        assertNull(log.accept(letter));
        log.deliver("Forgeron", GameContent.quests());
        assertNull(log.turnIn(letter));
        com.lucas.guild.game.Story story = new com.lucas.guild.game.Story(s);
        story.advance();
        story.set("plaque_found");
        story.set("plaque_shown");
        story.advance();
        assertEquals(1, p.getRank(), "prologue termine : rang E");
        assertNull(log.accept(hunt), "la chasse aux gobelins est ouverte");
    }

    @Test
    void goblinsDropTheHuntInsigniaOnlyWhileTheHuntIsActive() {
        Adventurer p = GameSession.newGame("facile").getPlayer();
        Monster goblin = Monster.spawn("goblin", 0, 0);
        String without = Combat.loot(p, goblin, new Random(1));
        assertFalse(has(p, "Insigne de gobelin"), without);

        p.getActiveQuests().add("Chasse aux gobelins");
        String with = Combat.loot(p, goblin, new Random(1));
        assertTrue(has(p, "Insigne de gobelin"), with);
        assertTrue(new QuestLog(p).hasProof(quest("Chasse aux gobelins")));
    }

    @Test
    void questProgressSurvivesSavingIncludingRank() {
        Adventurer p = GameSession.newGame("facile").getPlayer();
        p.getDoneQuests().addAll(List.of("a", "b", "c"));
        p.setRank(1);
        com.google.gson.Gson gson = GameGson.create();
        assertEquals(1, gson.fromJson(gson.toJson(p), Adventurer.class).getRank());
    }
}
