import controlleur.Gamecontrolleur;

/**
 * Classe principale de l'application.
 * Point d'entrée du programme.
 */
public class Main {
    /**
     * Méthode principale qui lance le jeu.
     * @param args Arguments de la ligne de commande (non utilisés).
     */
    public static void main(String[] args) {
        // Crée le contrôleur et démarre le jeu
        Gamecontrolleur game = new Gamecontrolleur();
        game.demarrerJeu();
    }
}
