import controlleur.Gamecontrolleur;
import model.*;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        GameState gameState = SaveManager.load();

        if (gameState != null && gameState.getJoueur() != null) {
            System.out.println("Une sauvegarde a été trouvée (Niveau " + gameState.getNiveauDuDonjon() + ").");
            System.out.println("1. Continuer la partie");
            System.out.println("2. Commencer une nouvelle partie");
            System.out.print("Choix : ");

            int choix = -1;
            try {
                choix = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                // Gère l'entrée invalide
            }

            if (choix == 2) {
                System.out.println("Création d'une nouvelle partie.");
                gameState = createNewGame();
            } else {
                System.out.println("Reprise de la partie.");
            }
        } else {
            System.out.println("Aucune sauvegarde valide trouvée. Création d'une nouvelle partie.");
            gameState = createNewGame();
        }

        // Lancement du jeu
        Gamecontrolleur gameController = new Gamecontrolleur(gameState);
        gameController.demarrerLeJeu();

        // Sauvegarde en fin de partie
        SaveManager.save(gameState);
    }

    private static GameState createNewGame() {
        Joueur joueur = new Joueur("Arthur");
        GameState newGameState = new GameState(joueur, 1);
        
        // Ajout des potions de départ
        Inventaire inventaire = newGameState.getJoueur().getInventaire();
        inventaire.ajouterItem(new PotionSoin());
        inventaire.ajouterItem(new PotionSoin());
        inventaire.ajouterItem(new PotionDps());
        inventaire.ajouterItem(new PotionDps());
        inventaire.ajouterItem(new PotionMix());
        inventaire.ajouterItem(new PotionMix());
        
        return newGameState;
    }
}