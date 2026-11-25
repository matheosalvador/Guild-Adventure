package com.lucas.guild;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.game.Combat;
import com.lucas.guild.game.DebugMode;
import com.lucas.guild.game.GameSession;
import com.lucas.guild.game.Monster;
import com.lucas.guild.game.PlayerFight;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Item;

/** Le combat vivant : coups annoncés, endurance, esquive, garde, parade, poids de l'arme, coup chargé. */
class PlayerFightTest {

    @AfterEach
    void resetDebug() {
        DebugMode.setEnabled(false);
    }

    private static Adventurer newPlayer() {
        return GameSession.newGame("normale").getPlayer();
    }

    private static PlayerFight fresh(Adventurer p) {
        PlayerFight f = new PlayerFight();
        f.refill(p);
        return f;
    }

    private static void hold(Adventurer p, String id) {
        Item item = GameContent.newItem(id);
        assertTrue(p.addItem(item));
        p.getInventaire().equip(item);
    }

    /** Laisse passer {@code seconds} secondes par pas de 0,01 s. */
    private static void pass(PlayerFight f, Adventurer p, float seconds) {
        for (float t = 0f; t < seconds - 1e-4f; t += 0.01f) {
            f.update(0.01f, p);
        }
    }

    // ------------------------------------------------------------------ le monstre annonce son coup

    @Test
    void aBlowIsAnnouncedBeforeItLands() {
        Monster m = Monster.spawn("giant_rat", 0, 1f);   // à portée
        assertFalse(m.update(0.1f, 0, 0));
        assertTrue(m.isWindingUp(), "il prépare son coup");
        assertTrue(m.windUpShare() > 0f && m.windUpShare() < 0.5f);
        assertFalse(m.update(0.3f, 0, 0), "le coup n'est pas encore parti");
        assertTrue(m.update(0.15f, 0, 0), "le coup part au bout de 0,5 s");
        assertFalse(m.isWindingUp());
    }

    @Test
    void steppingBackMakesTheBlowMiss() {
        Monster m = Monster.spawn("giant_rat", 0, 1f);
        m.update(0.1f, 0, 0);
        assertTrue(m.isWindingUp());
        // le joueur recule pendant la préparation : le monstre reste planté, le coup frappe dans le vide
        assertFalse(m.update(0.5f, 0, -4f));
        assertFalse(m.isWindingUp());
        assertEquals(1f, m.z, 1e-4, "il ne bouge pas pendant la préparation");
    }

    @Test
    void aStaggeredMonsterNeitherMovesNorStrikes() {
        Monster m = Monster.spawn("giant_rat", 0, 1f);
        m.update(0.1f, 0, 0);
        m.stagger(1f);
        assertTrue(m.isStaggered());
        assertFalse(m.isWindingUp(), "le coup préparé est perdu");
        for (int i = 0; i < 9; i++) {
            assertFalse(m.update(0.1f, 0, 5f));
        }
        assertEquals(1f, m.z, 1e-4);
        assertFalse(m.update(0.2f, 0, 0));
        assertFalse(m.isStaggered());
    }

    @Test
    void everyMonsterWindsUpFasterThanItAttacks() {
        for (String id : GameContent.monsters().keySet()) {
            Monster m = Monster.spawn(id, 0, 0);
            assertTrue(m.windUp() < m.data.attackCooldown, id);
        }
    }

    // ------------------------------------------------------------------ l'endurance et les coups

    @Test
    void swingsCostStaminaWhichComesBack() {
        Adventurer p = newPlayer();
        PlayerFight f = fresh(p);
        float kg = PlayerFight.weaponKg(p);
        assertEquals(PlayerFight.BARE_HANDS_KG, kg, 1e-6);
        f.startCharge();
        assertEquals(1f, f.release(kg), 1e-6, "un coup normal");
        assertEquals(PlayerFight.BASE_STAMINA - PlayerFight.swingCost(kg, false), f.getStamina(), 1e-3);
        f.startCharge();
        assertEquals(0f, f.release(kg), 1e-6, "pas deux coups sans attendre");
        pass(f, p, 2f);
        assertEquals(PlayerFight.BASE_STAMINA, f.getStamina(), 1e-3, "l'endurance revient au repos");
    }

    @Test
    void noStaminaNoSwing() {
        Adventurer p = newPlayer();
        PlayerFight f = fresh(p);
        float kg = PlayerFight.weaponKg(p);
        int swings = 0;
        for (int i = 0; i < 100; i++) {
            f.startCharge();
            if (f.release(kg) > 0f) swings++;
            pass(f, p, PlayerFight.swingTime(kg));
        }
        assertTrue(swings < 100, "à force de frapper sans s'arrêter, on doit reprendre son souffle (" + swings + ")");
        assertTrue(swings > 20, "mais on frappe longtemps avant (" + swings + ")");
    }

    @Test
    void heavierWeaponsAreSlowerAndMoreTiring() {
        assertTrue(PlayerFight.swingTime(1f) < PlayerFight.swingTime(3.5f));
        assertTrue(PlayerFight.swingCost(1f, false) < PlayerFight.swingCost(3.5f, false));
        assertTrue(PlayerFight.chargeFactor(1f) < PlayerFight.chargeFactor(3.5f), "mais un coup chargé lourd frappe plus fort");
        Adventurer p = newPlayer();
        hold(p, "battle_axe");
        assertEquals(3.5f, PlayerFight.weaponKg(p), 1e-6);
    }

