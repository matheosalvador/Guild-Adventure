package com.lucas.guild.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.lucas.guild.data.GameContent;

public class Guild {

    private List<Quest> availableQuests = new ArrayList<>();

    public Guild() {
        initializeQuests();
    }

    /** Les quêtes viennent de assets/data/quests.json (rang 0 = F, 1 = E...). */
    private void initializeQuests() {
        availableQuests.addAll(GameContent.quests());
    }

    /**
     * Affiche les quêtes disponibles pour un certain rang.
     * @param adventurerRank Le rang de l'aventurier.
     */
    public void displayAvailableQuests(int adventurerRank) {
        System.out.println("\n--- TABLEAU DES QUÊTES DE LA GUILDE ---");
        List<Quest> questsForRank = availableQuests.stream()
                .filter(q -> q.getRank() <= adventurerRank)
                .collect(Collectors.toList());

        if (questsForRank.isEmpty()) {
            System.out.println("Aucune quête disponible pour votre rang.");
            return;
        }

        for (int i = 0; i < questsForRank.size(); i++) {
            System.out.println((i + 1) + ". " + questsForRank.get(i).getTitle());
        }
    }

    public List<Quest> getQuestsForRank(int rank) {
        return availableQuests.stream()
                .filter(q -> q.getRank() <= rank)
                .collect(Collectors.toList());
    }
}
