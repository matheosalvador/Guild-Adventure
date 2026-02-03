package com.lucas.guild.ui;

import com.lucas.guild.game.Guild;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Quest;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;

public class GuildView extends VBox {

    public GuildView(Adventurer player, Guild guild) {
        this.getStyleClass().add("view-pane");

        Label title = new Label("Tableau des Quêtes");
        title.getStyleClass().add("h1");

        this.getChildren().add(title);

        List<Quest> availableQuests = guild.getQuestsForRank(player.getRank());

        if (availableQuests.isEmpty()) {
            this.getChildren().add(new Label("Aucune quête disponible pour votre rang."));
            return;
        }

        for (Quest quest : availableQuests) {
            this.getChildren().add(createQuestRow(player, quest));
        }
    }

    private HBox createQuestRow(Adventurer player, Quest quest) {
        HBox questRow = new HBox(15);
        questRow.setStyle("-fx-border-color: #78909C; -fx-padding: 10; -fx-border-radius: 5;");

        Label questInfo = new Label("[" + quest.getType() + "] " + quest.getTitle());
        questInfo.setPrefWidth(300);

        Button acceptButton = new Button("Accepter");
        acceptButton.setOnAction(e -> {
            player.acceptQuest(quest);
            acceptButton.setDisable(true);
        });

        if (player.getCurrentQuest() != null) {
            acceptButton.setDisable(true);
        }

        questRow.getChildren().addAll(questInfo, acceptButton);
        return questRow;
    }
}
