package com.lucas.guild.game;

import java.util.ArrayList;
import java.util.List;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.ZoneData;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Item;

/** Les coffres des zones sauvages : ouverts, ils restent vides {@code respawnDays} jours puis se remplissent de nouveau. */
public final class Chests {

    private Chests() {
    }

    /** Identifiant d'un coffre dans la sauvegarde : « zone#numéro ». */
    public static String key(String zoneId, int index) {
        return zoneId + "#" + index;
    }

    /** Le coffre est-il fermé et plein à ce jour ? */
    public static boolean isFull(Adventurer player, String key, ZoneData.ChestData chest, int today) {
        Integer openedOn = player.getOpenedChests().get(key);
        return openedOn == null || today - openedOn >= chest.respawnDays;
    }

    /**
     * Ouvre le coffre : l'or va au joueur, les objets dans son sac ; ceux qui sont trop lourds sont
     * rendus dans {@code overflow} (l'écran les posera au sol).
     *
     * @return la phrase à afficher, ou null si le coffre est vide
     */
    public static String open(Adventurer player, String key, ZoneData.ChestData chest, int today, List<Item> overflow) {
        if (!isFull(player, key, chest, today)) {
            return null;
        }
        player.getOpenedChests().put(key, today);
        player.addGold(chest.gold);
        List<String> names = new ArrayList<>();
        for (String id : chest.items) {
            Item item = GameContent.newItem(id);
            if (player.addItem(item)) {
                names.add(item.getName());
            } else {
                overflow.add(item);
            }
        }
        StringBuilder message = new StringBuilder("Coffre ouvert : +").append(chest.gold).append(" Po");
        if (!names.isEmpty()) {
            message.append(", ").append(String.join(", ", names));
        }
        if (!overflow.isEmpty()) {
            message.append(" (trop lourd : le reste tombe au sol)");
        }
        return message.append(".").toString();
    }
}
