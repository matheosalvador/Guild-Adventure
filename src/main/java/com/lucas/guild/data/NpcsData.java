package com.lucas.guild.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Ce que disent les personnages, tel qu'écrit dans {@code npcs.json} : les PNJ des bâtiments (leur accueil et les
 * choix qu'ils proposent) et les habitants qui se promènent en ville (leurs répliques). Ne contient que des données ;
 * les règles sont dans {@code game/NpcTalk}. Voir {@code assets/data/README.txt}.
 */
public class NpcsData {

    /**
     * Ce que fait un choix. {@code quests} : le tableau des quêtes ; {@code shop} : une boutique de shops.json ;
     * {@code deliver} : remettre une livraison ; {@code serve} : payer pour un service (repas, boisson) ;
     * {@code rumor} : payer une rumeur ; {@code news} : les nouvelles des autres villes ; {@code talk} : une réplique.
     */
    public static final List<String> ACTIONS = Arrays.asList("quests", "shop", "deliver", "serve", "rumor", "news", "talk");

    public List<NpcData> npcs = new ArrayList<>();
    public List<VillagerTalkData> villagers = new ArrayList<>();

    /** Un PNJ de bâtiment : son identifiant est celui de l'écran (« innkeeper ») et des dialogues de story.json. */
    public static class NpcData {
        public String id;
        /** Le texte quand on le vise, sans « (E) » : « Parler à l'aubergiste ». */
        public String prompt;
        /** La première ligne du dialogue. {@code {lieu}} est remplacé par le nom du bâtiment. */
        public String greeting;
        /** Le bouton qui ferme le dialogue. */
        public String leave = "Partir";
        public List<OptionData> options = new ArrayList<>();
    }

    /** Un choix proposé par un PNJ. Les champs utiles dépendent de {@code action}. */
    public static class OptionData {
        public String action;
        /** Le texte du bouton ; le prix s'y ajoute tout seul (« Manger un repas (5 Po) »). */
        public String label;
        /** Conditions d'histoire (comme dans story.json) : le choix n'apparaît que si elles sont vraies. */
        public List<String> requires = new ArrayList<>();
        /** quests : la réponse quand la Guilde ne confie plus rien (une fin de l'histoire). */
        public String closedText;
        /** shop : la boutique (shops.json) ; absente chez le commerçant, c'est celle du bâtiment (town.json). */
        public String shop;
        /** deliver : le destinataire des quêtes de livraison ; absent, c'est le nom du bâtiment. */
        public String target;
        /** serve et rumor : le prix en Po. */
        public int price;
        /** serve : les PV rendus, et l'entraînement de la Vigueur. */
        public int heal;
        public int vigor;
        /** serve : le message une fois servi ; talk : la réplique. */
        public String text;
        /** serve et rumor : le message quand l'or manque ; {@code {prix}} est remplacé par le prix. */
        public String noGoldText = "Pas assez d'or : il faut {prix} Po.";
        /** rumor et news : la phrase avant la rumeur ou les nouvelles, et la réponse quand il n'y a rien. */
        public String intro;
        public String emptyText;
        /** rumor, news et talk : le bouton qui revient au dialogue. */
        public String back = "Merci";
    }

    /** Ce que dit un habitant : son nom au-dessus du dialogue, et des répliques dont une est choisie à chaque fois. */
    public static class VillagerTalkData {
        public String id;
        public String name;
        public List<LineData> lines = new ArrayList<>();
    }

    /** Une réplique ; {@code town} la réserve à une ville, {@code requires} à un moment de l'histoire. */
    public static class LineData {
        public String text;
        public String town;
        public List<String> requires = new ArrayList<>();
    }

    /** Le PNJ {@code id}, ou null. */
    public NpcData npc(String id) {
        for (NpcData n : npcs) {
            if (n.id.equals(id)) {
                return n;
            }
        }
        return null;
    }

    /** Les répliques de l'habitant {@code id}, ou null. */
    public VillagerTalkData villager(String id) {
        for (VillagerTalkData v : villagers) {
            if (v.id.equals(id)) {
                return v;
            }
        }
        return null;
    }

