package com.lucas.guild;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.MonsterData;
import com.lucas.guild.game.Combat;
import com.lucas.guild.game.GameSession;
import com.lucas.guild.game.Monster;
import com.lucas.guild.game.PlayerFight;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Item;

/**
 * Le réglage des combats, par simulation, avec les vraies règles du jeu ({@link Monster}, {@link Combat}, un
 * personnage équipé) : à chaque étape de l'histoire, un personnage équipé comme on peut l'être à ce moment-là
 * affronte les combats du lieu. Il reste planté sur place, frappe dès que son endurance le permet et boit une potion
 * quand sa vie baisse. Deux façons de se battre :
 * <ul>
 * <li><b>planté</b> : il ne se protège jamais (le pire cas ; les objectifs sont vérifiés sur lui) ;</li>
 * <li><b>en garde</b> : il lève la garde quand un coup s'annonce (un peu tôt : il bloque, il ne pare pas) et la
 * baisse après, sans jamais esquiver ni reculer.</li>
 * </ul>
 * Le rapport est écrit dans {@code target/test-run/combats.txt} et recopié dans {@code docs/REGLAGE-COMBATS.md}.
 */
class CombatBalanceTest {

    private static final float DT = 0.02f;
    /** En garde : il la lève quand il reste moins de ce temps avant que le coup annoncé parte. */
    private static final float GUARD_BEFORE_HIT = 0.35f;

    /** Ce que le personnage a sur lui à une étape de l'histoire. */
    private static final class Kit {
        final String label;
        final String weapon;
        final String armor;
        final int meleeHits;
        final int blowsTaken;
        final int potions;
        final int greaterPotions;

        Kit(String label, String weapon, String armor, int meleeHits, int blowsTaken, int potions, int greaterPotions) {
            this.label = label;
            this.weapon = weapon;
            this.armor = armor;
            this.meleeHits = meleeHits;
            this.blowsTaken = blowsTaken;
            this.potions = potions;
            this.greaterPotions = greaterPotions;
        }
    }

    /** Le résultat d'un combat. */
    private static final class Result {
        boolean won;
        int healthLeft;
        int maxHealth;
        int potionsUsed;
        float seconds;
        int hitsTaken;
        int blocked;
    }

    private static Adventurer equip(Kit kit, Set<String> flags) {
        GameSession s = GameSession.newGame("normale");
        Adventurer p = s.getPlayer();
        for (Item i : new ArrayList<>(p.getInventoryItems())) {
            p.removeItem(i);   // on part du kit de l'étape, rien d'autre
        }
        p.addItem(GameContent.newItem("backpack"));
        for (Item i : new ArrayList<>(p.getInventoryItems())) {
            p.getInventaire().equip(i);
        }
        for (String id : new String[] {kit.weapon, kit.armor}) {
            if (id != null) {
                Item item = GameContent.newItem(id);
                assertTrue(p.addItem(item), "trop lourd : " + id);
                p.getInventaire().equip(item);
            }
        }
        for (int i = 0; i < kit.greaterPotions; i++) {
            p.addItem(GameContent.newItem("greater_healing_potion"));
        }
        for (int i = 0; i < kit.potions; i++) {
            p.addItem(GameContent.newItem("healing_potion"));
        }
        p.train("melee", kit.meleeHits);
        p.train("robustness", kit.blowsTaken);
        // les sceaux intacts donnent leurs avantages au château
        p.setStoryBoons(flags.contains("seal_force_intact") ? 15 : 0, flags.contains("seal_endurance_intact") ? 20 : 0);
        p.ajouterVie(p.getMaxHealth());
        return p;
    }

    /** Le personnage affronte des groupes l'un après l'autre (chaque groupe : des monstres qui arrivent ensemble). */
    private static Result fight(Kit kit, Set<String> flags, boolean guard, String[]... groups) {
        Adventurer p = equip(kit, flags);
        PlayerFight fight = new PlayerFight();
        fight.refill(p);
        float kg = PlayerFight.weaponKg(p);
        Result r = new Result();
        r.maxHealth = p.getMaxHealth();
        int potionsBefore = countPotions(p);
        float t = 0f;
        for (String[] group : groups) {
            List<Monster> monsters = new ArrayList<>();
            for (String id : group) {
                monsters.add(arrive(id, monsters.size(), flags));
            }
            float busy = 0f;   // boire prend le temps d'un coup
            while (p.estVivant() && !allDown(monsters) && t < 900f) {
                t += DT;
                busy -= DT;
                fight.update(DT, p);
                if (guard && blowComing(monsters)) {
                    fight.raiseGuard();
                } else {
                    fight.lowerGuard();
                }
                if (busy <= 0f && !fight.isBlocking()) {
                    if (shouldDrink(p, monsters) && countPotions(p) > 0) {
                        p.drinkHealingPotion();
                        busy = PlayerFight.swingTime(kg);
                    } else if (fight.canSwing(kg, false)) {
                        fight.startCharge();
                        Combat.playerHit(p, firstStanding(monsters), fight.release(kg));
                    }
                }
                List<Monster> added = new ArrayList<>();
                for (Monster m : monsters) {
                    if (m.update(DT, 0f, 0f) && p.estVivant()) {
                        Combat.Received received = Combat.monsterHit(m, p, fight);
                        r.hitsTaken++;
                        if (received.outcome != PlayerFight.Outcome.HIT && received.outcome != PlayerFight.Outcome.GUARD_BROKEN) {
                            r.blocked++;
                        }
                    }
                    MonsterData.Phase phase;
                    while ((phase = m.pollStartedPhase()) != null) {
                        for (int i = 0; phase.summon != null && i < phase.summonCount; i++) {
                            added.add(arrive(phase.summon, monsters.size() + added.size(), flags));
                        }
                    }
                }
                monsters.addAll(added);
            }
            if (!p.estVivant()) {
                break;
            }
        }
        r.won = p.estVivant();
        r.healthLeft = p.getHealth();
        r.potionsUsed = potionsBefore - countPotions(p);
        r.seconds = t;
        return r;
    }

