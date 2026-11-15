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
    /** Objet remis au joueur à l'acceptation (livraisons : la lettre à porter), ou null. */
    private String giveItem;
    /** La ville dont le tableau propose la quete (la ville de depart si absent). */
    private String town;
    /** COLLECT : l'objet a rapporter (identifiant de items.json) et en quelle quantite. */
    private String collectItem;
    private int collectCount;
    /** DELIVERY : la quete est payee et terminee des la remise, sans rapporter de preuve au tableau. */
    private boolean payOnDelivery;
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

    public String getGiveItem() {
        return giveItem;
    }

    /** L'identifiant de la ville du tableau qui propose cette quete. */
    public String getTown() {
        return town != null ? town : com.lucas.guild.data.GameContent.world().startPlace;
    }

    public String getCollectItem() {
        return collectItem;
    }

    public int getCollectCount() {
        return collectCount;
    }

    public boolean isPayOnDelivery() {
        return payOnDelivery;
    }

    /** Le texte de ce qu'il faut rapporter ou faire, pour l'affichage. */
    public String getObjectiveText() {
        if (type == QuestType.COLLECT && collectItem != null) {
            Item item = com.lucas.guild.data.GameContent.newItem(collectItem);
            return "Rapporter " + collectCount + " x " + (item != null ? item.getName() : collectItem);
        }
        if (type == QuestType.DELIVERY && payOnDelivery) {
            return "Livrer à : " + target + " (payé à la remise)";
        }
        return "Preuve à rapporter : " + proofItem;
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
               "  Récompense: " + goldReward + " Po\n" +
               "  Preuve requise: " + proofItem;
    }
}
