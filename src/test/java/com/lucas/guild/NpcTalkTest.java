package com.lucas.guild;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.NpcsData;
import com.lucas.guild.data.TownData;
import com.lucas.guild.game.GameSession;
import com.lucas.guild.game.Lore;
import com.lucas.guild.game.NpcTalk;
import com.lucas.guild.game.Story;
import com.lucas.guild.model.Adventurer;

/** Les conversations de npcs.json : les mêmes comportements qu'avant pour les PNJ, et les répliques des habitants. */
class NpcTalkTest {

    private static NpcsData.OptionData option(String npc, String label) {
        for (NpcsData.OptionData o : GameContent.npcs().npc(npc).options) {
            if (o.label.equals(label)) {
                return o;
            }
        }
        throw new AssertionError("choix introuvable : " + npc + " / " + label);
    }

    @Test
    void theFourNpcsKeepTheirTexts() {
        NpcsData npcs = GameContent.npcs();
        assertEquals("Parler à l'aubergiste", npcs.npc("innkeeper").prompt);
        assertEquals("Bienvenue à La Pinte qui Chante ! Que désirez-vous ?",
                NpcTalk.fill(npcs.npc("innkeeper").greeting, "La Pinte qui Chante", 0));
        assertEquals("Bienvenue au Comptoir du Port ! Je regarde ce qu'il vous faut ?",
                NpcTalk.fill(npcs.npc("shopkeeper").greeting, "Comptoir du Port", 0));
        assertEquals("Que puis-je faire pour vous ?", npcs.npc("blacksmith").greeting);
        assertEquals("Partir", npcs.npc("guildmaster").leave);
    }

    @Test
    void labelsShowThePrice() {
        assertEquals("Manger un repas (5 Po)", NpcTalk.label(option("innkeeper", "Manger un repas")));
        assertEquals("Offrir une bière et écouter les rumeurs (2 Po)", NpcTalk.label(option("innkeeper", "Offrir une bière et écouter les rumeurs")));
        assertEquals("Voir les quêtes", NpcTalk.label(option("guildmaster", "Voir les quêtes")));
    }

    @Test
    void aMealCostsHealsAndTrainsVigor() {
        Adventurer p = GameSession.newGame("normale").getPlayer();
        p.addGold(100 - p.getGold());
        p.perdreVie(30);
        int health = p.getHealth();
        double vigor = p.getSkills().getPractice("vigor");
        String message = NpcTalk.serve(p, option("innkeeper", "Manger un repas"));
        assertEquals("Vous mangez un bon repas (+20 PV).", message);
        assertEquals(95, p.getGold());
        assertEquals(health + 20, p.getHealth());
        assertTrue(p.getSkills().getPractice("vigor") > vigor, "le repas entraîne la Vigueur");
    }

    @Test
    void noGoldNoMeal() {
        Adventurer p = GameSession.newGame("normale").getPlayer();
        p.spendGold(p.getGold());
        p.addGold(4);
        p.perdreVie(30);
        int health = p.getHealth();
        assertEquals("Pas assez d'or : un repas coûte 5 Po.", NpcTalk.serve(p, option("innkeeper", "Manger un repas")));
        assertEquals(4, p.getGold());
        assertEquals(health, p.getHealth());
    }

    @Test
    void aRumorIsPaidOnlyWhenThereIsOne() {
        Adventurer p = GameSession.newGame("normale").getPlayer();
        p.addGold(50);
        int gold = p.getGold();
        NpcsData.OptionData rumorOption = option("innkeeper", "Offrir une bière et écouter les rumeurs");
        NpcTalk.RumorResult first = NpcTalk.buyRumor(p, p.getTownId(), rumorOption);
        assertNotNull(first.rumor);
        assertNull(first.refusal);
        assertEquals(gold - 2, p.getGold());
        assertTrue(Lore.isKnown(p, Lore.rumorKey(first.rumor.id)), "la rumeur est dans le journal");

        // on les achète toutes : ensuite, l'aubergiste n'a plus rien et on ne paie plus
        NpcTalk.RumorResult r = first;
        for (int i = 0; i < 50 && r.rumor != null; i++) {
            r = NpcTalk.buyRumor(p, p.getTownId(), rumorOption);
        }
        int left = p.getGold();
        NpcTalk.RumorResult none = NpcTalk.buyRumor(p, p.getTownId(), rumorOption);
        assertNull(none.rumor);
        assertEquals("L'aubergiste n'a plus rien de neuf à raconter, pour l'instant.", none.refusal);
        assertEquals(left, p.getGold());
    }

