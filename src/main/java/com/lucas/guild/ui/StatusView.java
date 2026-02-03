package com.lucas.guild.ui;

import com.lucas.guild.model.Adventurer;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.GridPane;

public class StatusView extends GridPane {

    private Adventurer player;

    private Label nameLabel, rankLabel, goldLabel, hpLabel;
    private ProgressBar hpBar, hungerBar, thirstBar, energyBar;

    public StatusView(Adventurer player) {
        this.player = player;
        this.getStyleClass().add("status-view"); // Appliquer la classe CSS
        setPadding(new Insets(10));
        setHgap(20);
        setVgap(5);

        // Initialisation des composants
        nameLabel = new Label();
        rankLabel = new Label();
        goldLabel = new Label();
        hpLabel = new Label();
        
        hpBar = new ProgressBar();
        hungerBar = new ProgressBar();
        thirstBar = new ProgressBar();
        energyBar = new ProgressBar();

        // Ajout des composants à la grille
        this.add(new Label("Nom:"), 0, 0);
        this.add(nameLabel, 1, 0);
        this.add(new Label("Rang:"), 0, 1);
        this.add(rankLabel, 1, 1);

        this.add(new Label("Or:"), 2, 0);
        this.add(goldLabel, 3, 0);
        this.add(new Label("PV:"), 2, 1);
        this.add(hpLabel, 3, 1);
        
        this.add(new Label("Santé:"), 4, 0);
        this.add(hpBar, 5, 0);
        this.add(new Label("Faim:"), 4, 1);
        this.add(hungerBar, 5, 1);
        
        this.add(new Label("Soif:"), 6, 0);
        this.add(thirstBar, 5, 0);
        this.add(new Label("Énergie:"), 6, 1);
        this.add(energyBar, 5, 1);

        update();
    }

    public void update() {
        nameLabel.setText(player.getName());
        rankLabel.setText(player.getRankLetter() + " (XP: " + player.getExperience() + "/" + player.getExperienceToNextRank() + ")");
        goldLabel.setText(String.valueOf(player.getGold()));
        hpLabel.setText(player.getHealth() + "/" + player.getMaxHealth());

        hpBar.setProgress((double) player.getHealth() / player.getMaxHealth());
        hungerBar.setProgress(player.getHunger() / 100.0);
        thirstBar.setProgress(player.getThirst() / 100.0);
        energyBar.setProgress(player.getEnergy() / 100.0);
    }
}
