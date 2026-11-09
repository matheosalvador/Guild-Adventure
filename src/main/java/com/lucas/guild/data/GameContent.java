package com.lucas.guild.data;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.lucas.guild.model.AgingStage;
import com.lucas.guild.model.Quest;
import com.lucas.guild.model.Skill;

/**
 * Contenu du jeu lu depuis les fichiers JSON de {@code assets/data/} : quêtes, compétences,
 * difficultés, ville. Voir {@code assets/data/README.txt} pour le format.
 *
 * <ul>
 *   <li>Un fichier manquant ou invalide lève une {@link IllegalStateException} qui nomme le fichier
 *       et le problème : on préfère échouer clairement plutôt que jouer avec un contenu vide.</li>
 *   <li>Le texte de chaque fichier est lu une seule fois ; chaque appel renvoie de <b>nouveaux</b>
 *       objets, donc modifier une quête ou une compétence n'altère pas le contenu d'origine.</li>
 * </ul>
 *
 * Les fichiers sont cherchés dans le classpath ({@code data/...}, cas de Maven) puis dans
 * {@code assets/data/} du dossier courant (cas de run.bat ou de l'IDE).
 */
public final class GameContent {

    private static final String RESOURCE_DIR = "data/";
    private static final Path FILE_DIR = Paths.get("assets", "data");

    private static final Gson GSON = GameGson.create();
    private static final Map<String, String> TEXT_CACHE = new ConcurrentHashMap<>();

    private GameContent() {
    }

    // ------------------------------------------------------------------ contenu

    /** Toutes les quêtes du jeu (le filtrage par rang se fait dans {@code Guild}). */
    public static List<Quest> quests() {
        List<Quest> quests = read("quests.json", new TypeToken<List<Quest>>() { }.getType());
        for (int i = 0; i < quests.size(); i++) {
            Quest q = quests.get(i);
            if (q == null || q.getTitle() == null) {
                throw new IllegalStateException("quests.json : la quete n°" + (i + 1) + " n'a pas de \"title\"");
            }
        }
        return quests;
    }

    /** Les compétences de la classe {@code className}, ou une liste vide si la classe n'a pas d'arbre. */
    public static List<Skill> skillsFor(String className) {
        Map<String, List<Skill>> all = read("skills.json", new TypeToken<Map<String, List<Skill>>>() { }.getType());
        List<Skill> skills = all.get(className);
        return skills != null ? skills : new ArrayList<Skill>();
    }

    /** Poids maximum de l'inventaire pour une difficulté, ou {@code null} si la difficulté est inconnue. */
    public static Double maxWeight(String difficulty) {
        Map<String, DifficultyData> all = read("difficulties.json", new TypeToken<Map<String, DifficultyData>>() { }.getType());
        DifficultyData data = difficulty != null ? all.get(difficulty) : null;
        return data != null ? Double.valueOf(data.maxWeight) : null;
    }

    /** La ville (nom et bâtiments). */
    public static TownData town() {
        TownData town = read("town.json", TownData.class);
        town.validate("town.json");
        return town;
    }

    /** Le calendrier (saisons, lever/coucher du soleil, début de partie...). */
    public static CalendarData calendar() {
        CalendarData calendar = read("calendar.json", CalendarData.class);
        calendar.validate("calendar.json");
        return calendar;
    }

    /** Les paliers de vieillesse, triés du plus jeune au plus vieux. */
    public static List<AgingStage> agingStages() {
        List<AgingStage> stages = read("aging.json", new TypeToken<List<AgingStage>>() { }.getType());
        if (stages.isEmpty()) {
            throw new IllegalStateException("aging.json : aucun palier");
        }
        for (int i = 0; i < stages.size(); i++) {
            AgingStage s = stages.get(i);
            String where = "aging.json : palier n°" + (i + 1);
            if (s == null || s.label == null || s.label.isEmpty()) throw new IllegalStateException(where + " : \"label\" manquant");
            if (s.maxHealthPercent <= 0 || s.carryPercent <= 0 || s.speedPercent <= 0) {
                throw new IllegalStateException(where + " (" + s.label + ") : les pourcentages doivent etre > 0");
            }
            if (i > 0 && s.fromAge <= stages.get(i - 1).fromAge) {
                throw new IllegalStateException(where + " (" + s.label + ") : \"fromAge\" doit etre croissant");
            }
        }
        if (stages.get(0).fromAge != 0) {
            throw new IllegalStateException("aging.json : le premier palier doit commencer a l'age 0");
        }
        return stages;
    }

    /**
     * Lit et vérifie tous les fichiers de données. À appeler au lancement du jeu pour qu'une erreur
     * dans un fichier apparaisse tout de suite plutôt qu'au milieu d'une partie.
     *
     * @throws IllegalStateException si un fichier est manquant ou invalide
     */
    public static void validateAll() {
        quests();
        skillsFor("Caserne");
        maxWeight("normale");
        town();
        calendar();
        agingStages();
    }

    /** Une difficulté telle qu'écrite dans {@code difficulties.json}. */
    static class DifficultyData {
        String label;
        double maxWeight;
    }

    // ------------------------------------------------------------------ lecture

    private static <T> T read(String file, Type type) {
        String json = text(file);
        try {
            T value = GSON.fromJson(json, type);
            if (value == null) {
                throw new IllegalStateException(file + " : fichier vide");
            }
            return value;
        } catch (JsonParseException | NumberFormatException e) {
            // NumberFormatException : Gson ne l'enveloppe pas toujours (ex. "maxWeight": "lourd")
            throw new IllegalStateException(file + " : JSON invalide (" + e.getMessage() + ")", e);
        }
    }

    private static String text(String file) {
        String cached = TEXT_CACHE.get(file);
        if (cached != null) {
            return cached;
        }
        String text = load(file);
        TEXT_CACHE.put(file, text);
        return text;
    }

    private static String load(String file) {
        // 1) classpath (Maven : le dossier assets est une ressource)
        try (InputStream in = GameContent.class.getClassLoader().getResourceAsStream(RESOURCE_DIR + file)) {
            if (in != null) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new IllegalStateException(file + " : lecture impossible dans le classpath (" + e.getMessage() + ")", e);
        }

        // 2) dossier assets/data du dossier courant (run.bat, IDE)
        Path path = FILE_DIR.resolve(file);
        if (Files.isRegularFile(path)) {
            try {
                return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException(file + " : lecture impossible (" + e.getMessage() + ")", e);
            }
        }

        throw new IllegalStateException("Fichier de donnees introuvable : " + file
                + " (cherche dans le classpath sous " + RESOURCE_DIR + " puis dans " + path.toAbsolutePath() + ")");
    }
}
