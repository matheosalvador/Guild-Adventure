package com.lucas.guild.model;

public class Skill {
    private String name;
    private String description;
    private int requiredRank;
    private String prerequisiteSkillName; // Nom de la compétence requise (null si aucune)
    private Attaque attackToLearn;
    private boolean learned;

    // No-arg constructor for Gson deserialization
    public Skill() {}

    public Skill(String name, String description, int requiredRank, String prerequisiteSkillName, Attaque attackToLearn) {
        this.name = name;
        this.description = description;
        this.requiredRank = requiredRank;
        this.prerequisiteSkillName = prerequisiteSkillName;
        this.attackToLearn = attackToLearn;
        this.learned = false; // Par défaut, une compétence n'est pas apprise
    }

    // Getters
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getRequiredRank() { return requiredRank; }
    public String getPrerequisiteSkillName() { return prerequisiteSkillName; }
    public Attaque getAttackToLearn() { return attackToLearn; }
    public boolean isLearned() { return learned; }

    public void learn() {
        this.learned = true;
    }
}
