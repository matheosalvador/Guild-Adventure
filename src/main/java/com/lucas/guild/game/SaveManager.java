package com.lucas.guild.game;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Arme;
import com.lucas.guild.model.Armure;
import com.lucas.guild.model.Attaque;
import com.lucas.guild.model.AttaqueDistance;
import com.lucas.guild.model.AttaqueMelee;
import com.lucas.guild.model.AttaqueSimple;
import com.lucas.guild.model.GameClock;
import com.lucas.guild.model.GameData;
import com.lucas.guild.model.Item;
import com.lucas.guild.model.ObjetDeQuete;
import com.lucas.guild.model.PotionDeSoin;
import com.lucas.guild.model.PotionDps;
import com.lucas.guild.model.PotionMix;
import com.lucas.guild.model.PotionSoin;

public class SaveManager {

    private static final String SAVE_FILE = "save.json";
    private final Gson gson;

    public SaveManager() {
        RuntimeTypeAdapterFactory<Item> itemAdapter = RuntimeTypeAdapterFactory.of(Item.class, "itemType")
                .registerSubtype(Arme.class)
                .registerSubtype(Armure.class)
                .registerSubtype(PotionSoin.class)
                .registerSubtype(PotionDps.class)
                .registerSubtype(PotionMix.class)
                .registerSubtype(PotionDeSoin.class)
                .registerSubtype(ObjetDeQuete.class);

        RuntimeTypeAdapterFactory<Attaque> attaqueAdapter = RuntimeTypeAdapterFactory.of(Attaque.class, "attaqueType")
                .registerSubtype(AttaqueSimple.class)
                .registerSubtype(AttaqueMelee.class)
                .registerSubtype(AttaqueDistance.class);

        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapterFactory(itemAdapter)
                .registerTypeAdapterFactory(attaqueAdapter)
                .create();
    }

    public void saveGame(Adventurer player, GameClock clock) {
        GameData data = new GameData(player, clock);
        try (FileWriter writer = new FileWriter(SAVE_FILE)) {
            gson.toJson(data, writer);
            System.out.println("Partie sauvegardee !");
        } catch (IOException e) {
            System.out.println("Erreur lors de la sauvegarde : " + e.getMessage());
        }
    }

    public void saveGame(Adventurer player, GameClock clock, String difficulty,
                         float playerX, float playerY, float playerZ, String currentScreen) {
        GameData data = new GameData(player, clock, difficulty, playerX, playerY, playerZ, currentScreen);
        try (FileWriter writer = new FileWriter(SAVE_FILE)) {
            gson.toJson(data, writer);
            System.out.println("Partie sauvegardee !");
        } catch (IOException e) {
            System.out.println("Erreur lors de la sauvegarde : " + e.getMessage());
        }
    }

    public GameData loadGame() {
        try (FileReader reader = new FileReader(SAVE_FILE)) {
            GameData data = gson.fromJson(reader, GameData.class);
            if (data != null && data.playerData != null) {
                System.out.println("Sauvegarde chargee !");
            }
            return data;
        } catch (IOException e) {
            System.out.println("Aucune sauvegarde trouvee ou erreur de lecture.");
            return null;
        }
    }

    public boolean hasSave() {
        java.io.File file = new java.io.File(SAVE_FILE);
        return file.exists() && file.length() > 0;
    }

    public boolean deleteSave() {
        java.io.File file = new java.io.File(SAVE_FILE);
        return file.delete();
    }
}
