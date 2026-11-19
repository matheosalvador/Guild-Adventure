package com.lucas.guild;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.game.Combat;
import com.lucas.guild.game.GameSession;
import com.lucas.guild.game.Monster;
import com.lucas.guild.game.QuestLog;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.ObjetDeQuete;
import com.lucas.guild.model.Quest;

/** Les bugs de logique trouves a la relecture (chantier A) : chaque test echouait avant la correction. */
class RelectureTest {

    private static Quest quest(String title) {
        return GameContent.quests().stream().filter(q -> q.getTitle().equals(title)).findFirst().get();
    }

    private static long count(Adventurer p, String name) {
        return p.getInventoryItems().stream().filter(i -> i instanceof ObjetDeQuete && i.getName().equals(name)).count();
    }

    /**
     * Un personnage qui porte plus que sa capacite (elle a baisse avec l'age) remet une livraison : il doit
     * recevoir la preuve en echange. Avant, la preuve etait refusee par l'inventaire (trop lourd) et perdue,
     * et la quete ne pouvait plus jamais etre rendue.
     */
    @Test
    void deliveringWhileOverloadedStillGivesTheProof() {
        Adventurer p = GameSession.newGame("normale").getPlayer();
        QuestLog log = new QuestLog(p);
        Quest delivery = quest("Livraison express");
        List<Quest> all = GameContent.quests();
        assertNull(log.accept(delivery));

        p.getInventaire().setCapacityFactor(0.01);   // grand age : la capacite passe sous ce qu'il porte deja
        assertTrue(p.getInventaire().getPoidsActuel() > p.getInventaire().getPoidsMax(), "le personnage est en surcharge");

        log.deliver("Forgeron", all);
        assertEquals(0, count(p, "Lettre à livrer"));
        assertEquals(1, count(p, "Lettre signée"), "la preuve est donnee meme en surcharge (elle remplace la lettre, le poids ne change pas)");
        assertNull(log.turnIn(delivery), "la quete peut etre rendue");
    }

    /**
     * Garde-fou sur les arrondis des prix : dans une meme boutique, au meme moment, on ne revend jamais un objet
     * plus cher qu'on ne l'achete (pas d'or infini), et un objet en vente coute toujours au moins 1 Po. Verifie pour
     * chaque boutique, chaque objet, chaque niveau de Marchandage, sans evenement puis avec chacun d'eux.
     */
    @Test
    void noShopBuysBackForMoreThanItSells() {
        String[][] shops = {{"forge", "aethelgard"}, {"guild", "aethelgard"}, {"portbrume_store", "portbrume"}, {"hautevigne_store", "hautevigne"}};
        List<String> events = new java.util.ArrayList<>();
        events.add(null);
        GameContent.events().events.forEach(e -> events.add(e.id));
        int checked = 0;
        for (String[] s : shops) {
            com.lucas.guild.game.Shop shop = new com.lucas.guild.game.Shop(s[0]);
            for (String event : events) {
                for (int level = 0; level <= 5; level++) {
                    Adventurer p = GameSession.newGame("normale").getPlayer();
                    p.setTownId(s[1]);
                    if (level > 0) {
                        p.getSkills().setLevel("haggling", level);
                        p.refreshBonuses();
                    }
                    if (event != null) {
                        com.lucas.guild.game.WorldEvents.start(p, event, s[1], 1);
                    }
                    for (com.lucas.guild.model.Item item : shop.getStock()) {
                        int buy = shop.buyPrice(p, item);
                        int sell = shop.sellPrice(p, item);
                        String where = s[0] + " / " + item.getId() + " / marchandage " + level + " / " + event;
                        assertTrue(buy >= 1, where + " : prix d'achat " + buy);
                        assertTrue(sell < buy, where + " : rachat " + sell + " Po pour un achat a " + buy + " Po");
                        checked++;
                    }
                }
            }
        }
        assertTrue(checked > 1000, "combinaisons verifiees : " + checked);
    }

    /**
     * Quete de combat : la preuve n'est donnee qu'une fois. Avant, chaque monstre tue en donnait une nouvelle
     * (trois rats = trois queues) et celles en trop restaient dans l'inventaire apres avoir rendu la quete.
     */
    @Test
    void combatProofIsGivenOnlyOnce() {
        Adventurer p = GameSession.newGame("normale").getPlayer();
        QuestLog log = new QuestLog(p);
        Quest cave = quest("Nettoyage de la cave");
        assertNull(log.accept(cave));

        for (int i = 0; i < 3; i++) {
            Combat.loot(p, Monster.spawn("giant_rat", 0, 0), new Random(i));
        }
        assertEquals(1, count(p, "Queue de rat"), "une seule preuve, meme apres trois rats");

        assertNull(log.turnIn(cave));
        assertFalse(p.getInventoryItems().stream().anyMatch(i -> i instanceof ObjetDeQuete), "plus aucun objet de quete apres avoir rendu la quete");
    }
}
