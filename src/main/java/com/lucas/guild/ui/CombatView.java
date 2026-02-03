package com.lucas.guild.ui;

import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Attaque;
import com.lucas.guild.model.Monstre;
import com.lucas.guild.model.PotionDeSoin;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class CombatView extends BorderPane {

    private Adventurer player;
    private Monstre monster;
    private Consumer<Boolean> onCombatEnd;
    private StatusView statusView;

    private Label playerName, monsterName;
    private ProgressBar playerHealthBar, monsterHealthBar;
    private HBox actionButtons;
    private VBox attackChoices, itemChoices;
    private ImageView monsterImageView; // Pour afficher l'image du monstre

    public CombatView(Adventurer player, Monstre monster, Consumer<Boolean> onCombatEnd, StatusView statusView) {
        this.player = player;
        this.monster = monster;
        this.onCombatEnd = onCombatEnd;
        this.statusView = statusView;
        this.getStyleClass().add("view-pane");

        // --- Panneau du haut : Stats des combattants ---
        setTop(createStatusPane());

        // --- Centre : Image du monstre ---
        monsterImageView = new ImageView();
        if (monster.getImagePath() != null) {
            try {
                Image monsterImage = new Image(getClass().getResourceAsStream(monster.getImagePath()));
                monsterImageView.setImage(monsterImage);
                monsterImageView.setFitWidth(200); // Taille de l'image
                monsterImageView.setPreserveRatio(true);
            } catch (Exception e) {
                System.err.println("Erreur de chargement de l'image du monstre: " + monster.getImagePath() + " - " + e.getMessage());
            }
        }
        VBox centerBox = new VBox(monsterImageView);
        centerBox.setAlignment(Pos.CENTER);
        setCenter(centerBox);

        // --- Panneau du bas : Actions du joueur ---
        actionButtons = createActionButtons();
        setBottom(actionButtons);

        System.out.println("!!! COMBAT !!!");
        System.out.println(player.getName() + " affronte un " + monster.getName() + " !");
        updateCombatantStatus();
    }

    private GridPane createStatusPane() {
        GridPane statusPane = new GridPane();
        statusPane.setHgap(20);
        statusPane.setVgap(10);
        statusPane.setPadding(new Insets(0, 0, 20, 0));

        playerName = new Label();
        playerName.getStyleClass().add("h2");
        playerHealthBar = new ProgressBar(1.0);
        playerHealthBar.setPrefWidth(250);

        monsterName = new Label();
        monsterName.getStyleClass().add("h2");
        monsterHealthBar = new ProgressBar(1.0);
        monsterHealthBar.setPrefWidth(250);

        statusPane.add(playerName, 0, 0);
        statusPane.add(playerHealthBar, 1, 0);
        statusPane.add(monsterName, 0, 1);
        statusPane.add(monsterHealthBar, 1, 1);

        return statusPane;
    }

    private HBox createActionButtons() {
        HBox hbox = new HBox(15);
        hbox.setAlignment(Pos.CENTER);
        Button attackButton = new Button("Attaquer");
        Button itemButton = new Button("Utiliser un Objet");
        hbox.getChildren().addAll(attackButton, itemButton);
        attackButton.setOnAction(e -> showAttackChoices());
        itemButton.setOnAction(e -> showItemChoices());
        return hbox;
    }

    private void showAttackChoices() {
        attackChoices = new VBox(10);
        attackChoices.setAlignment(Pos.CENTER);
        for (Attaque attaque : player.getAttaques()) {
            Button btn = new Button(attaque.getNom());
            btn.setOnAction(e -> executePlayerAttack(attaque));
            attackChoices.getChildren().add(btn);
        }
        Button cancelButton = new Button("Annuler");
        cancelButton.setOnAction(e -> setBottom(actionButtons));
        attackChoices.getChildren().add(cancelButton);
        setBottom(attackChoices);
    }

    private void showItemChoices() {
        itemChoices = new VBox(10);
        itemChoices.setAlignment(Pos.CENTER);
        List<PotionDeSoin> potions = player.getInventory().stream()
                .filter(i -> i instanceof PotionDeSoin)
                .map(i -> (PotionDeSoin) i)
                .collect(Collectors.toList());

        if (potions.isEmpty()) {
            System.out.println("Vous n'avez aucune potion !");
            return;
        }

        for (PotionDeSoin potion : potions) {
            Button btn = new Button(potion.getName());
            btn.setOnAction(e -> executeItemUse(potion));
            itemChoices.getChildren().add(btn);
        }
        Button cancelButton = new Button("Annuler");
        cancelButton.setOnAction(e -> setBottom(actionButtons));
        itemChoices.getChildren().add(cancelButton);
        setBottom(itemChoices);
    }

    private void executePlayerAttack(Attaque attaque) {
        setBottom(actionButtons);
        actionButtons.setDisable(true);
        attaque.executer(player, monster);
        updateCombatantStatus();
        statusView.update();
        if (!monster.estVivant()) {
            onCombatEnd.accept(true);
        } else {
            new Thread(() -> {
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                javafx.application.Platform.runLater(() -> {
                    monsterTurn();
                    actionButtons.setDisable(false);
                });
            }).start();
        }
    }

    private void executeItemUse(PotionDeSoin potion) {
        setBottom(actionButtons);
        actionButtons.setDisable(true);
        potion.utiliser(player);
        player.removeItem(potion);
        updateCombatantStatus();
        statusView.update();
        new Thread(() -> {
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            javafx.application.Platform.runLater(() -> {
                monsterTurn();
                actionButtons.setDisable(false);
            });
        }).start();
    }

    private void monsterTurn() {
        if (!monster.estVivant()) return;
        Attaque chosenAttack = monster.getAttaques().get(new Random().nextInt(monster.getAttaques().size()));
        chosenAttack.executer(monster, player);
        updateCombatantStatus();
        statusView.update();
        if (!player.estVivant()) {
            onCombatEnd.accept(false);
        }
    }

    private void updateCombatantStatus() {
        playerName.setText(player.getName() + " PV:");
        playerHealthBar.setProgress((double) player.getHealth() / player.getMaxHealth());
        monsterName.setText(monster.getName() + " PV:");
        monsterHealthBar.setProgress((double) monster.getHealth() / monster.getMaxHealth());
    }
}
