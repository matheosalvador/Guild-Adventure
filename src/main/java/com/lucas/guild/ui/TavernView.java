package com.lucas.guild.ui;

import com.lucas.guild.game.GameClock;
import com.lucas.guild.model.Adventurer;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class TavernView extends VBox {

    public TavernView(Adventurer player, GameClock clock) {
        this.getStyleClass().add("view-pane");

        Label title = new Label("Taverne du Chêne Doré");
        title.getStyleClass().add("h1");

        Label welcome = new Label("Le tavernier vous salue d'un signe de tête. Que désirez-vous ?");

        Button eatButton = new Button("Manger un repas (5 Or)");
        eatButton.setOnAction(e -> {
            player.eat();
            clock.passTime(1);
            System.out.println("Vous vous sentez repu. Le temps a passé.");
        });

        Button drinkButton = new Button("Boire une chope (2 Or)");
        drinkButton.setOnAction(e -> {
            player.drink();
            clock.passTime(1);
            System.out.println("Vous vous sentez désaltéré. Le temps a passé.");
        });

        this.getChildren().addAll(title, welcome, eatButton, drinkButton);
    }
}
