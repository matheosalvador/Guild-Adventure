import controlleur.Gamecontrolleur;

public class Main {
    public static void main(String[] args) {
        // Création du controlleur du jeu
        Gamecontrolleur gameController = new Gamecontrolleur();

        // Lancer le jeu
        gameController.demarrerJeu();
    }
}