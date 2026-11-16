package com.lucas.guild.game;

import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Item;
import com.lucas.guild.model.ObjetDeQuete;
import com.lucas.guild.model.Quest;
import com.lucas.guild.model.QuestType;

/**
 * Le suivi des quêtes d'un personnage : accepter à la guilde, remettre une livraison, rendre la
 * quête contre l'or promis. Sans dépendance à l'interface.
 *
 * <p>Le principe : la <b>preuve</b> ({@code proofItem}) est un objet que le joueur doit rapporter.
 * Pour un combat, ce sera un butin ; pour une livraison, c'est le reçu qu'on obtient en remettant
 * l'objet à livrer ({@code giveItem}) à son destinataire. La guilde échange la preuve contre l'or.
 * Les méthodes renvoient {@code null} quand c'est fait, sinon la phrase à afficher.</p>
 */
public class QuestLog {

    public enum Status { AVAILABLE, ACTIVE, DONE }

    /** Combien de quetes terminees pour monter d'un rang (rang = quetes terminees / ce nombre). */
    public static final int QUESTS_PER_RANK = 3;

    private final Adventurer player;

    public QuestLog(Adventurer player) {
        this.player = player;
    }

    public Status status(Quest quest) {
        if (player.getDoneQuests().contains(quest.getTitle())) {
            return Status.DONE;
        }
        if (player.getActiveQuests().contains(quest.getTitle())) {
            return Status.ACTIVE;
        }
        return Status.AVAILABLE;
    }

    /** Accepte la quête (donne l'objet à livrer s'il y en a un). */
    public String accept(Quest quest) {
        Status status = status(quest);
        if (status == Status.DONE) {
            return "Vous avez déjà accompli cette quête.";
        }
        if (status == Status.ACTIVE) {
            return "Vous avez déjà accepté cette quête.";
        }
        if (quest.getRank() > player.getRank()) {
            return "Votre rang est trop faible pour cette quête.";
        }
        if (quest.getGiveItem() != null && !quest.getGiveItem().isEmpty()) {
            if (!player.addItem(new ObjetDeQuete(quest.getGiveItem()))) {
                return "Vous portez trop : libérez de la place pour " + quest.getGiveItem() + ".";
            }
        }
        player.getActiveQuests().add(quest.getTitle());
        return null;
    }

    /** Le joueur porte-t-il la preuve de cette quête ? */
    public boolean hasProof(Quest quest) {
        if (quest.getType() == QuestType.COLLECT) {
            return countCollected(quest) >= quest.getCollectCount();
        }
        return findQuestItem(quest.getProofItem()) != null;
    }

    private int countCollected(Quest quest) {
        int n = 0;
        for (Item item : player.getInventoryItems()) {
            if (quest.getCollectItem().equals(item.getId())) {
                n++;
            }
        }
        return n;
    }

    /**
     * Rend la quête : la preuve est reprise et l'or versé.
     *
     * @return null si c'est fait, sinon pourquoi pas (quête non acceptée, preuve manquante)
     */
    public String turnIn(Quest quest) {
        if (status(quest) != Status.ACTIVE) {
            return status(quest) == Status.DONE ? "Vous avez déjà rendu cette quête." : "Vous n'avez pas accepté cette quête.";
        }
        if (quest.getType() == QuestType.COLLECT) {
            if (!hasProof(quest)) {
                return "Il vous manque des objets : " + quest.getObjectiveText() + " (vous en avez " + countCollected(quest) + ").";
            }
            int toTake = quest.getCollectCount();
            for (Item item : new java.util.ArrayList<>(player.getInventoryItems())) {
                if (toTake > 0 && quest.getCollectItem().equals(item.getId())) {
                    player.removeItem(item);
                    toTake--;
                }
            }
        } else {
            Item proof = findQuestItem(quest.getProofItem());
            if (proof == null) {
                return "Il vous manque la preuve : " + quest.getProofItem() + ".";
            }
            player.removeItem(proof);
        }
        finish(quest);
        return null;
    }

    /** La quete est terminee : or verse, quete rangee, rang remonte si le personnage en a fait assez. */
    private void finish(Quest quest) {
        player.getActiveQuests().remove(quest.getTitle());
        player.getDoneQuests().add(quest.getTitle());
        player.addGold(quest.getGoldReward());
        player.setRank(Math.max(player.getRank(), player.getDoneQuests().size() / QUESTS_PER_RANK));
    }

    // ------------------------------------------------------------------ livraisons

    /** Y a-t-il une livraison en cours à remettre à {@code recipient} (le « target » de la quête) ? */
    public boolean canDeliver(String recipient, java.util.List<Quest> allQuests) {
        return pendingDelivery(recipient, allQuests) != null;
    }

    /**
     * Remet l'objet à livrer à son destinataire et reçoit la preuve en échange.
     *
     * @return la phrase à afficher (succès ou raison de l'échec)
     */
    public String deliver(String recipient, java.util.List<Quest> allQuests) {
        Quest quest = pendingDelivery(recipient, allQuests);
        if (quest == null) {
            return "Vous n'avez rien à me remettre.";
        }
        Item parcel = findQuestItem(quest.getGiveItem());
        if (quest.isPayOnDelivery()) {
            player.removeItem(parcel);
            finish(quest);
            return quest.getGiveItem() + " remis : +" + quest.getGoldReward() + " Po !";
        }
        // la preuve prend la place du colis, même si le personnage est en surcharge : sinon elle serait perdue
        player.getInventaire().remplacerItem(parcel, new ObjetDeQuete(quest.getProofItem()));
        return quest.getGiveItem() + " remise. Rapportez à la guilde : " + quest.getProofItem() + ".";
    }

    private Quest pendingDelivery(String recipient, java.util.List<Quest> allQuests) {
        for (Quest quest : allQuests) {
            if (quest.getType() == QuestType.DELIVERY
                    && recipient.equals(quest.getTarget())
                    && status(quest) == Status.ACTIVE
                    && quest.getGiveItem() != null
                    && findQuestItem(quest.getGiveItem()) != null) {
                return quest;
            }
        }
        return null;
    }

    private Item findQuestItem(String name) {
        if (name == null) {
            return null;
        }
        for (Item item : player.getInventoryItems()) {
            if (item instanceof ObjetDeQuete && name.equals(item.getName())) {
                return item;
            }
        }
        return null;
    }
}
