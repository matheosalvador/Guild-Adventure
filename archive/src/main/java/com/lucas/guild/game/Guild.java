package com.lucas.guild.game;

import com.lucas.guild.model.Quest;
import com.lucas.guild.model.QuestType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
// a iplementer
public class Guild {

    private List<Quest> allQuests = new ArrayList<>();

    public Guild() {
        initializeQuests();
    }

    private void initializeQuests() {
        // --- RANG F (0) ---
        allQuests.add(new Quest("Rats des caves", "Tuer le Rat Géant dans la cave de la taverne.", 0, QuestType.COMBAT, "Rat Géant", 20, 50, "Queue de rat"));
        allQuests.add(new Quest("Colis pour le forgeron", "Livrer un colis d'outils au forgeron.", 0, QuestType.DELIVERY, "Forgeron", 10, 20, "Reçu du forgeron"));

        // --- RANG E (1) ---
        allQuests.add(new Quest("Menace gobeline", "Éliminer le chef des gobelins qui terrorise la route.", 1, QuestType.COMBAT, "Chef Gobelin", 100, 150, "Tête de Gobelin"));
        allQuests.add(new Quest("Herbes médicinales", "Trouver et livrer des herbes rares à l'apothicaire.", 1, QuestType.DELIVERY, "Apothicaire", 80, 100, "Sachet d'herbes signé"));
        
        // --- RANG D (2) ---
        allQuests.add(new Quest("Le loup solitaire", "Traquer et abattre un loup féroce près du village.", 2, QuestType.COMBAT, "Loup Alpha", 250, 300, "Peau de Loup Alpha"));
    }

    /**
     * Affiche les quêtes non complétées et adaptées au rang de l'aventurier.
     */
    public void displayAvailableQuests(int adventurerRank) {
        System.out.println("\n--- TABLEAU DES QUÊTES ---");
        List<Quest> questsToList = getQuestsForRank(adventurerRank);

        if (questsToList.isEmpty()) {
            System.out.println("Aucune nouvelle quête disponible pour votre rang. Revenez plus tard !");
            return;
        }

        for (int i = 0; i < questsToList.size(); i++) {
            Quest q = questsToList.get(i);
            System.out.println((i + 1) + ". [" + q.getType() + " - Rang " + q.getRank() + "] " + q.getTitle());
        }
    }

    public List<Quest> getQuestsForRank(int rank) {
        return allQuests.stream()
                .filter(q -> !q.isCompleted() && q.getRank() <= rank)
                .collect(Collectors.toList());
    }
}
