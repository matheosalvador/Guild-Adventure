package service;

import com.google.gson.Gson;
import model.GameState;

import java.io.*;
import java.nio.file.Paths;

/**
 * Gère la sauvegarde et le chargement de l'état du jeu.
 */
public class SaveManager {

    // Utilise le répertoire courant de l'exécution pour le fichier de sauvegarde
    private static final String SAVE_FILE = Paths.get("save.json").toAbsolutePath().toString();
    private static final Gson gson = new Gson();

    /**
     * Sauvegarde l'état du jeu dans un fichier JSON.
     * @param gameState L'état du jeu à sauvegarder.
     */
    public static void save(GameState gameState) {
        try (Writer writer = new FileWriter(SAVE_FILE)) {
            gson.toJson(gameState, writer);
            // System.out.println(" Partie sauvegardée dans : " + SAVE_FILE); // Debug
        } catch (IOException e) {
            System.err.println(" Erreur lors de la sauvegarde : " + e.getMessage());
        }
    }

    /**
     * Charge l'état du jeu depuis le fichier JSON.
     * @return L'état du jeu chargé, ou null si aucune sauvegarde n'existe ou en cas d'erreur.
     */
    public static GameState load() {
        File file = new File(SAVE_FILE);
        if (!file.exists()) {
            System.out.println("ℹ Aucune sauvegarde trouvée.");
            return null;
        }

        try (Reader reader = new FileReader(file)) {
            return gson.fromJson(reader, GameState.class);
        } catch (IOException e) {
            System.err.println(" Erreur lors du chargement : " + e.getMessage());
            return null;
        }
    }
}
