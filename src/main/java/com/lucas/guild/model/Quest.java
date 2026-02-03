package com.lucas.guild.model;

public class Quest {

    public enum QuestType { COMBAT, DELIVERY }

    private String title;
    private String description;
    private int rank;
    private int goldReward;
    private int xpReward;
    private String proofItem;
    private boolean isCompleted;
    
    private QuestType type;
    private String target; // Nom du monstre ou du PNJ

    public Quest(String title, String description, int rank, QuestType type, String target, int goldReward, int xpReward, String proofItem) {
        this.title = title;
        this.description = description;
        this.rank = rank;
        this.type = type;
        this.target = target;
        this.goldReward = goldReward;
        this.xpReward = xpReward;
        this.proofItem = proofItem;
        this.isCompleted = false;
    }

    // Getters
    public String getTitle() { return title; }
    public int getGoldReward() { return goldReward; }
    public int getXpReward() { return xpReward; }
    public String getProofItem() { return proofItem; }
    public int getRank() { return rank; }
    public QuestType getType() { return type; }
    public String getTarget() { return target; }

    public void complete() { this.isCompleted = true; }
    public boolean isCompleted() { return isCompleted; }

    @Override
    public String toString() {
        return "Quête de Rang " + rank + ": " + title + "\n" +
               "  Objectif: " + description + "\n" +
               "  Récompense: " + goldReward + " Or, " + xpReward + " XP";
    }
}
