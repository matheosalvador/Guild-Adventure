package com.lucas.guild.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * L'histoire telle qu'écrite dans {@code story.json} : les rangs, les chapitres et leurs objectifs, les dialogues
 * d'histoire, les objets à ramasser, les déclencheurs, les fins et ce qu'elles changent. Ne contient que des données ;
 * les règles sont dans {@code game/Story}.
 *
 * <p>Tout repose sur des <b>drapeaux</b> (des noms, sauvegardés avec le personnage) et des <b>conditions</b> : une liste
 * de conditions est vraie quand toutes le sont. Une condition s'écrit {@code type:argument}, précédée de {@code !}
 * pour dire le contraire. Voir {@link #CONDITION_TYPES} et {@code assets/data/README.txt}.</p>
 */
public class StoryData {

    /** Les types de condition reconnus. */
    public static final List<String> CONDITION_TYPES =
            Arrays.asList("flag", "quest", "lore", "rank", "quests", "item", "since", "within");

    /** Les rangs, du plus bas (au-dessus de F) au plus haut. Le rang F (0) n'a pas d'entrée : c'est le départ. */
    public List<RankData> ranks = new ArrayList<>();
    public List<ChapterData> chapters = new ArrayList<>();
    /** Nom des endroits où l'on peut parler d'histoire qui ne sont pas des PNJ (cairn, sablier...). */
    public Map<String, String> points = new LinkedHashMap<>();
    public List<DialogData> dialogs = new ArrayList<>();
    public List<PickupData> pickups = new ArrayList<>();
    public List<TriggerData> triggers = new ArrayList<>();
    /** Des drapeaux posés tout seuls dès que des conditions sont vraies (« le journal contient tous les indices »). */
    public List<RuleData> rules = new ArrayList<>();
    public List<EndingData> endings = new ArrayList<>();
    public List<BoonData> boons = new ArrayList<>();
    public List<EffectData> effects = new ArrayList<>();

    /** Un rang : atteint quand ses conditions sont vraies (et le rang précédent atteint). */
    public static class RankData {
        public int rank;
        /** La lettre affichée (« E », « D »...). */
        public String label;
        public List<String> requires = new ArrayList<>();
    }

    /** Un chapitre : il commence quand {@code requires} est vrai, et se termine quand tous ses objectifs sont faits. */
    public static class ChapterData {
        public String id;
        public String title;
        /** Texte montré au joueur quand le chapitre commence, et quand il se termine. */
        public String intro;
        public String outro;
        public List<String> requires = new ArrayList<>();
        public List<ObjectiveData> objectives = new ArrayList<>();
        public int goldReward;
        /** Drapeaux posés à la fin du chapitre, en plus de {@code done:<id>}. */
        public List<String> sets = new ArrayList<>();
    }

    /** Un objectif : une ligne du journal, cochée quand ses conditions sont vraies. */
    public static class ObjectiveData {
        public String text;
        public List<String> done = new ArrayList<>();
    }

    /**
     * Un dialogue d'histoire, proposé à un endroit ({@code at} : « guildmaster@aethelgard », ou un point de
     * {@link #points}) quand {@code requires} est vrai. Sans {@code options}, ses effets s'appliquent à la lecture ;
     * avec, le joueur choisit et ce sont les effets de l'option qui s'appliquent.
     */
    public static class DialogData {
        public String id;
        public String at;
        public List<String> requires = new ArrayList<>();
        /** Le texte du bouton qui ouvre le dialogue. */
        public String label;
        public String text;
        public List<OptionData> options = new ArrayList<>();
        public List<String> sets = new ArrayList<>();
        public List<String> unsets = new ArrayList<>();
        /** Objets donnés au joueur (items.json) ; refusé si le joueur ne peut pas les porter. */
        public List<String> gives = new ArrayList<>();
        /** Objets repris au joueur. */
        public List<String> takes = new ArrayList<>();
    }

    /** Un choix dans un dialogue. */
    public static class OptionData {
        public String label;
        /** Ce que le joueur lit après avoir choisi. */
        public String text;
        /** L'option n'est proposée que si ces conditions sont vraies. */
        public List<String> requires = new ArrayList<>();
        public List<String> sets = new ArrayList<>();
        public List<String> unsets = new ArrayList<>();
        public List<String> gives = new ArrayList<>();
        public List<String> takes = new ArrayList<>();
        /** Le dialogue qui s'ouvre ensuite ({@code id}), ou absent. */
        public String next;
    }

    /** Un objet d'histoire posé dans une scène : le ramasser (E) pose un drapeau. */
    public static class PickupData {
        /** La scène : « ZoneScreen@ruines_gobelines », « CellarScreen@aethelgard »... */
        public String scene;
        public float x;
        public float z;
        public String name;
        public String flag;
        public List<String> requires = new ArrayList<>();
        /** La phrase affichée quand on le ramasse. */
        public String message;
    }

    /**
     * Un déclencheur : entrer dans le cercle pose des drapeaux, affiche une phrase courte ({@code message}) ou ouvre
     * un texte de scène ({@code title} et {@code text}), une seule fois.
     */
    public static class TriggerData {
        public String id;
        public String scene;
        public float x;
        public float z;
        public float radius = 4f;
        public List<String> requires = new ArrayList<>();
        public List<String> sets = new ArrayList<>();
        public String message;
        public String title;
        public String text;
    }

    /** Une règle : dès que {@code requires} est vrai, les drapeaux {@code sets} sont posés. */
    public static class RuleData {
        public List<String> requires = new ArrayList<>();
        public List<String> sets = new ArrayList<>();
    }

    /** Une fin : son épilogue est montré quand {@code requires} devient vrai. */
    public static class EndingData {
        public String id;
        public String title;
        public String text;
        public List<String> requires = new ArrayList<>();
        public int goldReward;
    }

    /** Un avantage de combat valable dans certains lieux quand ses conditions sont vraies (les sceaux, au château). */
    public static class BoonData {
        public List<String> requires = new ArrayList<>();
        /** Les lieux (world.json) où il s'applique. */
        public List<String> places = new ArrayList<>();
        public int damagePercent;
        public int healthBonus;
    }

    /** Ce qu'une fin change au monde, tant que ses conditions sont vraies. */
    public static class EffectData {
        public List<String> requires = new ArrayList<>();
        /** Chance d'embuscade, en % de la chance normale (50 = deux fois moins). */
        public Integer ambushPercent;
        /** Nouveau {@code priceFactor} de certaines boutiques (shops.json). */
        public Map<String, Integer> priceFactor = new LinkedHashMap<>();
        /** Le personnage ne vieillit plus. */
        public boolean stopAging;
        /** La guilde ne lui confie plus de quêtes. */
        public boolean guildClosed;
        /** Un titre affiché dans le journal. */
        public String title;
    }

    public ChapterData chapter(String id) {
        for (ChapterData c : chapters) {
            if (c.id.equals(id)) {
                return c;
            }
        }
        return null;
    }

    public DialogData dialog(String id) {
        for (DialogData d : dialogs) {
            if (d.id.equals(id)) {
                return d;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ vérification

    private static <T> List<T> orEmpty(List<T> list) {
        return list != null ? list : new ArrayList<T>();
    }

    /** Vérifie la forme d'une liste de conditions (les références à d'autres fichiers sont vérifiées par GameContent). */
    static void validateConditions(List<String> conditions, String where) {
        for (String c : conditions) {
            if (c == null || c.isEmpty()) throw new IllegalStateException(where + " : condition vide");
            String body = c.startsWith("!") ? c.substring(1) : c;
            int colon = body.indexOf(':');
            String type = colon > 0 ? body.substring(0, colon) : "";
            String arg = colon > 0 ? body.substring(colon + 1) : "";
            if (!CONDITION_TYPES.contains(type) || arg.isEmpty()) {
                throw new IllegalStateException(where + " : condition inconnue \"" + c + "\" (attendu : type:argument, avec un type parmi " + CONDITION_TYPES + ")");
            }
            if ("rank".equals(type) || "quests".equals(type)) {
                if (!arg.matches("\\d+")) throw new IllegalStateException(where + " : \"" + c + "\" attend un nombre");
            }
            if ("since".equals(type) || "within".equals(type)) {
                if (!arg.matches(".+:\\d+")) throw new IllegalStateException(where + " : \"" + c + "\" attend drapeau:minutes");
            }
        }
    }

    void validate(String file) {
        ranks = orEmpty(ranks);
        chapters = orEmpty(chapters);
        dialogs = orEmpty(dialogs);
        pickups = orEmpty(pickups);
        triggers = orEmpty(triggers);
        endings = orEmpty(endings);
        boons = orEmpty(boons);
        effects = orEmpty(effects);
        if (points == null) points = new LinkedHashMap<>();

        int expected = 1;
        for (RankData r : ranks) {
            String where = file + " : rang n°" + expected;
            if (r == null) throw new IllegalStateException(where + " : vide");
            if (r.rank != expected) throw new IllegalStateException(where + " : \"rank\" doit valoir " + expected + " (les rangs se suivent a partir de 1)");
            if (r.label == null || r.label.isEmpty()) throw new IllegalStateException(where + " : \"label\" manquant");
            r.requires = orEmpty(r.requires);
            validateConditions(r.requires, where + " : \"requires\"");
            expected++;
        }

        List<String> seen = new ArrayList<>();
        for (int i = 0; i < chapters.size(); i++) {
            ChapterData c = chapters.get(i);
            String where = file + " : chapitre n°" + (i + 1) + (c != null && c.id != null ? " (" + c.id + ")" : "");
            if (c == null || c.id == null || c.id.isEmpty()) throw new IllegalStateException(where + " : \"id\" manquant");
            if (seen.contains(c.id)) throw new IllegalStateException(where + " : \"id\" en double");
            seen.add(c.id);
            if (c.title == null || c.title.isEmpty()) throw new IllegalStateException(where + " : \"title\" manquant");
            if (c.intro == null || c.intro.isEmpty()) throw new IllegalStateException(where + " : \"intro\" manquant");
            if (c.outro == null || c.outro.isEmpty()) throw new IllegalStateException(where + " : \"outro\" manquant");
            if (c.goldReward < 0) throw new IllegalStateException(where + " : \"goldReward\" doit etre >= 0");
            c.requires = orEmpty(c.requires);
            c.sets = orEmpty(c.sets);
            validateConditions(c.requires, where + " : \"requires\"");
            if (c.objectives == null || c.objectives.isEmpty()) throw new IllegalStateException(where + " : aucun objectif (\"objectives\")");
            for (int j = 0; j < c.objectives.size(); j++) {
                ObjectiveData o = c.objectives.get(j);
                String at = where + " : objectif n°" + (j + 1);
                if (o == null || o.text == null || o.text.isEmpty()) throw new IllegalStateException(at + " : \"text\" manquant");
                if (o.done == null || o.done.isEmpty()) throw new IllegalStateException(at + " : \"done\" manquant (les conditions qui le cochent)");
                validateConditions(o.done, at + " : \"done\"");
            }
        }

        for (Map.Entry<String, String> p : points.entrySet()) {
            if (p.getValue() == null || p.getValue().isEmpty()) {
                throw new IllegalStateException(file + " : point \"" + p.getKey() + "\" : nom manquant");
            }
        }

        List<String> dialogIds = new ArrayList<>();
        for (int i = 0; i < dialogs.size(); i++) {
            DialogData d = dialogs.get(i);
            String where = file + " : dialogue n°" + (i + 1) + (d != null && d.id != null ? " (" + d.id + ")" : "");
            if (d == null || d.id == null || d.id.isEmpty()) throw new IllegalStateException(where + " : \"id\" manquant");
            if (dialogIds.contains(d.id)) throw new IllegalStateException(where + " : \"id\" en double");
            dialogIds.add(d.id);
            if (d.at == null || d.at.isEmpty()) throw new IllegalStateException(where + " : \"at\" manquant (ou se tient le dialogue)");
            if (d.label == null || d.label.isEmpty()) throw new IllegalStateException(where + " : \"label\" manquant");
            if (d.text == null || d.text.isEmpty()) throw new IllegalStateException(where + " : \"text\" manquant");
            d.requires = orEmpty(d.requires);
            d.options = orEmpty(d.options);
            d.sets = orEmpty(d.sets);
            d.unsets = orEmpty(d.unsets);
            d.gives = orEmpty(d.gives);
            d.takes = orEmpty(d.takes);
            validateConditions(d.requires, where + " : \"requires\"");
            for (int j = 0; j < d.options.size(); j++) {
                OptionData o = d.options.get(j);
                String at = where + " : option n°" + (j + 1);
                if (o == null || o.label == null || o.label.isEmpty()) throw new IllegalStateException(at + " : \"label\" manquant");
                o.requires = orEmpty(o.requires);
                o.sets = orEmpty(o.sets);
                o.unsets = orEmpty(o.unsets);
                o.gives = orEmpty(o.gives);
                o.takes = orEmpty(o.takes);
                validateConditions(o.requires, at + " : \"requires\"");
            }
        }
        for (DialogData d : dialogs) {
            for (OptionData o : d.options) {
                if (o.next != null && !dialogIds.contains(o.next)) {
                    throw new IllegalStateException(file + " : dialogue \"" + d.id + "\" : \"next\" inconnu \"" + o.next + "\"");
                }
            }
        }

        for (int i = 0; i < pickups.size(); i++) {
            PickupData p = pickups.get(i);
            String where = file + " : objet a ramasser n°" + (i + 1);
            if (p == null) throw new IllegalStateException(where + " : vide");
            if (p.scene == null || p.scene.isEmpty()) throw new IllegalStateException(where + " : \"scene\" manquant");
            if (p.name == null || p.name.isEmpty()) throw new IllegalStateException(where + " : \"name\" manquant");
            if (p.flag == null || p.flag.isEmpty()) throw new IllegalStateException(where + " : \"flag\" manquant");
            p.requires = orEmpty(p.requires);
            validateConditions(p.requires, where + " : \"requires\"");
        }

        List<String> triggerIds = new ArrayList<>();
        for (int i = 0; i < triggers.size(); i++) {
            TriggerData t = triggers.get(i);
            String where = file + " : declencheur n°" + (i + 1) + (t != null && t.id != null ? " (" + t.id + ")" : "");
            if (t == null || t.id == null || t.id.isEmpty()) throw new IllegalStateException(where + " : \"id\" manquant");
            if (triggerIds.contains(t.id)) throw new IllegalStateException(where + " : \"id\" en double");
            triggerIds.add(t.id);
            if (t.scene == null || t.scene.isEmpty()) throw new IllegalStateException(where + " : \"scene\" manquant");
            if (t.radius <= 0) throw new IllegalStateException(where + " : \"radius\" doit etre > 0");
            t.requires = orEmpty(t.requires);
            t.sets = orEmpty(t.sets);
            validateConditions(t.requires, where + " : \"requires\"");
            if (t.sets.isEmpty() && (t.message == null || t.message.isEmpty()) && (t.text == null || t.text.isEmpty())) {
                throw new IllegalStateException(where + " : ne fait rien (\"sets\", \"message\" ou \"text\" attendu)");
            }
        }

        rules = orEmpty(rules);
        for (int i = 0; i < rules.size(); i++) {
            RuleData r = rules.get(i);
            String where = file + " : regle n°" + (i + 1);
            if (r == null) throw new IllegalStateException(where + " : vide");
            r.requires = orEmpty(r.requires);
            r.sets = orEmpty(r.sets);
            if (r.requires.isEmpty()) throw new IllegalStateException(where + " : \"requires\" manquant");
            if (r.sets.isEmpty()) throw new IllegalStateException(where + " : \"sets\" manquant");
            validateConditions(r.requires, where + " : \"requires\"");
        }

        List<String> endingIds = new ArrayList<>();
        for (int i = 0; i < endings.size(); i++) {
            EndingData e = endings.get(i);
            String where = file + " : fin n°" + (i + 1) + (e != null && e.id != null ? " (" + e.id + ")" : "");
            if (e == null || e.id == null || e.id.isEmpty()) throw new IllegalStateException(where + " : \"id\" manquant");
            if (endingIds.contains(e.id)) throw new IllegalStateException(where + " : \"id\" en double");
            endingIds.add(e.id);
            if (e.title == null || e.title.isEmpty()) throw new IllegalStateException(where + " : \"title\" manquant");
            if (e.text == null || e.text.isEmpty()) throw new IllegalStateException(where + " : \"text\" manquant");
            if (e.goldReward < 0) throw new IllegalStateException(where + " : \"goldReward\" doit etre >= 0");
            e.requires = orEmpty(e.requires);
            if (e.requires.isEmpty()) throw new IllegalStateException(where + " : \"requires\" manquant (ce qui declenche la fin)");
            validateConditions(e.requires, where + " : \"requires\"");
        }

        for (int i = 0; i < boons.size(); i++) {
            BoonData b = boons.get(i);
            String where = file + " : avantage n°" + (i + 1);
            if (b == null) throw new IllegalStateException(where + " : vide");
            b.requires = orEmpty(b.requires);
            b.places = orEmpty(b.places);
            validateConditions(b.requires, where + " : \"requires\"");
            if (b.places.isEmpty()) throw new IllegalStateException(where + " : \"places\" manquant (les lieux ou il s'applique)");
            if (b.damagePercent == 0 && b.healthBonus == 0) throw new IllegalStateException(where + " : aucun effet (\"damagePercent\" ou \"healthBonus\" attendu)");
        }

        for (int i = 0; i < effects.size(); i++) {
            EffectData e = effects.get(i);
            String where = file + " : effet n°" + (i + 1);
            if (e == null) throw new IllegalStateException(where + " : vide");
            e.requires = orEmpty(e.requires);
            if (e.priceFactor == null) e.priceFactor = new LinkedHashMap<>();
            if (e.requires.isEmpty()) throw new IllegalStateException(where + " : \"requires\" manquant");
            validateConditions(e.requires, where + " : \"requires\"");
            if (e.ambushPercent != null && (e.ambushPercent < 0 || e.ambushPercent > 300)) {
                throw new IllegalStateException(where + " : \"ambushPercent\" doit etre entre 0 et 300");
            }
            for (Map.Entry<String, Integer> p : e.priceFactor.entrySet()) {
                if (p.getValue() == null || p.getValue() < 10 || p.getValue() > 500) {
                    throw new IllegalStateException(where + " : \"priceFactor\" de \"" + p.getKey() + "\" doit etre entre 10 et 500 (en %)");
                }
            }
        }
    }
}