    /**
     * Le coup annoncé du plus dangereux des monstres debout va partir bientôt (en garde, on la lève). Il ne se garde
     * que de celui-là : contre tous à la fois, il ne frapperait plus jamais.
     */
    private static boolean blowComing(List<Monster> monsters) {
        Monster worst = null;
        for (Monster m : monsters) {
            if (!m.isDead() && !m.hasYielded() && (worst == null || m.getDamage() > worst.getDamage())) {
                worst = m;
            }
        }
        return worst != null && worst.isWindingUp() && (1f - worst.windUpShare()) * worst.windUp() <= GUARD_BEFORE_HIT;
    }

    /** Un monstre qui arrive au contact, autour du joueur (en (0, 0)). */
    private static Monster arrive(String id, int index, Set<String> flags) {
        double angle = index * 2.1;
        Monster m = Monster.spawn(id, (float) Math.cos(angle) * 1.2f, (float) Math.sin(angle) * 1.2f);
        m.setConditions(conditions -> holds(conditions, flags));
        return m;
    }

    private static boolean holds(List<String> conditions, Set<String> flags) {
        for (String c : conditions) {
            boolean not = c.startsWith("!");
            String flag = c.substring(not ? 1 : 0).replace("flag:", "");
            if (flags.contains(flag) == not) {
                return false;
            }
        }
        return true;
    }

    /** Boire quand deux coups des monstres présents pourraient tuer, ou sous 35 % de la vie. */
    private static boolean shouldDrink(Adventurer p, List<Monster> monsters) {
        int incoming = 0;
        for (Monster m : monsters) {
            if (!m.isDead() && !m.hasYielded()) {
                incoming += Math.max(1, m.getDamage() - p.getDefense());
            }
        }
        return p.getHealth() <= Math.max(p.getMaxHealth() * 35 / 100, incoming * 2);
    }

    private static int countPotions(Adventurer p) {
        int n = 0;
        for (Item i : p.getInventoryItems()) {
            if (i.estUtilisableHorsCombat()) {
                n++;
            }
        }
        return n;
    }

    private static boolean allDown(List<Monster> monsters) {
        return firstStanding(monsters) == null;
    }

    private static Monster firstStanding(List<Monster> monsters) {
        for (Monster m : monsters) {
            if (!m.isDead() && !m.hasYielded()) {
                return m;
            }
        }
        return null;
    }

    private static String[] g(String... ids) {
        return ids;
    }

    private static String[] times(int n, String id) {
        String[] ids = new String[n];
        Arrays.fill(ids, id);
        return ids;
    }

    // ------------------------------------------------------------------ les étapes de l'histoire

    /** Au départ, en « normale », sans classe : les poings (10 de dégâts), une potion. */
    static final Kit START = new Kit("Départ : sans arme ni armure, 1 potion", null, null, 0, 0, 1, 0);
    /** Chapitre 1 (forêt, ruines, camps) : ce qu'on achète avec l'or du prologue et des premières quêtes. */
    static final Kit CH1 = new Kit("Chapitre 1 : épée courte, armure de cuir, Mêlée 1, 3 potions", "short_sword", "leather_armor", 20, 10, 3, 0);
    /** Chapitres 2 et 3 (mine, fosse) : ce que dit le cahier (hache et cotte de mailles). */
    static final Kit CH2 = new Kit("Chapitres 2-3 : hache, cotte de mailles, Mêlée 2, Robustesse 2, 4 potions", "battle_axe", "chain_mail", 60, 35, 4, 0);
    /** Chapitre 4 (château) : épée longue et plaques, comme dans le cahier. */
    static final Kit CH4 = new Kit("Chapitre 4 : épée longue, plaques, Mêlée 3, Robustesse 3, 4 potions + 2 grandes", "long_sword", "plate_armor", 120, 90, 4, 2);

