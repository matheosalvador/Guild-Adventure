package com.lucas.guild.ui;

import com.lucas.guild.game.Trainer;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Skill;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class TrainerView extends VBox {

    public TrainerView(Adventurer player, Trainer trainer) {
        this.getStyleClass().add("view-pane");

        Label title = new Label("Entraîneur de la Guilde");
        title.getStyleClass().add("h1");

        Label pointsLabel = new Label("Points de compétence disponibles : " + player.getSkillPoints());
        pointsLabel.getStyleClass().add("h2");

        this.getChildren().addAll(title, pointsLabel);

        List<Skill> learnableSkills = trainer.getLearnableSkills(player);
        if (learnableSkills.isEmpty()) {
            this.getChildren().add(new Label("Aucune nouvelle compétence à apprendre. Gagnez des rangs !"));
        } else {
            for (Skill skill : learnableSkills) {
                this.getChildren().add(createSkillRow(player, skill));
            }
        }
    }

    private HBox createSkillRow(Adventurer player, Skill skill) {
        HBox skillRow = new HBox(15);
        skillRow.setStyle("-fx-border-color: #78909C; -fx-padding: 10; -fx-border-radius: 5;");

        String prereqText = (skill.getPrerequisiteSkillName() != null) ? " (Requiert: " + skill.getPrerequisiteSkillName() + ")" : "";
        Label skillInfo = new Label(skill.getName() + " - " + skill.getDescription() + prereqText);
        skillInfo.setPrefWidth(400);

        Button learnButton = new Button("Apprendre (1 pt)");
        learnButton.setOnAction(e -> {
            player.learnSkill(skill);
            learnButton.setDisable(true);
        });

        if (player.getSkillPoints() <= 0 || (skill.getPrerequisiteSkillName() != null && !player.hasLearnedSkill(skill.getPrerequisiteSkillName()))) {
            learnButton.setDisable(true);
        }

        skillRow.getChildren().addAll(skillInfo, learnButton);
        return skillRow;
    }
}
