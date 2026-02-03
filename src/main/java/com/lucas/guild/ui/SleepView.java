package com.lucas.guild.ui;

import com.lucas.guild.game.GameClock;
import com.lucas.guild.model.Adventurer;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class SleepView extends VBox {

    public SleepView(Adventurer player, GameClock clock) {
        this.getStyleClass().add("view-pane");

        Label title = new Label("La nuit tombe...");
        title.getStyleClass().add("h1");

        Label question = new Label("Où souhaitez-vous passer la nuit ?");

        Button innButton = new Button("Auberge (10 Or) - Repos complet et sûr");
        innButton.setOnAction(e -> {
            if (player.sleepInInn()) {
                clock.sleepUntilNextMorning();
            }
        });

        Button streetButton = new Button("Ruelle sombre (Gratuit) - Repos partiel et risqué");
        streetButton.setOnAction(e -> {
            player.sleepRough();
            clock.sleepUntilNextMorning();
        });
        
        if (player.getGold() < 10) {
            innButton.setDisable(true);
        }

        this.getChildren().addAll(title, question, innButton, streetButton);
    }
}
