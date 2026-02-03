package model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Guild {

    private List<Quest> availableQuests = new ArrayList<>();

    public Guild() {
        initializeQuests();
    }

    private void initializeQuests() {
        // Quêtes de Rang F (0)
        availableQuests.add(new Quest("Nettoyage de la cave", "Des rats ont envahi la cave du tavernier.", 0, 20, 5, "Queue de rat"));
        availableQuests.add(new Quest("Livraison express", "Livrer une lettre au forgeron.", 0, 10, 2, "Lettre signée"));

        // Quêtes de Rang E (1)
        availableQuests.add(new Quest("Chasse aux gobelins", "Un groupe de gobelins attaque les voyageurs.", 1, 100, 15, "Oreille de gobelin"));
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
