package model;

public class Quest {

    private String title;
    private String description;
    private int rank; // 0=F, 1=E, etc.
    private int goldReward;
    private int reputationReward;
    private String proofItem; // L'objet à ramener pour prouver le succès
    private boolean isCompleted;

    public Quest(String title, String description, int rank, int goldReward, int reputationReward, String proofItem) {
        this.title = title;
        this.description = description;
        this.rank = rank;
        this.goldReward = goldReward;
        this.reputationReward = reputationReward;
        this.proofItem = proofItem;
        this.isCompleted = false;
    }

    public String getTitle() {
        return title;
    }

    public int getGoldReward() {
        return goldReward;
    }

    public String getProofItem() {
        return proofItem;
    }

    public int getRank() {
        return rank;
    }

    public void complete() {
        this.isCompleted = true;
    }

    @Override
    public String toString() {
        return "Quête de Rang " + rank + ": " + title + "\n" +
               "  Description: " + description + "\n" +
               "  Récompense: " + goldReward + " Or, " + reputationReward + " Réputation\n" +
               "  Preuve requise: " + proofItem;
    }
}