    /** Vérifie la forme du fichier (ce qui renvoie à d'autres fichiers est vérifié par {@link GameContent#npcs()}). */
    void validate(String file, List<String> requiredNpcs) {
        if (npcs == null) npcs = new ArrayList<>();
        if (villagers == null) villagers = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < npcs.size(); i++) {
            NpcData n = npcs.get(i);
            String where = file + " : PNJ n°" + (i + 1);
            if (n == null || n.id == null || n.id.isEmpty()) throw new IllegalStateException(where + " : \"id\" manquant");
            where = file + " : PNJ \"" + n.id + "\"";
            if (!requiredNpcs.contains(n.id)) throw new IllegalStateException(where + " : \"id\" inconnu (attendu : " + requiredNpcs + ")");
            if (!seen.add(n.id)) throw new IllegalStateException(where + " : en double");
            if (n.prompt == null || n.prompt.isEmpty()) throw new IllegalStateException(where + " : \"prompt\" manquant");
            if (n.greeting == null || n.greeting.isEmpty()) throw new IllegalStateException(where + " : \"greeting\" manquant");
            if (n.leave == null || n.leave.isEmpty()) throw new IllegalStateException(where + " : \"leave\" vide");
            if (n.options == null) n.options = new ArrayList<>();
            for (int j = 0; j < n.options.size(); j++) {
                validateOption(n.options.get(j), n.id, where + " : choix n°" + (j + 1));
            }
        }
        for (String id : requiredNpcs) {
            if (!seen.contains(id)) throw new IllegalStateException(file + " : PNJ manquant \"" + id + "\"");
        }
        Set<String> seenVillagers = new HashSet<>();
        for (int i = 0; i < villagers.size(); i++) {
            VillagerTalkData v = villagers.get(i);
            String where = file + " : habitant n°" + (i + 1);
            if (v == null || v.id == null || v.id.isEmpty()) throw new IllegalStateException(where + " : \"id\" manquant");
            where = file + " : habitant \"" + v.id + "\"";
            if (!seenVillagers.add(v.id)) throw new IllegalStateException(where + " : en double");
            if (v.name == null || v.name.isEmpty()) throw new IllegalStateException(where + " : \"name\" manquant");
            if (v.lines == null || v.lines.isEmpty()) throw new IllegalStateException(where + " : \"lines\" vide (au moins une replique)");
            for (int j = 0; j < v.lines.size(); j++) {
                LineData line = v.lines.get(j);
                if (line == null || line.text == null || line.text.isEmpty()) {
                    throw new IllegalStateException(where + " : replique n°" + (j + 1) + " : \"text\" manquant");
                }
                if (line.requires == null) line.requires = new ArrayList<>();
            }
        }
    }

    private static void validateOption(OptionData o, String npc, String where) {
        if (o == null || o.action == null || o.action.isEmpty()) throw new IllegalStateException(where + " : \"action\" manquante");
        if (!ACTIONS.contains(o.action)) throw new IllegalStateException(where + " : \"action\" inconnue \"" + o.action + "\" (attendu : " + ACTIONS + ")");
        where = where + " (" + o.action + ")";
        if (o.label == null || o.label.isEmpty()) throw new IllegalStateException(where + " : \"label\" manquant");
        if (o.requires == null) o.requires = new ArrayList<>();
        if (o.price < 0) throw new IllegalStateException(where + " : \"price\" doit etre >= 0");
        if (o.heal < 0 || o.vigor < 0) throw new IllegalStateException(where + " : \"heal\" et \"vigor\" doivent etre >= 0");
        if (o.noGoldText == null || o.noGoldText.isEmpty()) throw new IllegalStateException(where + " : \"noGoldText\" vide");
        if (o.back == null || o.back.isEmpty()) throw new IllegalStateException(where + " : \"back\" vide");
        switch (o.action) {
            case "shop":
                if (o.shop == null && !"shopkeeper".equals(npc)) {
                    throw new IllegalStateException(where + " : \"shop\" manquant (seul le commerçant prend la boutique de son batiment)");
                }
                break;
            case "serve":
            case "talk":
                if (o.text == null || o.text.isEmpty()) throw new IllegalStateException(where + " : \"text\" manquant");
                break;
            case "rumor":
            case "news":
                if (o.intro == null || o.intro.isEmpty()) throw new IllegalStateException(where + " : \"intro\" manquant");
                if (o.emptyText == null || o.emptyText.isEmpty()) throw new IllegalStateException(where + " : \"emptyText\" manquant");
                break;
            default:
                break;
        }
    }
}
