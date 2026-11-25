package com.lucas.guild.game;

import java.util.Random;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.ObjetDeQuete;
import com.lucas.guild.model.Quest;

/**
 * Les règles du combat en mêlée, sans interface : dégâts, défense, progression des compétences,
 * butin. L'écran décide quand un coup est porté ; ici on décide ce qu'il fait.
 */
public final class Combat {

    /** Portée d'un coup du joueur, en m (mesurée depuis ses yeux jusqu'au monstre visé). */
    public static final float PLAYER_RANGE = 3.5f;

    private Combat() {
    }

    /**
     * Le joueur frappe un monstre : le coup fait les dégâts de son arme (et de sa compétence Mêlée)
     * et entraîne la Mêlée.
     *
     * @return les dégâts infligés
     */
    public static int playerHit(Adventurer player, Monster monster) {
        return playerHit(player, monster, 1f);
    }

    /**
     * Un coup du joueur, multiplié par {@code factor} (un coup chargé : voir {@link PlayerFight#chargeFactor}).
     * Un coup chargé fait chanceler un monstre ordinaire.
     *
     * @return les dégâts infligés
     */
    public static int playerHit(Adventurer player, Monster monster, float factor) {
        int damage = Math.round(player.getAttackDamage() * factor);
        monster.takeDamage(damage);
        player.train("melee", 1);
        if (factor > 1f && !monster.data.boss) {
            monster.stagger(PlayerFight.CHARGE_STAGGER);
        }
        return damage;
    }

    /** Ce qu'a donné un coup de monstre : comment il a été reçu et les PV perdus. */
    public static final class Received {
        public final PlayerFight.Outcome outcome;
        public final int damage;

        Received(PlayerFight.Outcome outcome, int damage) {
            this.outcome = outcome;
            this.damage = damage;
        }
    }

    /**
     * Un monstre frappe un joueur qui peut esquiver, bloquer ou parer ({@code fight}).
     * Esquivé ou paré : rien (paré, le monstre chancelle). Bloqué : le quart des dégâts. Garde brisée ou en plein :
     * les dégâts entiers. Dans tous les cas, la défense s'applique comme dans {@link #monsterHit(Monster, Adventurer)}.
     */
    public static Received monsterHit(Monster monster, Adventurer player, PlayerFight fight) {
        PlayerFight.Outcome outcome = fight.receive(monster.getDamage());
        switch (outcome) {
            case DODGED:
                return new Received(outcome, 0);
            case PARRIED:
                monster.stagger(monster.data.boss ? PlayerFight.PARRY_STAGGER_BOSS : PlayerFight.PARRY_STAGGER);
                player.train("melee", 1);
                return new Received(outcome, 0);
            case BLOCKED:
                return new Received(outcome, hurt(player, Math.round(monster.getDamage() * PlayerFight.BLOCK_KEEP)));
            default:
                return new Received(outcome, hurt(player, monster.getDamage()));
        }
    }

    /**
     * Un monstre frappe le joueur : dégâts du monstre moins la défense (armure, Robustesse), au moins 1.
     * Encaisser entraîne Robustesse. Un joueur invincible (debug) ne perd rien.
     *
     * @return les dégâts subis
     */
    public static int monsterHit(Monster monster, Adventurer player) {
        return hurt(player, monster.getDamage());
    }

    /** Le joueur encaisse {@code raw} dégâts, moins sa défense (au moins 1, sauf un coup réduit à rien). */
    private static int hurt(Adventurer player, int raw) {
        if (DebugMode.isInvincible() || raw <= 0) {
            return 0;
        }
        int damage = Math.max(1, raw - player.getDefense());
        player.perdreVie(damage);
        player.train("robustness", 1);
        return damage;
    }

    /**
     * Le monstre vient de mourir : or lâché et, si une quête active demande cet objet comme preuve,
     * l'objet de quête.
     *
     * @return la phrase à afficher au joueur
     */
    public static String loot(Adventurer player, Monster monster, Random random) {
        int gold = monster.data.goldMin + random.nextInt(monster.data.goldMax - monster.data.goldMin + 1);
        player.addGold(gold);
        StringBuilder message = new StringBuilder(monster.getName()).append(" vaincu : +").append(gold).append(" Po");

        String loot = monster.data.lootItem;
        // une seule preuve suffit : on n'en redonne pas à chaque monstre tant que le joueur porte la sienne
        if (loot != null && wantedByActiveQuest(player, loot) && !carriesQuestItem(player, loot)) {
            if (player.addItem(new ObjetDeQuete(loot))) {
                message.append(", ").append(loot);
            } else {
                message.append(" (trop lourd pour ramasser : ").append(loot).append(")");
            }
        }
        return message.append(".").toString();
    }

    /** Les objets qui tombent au sol quand {@code monster} meurt (chacun selon sa chance). */
    public static java.util.List<com.lucas.guild.model.Item> rollDrops(Monster monster, Random random) {
        java.util.List<com.lucas.guild.model.Item> drops = new java.util.ArrayList<>();
        for (com.lucas.guild.data.MonsterData.Drop drop : monster.data.drops) {
            if (random.nextInt(100) < drop.chance) {
                drops.add(GameContent.newItem(drop.item));
            }
        }
        return drops;
    }

    private static boolean carriesQuestItem(Adventurer player, String name) {
        for (com.lucas.guild.model.Item item : player.getInventoryItems()) {
            if (item instanceof ObjetDeQuete && name.equals(item.getName())) {
                return true;
            }
        }
        return false;
    }

    private static boolean wantedByActiveQuest(Adventurer player, String item) {
        for (Quest quest : GameContent.quests()) {
            if (item.equals(quest.getProofItem()) && player.getActiveQuests().contains(quest.getTitle())) {
                return true;
            }
        }
        return false;
    }
}
