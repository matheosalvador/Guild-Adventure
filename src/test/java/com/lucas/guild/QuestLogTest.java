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
import com.lucas.guild.game.GameSession;
import com.lucas.guild.game.QuestLog;
import com.lucas.guild.game.SaveManager;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.GameData;
import com.lucas.guild.model.ObjetDeQuete;
import com.lucas.guild.model.Quest;

class QuestLogTest {

    private static Quest quest(String title) {
        return GameContent.quests().stream().filter(q -> q.getTitle().equals(title)).findFirst().get();
    }

    private static boolean has(Adventurer p, String name) {
        return p.getInventoryItems().stream().anyMatch(i -> i instanceof ObjetDeQuete && i.getName().equals(name));
    }

    @Test
    void deliveryQuestFullFlow() {
        Adventurer p = GameSession.newGame("normale").getPlayer();
        QuestLog log = new QuestLog(p);
        Quest delivery = quest("Livraison express");
        List<Quest> all = GameContent.quests();
        int gold = p.getGold();

        assertEquals(QuestLog.Status.AVAILABLE, log.status(delivery));
        assertFalse(log.canDeliver("Forgeron", all), "rien a livrer avant d'avoir accepte");
        assertNull(log.accept(delivery));
        assertEquals(QuestLog.Status.ACTIVE, log.status(delivery));
        assertTrue(has(p, "Lettre à livrer"), "la lettre est remise a l'acceptation");
        assertNotNull(log.accept(delivery), "on ne peut pas accepter deux fois");

        assertNotNull(log.turnIn(delivery), "sans preuve, la guilde refuse");
        assertEquals(gold, p.getGold());

        assertFalse(log.canDeliver("Aubergiste", all), "mauvais destinataire");
        assertTrue(log.canDeliver("Forgeron", all));
        String done = log.deliver("Forgeron", all);
        assertTrue(done.contains("Lettre signée"), done);
        assertFalse(has(p, "Lettre à livrer"));
        assertTrue(has(p, "Lettre signée"));
        assertFalse(log.canDeliver("Forgeron", all), "deja remise");

        assertNull(log.turnIn(delivery));
        assertEquals(gold + delivery.getGoldReward(), p.getGold(), "l'or promis est verse");
        assertFalse(has(p, "Lettre signée"), "la preuve est reprise");
        assertEquals(QuestLog.Status.DONE, log.status(delivery));
        assertNotNull(log.accept(delivery), "une quete terminee ne se reprend pas");
        assertNotNull(log.turnIn(delivery), "ni ne se rend deux fois");
        assertEquals(gold + delivery.getGoldReward(), p.getGold());
    }

    @Test
    void combatQuestNeedsItsProof() {
        Adventurer p = GameSession.newGame("normale").getPlayer();
        QuestLog log = new QuestLog(p);
        Quest rats = quest("Nettoyage de la cave");
        assertNull(log.accept(rats));
        assertFalse(has(p, "Queue de rat"));
        assertTrue(log.turnIn(rats).contains("Queue de rat"), "la preuve manquante est nommee");
        p.addItem(new ObjetDeQuete("Queue de rat"));
        assertTrue(log.hasProof(rats));
        int gold = p.getGold();
        assertNull(log.turnIn(rats));
        assertEquals(gold + 20, p.getGold());
    }

    @Test
    void rankIsChecked() {
        Adventurer p = GameSession.newGame("normale").getPlayer();
        Quest hard = quest("Chasse aux gobelins");
        assertTrue(new QuestLog(p).accept(hard).contains("rang"));
        assertEquals(QuestLog.Status.AVAILABLE, new QuestLog(p).status(hard));
    }

    @Test
    void tooHeavyToAcceptDelivery() {
        Adventurer p = GameSession.newGame("realiste").getPlayer();
        while (p.addItem(com.lucas.guild.data.GameContent.newItem("healing_potion"))) { /* remplir */ }
        QuestLog log = new QuestLog(p);
        String refusal = log.accept(quest("Livraison express"));
        // il reste peut-etre 0,1 kg ; sinon refus : dans les deux cas l'etat doit etre coherent
        if (refusal != null) {
            assertEquals(QuestLog.Status.AVAILABLE, log.status(quest("Livraison express")));
        } else {
            assertEquals(QuestLog.Status.ACTIVE, log.status(quest("Livraison express")));
        }
    }

    @Test
    void questStateSurvivesSaveAndOldSavesHaveNone() throws Exception {
        GameSession s = GameSession.newGame("normale");
        QuestLog log = new QuestLog(s.getPlayer());
        log.accept(quest("Livraison express"));
        log.accept(quest("Nettoyage de la cave"));
        s.getPlayer().getDoneQuests().add("Autre");
        SaveManager sm = new SaveManager();
        assertTrue(sm.saveGame(s.getPlayer(), s.getClock(), "normale", 0, 2, 0, "Town"));
        GameData data = sm.loadGame();
        Adventurer back = GameSession.fromSave(data).getPlayer();
        assertEquals(List.of("Livraison express", "Nettoyage de la cave"), back.getActiveQuests());
        assertEquals(List.of("Autre"), back.getDoneQuests());
        assertTrue(has(back, "Lettre à livrer"), "l'objet de quete est sauvegarde aussi");
        Files.deleteIfExists(Paths.get("save.json"));

        GameData old = new com.google.gson.Gson().fromJson("{\"playerData\":{\"name\":\"Vieux\",\"health\":80,\"maxHealth\":100},\"currentDay\":42}", GameData.class);
        Adventurer o = GameSession.fromSave(old).getPlayer();
        assertTrue(o.getActiveQuests().isEmpty() && o.getDoneQuests().isEmpty(), "ancienne sauvegarde : aucune quete, sans plantage");
    }

    @Test
    void questDataIsValid() {
        List<Quest> quests = GameContent.quests();
        assertTrue(quests.stream().allMatch(q -> q.getGoldReward() > 0), "chaque quete paie de l'or");
        assertTrue(quests.stream().anyMatch(q -> q.getGiveItem() != null), "une livraison donne un objet a porter");
    }
}
