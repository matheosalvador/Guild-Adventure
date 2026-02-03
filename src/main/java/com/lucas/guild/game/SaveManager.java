package com.lucas.guild.game;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.GameData; // On va créer cette classe juste après

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class SaveManager {

    private static final String SAVE_FILE = "save.json";
    private Gson gson;

    public SaveManager() {
        // On configure Gson pour qu'il formate joliment le JSON
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    public void saveGame(Adventurer player, GameClock clock) {
        GameData data = new GameData(player, clock);
        try (FileWriter writer = new FileWriter(SAVE_FILE)) {
            gson.toJson(data, writer);
            System.out.println("Partie sauvegardée !");
        } catch (IOException e) {
            System.out.println("Erreur lors de la sauvegarde : " + e.getMessage());
        }
    }

    public GameData loadGame() {
        try (FileReader reader = new FileReader(SAVE_FILE)) {
            GameData data = gson.fromJson(reader, GameData.class);
            System.out.println("Sauvegarde chargée !");
            return data;
        } catch (IOException e) {
            System.out.println("Aucune sauvegarde trouvée ou erreur de lecture.");
            return null;
        }
    }
}
