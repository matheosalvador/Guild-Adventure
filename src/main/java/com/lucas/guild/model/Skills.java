package com.lucas.guild.model;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.SkillData;

/**
 * Les compétences d'un personnage. Elles ne s'achètent pas : on gagne des points de pratique
 * en faisant l'action (marcher, marchander...), et un niveau se débloque à chaque seuil
 * ({@code thresholds} dans skills.json). Chaque niveau donne des effets (vitesse, charge, prix...).
 *
 * Seule la pratique est sauvegardée ; niveaux et effets se recalculent à partir de skills.json.
 */
public class Skills {

    /** Pratique cumulée, par identifiant de compétence. */
    private Map<String, Double> practice = new LinkedHashMap<>();

    private transient Map<String, SkillData> defs;
    private transient List<SkillData> ordered;
    private transient Map<String, Integer> practiceBonusPercent = new HashMap<>();
    private transient Map<String, Double> bonuses = new HashMap<>();

    public Skills() {
    }

    /**
     * (Re)lit skills.json et applique les bonus de progression du personnage.
     *
     * @param practiceBonusPercent identifiant -> % de pratique en plus ou en moins (classe, origine)
     */
    public void bind(Map<String, Integer> practiceBonusPercent) {
        if (practice == null) {
            practice = new LinkedHashMap<>();
        }
        ordered = GameContent.skills();
        defs = new LinkedHashMap<>();
        for (SkillData data : ordered) {
            defs.put(data.id, data);
        }
        this.practiceBonusPercent = practiceBonusPercent != null ? practiceBonusPercent : new HashMap<String, Integer>();
        recomputeBonuses();
    }

    private void ensureBound() {
        if (defs == null) {
            bind(null);
        }
    }

    /** Toutes les compétences du jeu, dans l'ordre du fichier. */
    public List<SkillData> definitions() {
        ensureBound();
        return ordered;
    }

    public SkillData definition(String id) {
        ensureBound();
        return defs.get(id);
    }

    public double getPractice(String id) {
        Double value = practice.get(id);
        return value != null ? value : 0;
    }

    /** Niveau actuel (0 = compétence pas encore débloquée). */
    public int getLevel(String id) {
        SkillData def = definition(id);
        return def != null ? def.levelFor(getPractice(id)) : 0;
    }

    /** Somme, sur toutes les compétences, de l'effet {@code effect} (niveau x valeur par niveau). */
    public double bonus(String effect) {
        Double value = bonuses.get(effect);
        return value != null ? value : 0;
    }

    /**
     * Ajoute de la pratique. Le bonus de progression de la classe et de l'origine s'applique.
     *
     * @return un message si un nouveau niveau est atteint (à montrer au joueur), sinon null
     */
    public String train(String id, double amount) {
        if (amount <= 0) {
            return null;
        }
        SkillData def = definition(id);
        if (def == null) {
            throw new IllegalArgumentException("Competence inconnue : " + id + " (voir skills.json)");
        }
        int before = def.levelFor(getPractice(id));
        Integer percent = practiceBonusPercent.get(id);
        double factor = percent != null ? Math.max(0.1, 1 + percent / 100.0) : 1.0;
        practice.put(id, getPractice(id) + amount * factor);
        int after = def.levelFor(getPractice(id));
        if (after == before) {
            return null;
        }
        recomputeBonuses();
        if (before == 0) {
            return "Compétence débloquée : " + def.label + " (niveau " + after + ") !";
        }
        return "Compétence " + def.label + " : niveau " + after + " !";
    }

    /** Donne au moins le niveau {@code level} à une compétence (départ du personnage). */
    public void setLevel(String id, int level) {
        SkillData def = definition(id);
        if (def == null || level < 1) {
            return;
        }
        int capped = Math.min(level, def.maxLevel());
        practice.put(id, Math.max(getPractice(id), def.thresholds.get(capped - 1)));
        recomputeBonuses();
    }

    private void recomputeBonuses() {
        Map<String, Double> sums = new HashMap<>();
        for (SkillData def : ordered) {
            int level = def.levelFor(getPractice(def.id));
            if (level == 0) {
                continue;
            }
            for (Map.Entry<String, Double> effect : def.effects.entrySet()) {
                Double sum = sums.get(effect.getKey());
                sums.put(effect.getKey(), (sum != null ? sum : 0) + effect.getValue() * level);
            }
        }
        bonuses = sums;
    }
}
