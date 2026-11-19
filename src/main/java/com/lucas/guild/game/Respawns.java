package com.lucas.guild.game;

import com.lucas.guild.model.Adventurer;

/**
 * Les monstres du monde ouvert ne reviennent pas dès qu'on tourne le dos : un monstre tué reste mort quelques jours
 * de jeu (openworld.json, « monsterRespawnDays »), comme un coffre vidé. Le jour de sa mort est sauvegardé avec le
 * personnage.
 */
public final class Respawns {

    private Respawns() {
    }

    /** L'identifiant d'un monstre placé à la main : son lieu et son rang dans le fichier du lieu. */
    public static String key(String placeId, int index) {
        return placeId + "#" + index;
    }

    /** Ce monstre est-il là ? Oui s'il n'a jamais été tué, ou s'il l'a été il y a au moins {@code respawnDays} jours. */
    public static boolean isAlive(Adventurer player, String key, int today, int respawnDays) {
        Integer killed = player.getMonsterKills().get(key);
        return killed == null || today - killed >= respawnDays;
    }

    /** Note la mort du monstre aujourd'hui. */
    public static void killed(Adventurer player, String key, int today) {
        player.getMonsterKills().put(key, today);
    }
}