    @Test
    void aChargedBlowHitsHarderAndStaggers() {
        Adventurer p = newPlayer();
        hold(p, "battle_axe");
        PlayerFight f = fresh(p);
        float kg = PlayerFight.weaponKg(p);
        f.startCharge();
        assertFalse(f.isCharged());
        pass(f, p, PlayerFight.CHARGE_TIME + 0.02f);
        assertTrue(f.isCharged());
        float factor = f.release(kg);
        assertEquals(PlayerFight.chargeFactor(kg), factor, 1e-6);
        Monster chief = Monster.spawn("garrison_soldier", 0, 0);   // 60 PV : il survit au coup
        int damage = Combat.playerHit(p, chief, factor);
        assertEquals(Math.round(p.getAttackDamage() * factor), damage);
        assertTrue(chief.isStaggered(), "un monstre ordinaire chancelle");
        Monster holt = Monster.spawn("sergeant_holt", 0, 0);
        Combat.playerHit(p, holt, factor);
        assertFalse(holt.isStaggered(), "un adversaire nommé, non");
    }

    // ------------------------------------------------------------------ garde, parade, esquive

    @Test
    void raisingTheGuardJustInTimeParries() {
        Adventurer p = newPlayer();
        PlayerFight f = fresh(p);
        Monster wolf = Monster.spawn("wolf", 0, 1f);
        f.raiseGuard();
        pass(f, p, PlayerFight.PARRY_WINDOW / 2f);
        int before = p.getHealth();
        Combat.Received r = Combat.monsterHit(wolf, p, f);
        assertEquals(PlayerFight.Outcome.PARRIED, r.outcome);
        assertEquals(0, r.damage);
        assertEquals(before, p.getHealth());
        assertTrue(wolf.isStaggered());
    }

    @Test
    void aRaisedGuardBlocksMostOfTheBlowForStamina() {
        Adventurer p = newPlayer();
        PlayerFight f = fresh(p);
        Monster varn = Monster.spawn("captain_varn", 0, 1f);
        f.raiseGuard();
        pass(f, p, 0.5f);
        Combat.Received r = Combat.monsterHit(varn, p, f);
        assertEquals(PlayerFight.Outcome.BLOCKED, r.outcome);
        assertEquals(Math.max(1, Math.round(25 * PlayerFight.BLOCK_KEEP) - p.getDefense()), r.damage);
        assertEquals(PlayerFight.BASE_STAMINA - 25 * PlayerFight.BLOCK_COST_PER_DAMAGE, f.getStamina(), 1e-3);
        assertFalse(varn.isStaggered());
        pass(f, p, 2f);
        assertEquals(PlayerFight.BASE_STAMINA - 25 * PlayerFight.BLOCK_COST_PER_DAMAGE, f.getStamina(), 1e-3,
                "en garde, on ne reprend pas son souffle");
    }

    @Test
    void theGuardBreaksWhenStaminaRunsOut() {
        Adventurer p = newPlayer();
        PlayerFight f = fresh(p);
        Monster varn = Monster.spawn("captain_varn", 0, 1f);
        f.raiseGuard();
        pass(f, p, 0.5f);
        PlayerFight.Outcome last = null;
        for (int i = 0; i < 10 && last != PlayerFight.Outcome.GUARD_BROKEN; i++) {
            last = Combat.monsterHit(varn, p, f).outcome;
        }
        assertEquals(PlayerFight.Outcome.GUARD_BROKEN, last);
        assertFalse(f.isBlocking());
        assertFalse(f.raiseGuard(), "pas de garde tout de suite après");
        pass(f, p, PlayerFight.GUARD_BROKEN_TIME + 0.05f);
        assertTrue(f.raiseGuard());
    }

    @Test
    void aDodgeAvoidsTheBlowForAMoment() {
        Adventurer p = newPlayer();
        PlayerFight f = fresh(p);
        Monster wolf = Monster.spawn("wolf", 0, 1f);
        assertTrue(f.dodge());
        assertTrue(f.isDodging());
        assertEquals(PlayerFight.BASE_STAMINA - PlayerFight.DODGE_COST, f.getStamina(), 1e-3);
        assertFalse(f.dodge(), "pas deux esquives d'affilée");
        int before = p.getHealth();
        assertEquals(PlayerFight.Outcome.DODGED, Combat.monsterHit(wolf, p, f).outcome);
        assertEquals(before, p.getHealth());
        pass(f, p, PlayerFight.DODGE_SAFE + 0.05f);
        assertFalse(f.isDodging());
        assertEquals(PlayerFight.Outcome.HIT, Combat.monsterHit(wolf, p, f).outcome);
        assertTrue(p.getHealth() < before);
    }

    @Test
    void noSwingWhileBlocking() {
        Adventurer p = newPlayer();
        PlayerFight f = fresh(p);
        f.raiseGuard();
        assertFalse(f.canSwing(PlayerFight.weaponKg(p), false));
        f.lowerGuard();
        assertTrue(f.canSwing(PlayerFight.weaponKg(p), false));
    }

    @Test
    void theEnduranceSkillRaisesMaxStamina() {
        Adventurer p = newPlayer();
        assertEquals(PlayerFight.BASE_STAMINA, PlayerFight.maxStamina(p), 1e-6);
        p.getSkills().setLevel("athletics", 2);
        p.refreshBonuses();
        assertEquals(PlayerFight.BASE_STAMINA + 10f, PlayerFight.maxStamina(p), 1e-6);
    }
}
