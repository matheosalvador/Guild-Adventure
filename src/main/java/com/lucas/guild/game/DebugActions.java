package com.lucas.guild.game;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.SkillData;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.GameCalendar;
import com.lucas.guild.model.GameClock;
import com.lucas.guild.model.Item;
import com.lucas.guild.model.Skills;

/**
 * Les actions du panneau de debug, sans dépendance à l'interface pour rester testables.
 * Chacune renvoie une phrase décrivant ce qui s'est passé (ou pourquoi rien ne s'est passé).
 */
public final class DebugActions {

    private final GameSession session;

    public DebugActions(GameSession session) {
        this.session = session;
    }

    private Adventurer player() {
        return session.getPlayer();
    }

    private GameClock clock() {
        return session.getClock();
    }

    // ------------------------------------------------------------------ événements du monde

    /** Démarre l'événement {@code eventId} dans la ville où se trouve le joueur. */
    public String startEvent(String eventId) {
        return WorldEvents.start(player(), eventId, player().getTownId(), clock().getCurrentDay());
    }

    /** Efface tous les événements en cours. */
    public String clearEvents() {
        int n = player().getActiveEvents().size();
        player().getActiveEvents().clear();
        return n + " événement(s) effacé(s).";
    }

    // ------------------------------------------------------------------ histoire

    /**
     * Termine le chapitre en cours en forçant chacun de ses objectifs (drapeau posé, quête notée terminée, texte
     * noté lu). Les sceaux forcés ainsi ne sont ni intacts ni fêlés : ils ouvrent le château sans avantage.
     */
    public String finishChapter() {
        Story story = new Story(session);
        story.advance();
        com.lucas.guild.data.StoryData.ChapterData chapter = story.currentChapter();
        if (chapter == null) {
            String hint = story.nextRankHint();
            return "Aucun chapitre en cours" + (hint != null ? " (" + hint + ")" : ".");
        }
        for (com.lucas.guild.data.StoryData.ObjectiveData objective : chapter.objectives) {
            for (String condition : objective.done) {
                String type = condition.substring(0, condition.indexOf(':'));
                String arg = condition.substring(condition.indexOf(':') + 1);
                if ("flag".equals(type)) {
                    story.set(arg);
                } else if ("quest".equals(type) && !player().getDoneQuests().contains(arg)) {
                    player().getActiveQuests().remove(arg);
                    player().getDoneQuests().add(arg);
                } else if ("lore".equals(type)) {
                    Lore.learn(player(), arg);
                }
            }
        }
        story.advance();
        return "Chapitre terminé : " + chapter.title + ".";
    }

    /** Donne le rang suivant de la Guilde sans en remplir les conditions. */
    public String nextRank() {
        int max = com.lucas.guild.data.GameContent.story().ranks.size();
        if (player().getRank() >= max) {
            return "Rang " + Story.rankLabel(player().getRank()) + " : c'est le plus haut.";
        }
        player().setRank(player().getRank() + 1);
        new Story(session).advance();
        return "Rang " + Story.rankLabel(player().getRank()) + " (forcé).";
    }

    // ------------------------------------------------------------------ personnage

    public String addGold(int amount) {
        player().addGold(amount);
        return "+" + amount + " Po (total " + player().getGold() + ").";
    }

    public String healFull() {
        player().heal(player().getMaxHealth());
        return "Vie au maximum (" + player().getHealth() + ").";
    }

    public String hurt(int amount) {
        player().perdreVie(amount);
        return "-" + amount + " PV (reste " + player().getHealth() + ").";
    }

    /** Donne un objet du catalogue (refusé s'il dépasse la charge portable). */
    public String giveItem(String itemId) {
        Item item = GameContent.newItem(itemId);
        if (item == null) {
            return "Objet inconnu : " + itemId;
        }
        if (!player().addItem(item)) {
            return item.getName() + " : trop lourd pour la charge actuelle.";
        }
        return item.getName() + " ajouté(e).";
    }

    /** Augmente de 1 le niveau d'une compétence (avec ses effets et son message). */
    public String skillLevelUp(String skillId) {
        Skills skills = player().getSkills();
        SkillData def = skills.definition(skillId);
        if (def == null) {
            return "Compétence inconnue : " + skillId;
        }
        int level = skills.getLevel(skillId);
        if (level >= def.maxLevel()) {
            return def.label + " est déjà au niveau maximum.";
        }
        int maxBefore = player().getMaxHealth();
        skills.setLevel(skillId, level + 1);
        player().refreshBonuses();
        int gain = player().getMaxHealth() - maxBefore;
        if (gain > 0) {
            player().ajouterVie(gain);
        }
        return def.label + " : niveau " + (level + 1) + ".";
    }

    /** Vieillit le personnage (sa date de naissance recule), avec le message de palier éventuel. */
    public String ageBy(int years) {
        int day = clock().getCurrentDay();
        player().setBirthDay(player().getBirthDay() - years * GameCalendar.standard().daysPerYear());
        String stage = player().refreshAging(day);
        if (stage != null) {
            clock().postMessage(stage);
        }
        return "Âge : " + player().getAge(day) + " ans.";
    }

    // ------------------------------------------------------------------ temps

    public String advanceHours(int hours) {
        clock().passTime(hours);
        return "Il est " + clock().formatTime() + ", jour " + clock().getCurrentDay() + " (" + clock().getDate() + ").";
    }

    public String advanceDays(int days) {
        clock().advanceMinutes(days * 24 * 60);
        return "Jour " + clock().getCurrentDay() + " (" + clock().getDate() + "), " + clock().formatTime() + ".";
    }

    /** Avance jusqu'à la prochaine fois qu'il est {@code hour} heures (tout de suite si c'est déjà l'heure). */
    public String advanceToHour(int hour) {
        int now = clock().getCurrentHour() * 60 + clock().getCurrentMinute();
        int target = hour * 60;
        int delta = Math.floorMod(target - now, 24 * 60);
        clock().advanceMinutes(delta);
        return "Il est " + clock().formatTime() + ", jour " + clock().getCurrentDay() + ".";
    }
}
