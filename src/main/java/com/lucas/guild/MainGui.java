package com.lucas.guild;

import com.lucas.guild.game.*;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.GameData;
import com.lucas.guild.model.Monstre;
import com.lucas.guild.model.Quest;
import com.lucas.guild.ui.*;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainGui extends Application {

    // ... (attributs identiques)
    private Adventurer player;
    private GameClock clock;
    private Guild guild;
    private Boutique shop;
    private Forge forge;
    private Trainer trainer;
    private SaveManager saveManager;
    private BorderPane root;
    private TextArea consoleOutput = new TextArea();
    private VBox centerPanel = new VBox();
    private VBox leftPanel;
    private StatusView statusView;

    @Override
    public void start(Stage primaryStage) {
        // ... (méthode start identique)
        guild = new Guild();
        shop = new Boutique();
        forge = new Forge();
        trainer = new Trainer();
        saveManager = new SaveManager();
        startNewGame();
        root = new BorderPane();
        statusView = new StatusView(player);
        root.setTop(statusView);
        leftPanel = createLeftPanel();
        root.setLeft(leftPanel);
        consoleOutput.getStyleClass().add("text-area");
        Console.redirectSystemOut(consoleOutput);
        consoleOutput.setEditable(false);
        consoleOutput.setWrapText(true);
        centerPanel.getChildren().add(consoleOutput);
        root.setCenter(centerPanel);
        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        primaryStage.setTitle("Guilde des Aventuriers");
        primaryStage.setScene(scene);
        primaryStage.show();
        System.out.println("Bienvenue dans l'aventure !");
    }

    private VBox createLeftPanel() {
        // ... (méthode createLeftPanel identique)
        VBox vbox = new VBox(10);
        vbox.getStyleClass().add("left-panel");
        Button statusButton = new Button("Mon État");
        Button guildButton = new Button("Guilde");
        Button tavernButton = new Button("Taverne");
        Button shopButton = new Button("Boutique");
        Button forgeButton = new Button("Forge");
        Button trainerButton = new Button("Entraîneur");
        Button equipmentButton = new Button("Équipement");
        Button sleepButton = new Button("Dormir");
        Button adventureButton = new Button("Aventure");
        Button saveButton = new Button("Sauvegarder");
        Button loadButton = new Button("Charger");
        statusButton.setOnAction(e -> showInCenter(consoleOutput, player.toString()));
        guildButton.setOnAction(e -> showInCenter(new GuildView(player, guild)));
        tavernButton.setOnAction(e -> showInCenter(new TavernView(player, clock)));
        shopButton.setOnAction(e -> showInCenter(new ShopView(player, shop, clock)));
        forgeButton.setOnAction(e -> showInCenter(new ForgeView(player, forge, clock)));
        trainerButton.setOnAction(e -> showInCenter(new TrainerView(player, trainer)));
        equipmentButton.setOnAction(e -> showInCenter(new EquipmentView(player)));
        sleepButton.setOnAction(e -> showInCenter(new SleepView(player, clock)));
        adventureButton.setOnAction(e -> startAdventure());
        saveButton.setOnAction(e -> saveManager.saveGame(player, clock));
        loadButton.setOnAction(e -> loadGame());
        vbox.getChildren().addAll(statusButton, guildButton, tavernButton, shopButton, forgeButton, trainerButton, equipmentButton, sleepButton, adventureButton, saveButton, loadButton);
        vbox.getChildren().forEach(button -> 
            button.addEventHandler(javafx.event.ActionEvent.ACTION, e -> {
                if (statusView != null) statusView.update();
            })
        );
        return vbox;
    }
    
    private Monstre createMonsterForQuest(Quest quest) {
        switch (quest.getTarget()) {
            case "Rat Géant":
                return new Monstre("Rat Géant", 40, 25, "/images/rat_geant.png");
            case "Chef Gobelin":
                return new Monstre("Chef Gobelin", 80, 100, "/images/chef_gobelin.png");
            default:
                return null;
        }
    }

    // ... (le reste des méthodes reste identique)
    private void startNewGame() {
        player = new Adventurer("Lucas", "Caserne");
        clock = new GameClock(player);
        System.out.println("Nouvelle aventure commencée !");
    }
    private void loadGame() {
        GameData savedData = saveManager.loadGame();
        if (savedData != null) {
            player = savedData.playerData;
            clock = new GameClock();
            clock.setCurrentDay(savedData.currentDay);
            clock.setCurrentHour(savedData.currentHour);
            clock.setPlayer(player);
            statusView = new StatusView(player);
            root.setTop(statusView);
            showInCenter(consoleOutput, "Partie chargée avec succès !");
        }
    }
    private void startAdventure() {
        Quest currentQuest = player.getCurrentQuest();
        if (currentQuest == null) { System.out.println("Vous n'avez pas de quête."); return; }
        if (currentQuest.getType() == Quest.QuestType.COMBAT) {
            System.out.println("\n... En route pour l'aventure ...\n");
            clock.passTime(2);
            Monstre monster = createMonsterForQuest(currentQuest);
            if (monster == null) return;
            CombatView combatView = new CombatView(player, monster, victory -> {
                if (victory) {
                    player.gainExperience(monster.getXpValue());
                    player.findItem(monster.getLoot());
                } else {
                    System.out.println("Vous vous réveillez à l'infirmerie...");
                    player.ajouterVie(50);
                }
                showInCenter(consoleOutput, "Le combat est terminé.");
                leftPanel.setDisable(false);
                statusView.update();
            }, statusView);
            showInCenter(combatView);
            leftPanel.setDisable(true);
        } else {
            System.out.println("La gestion des quêtes de livraison en GUI n'est pas encore implémentée.");
        }
    }
    private void showInCenter(Node content) {
        centerPanel.getChildren().clear();
        centerPanel.getChildren().add(content);
    }
    private void showInCenter(Node content, String message) {
        showInCenter(content);
        System.out.println(message);
    }
    public static void main(String[] args) {
        launch(args);
    }
}
