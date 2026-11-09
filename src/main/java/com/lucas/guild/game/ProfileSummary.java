package com.lucas.guild.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.ProfileData;
import com.lucas.guild.data.SkillData;
import com.lucas.guild.model.Item;

/**
 * Met en mots les avantages et les inconvénients d'une classe ou d'une origine, directement à
 * partir de ses chiffres : le texte affiché à la création ne peut donc pas contredire les données.
 * Les lignes sont écrites sans signe (« 20 points de vie ») : l'écran les range sous « + » ou « - ».
 */
public final class ProfileSummary {

    private ProfileSummary() {
    }

    /** Ce que le choix apporte de bien. */
    public static List<String> advantages(ProfileData p) {
        List<String> lines = new ArrayList<>();
        if (p.healthBonus > 0) lines.add(p.healthBonus + " points de vie");
        if (p.carryBonus > 0) lines.add(kg(p.carryBonus) + " de charge portable");
        if (p.speedBonusPercent > 0) lines.add(p.speedBonusPercent + " % de vitesse de déplacement");
        if (p.goldBonus > 0) lines.add(p.goldBonus + " Po au départ");
        for (String itemId : p.startingItems) {
            lines.add("Départ avec : " + itemName(itemId));
        }
        for (Map.Entry<String, Integer> start : p.skillStart.entrySet()) {
            lines.add("Compétence " + skillLabel(start.getKey()) + " déjà au niveau " + start.getValue());
        }
        for (Map.Entry<String, Integer> bonus : p.practiceBonusPercent.entrySet()) {
            if (bonus.getValue() > 0) {
                lines.add(skillLabel(bonus.getKey()) + " progresse " + bonus.getValue() + " % plus vite");
            }
        }
        return lines;
    }

    /** Ce que le choix coûte. */
    public static List<String> drawbacks(ProfileData p) {
        List<String> lines = new ArrayList<>();
        if (p.healthBonus < 0) lines.add(-p.healthBonus + " points de vie");
        if (p.carryBonus < 0) lines.add(kg(-p.carryBonus) + " de charge portable");
        if (p.speedBonusPercent < 0) lines.add(-p.speedBonusPercent + " % de vitesse de déplacement");
        if (p.goldBonus < 0) lines.add(-p.goldBonus + " Po au départ");
        for (Map.Entry<String, Integer> bonus : p.practiceBonusPercent.entrySet()) {
            if (bonus.getValue() < 0) {
                lines.add(skillLabel(bonus.getKey()) + " progresse " + (-bonus.getValue()) + " % moins vite");
            }
        }
        return lines;
    }

    private static String kg(double value) {
        return String.format(Locale.FRANCE, value == Math.rint(value) ? "%.0f kg" : "%.1f kg", value);
    }

    private static String skillLabel(String id) {
        for (SkillData skill : GameContent.skills()) {
            if (skill.id.equals(id)) {
                return skill.label;
            }
        }
        return id;
    }

    private static String itemName(String id) {
        Item item = GameContent.newItem(id);
        return item != null ? item.getName() : id;
    }
}
