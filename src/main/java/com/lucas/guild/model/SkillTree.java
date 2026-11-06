package com.lucas.guild.model;

import java.util.ArrayList;
import java.util.List;

public class SkillTree {
    private String className;
    private List<Skill> skills = new ArrayList<>();

    public SkillTree(String className) {
        this.className = className;
        initializeTree();
    }

    // No-arg constructor for Gson deserialization
    public SkillTree() {
        this.skills = new ArrayList<>();
    }

    private void initializeTree() {
        if ("Caserne".equals(className)) {
            // --- TIER 1 (Rang F) ---
            skills.add(new Skill("Attaque Puissante", "Une frappe lente mais dévastatrice.", 0, null, new AttaqueSimple("Attaque Puissante", 25)));

            // --- TIER 2 (Rang E) ---
            skills.add(new Skill("Coup de Bouclier", "Repousse l'ennemi, infligeant de légers dégâts.", 1, "Attaque Puissante", new AttaqueSimple("Coup de Bouclier", 10)));
            
            // --- TIER 3 (Rang D) ---
            skills.add(new Skill("Frappe Circulaire", "Une attaque qui pourrait toucher plusieurs ennemis.", 2, "Coup de Bouclier", new AttaqueSimple("Frappe Circulaire", 20)));
        }
        // On pourra ajouter ici les arbres pour "Académie" et "Église"
    }

    public List<Skill> getSkills() {
        return skills;
    }
}
