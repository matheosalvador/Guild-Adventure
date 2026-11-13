package com.lucas.guild.data;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import com.lucas.guild.model.AgingStage;
import com.lucas.guild.model.Item;
import com.lucas.guild.model.Quest;

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

    /** Pour les tests : lire les fichiers dans ce dossier (et lui seul) au lieu du classpath. */
    private static volatile Path overrideDir;

    /** Pour les tests : {@code null} remet la lecture normale. Vide aussi le cache des fichiers lus. */
    static void overrideDataDirForTests(Path dir) {
        overrideDir = dir;
        TEXT_CACHE.clear();
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

    /** Toutes les compétences du jeu, dans l'ordre du fichier (nouvelles instances à chaque appel). */
    public static List<SkillData> skills() {
        List<SkillData> skills = read("skills.json", new TypeToken<List<SkillData>>() { }.getType());
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < skills.size(); i++) {
            SkillData s = skills.get(i);
            String where = "skills.json : competence n°" + (i + 1);
            if (s == null) throw new IllegalStateException(where + " : vide");
            if (s.id == null || s.id.isEmpty()) throw new IllegalStateException(where + " : \"id\" manquant");
            where += " (" + s.id + ")";
            if (!seen.add(s.id)) throw new IllegalStateException(where + " : \"id\" en double");
            if (s.label == null || s.label.isEmpty()) throw new IllegalStateException(where + " : \"label\" manquant");
            if (s.description == null) s.description = "";
            if (s.trainedBy == null) s.trainedBy = "";
            if (s.unit == null) s.unit = "";
            if (s.thresholds == null || s.thresholds.isEmpty()) throw new IllegalStateException(where + " : \"thresholds\" doit contenir au moins un seuil");
            double previous = 0;
            for (Double threshold : s.thresholds) {
                if (threshold == null || threshold <= previous) {
                    throw new IllegalStateException(where + " : \"thresholds\" doit etre une liste croissante de nombres > 0");
                }
                previous = threshold;
            }
            if (s.effects == null || s.effects.isEmpty()) throw new IllegalStateException(where + " : \"effects\" manquant");
            for (Map.Entry<String, Double> effect : s.effects.entrySet()) {
                if (!SkillData.EFFECTS.containsKey(effect.getKey())) {
                    throw new IllegalStateException(where + " : effet inconnu \"" + effect.getKey() + "\" (connus : " + SkillData.EFFECTS.keySet() + ")");
                }
                if (effect.getValue() == null || effect.getValue() == 0) {
                    throw new IllegalStateException(where + " : l'effet \"" + effect.getKey() + "\" doit avoir une valeur non nulle");
                }
            }
        }
        return skills;
    }

    /** Les classes de personnage proposées à la création (classes.json). */
    public static List<ProfileData> characterClasses() {
        return profiles("classes.json");
    }

    /** Les origines de personnage proposées à la création (backgrounds.json). */
    public static List<ProfileData> backgrounds() {
        return profiles("backgrounds.json");
    }

    /** La classe {@code id}, ou {@code null} si elle n'existe pas. */
    public static ProfileData characterClass(String id) {
        return findProfile(characterClasses(), id);
    }

    /** L'origine {@code id}, ou {@code null} si elle n'existe pas. */
    public static ProfileData background(String id) {
        return findProfile(backgrounds(), id);
    }

    private static ProfileData findProfile(List<ProfileData> all, String id) {
        if (id != null) {
            for (ProfileData p : all) {
                if (id.equals(p.id)) {
                    return p;
                }
            }
        }
        return null;
    }

    private static List<ProfileData> profiles(String file) {
        List<ProfileData> all = read(file, new TypeToken<List<ProfileData>>() { }.getType());
        if (all.isEmpty()) {
            throw new IllegalStateException(file + " : au moins une entree est necessaire");
        }
        Set<String> itemIds = itemIds();
        Map<String, SkillData> skills = new java.util.HashMap<>();
        for (SkillData s : skills()) {
            skills.put(s.id, s);
        }
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < all.size(); i++) {
            ProfileData p = all.get(i);
            String where = file + " : entree n°" + (i + 1);
            if (p == null) throw new IllegalStateException(where + " : vide");
            if (p.id == null || p.id.isEmpty()) throw new IllegalStateException(where + " : \"id\" manquant");
            where += " (" + p.id + ")";
            if (!seen.add(p.id)) throw new IllegalStateException(where + " : \"id\" en double");
            if (p.label == null || p.label.isEmpty()) throw new IllegalStateException(where + " : \"label\" manquant");
            if (p.description == null) p.description = "";
            if (p.startingItems == null) p.startingItems = new ArrayList<>();
            if (p.skillStart == null) p.skillStart = new java.util.LinkedHashMap<>();
            if (p.practiceBonusPercent == null) p.practiceBonusPercent = new java.util.LinkedHashMap<>();
            for (String itemId : p.startingItems) {
                if (!itemIds.contains(itemId)) throw new IllegalStateException(where + " : objet de depart inconnu \"" + itemId + "\" (voir items.json)");
            }
            for (Map.Entry<String, Integer> start : p.skillStart.entrySet()) {
                SkillData skill = skills.get(start.getKey());
                if (skill == null) throw new IllegalStateException(where + " : competence inconnue \"" + start.getKey() + "\" dans \"skillStart\" (voir skills.json)");
                if (start.getValue() == null || start.getValue() < 1 || start.getValue() > skill.maxLevel()) {
                    throw new IllegalStateException(where + " : \"skillStart\" de \"" + start.getKey() + "\" doit etre un niveau entre 1 et " + skill.maxLevel());
                }
            }
            for (Map.Entry<String, Integer> bonus : p.practiceBonusPercent.entrySet()) {
                if (!skills.containsKey(bonus.getKey())) {
                    throw new IllegalStateException(where + " : competence inconnue \"" + bonus.getKey() + "\" dans \"practiceBonusPercent\" (voir skills.json)");
                }
                if (bonus.getValue() == null || bonus.getValue() <= -100) {
                    throw new IllegalStateException(where + " : \"practiceBonusPercent\" de \"" + bonus.getKey() + "\" doit etre > -100");
                }
            }
        }
        return all;
    }

    /** Poids maximum de l'inventaire (sans sac) pour une difficulté, ou {@code null} si la difficulté est inconnue. */
    public static Double maxWeight(String difficulty) {
        DifficultyData data = difficulty(difficulty);
        return data != null ? Double.valueOf(data.maxWeight) : null;
    }

    /** Les réglages d'une difficulté (poids, encombrement, or et objets de départ), ou {@code null} si inconnue. */
    public static DifficultyData difficulty(String difficulty) {
        Map<String, DifficultyData> all = difficulties();
        return difficulty != null ? all.get(difficulty) : null;
    }

    private static Map<String, DifficultyData> difficulties() {
        Map<String, DifficultyData> all = read("difficulties.json", new TypeToken<Map<String, DifficultyData>>() { }.getType());
        Set<String> itemIds = null;
        for (Map.Entry<String, DifficultyData> e : all.entrySet()) {
            DifficultyData d = e.getValue();
            String where = "difficulties.json : \"" + e.getKey() + "\"";
            if (d == null) throw new IllegalStateException(where + " : vide");
            if (d.maxWeight <= 0) throw new IllegalStateException(where + " : \"maxWeight\" doit etre > 0");
            if (d.encumbranceStart <= 0 || d.encumbranceStart > 1) throw new IllegalStateException(where + " : \"encumbranceStart\" doit etre entre 0 (exclu) et 1");
            if (d.minSpeedPercent < 1 || d.minSpeedPercent > 100) throw new IllegalStateException(where + " : \"minSpeedPercent\" doit etre entre 1 et 100");
            if (d.startingGold < 0) throw new IllegalStateException(where + " : \"startingGold\" doit etre >= 0");
            if (d.startingItems == null) d.startingItems = new ArrayList<>();
            for (String id : d.startingItems) {
                if (itemIds == null) itemIds = itemIds();
                if (!itemIds.contains(id)) throw new IllegalStateException(where + " : objet de depart inconnu \"" + id + "\" (voir items.json)");
            }
        }
        return all;
    }

    /** Tous les objets du catalogue (nouvelles instances à chaque appel). */
    public static List<Item> items() {
        List<Item> items = read("items.json", new TypeToken<List<Item>>() { }.getType());
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            String where = "items.json : objet n°" + (i + 1);
            if (item == null) throw new IllegalStateException(where + " : vide");
            if (item.getId() == null || item.getId().isEmpty()) throw new IllegalStateException(where + " : \"id\" manquant");
            where += " (" + item.getId() + ")";
            if (!seen.add(item.getId())) throw new IllegalStateException(where + " : \"id\" en double");
            if (item.getName() == null || item.getName().isEmpty()) throw new IllegalStateException(where + " : \"name\" manquant");
            if (item.getPoids() <= 0) throw new IllegalStateException(where + " : \"poids\" doit etre > 0");
            if (item.getPrix() < 0) throw new IllegalStateException(where + " : \"prix\" doit etre >= 0");
        }
        return items;
    }

    private static Set<String> itemIds() {
        Set<String> ids = new HashSet<>();
        for (Item item : items()) {
            ids.add(item.getId());
        }
        return ids;
    }

    /** Une nouvelle instance de l'objet {@code id} du catalogue, ou {@code null} s'il n'existe pas. */
    public static Item newItem(String id) {
        for (Item item : items()) {
            if (item.getId().equals(id)) {
                return item;
            }
        }
        return null;
    }

    /** La boutique {@code shopId} (« forge », « guild »...). @throws IllegalStateException si elle n'existe pas ou est invalide. */
    public static ShopData shop(String shopId) {
        Map<String, ShopData> shops = shops();
        ShopData shop = shops.get(shopId);
        if (shop == null) {
            throw new IllegalStateException("shops.json : boutique inconnue \"" + shopId + "\"");
        }
        return shop;
    }

    private static Map<String, ShopData> shops() {
        Map<String, ShopData> shops = read("shops.json", new TypeToken<Map<String, ShopData>>() { }.getType());
        Set<String> itemIds = itemIds();
        for (Map.Entry<String, ShopData> e : shops.entrySet()) {
            ShopData shop = e.getValue();
            String where = "shops.json : boutique \"" + e.getKey() + "\"";
            if (shop == null) throw new IllegalStateException(where + " : vide");
            if (shop.title == null || shop.title.isEmpty()) throw new IllegalStateException(where + " : \"title\" manquant");
            if (shop.sellPercent < 0 || shop.sellPercent > 100) throw new IllegalStateException(where + " : \"sellPercent\" doit etre entre 0 et 100");
            if (shop.stock == null) shop.stock = new ArrayList<>();
            for (String id : shop.stock) {
                if (!itemIds.contains(id)) throw new IllegalStateException(where + " : objet inconnu \"" + id + "\" (voir items.json)");
            }
        }
        return shops;
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
        skills();
        characterClasses();
        backgrounds();
        maxWeight("normale");
        shop("forge");
        shop("guild");
        town();
        calendar();
        agingStages();
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
        if (overrideDir != null) {
            Path path = overrideDir.resolve(file);
            if (!Files.isRegularFile(path)) {
                throw new IllegalStateException("Fichier de donnees introuvable : " + file + " (cherche dans " + path.toAbsolutePath() + ")");
            }
            try {
                return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            } catch (IOException e) {
                throw new IllegalStateException(file + " : lecture impossible (" + e.getMessage() + ")", e);
            }
        }
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
