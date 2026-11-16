package com.lucas.guild.game;

import java.util.ArrayList;
import java.util.List;

import com.lucas.guild.data.LoreData;
import com.lucas.guild.data.NpcsData;
import com.lucas.guild.model.Adventurer;

/**
 * Les règles des conversations de npcs.json, sans affichage : quels choix un PNJ propose, ce que coûte et rapporte un
 * service, quelle rumeur on achète, ce que peut dire un habitant. L'écran ne fait que les montrer.
 */
public final class NpcTalk {

    private NpcTalk() {
    }

    /** Le texte du bouton d'un choix, avec son prix s'il en a un : « Manger un repas (5 Po) ». */
    public static String label(NpcsData.OptionData option) {
        return option.price > 0 ? option.label + " (" + option.price + " Po)" : option.label;
    }

    /** Remplace {@code {lieu}} par le nom du bâtiment et {@code {prix}} par le prix du choix. */
    public static String fill(String text, String place, int price) {
        if (text == null) {
            return null;
        }
        return text.replace("{lieu}", place != null ? place : "").replace("{prix}", String.valueOf(price));
    }

    /** Les choix que le PNJ propose en ce moment (ceux dont les conditions d'histoire sont vraies), dans l'ordre du fichier. */
    public static List<NpcsData.OptionData> options(NpcsData.NpcData npc, Story story) {
        List<NpcsData.OptionData> shown = new ArrayList<>();
        for (NpcsData.OptionData o : npc.options) {
            if (story == null || story.holds(o.requires)) {
                shown.add(o);
            }
        }
        return shown;
    }

    /**
     * Un service payé (repas, boisson) : l'or est retiré, les PV rendus et la Vigueur entraînée.
     *
     * @return le message à afficher (celui du service, ou celui du manque d'or)
     */
    public static String serve(Adventurer player, NpcsData.OptionData option) {
        if (!player.spendGold(option.price)) {
            return fill(option.noGoldText, null, option.price);
        }
        if (option.heal > 0) {
            player.heal(option.heal);
        }
        if (option.vigor > 0) {
            player.train("vigor", option.vigor);
        }
        return option.text;
    }

    /** Ce que rapporte une rumeur achetée : la rumeur apprise, ou la raison du refus. */
    public static final class RumorResult {
        /** La rumeur, ajoutée au journal ; null si refusée. */
        public final LoreData.RumorData rumor;
        /** Le message quand il n'y a rien à raconter ou que l'or manque ; null sinon. */
        public final String refusal;

        private RumorResult(LoreData.RumorData rumor, String refusal) {
            this.rumor = rumor;
            this.refusal = refusal;
        }
    }

    /** Achète la prochaine rumeur de la ville : on ne paie que s'il y a quelque chose de neuf à raconter. */
    public static RumorResult buyRumor(Adventurer player, String townId, NpcsData.OptionData option) {
        LoreData.RumorData rumor = Lore.nextRumor(player, townId);
        if (rumor == null) {
            return new RumorResult(null, option.emptyText);
        }
        if (!player.spendGold(option.price)) {
            return new RumorResult(null, fill(option.noGoldText, null, option.price));
        }
        Lore.learn(player, Lore.rumorKey(rumor.id));
        return new RumorResult(rumor, null);
    }

    /** Les répliques qu'un habitant peut dire en ce moment, dans cette ville (vide si aucune ne vaut : il ne fait que passer). */
    public static List<String> lines(NpcsData.VillagerTalkData villager, String townId, Story story) {
        List<String> lines = new ArrayList<>();
        for (NpcsData.LineData line : villager.lines) {
            if (line.town != null && !line.town.equals(townId)) {
                continue;
            }
            if (story != null && !story.holds(line.requires)) {
                continue;
            }
            lines.add(line.text);
        }
        return lines;
    }

    /**
     * La réplique suivante : on commence par la plus récente de l'histoire (la dernière de la liste qui vaut en ce
     * moment), puis on fait le tour des autres à chaque nouvelle question.
     *
     * @param asked combien de fois on a déjà parlé à cet habitant pendant cette visite
     * @return null s'il n'a rien à dire
     */
    public static String line(List<String> lines, int asked) {
        if (lines.isEmpty()) {
            return null;
        }
        int n = lines.size();
        return lines.get(((n - 1 - asked) % n + n) % n);
    }
}
