package com.lucas.guild.game;

import java.util.ArrayList;
import java.util.List;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.LoreData;
import com.lucas.guild.model.Adventurer;

/**
 * Ce que le personnage sait du monde : les rumeurs entendues à l'auberge et les textes lus sur les
 * panneaux, stèles et livres. Tout est noté dans le personnage (donc sauvegardé) et relu dans le
 * journal. Sans dépendance à l'interface.
 */
public final class Lore {

    private Lore() {
    }

    public static String rumorKey(String id) {
        return "rumor:" + id;
    }

    public static String textKey(String id) {
        return "text:" + id;
    }

    /** Le personnage connaît-il déjà ce savoir ? */
    public static boolean isKnown(Adventurer player, String key) {
        return player.getKnownLore().contains(key);
    }

    /** Note un savoir dans le journal. @return true si c'était nouveau */
    public static boolean learn(Adventurer player, String key) {
        if (isKnown(player, key)) {
            return false;
        }
        player.getKnownLore().add(key);
        return true;
    }

    /** La prochaine rumeur que l'aubergiste de {@code townId} peut raconter (dans l'ordre du fichier), ou null s'il n'a plus rien de neuf. */
    public static LoreData.RumorData nextRumor(Adventurer player, String townId) {
        Story story = new Story(player, null);
        for (LoreData.RumorData rumor : GameContent.lore().rumors) {
            // certaines rumeurs ne se racontent qu'à partir d'un moment de l'histoire
            if ((rumor.town == null || rumor.town.equals(townId)) && !isKnown(player, rumorKey(rumor.id)) && story.holds(rumor.requires)) {
                return rumor;
            }
        }
        return null;
    }

    /** Les rumeurs entendues, dans l'ordre du fichier. */
    public static List<LoreData.RumorData> heardRumors(Adventurer player) {
        List<LoreData.RumorData> heard = new ArrayList<>();
        for (LoreData.RumorData rumor : GameContent.lore().rumors) {
            if (isKnown(player, rumorKey(rumor.id))) {
                heard.add(rumor);
            }
        }
        return heard;
    }

    /** Les textes lus, dans l'ordre du fichier. */
    public static List<LoreData.TextData> readTexts(Adventurer player) {
        List<LoreData.TextData> read = new ArrayList<>();
        for (LoreData.TextData text : GameContent.lore().texts.values()) {
            if (isKnown(player, textKey(text.id))) {
                read.add(text);
            }
        }
        return read;
    }
}
