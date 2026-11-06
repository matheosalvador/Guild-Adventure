package com.lucas.guild.model;

public class Quest {

    private String title;
    private String description;
    private int rank; // 0=F, 1=E, etc.
    private QuestType type;
    private String target;
    private int goldReward;
    private int experienceReward;
    private String proofItem; // L'objet à ramener pour prouver le succès
    private boolean isCompleted;

    public Quest(String title, String description, int rank, QuestType type, String target, int goldReward, int experienceReward, String proofItem) {
        this.title = title;
        this.description = description;
        this.rank = rank;
        this.type = type;
        this.target = target;
        this.goldReward = goldReward;
        this.experienceReward = experienceReward;
        this.proofItem = proofItem;
        this.isCompleted = false;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public int getRank() {
        return rank;
    }

    public QuestType getType() {
        return type;
    }

    public String getTarget() {
        return target;
    }

    public int getGoldReward() {
        return goldReward;
    }

    public int getExperienceReward() {
        return experienceReward;
    }

    public String getProofItem() {
        return proofItem;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void complete() {
        this.isCompleted = true;
    }

    @Override
    public String toString() {
        return "Quête de Rang " + rank + ": " + title + "\n" +
               "  Description: " + description + "\n" +
               "  Récompense: " + goldReward + " Or, " + experienceReward + " XP\n" +
               "  Preuve requise: " + proofItem;
    }
}
