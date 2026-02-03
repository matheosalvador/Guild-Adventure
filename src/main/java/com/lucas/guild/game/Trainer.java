package com.lucas.guild.game;

import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Skill;
import com.lucas.guild.model.SkillTree;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Trainer {
    private Map<String, SkillTree> skillTrees = new HashMap<>();

    public Trainer() {
        skillTrees.put("Caserne", new SkillTree("Caserne"));
        // skillTrees.put("Académie", new SkillTree("Académie"));
    }

    public void displayAvailableSkills(Adventurer player) {
        SkillTree tree = skillTrees.get(player.getOrigin());
        if (tree == null) {
            System.out.println("Désolé, je n'ai rien à enseigner à quelqu'un comme vous.");
            return;
        }

        System.out.println("--- Compétences du Guerrier ---");
        System.out.println("Vous avez " + player.getSkillPoints() + " point(s) de compétence.");

        List<Skill> availableSkills = tree.getSkills().stream()
            .filter(skill -> player.getRank() >= skill.getRequiredRank() && !player.hasLearnedSkill(skill.getName()))
            .collect(Collectors.toList());

        if (availableSkills.isEmpty()) {
            System.out.println("Aucune nouvelle compétence à apprendre pour le moment. Revenez quand vous serez plus fort.");
            return;
        }

        for (int i = 0; i < availableSkills.size(); i++) {
            Skill skill = availableSkills.get(i);
            String prereq = player.hasLearnedSkill(skill.getPrerequisiteSkillName()) || skill.getPrerequisiteSkillName() == null ? "" : " (Requiert: " + skill.getPrerequisiteSkillName() + ")";
            System.out.println((i + 1) + ". " + skill.getName() + " - " + skill.getDescription() + prereq);
        }
    }

    public List<Skill> getLearnableSkills(Adventurer player) {
        SkillTree tree = skillTrees.get(player.getOrigin());
        if (tree == null) return new ArrayList<>();

        return tree.getSkills().stream()
            .filter(skill -> player.getRank() >= skill.getRequiredRank() && !player.hasLearnedSkill(skill.getName()))
            .collect(Collectors.toList());
    }
}
