package com.lucas.guild.ui;

import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Arme;
import com.lucas.guild.model.Armure;
import com.lucas.guild.model.Item;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.stream.Collectors;

public class EquipmentView extends VBox {

    private Adventurer player;
    private VBox equipmentViewRoot;

    public EquipmentView(Adventurer player) {
        this.player = player;
        this.equipmentViewRoot = this;
        this.getStyleClass().add("view-pane");
        
        refreshView();
    }

    private void refreshView() {
        equipmentViewRoot.getChildren().clear();

        Label title = new Label("Gestion de l'Équipement");
        title.getStyleClass().add("h1");
        
        GridPane currentEquipmentPane = new GridPane();
        currentEquipmentPane.setHgap(10);
        currentEquipmentPane.setVgap(10);
        currentEquipmentPane.add(new Label("Arme équipée:"), 0, 0);
        currentEquipmentPane.add(new Label(player.getArmeEquipee() != null ? player.getArmeEquipee().toString() : "Mains nues"), 1, 0);
        currentEquipmentPane.add(new Label("Armure équipée:"), 0, 1);
        currentEquipmentPane.add(new Label(player.getArmureEquipee() != null ? player.getArmureEquipee().toString() : "Aucune"), 1, 1);

        Label inventoryTitle = new Label("Équipement dans l'inventaire");
        inventoryTitle.getStyleClass().add("h2");

        equipmentViewRoot.getChildren().addAll(title, currentEquipmentPane, inventoryTitle);

        List<Item> equippableItems = player.getInventory().stream()
            .filter(i -> i instanceof Arme || i instanceof Armure)
            .collect(Collectors.toList());

        if (equippableItems.isEmpty()) {
            equipmentViewRoot.getChildren().add(new Label("Aucun équipement à équiper."));
        } else {
            for (Item item : equippableItems) {
                equipmentViewRoot.getChildren().add(createEquipmentRow(item));
            }
        }
    }

    private HBox createEquipmentRow(Item item) {
        HBox row = new HBox(15);
        Label itemLabel = new Label(item.toString());
        itemLabel.setPrefWidth(300);
        
        Button equipButton = new Button("Équiper");
        equipButton.setOnAction(e -> {
            player.equiper(item);
            refreshView();
        });

        row.getChildren().addAll(itemLabel, equipButton);
        return row;
    }
}