    private static final Set<String> NO_FLAGS = new HashSet<>();
    private static final Set<String> SEALS_INTACT = new HashSet<>(Arrays.asList("seal_force_intact", "seal_sagesse_intact", "seal_endurance_intact"));
    private static final Set<String> SEALS_CRACKED = new HashSet<>(Arrays.asList("seal_force_cracked", "seal_sagesse_cracked", "seal_endurance_cracked"));

    private final StringBuilder report = new StringBuilder();
    private final List<String> failures = new ArrayList<>();

    /** Un combat, sa ligne dans le rapport, et l'objectif : gagné, avec au plus {@code maxPotions}, et au moins {@code minHealthPercent} de vie. */
    private void scenario(String name, Kit kit, Set<String> flags, int maxPotions, int minHealthPercent, String[]... groups) {
        Result r = fight(kit, flags, false, groups);
        Result g = fight(kit, flags, true, groups);
        int percent = r.healthLeft * 100 / r.maxHealth;
        boolean ok = r.won && r.potionsUsed <= maxPotions && percent >= minHealthPercent;
        report.append(String.format(Locale.FRANCE, "| %s | %s | %s | %d %% | %d / %d | %.0f s | %d | %s | %s, %d %%, %d potion(s), %.0f s |%n",
                name, kit.label, r.won ? "gagné" : "**perdu**", percent, r.potionsUsed, maxPotions, r.seconds, r.hitsTaken,
                ok ? "oui" : "**non**",
                g.won ? "gagné" : "**perdu**", g.healthLeft * 100 / g.maxHealth, g.potionsUsed, g.seconds));
        if (!ok) {
            failures.add(name);
        }
    }

    @Test
    void everyFightOfTheStoryIsWinnableWithTheGearOfItsTime() throws Exception {
        report.append("| Combat | Équipement | Planté : issue | Vie restante | Potions bues / permises | Durée | Coups reçus | Objectif | En garde : issue, vie, potions, durée |\n");
        report.append("| --- | --- | --- | --- | --- | --- | --- | --- | --- |\n");
        // prologue
        scenario("Cave : 3 rats ensemble", START, NO_FLAGS, 1, 20, times(3, "giant_rat"));
        scenario("Forêt : 1 loup", START, NO_FLAGS, 1, 20, g("wolf"));
        // chapitre 1
        scenario("Forêt : 3 paires de loups", CH1, NO_FLAGS, 2, 20, times(2, "wolf"), times(2, "wolf"), times(2, "wolf"));
        scenario("Ruines : 2 + 3 gobelins", CH1, NO_FLAGS, 2, 20, times(2, "goblin"), times(3, "goblin"));
        scenario("Ruines : le chef et ses 2 gobelins", CH1, NO_FLAGS, 3, 10, g("goblin_chief", "goblin", "goblin"));
        scenario("Camp de bandits : 3 bandits", CH1, NO_FLAGS, 2, 15, times(3, "bandit"));
        scenario("Campement gobelin : 3 gobelins", CH1, NO_FLAGS, 2, 20, times(3, "goblin"));
        // chapitres 2 et 3
        scenario("Mine : chauves-souris (3 + 3) puis squelettes (2 + 2)", CH2, NO_FLAGS, 2, 20,
                times(3, "bat"), times(3, "bat"), times(2, "skeleton_miner"), times(2, "skeleton_miner"));
        scenario("Mine : le sergent Holt", CH2, NO_FLAGS, 3, 10, g("sergeant_holt"));
        scenario("Fosse : les trois vagues de l'arène", CH2, NO_FLAGS, 3, 10,
                times(3, "wolf"), g("goblin", "goblin", "goblin", "wolf"), g("skeleton_miner", "skeleton_miner", "skeleton_miner", "bat", "bat"));
        // chapitre 4
        scenario("Corvenne : les soldats de la cour (2 + 2 + 1)", CH4, SEALS_INTACT, 2, 20,
                times(2, "garrison_soldier"), times(2, "garrison_soldier"), g("garrison_soldier"));
        scenario("Corvenne : le capitaine Varn", CH4, SEALS_INTACT, 4, 10, g("captain_varn"));
        scenario("Corvenne : Aldric, sceaux intacts", CH4, SEALS_INTACT, 5, 5, g("lord_aldric"));
        scenario("Corvenne : Aldric, sceaux fêlés", CH4, SEALS_CRACKED, 6, 1, g("lord_aldric"));

        String text = report.toString();
        System.out.print(text);
        Files.write(Paths.get("combats.txt"), text.getBytes(StandardCharsets.UTF_8));
        assertTrue(failures.isEmpty(), "combats hors objectif : " + failures + "\n" + text);
    }
}
