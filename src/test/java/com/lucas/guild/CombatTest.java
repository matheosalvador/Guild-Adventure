package com.lucas.guild;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.game.Combat;
import com.lucas.guild.game.DeathPenalty;
import com.lucas.guild.game.DebugMode;
import com.lucas.guild.game.GameSession;
import com.lucas.guild.game.Monster;
import com.lucas.guild.game.QuestLog;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.ObjetDeQuete;
import com.lucas.guild.model.Quest;

class CombatTest {

    @AfterEach
    void resetDebug() {
        DebugMode.setEnabled(false);
    }

    private static Adventurer newPlayer() {
        return GameSession.newGame("normale").getPlayer();
    }

    private static Monster rat(float x, float z) {
        return Monster.spawn("giant_rat", x, z);
    }

    // ------------------------------------------------------------------ monstre

    @Test
    void ratData() {
        Monster m = rat(0, 0);
        assertEquals("Rat géant", m.getName());
        assertEquals(20, m.getHealth());
        assertEquals(20, m.getMaxHealth());
        assertFalse(m.isDead());
    }

    @Test
    void farMonsterStaysStill() {
        Monster m = rat(0, 0);
        assertFalse(m.update(1f, 0, 50));
        assertEquals(0f, m.x);
        assertEquals(0f, m.z);
    }

    @Test
    void monsterChasesAtItsSpeedWithoutOvershooting() {
        Monster m = rat(0, 0);
        m.update(1f, 0, 10);   // 10 m : repere (aggro 12), 3,5 m/s
        assertEquals(3.5f, m.z, 1e-4);
        for (int i = 0; i < 100; i++) m.update(0.1f, 0, 10);
        double stopAt = m.data.attackRange * 0.8;
        assertEquals(10 - stopAt, m.z, 1e-3, "il s'arrete a portee de coup sans traverser le joueur");
    }

    @Test
    void monsterAttacksWithDelayThenOnCooldown() {
        Monster m = rat(0, 1.0f);   // deja a portee (1,6 m)
        assertFalse(m.update(0.3f, 0, 0), "pas de coup avant le delai de reaction (0,5 s)");
        assertTrue(m.update(0.3f, 0, 0), "premier coup");
        int hits = 0;
        for (int i = 0; i < 115; i++) if (m.update(0.05f, 0, 0)) hits++;   // 5,75 s apres le premier coup
        assertEquals(4, hits, "un coup toutes les 1,2 s : 4 coups en 5,75 s (" + hits + ")");
    }

    @Test
    void monsterOutOfAttackRangeDoesNotHit() {
        Monster m = rat(0, 8f);
        assertFalse(m.update(0.6f, 0, 0), "il doit d'abord s'approcher");
    }

    @Test
    void damageAndDeath() {
        Monster m = rat(0, 0);
        m.takeDamage(8);
        assertEquals(12, m.getHealth());
        assertTrue(m.getHurtTimer() > 0);
        m.takeDamage(0);
        m.takeDamage(-5);
        assertEquals(12, m.getHealth());
        m.takeDamage(1000);
        assertEquals(0, m.getHealth());
        assertTrue(m.isDead());
        assertFalse(m.update(1f, 0, 0), "un monstre mort n'attaque plus");
    }

    @Test
    void clampKeepsMonsterInsideRoom() {
        Monster m = rat(50, -50);
        m.clamp(-7, 7, -7, 7);
        assertEquals(7f, m.x);
        assertEquals(-7f, m.z);
    }

    // ------------------------------------------------------------------ degats

    @Test
    void playerDamageUsesWeaponAndMelee() {
        Adventurer p = newPlayer();
        assertEquals(10, p.getAttackDamage(), "a mains nues : 10");
        p.addItem(GameContent.newItem("short_sword"));
        p.equip(p.getInventoryItems().stream().filter(i -> "short_sword".equals(i.getId())).findFirst().get());
        assertEquals(16, p.getAttackDamage(), "epee courte : 10 + 6");
        p.getSkills().setLevel("melee", 2);
        p.refreshBonuses();
        assertEquals(17, p.getAttackDamage(), "Melee 2 : +6 % de 16 = 16,96");
        p.getSkills().setLevel("melee", 5);
        assertEquals(18, p.getAttackDamage(), "Melee 5 : +15 % de 16 = 18,4");
    }

    @Test
    void playerHitDamagesAndTrainsMelee() {
        Adventurer p = newPlayer();
        Monster m = rat(0, 0);
        assertEquals(10, Combat.playerHit(p, m));
        assertEquals(10, m.getHealth());
        assertEquals(1.0, p.getSkills().getPractice("melee"), 1e-9);
        for (int i = 0; i < 9; i++) Combat.playerHit(p, rat(0, 0));
        assertEquals(1, p.getSkills().getLevel("melee"), "10 coups : Melee niveau 1");
    }

    @Test
    void monsterHitReducedByArmourAndRobustness() {
        Adventurer p = newPlayer();
        Monster m = rat(0, 0);
        assertEquals(5, Combat.monsterHit(m, p));
        assertEquals(95, p.getHealth());
        assertEquals(1.0, p.getSkills().getPractice("robustness"), 1e-9);

        p.addItem(GameContent.newItem("chain_mail"));   // defense 5
        p.equip(p.getInventoryItems().stream().filter(i -> "chain_mail".equals(i.getId())).findFirst().get());
        assertEquals(5, p.getDefense());
        assertEquals(1, Combat.monsterHit(m, p), "toujours au moins 1 degat");
        p.getSkills().setLevel("robustness", 2);
        assertEquals(6, p.getDefense(), "Robustesse 2 : +1 de defense");
    }

