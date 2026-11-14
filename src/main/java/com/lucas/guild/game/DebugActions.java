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
