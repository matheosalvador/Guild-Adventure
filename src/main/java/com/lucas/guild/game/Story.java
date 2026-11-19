package com.lucas.guild.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.MonsterData;
import com.lucas.guild.data.StoryData;
import com.lucas.guild.data.WorldData;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.GameClock;
import com.lucas.guild.model.Item;

/**
 * Les règles de l'histoire (story.json) pour un personnage : ses drapeaux, les conditions, l'avancement des
 * chapitres, les rangs, les dialogues, les objets à ramasser, les fins et ce qu'elles changent au monde.
 * Sans dépendance à l'interface : l'écran affiche, cette classe décide.
 *
 * <p>Tout l'état est dans le personnage ({@link Adventurer#getStoryFlags()}), donc sauvegardé. Les drapeaux que cette
 * classe pose elle-même : {@code started:<chapitre>}, {@code done:<chapitre>}, {@code ended:<fin>},
 * {@code shown:...} (texte déjà montré) et {@code trigger:<id>}.</p>
 */
public final class Story {

    /** Un texte d'histoire à montrer au joueur : l'ouverture ou la conclusion d'un chapitre, ou un épilogue. */
    public static final class Text {
        public final String title;
        public final String body;
        /** Vrai pour un épilogue : il a droit à son écran de fin. */
        public final boolean ending;
        private final String shownFlag;

        Text(String title, String body, boolean ending, String shownFlag) {
            this.title = title;
            this.body = body;
            this.ending = ending;
            this.shownFlag = shownFlag;
        }
    }

    private final Adventurer player;
    private final GameClock clock;
    private final StoryData data;

    /** @param clock l'horloge de la partie ; {@code null} quand on n'a besoin que de lire (les drapeaux posés le sont alors à la minute 0) */
    public Story(Adventurer player, GameClock clock) {
        this.player = player;
        this.clock = clock;
        this.data = GameContent.story();
    }

    public Story(GameSession session) {
        this(session.getPlayer(), session.getClock());
    }

    // ------------------------------------------------------------------ drapeaux et conditions

    /** La minute de jeu courante, comptée depuis le début du jour 1. */
    public int now() {
        if (clock == null) {
            return 0;
        }
        return (clock.getCurrentDay() - 1) * 24 * 60 + clock.getCurrentHour() * 60 + clock.getCurrentMinute();
    }

    public boolean has(String flag) {
        return player.getStoryFlags().containsKey(flag);
    }

    /** Pose un drapeau (sans effet s'il l'est déjà : on garde la date de la première fois). */
    public void set(String flag) {
        if (!has(flag)) {
            player.getStoryFlags().put(flag, now());
        }
    }

    public void unset(String flag) {
        player.getStoryFlags().remove(flag);
    }

    /** Toutes les conditions sont-elles vraies ? (une liste vide ou absente est vraie) */
    public boolean holds(List<String> conditions) {
        if (conditions == null) {
            return true;
        }
        for (String condition : conditions) {
            if (!holds(condition)) {
                return false;
            }
        }
        return true;
    }

    private boolean holds(String condition) {
        boolean negated = condition.startsWith("!");
        String body = negated ? condition.substring(1) : condition;
        int colon = body.indexOf(':');
        String type = body.substring(0, colon);
        String arg = body.substring(colon + 1);
        return check(type, arg) != negated;
    }

    private boolean check(String type, String arg) {
        switch (type) {
            case "flag":
                return has(arg);
            case "quest":
                return player.getDoneQuests().contains(arg);
            case "lore":
                return player.getKnownLore().contains(arg);
            case "rank":
                return player.getRank() >= Integer.parseInt(arg);
            case "quests":
                return player.getDoneQuests().size() >= Integer.parseInt(arg);
            case "item":
                return findItem(arg) != null;
            case "since":
            case "within": {
                int split = arg.lastIndexOf(':');
                Integer when = player.getStoryFlags().get(arg.substring(0, split));
                if (when == null || clock == null) {
                    return false;
                }
                int elapsed = now() - when;
                int minutes = Integer.parseInt(arg.substring(split + 1));
                return "since".equals(type) ? elapsed >= minutes : elapsed <= minutes;
            }
            default:
                return false;
        }
    }

