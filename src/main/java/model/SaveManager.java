package model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;

public class SaveManager {

    private static final String SAVE_FILE = "save.json";
    private static final String TRAITOR_FILE = "traitor.json";
    private static final String NEW_KING_FILE = "new_king.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    // Sauvegarde de la partie en cours
    public static void save(GameState gameState) {
        try (Writer writer = new FileWriter(SAVE_FILE)) {
            gson.toJson(gameState, writer);
        } catch (IOException e) {
            System.err.println("Erreur de sauvegarde : " + e.getMessage());
        }
    }

    // Chargement de la partie
    public static GameState load() {
        File file = new File(SAVE_FILE);
        if (!file.exists()) return null;
        try (Reader reader = new FileReader(file)) {
            return gson.fromJson(reader, GameState.class);
        } catch (IOException e) {
            return null;
        }
    }

    // Sauvegarde d'un groupe (joueur + alliés) dans un fichier spécifique
    public static void saveParty(PartyData partyData, String filename) {
        try (Writer writer = new FileWriter(filename)) {
            gson.toJson(partyData, writer);
        } catch (IOException e) {
            System.err.println("Erreur de sauvegarde du groupe : " + e.getMessage());
        }
    }

    // Chargement d'un groupe depuis un fichier
    public static PartyData loadParty(String filename) {
        File file = new File(filename);
        if (!file.exists()) return null;
        try (Reader reader = new FileReader(file)) {
            return gson.fromJson(reader, PartyData.class);
        } catch (IOException e) {
            return null;
        }
    }
    
    // Méthodes de convenance pour les fichiers spéciaux
    public static void saveTraitor(PartyData party) { saveParty(party, TRAITOR_FILE); }
    public static PartyData loadTraitor() { return loadParty(TRAITOR_FILE); }
    public static void saveNewKing(PartyData party) { saveParty(party, NEW_KING_FILE); }
    public static PartyData loadNewKing() { return loadParty(NEW_KING_FILE); }
}