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

    /** Temps minimum entre deux coups du joueur, en secondes. */
    public static final float PLAYER_COOLDOWN = 0.6f;
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
        int damage = player.getAttackDamage();
        monster.takeDamage(damage);
        player.train("melee", 1);
        return damage;
    }

    /**
     * Un monstre frappe le joueur : dégâts du monstre moins la défense (armure, Robustesse), au moins 1.
     * Encaisser entraîne Robustesse. Un joueur invincible (debug) ne perd rien.
     *
     * @return les dégâts subis
     */
    public static int monsterHit(Monster monster, Adventurer player) {
        if (DebugMode.isInvincible()) {
            return 0;
        }
        int damage = Math.max(1, monster.data.damage - player.getDefense());
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
        if (loot != null && wantedByActiveQuest(player, loot)) {
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

    private static boolean wantedByActiveQuest(Adventurer player, String item) {
        for (Quest quest : GameContent.quests()) {
            if (item.equals(quest.getProofItem()) && player.getActiveQuests().contains(quest.getTitle())) {
                return true;
            }
        }
        return false;
    }
}
