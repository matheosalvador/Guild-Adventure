package service;

import com.google.gson.Gson;
import model.GameState;

import java.io.*;

public class SaveManager {

    private static final String SAVE_FILE = "save.json";
    private static final Gson gson = new Gson();

    public static void save(GameState gameState) {
        try (Writer writer = new FileWriter(SAVE_FILE)) {
            gson.toJson(gameState, writer);
            System.out.println(" Partie sauvegardée !");
        } catch (IOException e) {
            System.err.println(" Erreur lors de la sauvegarde");
        }
    }

    public static GameState load() {
        File file = new File(SAVE_FILE);
        if (!file.exists()) {
            System.out.println("ℹ Aucune sauvegarde trouvée");
            return null;
        }

        try (Reader reader = new FileReader(file)) {
            return gson.fromJson(reader, GameState.class);
        } catch (IOException e) {
            System.err.println(" Erreur lors du chargement");
            return null;
        }
    }
}
