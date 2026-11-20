package com.lucas.guild.game;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.OpenWorldData;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Item;

/**
 * Ramasser dans la campagne (une herbe de soin, du minerai) : la chose disparaît et repousse au bout de quelques
 * jours de jeu ; le jour où on l'a prise est sauvegardé avec le personnage (voir {@link Terrain#gatherables}).
 */
public final class Gathering {

    private Gathering() {
    }

    /** La chose est-elle là ? Oui si on ne l'a jamais prise, ou s'il y a au moins {@code respawnDays} jours. */
    public static boolean isThere(Adventurer player, String key, int today, int respawnDays) {
        Integer taken = player.getGathered().get(key);
        return taken == null || today - taken >= respawnDays;
    }

    /**
     * Le joueur ramasse la chose {@code key} de la sorte {@code kind}.
     *
     * @return la phrase à afficher ; la chose n'est prise que si l'objet entre dans le sac
     */
    public static String take(Adventurer player, String key, OpenWorldData.GatherData kind, int today) {
        Item item = GameContent.newItem(kind.item);
        if (!player.addItem(item)) {
            return "Trop chargé pour prendre : " + item.getName() + ".";
        }
        player.getGathered().put(key, today);
        return kind.verb + " : " + item.getName() + ".";
    }
}
