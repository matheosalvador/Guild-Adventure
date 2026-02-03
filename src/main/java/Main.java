import model.Adventurer;
import model.Guild;
import model.Quest;

import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        // --- INITIALISATION DU JEU ---
        Scanner scanner = new Scanner(System.in);
        Adventurer player = new Adventurer("Lucas", "Caserne");
        Guild guild = new Guild();
        System.out.println("Bienvenue dans la guilde, jeune aventurier !");
        System.out.println(player);

        // --- BOUCLE DE JEU PRINCIPALE (simplifiée) ---
        guild.displayAvailableQuests(player.getRank());

        System.out.print("Choisissez une quête (entrez le numéro) : ");
        int choice = scanner.nextInt();

        List<Quest> availableQuests = guild.getQuestsForRank(player.getRank());
        if (choice > 0 && choice <= availableQuests.size()) {
            Quest selectedQuest = availableQuests.get(choice - 1);
            player.acceptQuest(selectedQuest);
        } else {
            System.out.println("Choix invalide.");
            return;
        }

        // --- SIMULATION DE L'AVENTURE ---
        System.out.println("\n... Vous partez à l'aventure ...\n");
        // On simule la réussite de la quête en trouvant l'objet requis
        player.findItem("Queue de rat"); // Changez ceci pour tester différentes quêtes

        System.out.println("\n... De retour à la guilde ...\n");
        System.out.println(player);
        player.completeQuest();
        System.out.println(player);

        scanner.close();
    }
}