    private Item findItem(String id) {
        for (Item item : player.getInventoryItems()) {
            if (id.equals(item.getId())) {
                return item;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ chapitres

    private boolean started(StoryData.ChapterData chapter) {
        return has("started:" + chapter.id);
    }

    public boolean isDone(String chapterId) {
        return has("done:" + chapterId);
    }

    /**
     * Fait avancer l'histoire : chapitres qui commencent, chapitres terminés (récompense), fins atteintes, rang.
     * À appeler régulièrement (chaque image) : sans changement, il ne se passe rien.
     *
     * @return les phrases à afficher au joueur
     */
    public List<String> advance() {
        List<String> messages = new ArrayList<>();
        boolean progress = true;
        while (progress) {
            progress = false;
            for (StoryData.RuleData rule : data.rules) {
                if (holds(rule.requires)) {
                    for (String flag : rule.sets) {
                        if (!has(flag)) {
                            set(flag);
                            progress = true;
                        }
                    }
                }
            }
            for (StoryData.ChapterData chapter : data.chapters) {
                if (!started(chapter) && holds(chapter.requires)) {
                    set("started:" + chapter.id);
                    progress = true;
                }
                if (started(chapter) && !isDone(chapter.id) && allDone(chapter)) {
                    set("done:" + chapter.id);
                    for (String flag : chapter.sets) {
                        set(flag);
                    }
                    player.addGold(chapter.goldReward);
                    messages.add("Chapitre terminé : " + chapter.title
                            + (chapter.goldReward > 0 ? " (+" + chapter.goldReward + " Po)." : "."));
                    progress = true;
                }
            }
        }
        for (StoryData.EndingData ending : data.endings) {
            if (!has("ended:" + ending.id) && holds(ending.requires)) {
                set("ended:" + ending.id);
                player.addGold(ending.goldReward);
            }
        }
        if (clock != null && stopsAging()) {
            player.freezeAge(clock.getCurrentDay());
        }
        String promoted = refreshRank();
        if (promoted != null) {
            messages.add(promoted);
        }
        return messages;
    }

    private boolean allDone(StoryData.ChapterData chapter) {
        for (StoryData.ObjectiveData objective : chapter.objectives) {
            if (!holds(objective.done)) {
                return false;
            }
        }
        return true;
    }

    /** Le chapitre en cours (commencé, pas terminé), ou null. */
    public StoryData.ChapterData currentChapter() {
        for (StoryData.ChapterData chapter : data.chapters) {
            if (started(chapter) && !isDone(chapter.id)) {
                return chapter;
            }
        }
        return null;
    }

    /** Les lignes du journal : le chapitre en cours et ses objectifs cochés ou non, puis le titre gagné s'il y en a un. */
    public List<String> journalLines() {
        List<String> lines = new ArrayList<>();
        StoryData.ChapterData chapter = currentChapter();
        if (chapter != null) {
            lines.add(chapter.title);
            for (StoryData.ObjectiveData objective : chapter.objectives) {
                lines.add((holds(objective.done) ? "[fait] " : "[ ] ") + objective.text);
            }
        } else if (!data.chapters.isEmpty() && isDone(data.chapters.get(data.chapters.size() - 1).id)) {
            lines.add("L'histoire est terminée. Le monde, lui, continue.");
        } else {
            // entre deux chapitres : le suivant attend un rang de la Guilde
            String hint = nextRankHint();
            lines.add(hint != null ? "La suite attend que la Guilde vous fasse confiance. " + hint : "Rien pour l'instant.");
        }
        String title = title();
        if (title != null) {
            lines.add("Titre : " + title);
        }
        return lines;
    }

    /** Le prochain texte d'histoire à montrer (dans l'ordre : ouverture, conclusion, puis épilogue), ou null. */
    public Text nextText() {
        for (StoryData.ChapterData chapter : data.chapters) {
            if (started(chapter) && !has("shown:intro:" + chapter.id)) {
                return new Text(chapter.title, chapter.intro, false, "shown:intro:" + chapter.id);
            }
            if (isDone(chapter.id) && !has("shown:outro:" + chapter.id)) {
                return new Text(chapter.title, chapter.outro, false, "shown:outro:" + chapter.id);
            }
        }
        for (StoryData.EndingData ending : data.endings) {
            if (has("ended:" + ending.id) && !has("shown:ending:" + ending.id)) {
                return new Text(ending.title, ending.text, true, "shown:ending:" + ending.id);
            }
        }
        return null;
    }

    /** Le texte a été montré : il ne reviendra plus. */
    public void markShown(Text text) {
        set(text.shownFlag);
    }

    // ------------------------------------------------------------------ rangs

    /** Le rang que mérite le personnage d'après story.json (les rangs s'obtiennent dans l'ordre). */
    public int earnedRank() {
        int rank = 0;
        for (StoryData.RankData r : data.ranks) {
            if (!holds(r.requires)) {
                break;
            }
            rank = r.rank;
        }
        return rank;
    }

    /** Monte le rang du personnage s'il en mérite un plus haut (il ne redescend jamais). @return la phrase à afficher, ou null */
    public String refreshRank() {
        int earned = earnedRank();
        if (earned <= player.getRank()) {
            return null;
        }
        player.setRank(earned);
        return "Vous passez au rang " + rankLabel(earned) + " de la Guilde.";
    }

    /** La lettre d'un rang : F pour 0, puis celles de story.json. */
    public static String rankLabel(int rank) {
        for (StoryData.RankData r : GameContent.story().ranks) {
            if (r.rank == rank) {
                return r.label;
            }
        }
        return rank <= 0 ? "F" : String.valueOf(rank);
    }

    /** Ce qu'il manque pour le rang suivant, en une phrase ; null au rang le plus haut. */
    public String nextRankHint() {
        for (StoryData.RankData r : data.ranks) {
            if (r.rank == player.getRank() + 1) {
                List<String> missing = new ArrayList<>();
                for (String c : r.requires) {
                    if (!holds(c)) {
                        missing.add(describe(c));
                    }
                }
                return "Rang " + r.label + " : " + (missing.isEmpty() ? "atteint." : "il manque " + String.join(", ", missing) + ".");
            }
        }
        return null;
    }

    private String describe(String condition) {
        String type = condition.substring(0, condition.indexOf(':'));
        String arg = condition.substring(condition.indexOf(':') + 1);
        if ("quests".equals(type)) {
            return arg + " quêtes terminées (" + player.getDoneQuests().size() + " faites)";
        }
        if ("flag".equals(type) && arg.startsWith("done:")) {
            StoryData.ChapterData chapter = data.chapter(arg.substring(5));
            return "le chapitre \"" + (chapter != null ? chapter.title : arg.substring(5)) + "\"";
        }
        if ("quest".equals(type)) {
            return "la quête \"" + arg + "\"";
        }
        return "une étape de l'histoire";
    }

    // ------------------------------------------------------------------ lieux verrouillés

    /** Pourquoi le lieu est fermé à ce personnage, ou null s'il peut y aller. */
    public String lockReason(String placeId) {
        WorldData.PlaceData place = GameContent.world().place(placeId);
        if (place == null || holds(place.requires)) {
            return null;
        }
        return place.lockedText != null ? place.lockedText : "Ce lieu est fermé pour l'instant.";
    }

    // ------------------------------------------------------------------ dialogues

    /** Les dialogues d'histoire proposés à cet endroit (« guildmaster@aethelgard », « cairn »...) en ce moment. */
    public List<StoryData.DialogData> dialogsAt(String point) {
        List<StoryData.DialogData> result = new ArrayList<>();
        for (StoryData.DialogData dialog : data.dialogs) {
            if (dialog.at.equals(point) && holds(dialog.requires)) {
                result.add(dialog);
            }
        }
        return result;
    }

    /** Les choix d'un dialogue que le personnage peut faire en ce moment. */
    public List<StoryData.OptionData> optionsOf(StoryData.DialogData dialog) {
        List<StoryData.OptionData> result = new ArrayList<>();
        for (StoryData.OptionData option : dialog.options) {
            if (holds(option.requires)) {
                result.add(option);
            }
        }
        return result;
    }

    /**
     * Le joueur lit un dialogue sans choix : ses effets s'appliquent.
     *
     * @return null si c'est fait, sinon pourquoi pas (trop chargé pour recevoir l'objet, objet à remettre manquant)
     */
    public String apply(StoryData.DialogData dialog) {
        return applyEffects(dialog.sets, dialog.unsets, dialog.gives, dialog.takes);
    }

    /** Le joueur fait un choix. @return null si c'est fait, sinon pourquoi pas */
    public String choose(StoryData.OptionData option) {
        return applyEffects(option.sets, option.unsets, option.gives, option.takes);
    }

    private String applyEffects(List<String> sets, List<String> unsets, List<String> gives, List<String> takes) {
        // on vérifie tout avant de rien changer : un échange refusé ne laisse pas de trace
        List<Item> taken = new ArrayList<>();
        double weight = player.getInventaire().getPoidsActuel();
        for (String id : takes) {
            Item item = null;
            for (Item candidate : player.getInventoryItems()) {
                if (id.equals(candidate.getId()) && !taken.contains(candidate)) {
                    item = candidate;
                    break;
                }
            }
            if (item == null) {
                return "Il vous manque : " + GameContent.newItem(id).getName() + ".";
            }
            taken.add(item);
            weight -= item.getPoids();
        }
        List<Item> given = new ArrayList<>();
        for (String id : gives) {
            Item item = GameContent.newItem(id);
            given.add(item);
            weight += item.getPoids();
        }
        double excess = weight - player.getInventaire().getPoidsMax();
        if (!given.isEmpty() && excess > 1e-9) {
            return String.format(Locale.FRANCE, "Vous portez trop : il faut libérer %.1f kg pour prendre %s.", excess, given.get(0).getName());
        }
        for (Item item : taken) {
            player.removeItem(item);
        }
        for (Item item : given) {
            player.addItem(item);
        }
        for (String flag : unsets) {
            unset(flag);
        }
        for (String flag : sets) {
            set(flag);
        }
        return null;
    }

    // ------------------------------------------------------------------ objets à ramasser, déclencheurs, monstres

    /** Les objets d'histoire à ramasser dans cette scène en ce moment. */
    public List<StoryData.PickupData> pickupsIn(String scene) {
        List<StoryData.PickupData> result = new ArrayList<>();
        for (StoryData.PickupData pickup : data.pickups) {
            if (pickup.scene.equals(scene) && !has(pickup.flag) && holds(pickup.requires)) {
                result.add(pickup);
            }
        }
        return result;
    }

    /** Le joueur ramasse l'objet. @return la phrase à afficher */
    public String pick(StoryData.PickupData pickup) {
        set(pickup.flag);
        return pickup.message != null ? pickup.message : "Vous ramassez : " + pickup.name + ".";
    }

    /**
     * Le joueur est en (x, z) dans cette scène : les déclencheurs qu'il vient de franchir s'appliquent (une seule
     * fois chacun).
     *
     * @return les déclencheurs qui viennent de s'appliquer (leur phrase ou leur texte de scène reste à afficher)
     */
    public List<StoryData.TriggerData> enter(String scene, float x, float z) {
        List<StoryData.TriggerData> fired = new ArrayList<>();
        for (StoryData.TriggerData trigger : data.triggers) {
            if (!trigger.scene.equals(scene) || has("trigger:" + trigger.id) || !holds(trigger.requires)) {
                continue;
            }
            if (Math.hypot(x - trigger.x, z - trigger.z) <= trigger.radius) {
                set("trigger:" + trigger.id);
                for (String flag : trigger.sets) {
                    set(flag);
                }
                fired.add(trigger);
            }
        }
        return fired;
    }

    /** Un monstre vient de mourir : son drapeau d'histoire est posé. @return la phrase à afficher, ou null */
    public String onKill(MonsterData monster) {
        if (monster.killFlag == null) {
            return null;
        }
        set(monster.killFlag);
        return monster.killMessage;
    }

    /** Un monstre vient de se rendre (il ne se bat plus) : son drapeau d'histoire est posé. */
    public void onYield(MonsterData monster) {
        if (monster.yieldFlag != null) {
            set(monster.yieldFlag);
        }
    }

    // ------------------------------------------------------------------ avantages et effets sur le monde

    /** Les avantages de combat de l'histoire dans ce lieu : {dégâts en %, vie max en plus}. */
    public int[] boonsIn(String placeId) {
        int damage = 0;
        int health = 0;
        for (StoryData.BoonData boon : data.boons) {
            if (boon.places.contains(placeId) && holds(boon.requires)) {
                damage += boon.damagePercent;
                health += boon.healthBonus;
            }
        }
        return new int[] {damage, health};
    }

    /** La chance d'embuscade, une fois appliqué ce que l'histoire a changé au monde. */
    public int ambushPercent(int base) {
        int percent = base;
        for (StoryData.EffectData effect : data.effects) {
            if (effect.ambushPercent != null && holds(effect.requires)) {
                percent = percent * effect.ambushPercent / 100;
            }
        }
        return Math.max(0, Math.min(100, percent));
    }

    /** Le {@code priceFactor} d'une boutique, une fois appliqué ce que l'histoire a changé au monde. */
    public int priceFactor(String shopId, int base) {
        int factor = base;
        for (StoryData.EffectData effect : data.effects) {
            Integer changed = effect.priceFactor.get(shopId);
            if (changed != null && holds(effect.requires)) {
                factor = changed;
            }
        }
        return factor;
    }

    private boolean stopsAging() {
        for (StoryData.EffectData effect : data.effects) {
            if (effect.stopAging && holds(effect.requires)) {
                return true;
            }
        }
        return false;
    }

    /** La guilde refuse-t-elle désormais ses quêtes à ce personnage ? */
    public boolean guildClosed() {
        for (StoryData.EffectData effect : data.effects) {
            if (effect.guildClosed && holds(effect.requires)) {
                return true;
            }
        }
        return false;
    }

    /** Le titre gagné par le personnage, ou null. */
    public String title() {
        for (StoryData.EffectData effect : data.effects) {
            if (effect.title != null && holds(effect.requires)) {
                return effect.title;
            }
        }
        return null;
    }

    /** Le nom d'un point de l'histoire (« Le Cairn des Anciens »), ou null. */
    public static String pointName(String point) {
        Map<String, String> points = GameContent.story().points;
        return points.get(point);
    }
}
