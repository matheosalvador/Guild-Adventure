package com.lucas.guild.model;

import java.util.ArrayList;
import java.util.List;

import com.lucas.guild.data.GameContent;

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

    /** Les compétences de la classe viennent de assets/data/skills.json (aucune si la classe n'y figure pas). */
    private void initializeTree() {
        skills.addAll(GameContent.skillsFor(className));
    }

    public List<Skill> getSkills() {
        return skills;
    }
}
