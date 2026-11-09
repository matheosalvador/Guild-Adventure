package com.lucas.guild.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Une compétence telle qu'écrite dans {@code skills.json}. Elle se gagne par la pratique. */
public class SkillData {

    /** Les effets connus du jeu : nom dans le JSON -> {libellé affiché, unité}. */
    public static final Map<String, String[]> EFFECTS = new LinkedHashMap<>();

    static {
        EFFECTS.put("speedPercent", new String[] {"Vitesse de déplacement", " %"});
        EFFECTS.put("carryKg", new String[] {"Charge portable", " kg"});
        EFFECTS.put("buyDiscountPercent", new String[] {"Réduction à l'achat", " %"});
        EFFECTS.put("sellBonusPercent", new String[] {"Bonus à la revente", " %"});
        EFFECTS.put("healBonusPercent", new String[] {"Soins des potions", " %"});
        EFFECTS.put("healthBonus", new String[] {"Vie maximale", " PV"});
    }

    public String id;
    public String label;
    public String description;
    /** Ce qui fait progresser la compétence (texte affiché au joueur). */
    public String trainedBy;
    /** Unité de pratique (m, Po, potions...), pour l'affichage de la progression. */
    public String unit = "";
    /** Pratique cumulée nécessaire pour atteindre le niveau 1, 2, 3... (croissante). */
    public List<Double> thresholds = new ArrayList<>();
    /** Effet gagné à chaque niveau : nom de l'effet (voir {@link #EFFECTS}) -> valeur par niveau. */
    public Map<String, Double> effects = new LinkedHashMap<>();

    public int maxLevel() {
        return thresholds.size();
    }

    /** Niveau atteint avec {@code practice} points de pratique (0 = pas encore débloquée). */
    public int levelFor(double practice) {
        int level = 0;
        for (double threshold : thresholds) {
            if (practice + 1e-9 >= threshold) {
                level++;
            }
        }
        return level;
    }
}