    @Test
    void robustnessEarnedByTakingHits() {
        Adventurer p = newPlayer();
        Monster m = rat(0, 0);
        p.addGold(0);
        for (int i = 0; i < 8; i++) {
            Combat.monsterHit(m, p);
        }
        assertEquals(1, p.getSkills().getLevel("robustness"));
    }

    @Test
    void invincibleOnlyInDebugMode() {
        Adventurer p = newPlayer();
        DebugMode.setInvincible(true);
        assertEquals(5, Combat.monsterHit(rat(0, 0), p), "invincible sans mode debug : sans effet");
        DebugMode.setEnabled(true);
        DebugMode.setInvincible(true);
        int hp = p.getHealth();
        assertEquals(0, Combat.monsterHit(rat(0, 0), p));
        assertEquals(hp, p.getHealth());
    }

    // ------------------------------------------------------------------ butin et quetes

    @Test
    void lootGoldWithinRange() {
        Adventurer p = newPlayer();
        Random random = new Random(42);
        int before = p.getGold();
        boolean sawMin = false, sawMax = false;
        for (int i = 0; i < 200; i++) {
            int g = p.getGold();
            Combat.loot(p, rat(0, 0), random);
            int gained = p.getGold() - g;
            assertTrue(gained >= 1 && gained <= 5, "or entre 1 et 5 : " + gained);
            sawMin |= gained == 1;
            sawMax |= gained == 5;
        }
        assertTrue(sawMin && sawMax);
        assertTrue(p.getGold() > before);
    }

    @Test
    void ratTailOnlyDroppedWhenQuestIsActive() {
        Adventurer p = newPlayer();
        String message = Combat.loot(p, rat(0, 0), new Random(1));
        assertFalse(message.contains("Queue de rat"));
        assertTrue(p.getInventoryItems().stream().noneMatch(i -> i instanceof ObjetDeQuete));

        Quest cave = GameContent.quests().stream().filter(q -> q.getTitle().equals("Nettoyage de la cave")).findFirst().get();
        assertNull(new QuestLog(p).accept(cave));
        String withQuest = Combat.loot(p, rat(0, 0), new Random(1));
        assertTrue(withQuest.contains("Queue de rat"), withQuest);
        assertTrue(new QuestLog(p).hasProof(cave));
        int gold = p.getGold();
        assertNull(new QuestLog(p).turnIn(cave));
        assertEquals(gold + 20, p.getGold(), "la quete de la cave rapporte 20 Po");
    }

    // ------------------------------------------------------------------ mort et potions

    @Test
    void deathPenalty() {
        GameSession s = GameSession.newGame("normale");
        Adventurer p = s.getPlayer();
        p.addGold(140);   // 200 Po
        int items = p.getInventoryItems().size();
        int day = s.getClock().getCurrentDay();
        int minutes = s.getClock().getCurrentHour() * 60 + s.getClock().getCurrentMinute();
        p.perdreVie(1000);
        assertFalse(p.estVivant());

        String message = DeathPenalty.apply(s);
        assertEquals(180, p.getGold(), "10 % d'or en moins");
        assertEquals(50, p.getHealth(), "la moitie de 100 PV");
        assertEquals(items, p.getInventoryItems().size(), "aucun objet perdu");
        int after = s.getClock().getCurrentDay() * 24 * 60 + s.getClock().getCurrentHour() * 60 + s.getClock().getCurrentMinute();
        assertEquals(day * 24 * 60 + minutes + 360, after, "6 heures ont passe");
        assertTrue(message.contains("auberge") && message.contains("20 Po"), message);
    }

    @Test
    void deathPenaltyWithNoGoldDoesNotBreak() {
        GameSession s = GameSession.newGame("realiste");
        Adventurer p = s.getPlayer();
        p.spendGold(p.getGold());
        p.perdreVie(1000);
        DeathPenalty.apply(s);
        assertEquals(0, p.getGold());
        assertTrue(p.estVivant());
    }

    @Test
    void quickPotion() {
        Adventurer p = newPlayer();
        assertTrue(p.drinkHealingPotion().contains("pleine forme"), "a pleine vie : refuse");
        assertEquals(1, p.getInventoryItems().size());
        p.perdreVie(60);
        String message = p.drinkHealingPotion();
        assertTrue(message.contains("+25 PV"), message);
        assertEquals(65, p.getHealth());
        assertEquals(0, p.getInventoryItems().size());
        assertTrue(p.drinkHealingPotion().contains("pas de potion"));
        p.addItem(GameContent.newItem("damage_potion"));
        assertTrue(p.drinkHealingPotion().contains("pas de potion"), "une potion de degats n'est pas une potion de soin");
    }

    @Test
    void newSkillsAndClassBonuses() {
        assertEquals(7, GameContent.skills().size());
        Adventurer warrior = GameSession.newGame("normale", "G", "warrior", "veteran").getPlayer();
        Adventurer herbalist = GameSession.newGame("normale", "H", "herbalist", "veteran").getPlayer();
        warrior.train("melee", 7);       // +50 % : 10,5 points
        herbalist.train("melee", 10);    // -30 % : 7 points
        assertEquals(1, warrior.getSkills().getLevel("melee"));
        assertEquals(0, herbalist.getSkills().getLevel("melee"));
    }
}
