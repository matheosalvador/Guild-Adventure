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
        storyCache = null;
        npcsCache = null;
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
            for (int j = 0; j < i; j++) {
                if (q.getTitle().equals(quests.get(j).getTitle())) {
                    throw new IllegalStateException("quests.json : la quete \"" + q.getTitle() + "\" est en double (les titres doivent etre uniques)");
                }
            }
            if (q.getGoldReward() < 0) {
                throw new IllegalStateException("quests.json : la quete \"" + q.getTitle() + "\" : \"goldReward\" doit etre >= 0");
            }
            String where = "quests.json : la quete \"" + q.getTitle() + "\"";
            boolean collect = q.getType() == com.lucas.guild.model.QuestType.COLLECT;
            if (!collect && !q.isPayOnDelivery() && (q.getProofItem() == null || q.getProofItem().isEmpty())) {
                throw new IllegalStateException(where + " : \"proofItem\" manquant");
            }
            if (collect) {
                if (q.getCollectItem() == null || newItem(q.getCollectItem()) == null) {
                    throw new IllegalStateException(where + " : \"collectItem\" doit etre un objet de items.json (" + q.getCollectItem() + ")");
                }
                if (q.getCollectCount() < 1) throw new IllegalStateException(where + " : \"collectCount\" doit etre >= 1");
            }
            if (q.isPayOnDelivery() && (q.getType() != com.lucas.guild.model.QuestType.DELIVERY || q.getGiveItem() == null || q.getGiveItem().isEmpty())) {
                throw new IllegalStateException(where + " : \"payOnDelivery\" demande une quete DELIVERY avec un \"giveItem\"");
            }
            WorldData.PlaceData board = world().place(q.getTown());
            if (board == null || board.zoneFile != null) {
                throw new IllegalStateException(where + " : \"town\" doit etre une ville de world.json (" + q.getTown() + ")");
            }
            if (q.getType() == com.lucas.guild.model.QuestType.DELIVERY && !deliveryRecipients().contains(q.getTarget())) {
                throw new IllegalStateException(where + " : destinataire inconnu \"" + q.getTarget() + "\" (attendu : " + deliveryRecipients() + ")");
            }
        }
        return quests;
    }

    /** Ceux a qui on peut remettre une livraison : le forgeron, et les auberges et boutiques des villes (par leur nom). */
    public static List<String> deliveryRecipients() {
        List<String> names = new ArrayList<>();
        names.add("Forgeron");
        for (WorldData.PlaceData place : world().places) {
            if (place.zoneFile == null) {
                for (TownData.BuildingData b : town(place.id).buildings) {
                    if ("inn".equals(b.interior) || "store".equals(b.interior)) {
                        names.add(b.name);
                    }
                }
            }
        }
        return names;
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

    /** Tous les types de monstres (identifiant -> données), nouvelles instances à chaque appel. */
    public static Map<String, MonsterData> monsters() {
        Map<String, MonsterData> all = read("monsters.json", new TypeToken<Map<String, MonsterData>>() { }.getType());
        for (Map.Entry<String, MonsterData> e : all.entrySet()) {
            MonsterData m = e.getValue();
            String where = "monsters.json : \"" + e.getKey() + "\"";
            if (m == null) throw new IllegalStateException(where + " : vide");
            m.id = e.getKey();
            if (m.name == null || m.name.isEmpty()) throw new IllegalStateException(where + " : \"name\" manquant");
            if (m.health <= 0) throw new IllegalStateException(where + " : \"health\" doit etre > 0");
            if (m.damage < 0) throw new IllegalStateException(where + " : \"damage\" doit etre >= 0");
            if (m.speed <= 0) throw new IllegalStateException(where + " : \"speed\" doit etre > 0");
            if (m.aggroRange <= 0 || m.attackRange <= 0) throw new IllegalStateException(where + " : \"aggroRange\" et \"attackRange\" doivent etre > 0");
            if (m.attackRange > m.aggroRange) throw new IllegalStateException(where + " : \"attackRange\" ne peut pas depasser \"aggroRange\"");
            if (m.attackCooldown <= 0) throw new IllegalStateException(where + " : \"attackCooldown\" doit etre > 0");
            if (m.goldMin < 0 || m.goldMax < m.goldMin) throw new IllegalStateException(where + " : \"goldMin\" >= 0 et \"goldMax\" >= \"goldMin\" attendus");
            if (m.color == null) throw new IllegalStateException(where + " : \"color\" manquant");
            if (m.w <= 0 || m.h <= 0 || m.d <= 0) throw new IllegalStateException(where + " : \"w\", \"h\" et \"d\" doivent etre > 0");
            if (m.model != null && !models().models.containsKey(m.model)) {
                throw new IllegalStateException(where + " : \"model\" inconnu \"" + m.model + "\" (voir models.json)");
            }
            if (m.flyHeight < 0) throw new IllegalStateException(where + " : \"flyHeight\" doit etre >= 0");
            if (m.yieldPercent < 0 || m.yieldPercent > 90) throw new IllegalStateException(where + " : \"yieldPercent\" doit etre entre 0 et 90");
            if (m.yieldPercent > 0 && (m.yieldFlag == null || m.yieldFlag.isEmpty())) {
                throw new IllegalStateException(where + " : \"yieldPercent\" demande un \"yieldFlag\" (le drapeau pose quand il se rend)");
            }
            if (m.phases == null) m.phases = new ArrayList<>();
            int previousBelow = 100;
            for (int i = 0; i < m.phases.size(); i++) {
                MonsterData.Phase phase = m.phases.get(i);
                String at = where + " : phase n°" + (i + 1);
                if (phase == null) throw new IllegalStateException(at + " : vide");
                if (phase.below < 1 || phase.below >= previousBelow) {
                    throw new IllegalStateException(at + " : \"below\" doit etre entre 1 et 99 et decroitre d'une phase a la suivante");
                }
                previousBelow = phase.below;
                if (phase.damage < 0 || phase.speed < 0 || phase.attackCooldown < 0) throw new IllegalStateException(at + " : \"damage\", \"speed\" et \"attackCooldown\" doivent etre >= 0");
                if ((phase.healEvery > 0) != (phase.healAmount > 0)) throw new IllegalStateException(at + " : \"healEvery\" et \"healAmount\" vont ensemble");
                if (phase.healRequires == null) phase.healRequires = new ArrayList<>();
                StoryData.validateConditions(phase.healRequires, at + " : \"healRequires\"");
                if (phase.summon != null && !all.containsKey(phase.summon)) {
                    throw new IllegalStateException(at + " : renfort inconnu \"" + phase.summon + "\" (voir monsters.json)");
                }
                if (phase.summon != null && (phase.summonCount < 1 || phase.summonCount > 6)) throw new IllegalStateException(at + " : \"summonCount\" doit etre entre 1 et 6");
            }
            if (m.drops == null) m.drops = new ArrayList<>();
            for (MonsterData.Drop drop : m.drops) {
                if (drop == null || drop.item == null || newItem(drop.item) == null) {
                    throw new IllegalStateException(where + " : objet lache inconnu \"" + (drop != null ? drop.item : null) + "\" (voir items.json)");
                }
                if (drop.chance < 1 || drop.chance > 100) throw new IllegalStateException(where + " : \"chance\" de \"" + drop.item + "\" doit etre entre 1 et 100");
            }
        }
        return all;
    }

    /** Le monstre {@code id}. @throws IllegalStateException s'il n'existe pas. */
    public static MonsterData monster(String id) {
        MonsterData m = monsters().get(id);
        if (m == null) {
            throw new IllegalStateException("monsters.json : monstre inconnu \"" + id + "\"");
        }
        return m;
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
            if (d.ambushPercent < 0 || d.ambushPercent > 100) throw new IllegalStateException(where + " : \"ambushPercent\" doit etre entre 0 et 100");
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
            if (shop.priceFactor < 10 || shop.priceFactor > 500) throw new IllegalStateException(where + " : \"priceFactor\" doit etre entre 10 et 500 (en %)");
            if (shop.stock == null) shop.stock = new ArrayList<>();
            for (String id : shop.stock) {
                if (!itemIds.contains(id)) throw new IllegalStateException(where + " : objet inconnu \"" + id + "\" (voir items.json)");
            }
        }
        return shops;
    }

    /** La ville de départ, Aethelgard (town.json). */
    public static TownData town() {
        return townFile("town.json");
    }

    /** La ville du lieu {@code placeId} (voir world.json). */
    public static TownData town(String placeId) {
        WorldData.PlaceData place = world().place(placeId);
        if (place == null) {
            throw new IllegalStateException("world.json : lieu inconnu \"" + placeId + "\"");
        }
        if (place.townFile == null) {
            throw new IllegalStateException("world.json : \"" + placeId + "\" est une zone sauvage, pas une ville");
        }
        return townFile(place.townFile);
    }

    /** La zone sauvage du lieu {@code placeId} (voir world.json). */
    public static ZoneData zone(String placeId) {
        WorldData.PlaceData place = world().place(placeId);
        if (place == null) {
            throw new IllegalStateException("world.json : lieu inconnu \"" + placeId + "\"");
        }
        if (place.zoneFile == null) {
            throw new IllegalStateException("world.json : \"" + placeId + "\" est une ville, pas une zone sauvage");
        }
        return zoneFile(place.zoneFile);
    }

    /** Le décor des embuscades sur la route (world.json, « ambush »), ou {@code null} s'il n'y en a pas. */
    public static ZoneData ambushZone() {
        WorldData.AmbushData ambush = world().ambush;
        return ambush != null ? zoneFile(ambush.zoneFile) : null;
    }

    /** Où on arrive dans le lieu {@code placeId}, ville ou zone : devant la porte ou au bord de la route. */
    public static TownData.Point3 arrival(String placeId) {
        WorldData.PlaceData place = world().place(placeId);
        return place != null && place.zoneFile != null ? zone(placeId).arrival : town(placeId).arrival;
    }

    private static ZoneData zoneFile(String file) {
        ZoneData zone = read(file, ZoneData.class);
        zone.validate(file);
        checkReadable(file, zone.decor);
        for (ZoneData.SpawnData m : zone.monsters) {
            if (m == null || m.id == null || monsters().get(m.id) == null) {
                throw new IllegalStateException(file + " : monstre inconnu \"" + (m != null ? m.id : null) + "\" (voir monsters.json)");
            }
        }
        for (ZoneData.ChestData c : zone.chests) {
            for (String id : c.items) {
                if (newItem(id) == null) throw new IllegalStateException(file + " : coffre : objet inconnu \"" + id + "\" (voir items.json)");
            }
        }
        for (ZoneData.DoorData d : zone.doors) {
            WorldData.PlaceData target = world().place(d.to);
            if (target == null || target.zoneFile == null) {
                throw new IllegalStateException(file + " : porte \"" + d.label + "\" : \"to\" doit etre une zone de world.json (" + d.to + ")");
            }
        }
        if (zone.arena != null) {
            for (List<ZoneData.SpawnData> wave : zone.arena.waves) {
                for (ZoneData.SpawnData m : wave) {
                    if (m == null || m.id == null || monsters().get(m.id) == null) {
                        throw new IllegalStateException(file + " : \"arena\" : monstre inconnu \"" + (m != null ? m.id : null) + "\" (voir monsters.json)");
                    }
                }
            }
        }
        return zone;
    }

    /** Chance, en %, d'une embuscade à chaque voyage entre villes pour cette difficulté (0 si inconnue). */
    public static int ambushPercent(String difficulty) {
        DifficultyData d = difficulty(difficulty);
        return d != null ? d.ambushPercent : 0;
    }

    private static TownData townFile(String file) {
        TownData town = read(file, TownData.class);
        town.validate(file);
        checkReadable(file, town.decor);
        for (TownData.VillagerData v : town.villagers) {
            if (!models().models.containsKey(v.model)) {
                throw new IllegalStateException(file + " : habitant : modele inconnu \"" + v.model + "\" (voir models.json)");
            }
            if (v.talk != null && !villagerTalkIds().contains(v.talk)) {
                throw new IllegalStateException(file + " : habitant : \"talk\" inconnu \"" + v.talk + "\" (voir \"villagers\" dans npcs.json)");
            }
        }
        for (TownData.BuildingData b : town.buildings) {
            checkReadable(file, b.parts);
        }
        for (TownData.BuildingData b : town.buildings) {
            if (b.shop != null) {
                try {
                    shop(b.shop);
                } catch (IllegalStateException e) {
                    throw new IllegalStateException(file + " : batiment \"" + b.name + "\" : " + e.getMessage(), e);
                }
            }
        }
        return town;
    }

    /** Le monde : lieux, positions et routes (world.json). */
    public static WorldData world() {
        WorldData world = read("world.json", WorldData.class);
        world.validate("world.json");
        return world;
    }

    /** Les rumeurs et les textes à lire (lore.json). */
    public static LoreData lore() {
        LoreData lore = read("lore.json", LoreData.class);
        lore.validate("lore.json");
        WorldData world = world();
        for (LoreData.RumorData r : lore.rumors) {
            if (r.town != null && (world.place(r.town) == null || world.place(r.town).zoneFile != null)) {
                throw new IllegalStateException("lore.json : rumeur \"" + r.id + "\" : \"town\" doit etre une ville de world.json (" + r.town + ")");
            }
        }
        return lore;
    }

    /** Les modèles 3D des personnages et de leur équipement (models.json). */
    public static ModelsData models() {
        ModelsData data = read("models.json", ModelsData.class);
        data.validate("models.json");
        for (String id : data.gear.keySet()) {
            // un equipement est celui d'un objet de items.json, ou un accessoire propre aux modeles (identifiant « prop_... »)
            if (!id.startsWith("prop_") && newItem(id) == null) {
                throw new IllegalStateException("models.json : equipement \"" + id + "\" : objet inconnu (voir items.json ; un accessoire de modele s'appelle prop_...)");
            }
        }
        for (String id : data.armorColors.keySet()) {
            if (newItem(id) == null) {
                throw new IllegalStateException("models.json : armorColors \"" + id + "\" : objet inconnu (voir items.json)");
            }
        }
        return data;
    }

    /** Les événements du monde (events.json). */
    public static EventsData events() {
        EventsData data = read("events.json", EventsData.class);
        data.validate("events.json");
        WorldData world = world();
        for (EventsData.EventData e : data.events) {
            String where = "events.json : evenement \"" + e.id + "\"";
            if (e.items != null) {
                for (String id : e.items) {
                    if (newItem(id) == null) throw new IllegalStateException(where + " : objet inconnu \"" + id + "\" (voir items.json)");
                }
            }
            if (e.towns != null) {
                for (String id : e.towns) {
                    if (world.place(id) == null || world.place(id).zoneFile != null) {
                        throw new IllegalStateException(where + " : \"towns\" doit contenir des villes de world.json (" + id + ")");
                    }
                }
            }
        }
        return data;
    }

    /** Les PNJ auprès de qui un dialogue d'histoire peut se tenir (« guildmaster@aethelgard »). */
    public static final List<String> STORY_NPCS = java.util.Arrays.asList("guildmaster", "innkeeper", "blacksmith", "shopkeeper");

    private static volatile StoryData storyCache;

    /**
     * L'histoire (story.json). Contrairement aux autres contenus, l'objet lu est gardé et partagé (l'histoire est
     * consultée à chaque image) : ne pas le modifier.
     */
    public static StoryData story() {
        StoryData cached = storyCache;
        if (cached != null) {
            return cached;
        }
        StoryData story = read("story.json", StoryData.class);
        story.validate("story.json");
        WorldData world = world();
        for (StoryData.RankData r : story.ranks) {
            checkConditions(r.requires, "story.json : rang " + r.label);
        }
        for (StoryData.ChapterData c : story.chapters) {
            String where = "story.json : chapitre \"" + c.id + "\"";
            checkConditions(c.requires, where);
            for (StoryData.ObjectiveData o : c.objectives) {
                checkConditions(o.done, where + " : objectif \"" + o.text + "\"");
            }
        }
        for (StoryData.DialogData d : story.dialogs) {
            String where = "story.json : dialogue \"" + d.id + "\"";
            checkStoryPoint(d.at, story, world, where);
            checkConditions(d.requires, where);
            checkItems(d.gives, where + " : \"gives\"");
            checkItems(d.takes, where + " : \"takes\"");
            for (StoryData.OptionData o : d.options) {
                checkConditions(o.requires, where + " : option \"" + o.label + "\"");
                checkItems(o.gives, where + " : option \"" + o.label + "\" : \"gives\"");
                checkItems(o.takes, where + " : option \"" + o.label + "\" : \"takes\"");
            }
        }
        for (StoryData.PickupData p : story.pickups) {
            checkConditions(p.requires, "story.json : objet a ramasser \"" + p.name + "\"");
        }
        for (StoryData.TriggerData t : story.triggers) {
            checkConditions(t.requires, "story.json : declencheur \"" + t.id + "\"");
        }
        for (StoryData.RuleData r : story.rules) {
            checkConditions(r.requires, "story.json : regle");
        }
        for (StoryData.EndingData e : story.endings) {
            checkConditions(e.requires, "story.json : fin \"" + e.id + "\"");
        }
        for (StoryData.BoonData b : story.boons) {
            checkConditions(b.requires, "story.json : avantage");
            for (String place : b.places) {
                if (world.place(place) == null) throw new IllegalStateException("story.json : avantage : lieu inconnu \"" + place + "\" (voir world.json)");
            }
        }
        for (StoryData.EffectData e : story.effects) {
            checkConditions(e.requires, "story.json : effet");
            for (String shopId : e.priceFactor.keySet()) {
                try {
                    shop(shopId);
                } catch (IllegalStateException ex) {
                    throw new IllegalStateException("story.json : effet : " + ex.getMessage(), ex);
                }
            }
        }
        storyCache = story;
        return story;
    }

    private static volatile NpcsData npcsCache;

    /**
     * Ce que disent les PNJ des bâtiments et les habitants (npcs.json). L'objet lu est gardé et partagé (les villes le
     * consultent à chaque chargement) : ne pas le modifier.
     */
    public static NpcsData npcs() {
        NpcsData cached = npcsCache;
        if (cached != null) {
            return cached;
        }
        String file = "npcs.json";
        NpcsData data = read(file, NpcsData.class);
        data.validate(file, STORY_NPCS);
        WorldData world = world();
        for (NpcsData.NpcData n : data.npcs) {
            for (NpcsData.OptionData o : n.options) {
                String where = file + " : PNJ \"" + n.id + "\" : choix \"" + o.label + "\"";
                checkConditions(o.requires, where);
                if (o.shop != null) {
                    try {
                        shop(o.shop);
                    } catch (IllegalStateException ex) {
                        throw new IllegalStateException(where + " : " + ex.getMessage(), ex);
                    }
                }
            }
        }
        for (NpcsData.VillagerTalkData v : data.villagers) {
            for (NpcsData.LineData line : v.lines) {
                String where = file + " : habitant \"" + v.id + "\"";
                checkConditions(line.requires, where);
                if (line.town != null && (world.place(line.town) == null || world.place(line.town).townFile == null)) {
                    throw new IllegalStateException(where + " : \"town\" doit etre une ville de world.json (" + line.town + ")");
                }
            }
        }
        npcsCache = data;
        return data;
    }

    /**
     * Les identifiants des habitants de npcs.json, lus sans vérifier le reste : npcs.json vérifie des conditions qui
     * renvoient aux quêtes, et les quêtes relisent les villes (sinon, une boucle sans fin).
     */
    private static Set<String> villagerTalkIds() {
        NpcsData data = read("npcs.json", NpcsData.class);
        Set<String> ids = new HashSet<>();
        if (data.villagers != null) {
            for (NpcsData.VillagerTalkData v : data.villagers) {
                if (v != null && v.id != null) {
                    ids.add(v.id);
                }
            }
        }
        return ids;
    }

    /**
     * Les noms des points d'histoire, lus sans vérifier le reste de story.json : les fichiers de lieux en ont besoin
     * pour leur propre vérification, et story.json vérifie à son tour des choses qui dépendent des lieux.
     */
    private static Set<String> storyPoints() {
        StoryData story = read("story.json", StoryData.class);
        return story.points != null ? story.points.keySet() : new HashSet<String>();
    }

    /** Un dialogue se tient auprès d'un PNJ d'une ville (« innkeeper@portbrume ») ou à un point nommé de l'histoire. */
    private static void checkStoryPoint(String at, StoryData story, WorldData world, String where) {
        if (story.points.containsKey(at)) {
            return;
        }
        int arobase = at.indexOf('@');
        String npc = arobase > 0 ? at.substring(0, arobase) : "";
        WorldData.PlaceData town = arobase > 0 ? world.place(at.substring(arobase + 1)) : null;
        if (!STORY_NPCS.contains(npc) || town == null || town.zoneFile != null) {
            throw new IllegalStateException(where + " : \"at\" inconnu \"" + at + "\" (attendu : un point de \"points\", ou "
                    + STORY_NPCS + "@une ville de world.json)");
        }
    }

    private static void checkItems(List<String> ids, String where) {
        for (String id : ids) {
            if (newItem(id) == null) throw new IllegalStateException(where + " : objet inconnu \"" + id + "\" (voir items.json)");
        }
    }

    /** Vérifie que les conditions d'histoire renvoient à des quêtes, des savoirs et des objets qui existent. */
    static void checkConditions(List<String> conditions, String where) {
        if (conditions == null) {
            return;
        }
        StoryData.validateConditions(conditions, where);
        for (String c : conditions) {
            String body = c.startsWith("!") ? c.substring(1) : c;
            String type = body.substring(0, body.indexOf(':'));
            String arg = body.substring(body.indexOf(':') + 1);
            if ("quest".equals(type)) {
                boolean found = false;
                for (Quest q : quests()) {
                    found |= q.getTitle().equals(arg);
                }
                if (!found) throw new IllegalStateException(where + " : quete inconnue \"" + arg + "\" (voir quests.json)");
            } else if ("lore".equals(type)) {
                LoreData lore = lore();
                boolean found = false;
                if (arg.startsWith("text:")) {
                    found = lore.texts.containsKey(arg.substring(5));
                } else if (arg.startsWith("rumor:")) {
                    for (LoreData.RumorData r : lore.rumors) {
                        found |= r.id.equals(arg.substring(6));
                    }
                }
                if (!found) throw new IllegalStateException(where + " : savoir inconnu \"" + arg + "\" (attendu : text:... ou rumor:... de lore.json)");
            } else if ("item".equals(type)) {
                if (newItem(arg) == null) throw new IllegalStateException(where + " : objet inconnu \"" + arg + "\" (voir items.json)");
            }
        }
    }

    /** Vérifie que chaque panneau à lire (action « read ») d'un fichier de lieu renvoie à un texte de lore.json. */
    private static void checkReadable(String file, List<TownData.PartData> parts) {
        if (parts == null) {
            return;
        }
        for (TownData.PartData p : parts) {
            if ("read".equals(p.action) && !lore().texts.containsKey(p.text)) {
                throw new IllegalStateException(file + " : texte a lire inconnu \"" + p.text + "\" (voir lore.json)");
            }
            if ("story".equals(p.action) && !storyPoints().contains(p.text)) {
                throw new IllegalStateException(file + " : point d'histoire inconnu \"" + p.text + "\" (voir \"points\" dans story.json)");
            }
        }
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
        monsters();
        maxWeight("normale");
        shop("forge");
        shop("guild");
        town();
        WorldData world = world();
        for (WorldData.PlaceData place : world.places) {
            if (place.zoneFile != null) {
                zone(place.id);
            } else {
                town(place.id);
            }
        }
        if (world.ambush != null) {
            ambushZone();
            for (String id : world.ambush.monsters) {
                if (monsters().get(id) == null) {
                    throw new IllegalStateException("world.json : \"ambush\" : monstre inconnu \"" + id + "\" (voir monsters.json)");
                }
            }
        }
        calendar();
        agingStages();
        lore();
        events();
        models();
        story();
        npcs();
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
