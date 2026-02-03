package com.lucas.guild.ui;

import com.lucas.guild.game.Forge;
import com.lucas.guild.game.GameClock;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Item;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ForgeView extends VBox {

    public ForgeView(Adventurer player, Forge forge, GameClock clock) {
        this.getStyleClass().add("view-pane");

        Label title = new Label("Forge du Nain Grincheux");
        title.getStyleClass().add("h1");

        Label welcome = new Label("'Qu'est-ce que tu veux ? Fais pas semblant de regarder, achète !'");

        this.getChildren().addAll(title, welcome);

        for (int i = 1; i <= 2; i++) {
            Item item = forge.getItem(i);
            if (item != null) {
                this.getChildren().add(createItemRow(player, clock, item, forge.getPrice(item)));
            }
        }
    }

    private HBox createItemRow(Adventurer player, GameClock clock, Item item, int price) {
        HBox itemRow = new HBox(15);
        itemRow.setStyle("-fx-border-color: #78909C; -fx-padding: 10; -fx-border-radius: 5;");

        Label itemInfo = new Label(item.toString() + " - " + price + " Or");
        itemInfo.setPrefWidth(300);

        Button buyButton = new Button("Acheter");
        buyButton.setOnAction(e -> {
            player.buyItem(item, price);
            clock.passTime(1);
        });

        itemRow.getChildren().addAll(itemInfo, buyButton);
        return itemRow;
    }
}
