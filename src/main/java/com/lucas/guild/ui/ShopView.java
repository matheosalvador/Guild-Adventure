package com.lucas.guild.ui;

import com.lucas.guild.game.Boutique;
import com.lucas.guild.game.GameClock;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Item;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class ShopView extends VBox {

    public ShopView(Adventurer player, Boutique shop, GameClock clock) {
        this.getStyleClass().add("view-pane");

        Label title = new Label("Boutique d'Objets");
        title.getStyleClass().add("h1");

        Label welcome = new Label("La marchande vous sourit. 'Regardez mes merveilles !'");

        this.getChildren().addAll(title, welcome);

        for (int i = 0; i < 2; i++) {
            Item item = shop.getItem(i);
            if (item != null) {
                this.getChildren().add(createItemRow(player, clock, item, shop.getPrice(item)));
            }
        }
    }

    private HBox createItemRow(Adventurer player, GameClock clock, Item item, int price) {
        HBox itemRow = new HBox(15);
        itemRow.setStyle("-fx-border-color: #78909C; -fx-padding: 10; -fx-border-radius: 5;");

        Label itemInfo = new Label(item.getName() + " - " + price + " Or");
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