    @Test
    void aRumorNeedsGold() {
        Adventurer p = GameSession.newGame("normale").getPlayer();
        p.spendGold(p.getGold());
        NpcTalk.RumorResult r = NpcTalk.buyRumor(p, p.getTownId(), option("innkeeper", "Offrir une bière et écouter les rumeurs"));
        assertNull(r.rumor);
        assertEquals("Pas assez d'or : une bière coûte 2 Po.", r.refusal);
        assertTrue(Lore.heardRumors(p).isEmpty(), "rien n'est appris sans payer");
    }

    @Test
    void optionsFollowTheStory() {
        GameSession session = GameSession.newGame("normale");
        Story story = new Story(session);
        NpcsData.NpcData npc = new NpcsData.NpcData();
        NpcsData.OptionData always = new NpcsData.OptionData();
        always.label = "Toujours";
        NpcsData.OptionData later = new NpcsData.OptionData();
        later.label = "Plus tard";
        later.requires = new ArrayList<>(Collections.singletonList("flag:mandat"));
        npc.options = Arrays.asList(always, later);
        assertEquals(1, NpcTalk.options(npc, story).size());
        story.set("mandat");
        assertEquals(2, NpcTalk.options(npc, story).size());
    }

    @Test
    void theGuardSaysWhatSuitsHisTownAndTheStory() {
        GameSession session = GameSession.newGame("normale");
        Story story = new Story(session);
        NpcsData.VillagerTalkData guard = GameContent.npcs().villager("garde");
        List<String> before = NpcTalk.lines(guard, "hautevigne", story);
        assertTrue(before.contains("La mine est fermée, ordre du conseil. Ne me demandez pas d'ouvrir la grille."));
        assertFalse(before.contains("Les marins se battent le soir. Tant que personne ne tombe à l'eau, ce ne sont pas mes affaires."),
                "une réplique de Port-Brume n'est pas dite à Hautevigne");
        story.set("mandat");
        List<String> after = NpcTalk.lines(guard, "hautevigne", story);
        assertFalse(after.contains("La mine est fermée, ordre du conseil. Ne me demandez pas d'ouvrir la grille."));
        assertTrue(after.get(after.size() - 1).startsWith("Vous avez le mandat"));
    }

    @Test
    void aVillagerStartsWithTheNewestLineThenGoesAround() {
        List<String> lines = Arrays.asList("a", "b", "c");
        assertEquals("c", NpcTalk.line(lines, 0));
        assertEquals("b", NpcTalk.line(lines, 1));
        assertEquals("a", NpcTalk.line(lines, 2));
        assertEquals("c", NpcTalk.line(lines, 3));
        assertNull(NpcTalk.line(Collections.<String>emptyList(), 0));
    }

    @Test
    void everyVillagerOfTheThreeTownsTalks() {
        Story story = new Story(GameSession.newGame("normale"));
        for (String town : Arrays.asList("aethelgard", "hautevigne", "portbrume")) {
            for (TownData.VillagerData v : GameContent.town(town).villagers) {
                assertNotNull(v.talk, town + " : un habitant sans répliques (" + v.model + ")");
                assertFalse(NpcTalk.lines(GameContent.npcs().villager(v.talk), town, story).isEmpty(),
                        town + " : " + v.talk + " n'a rien à dire au début de la partie");
            }
        }
    }
}
