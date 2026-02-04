package com.lucas.guild.game;

import com.lucas.guild.model.*;

import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Combat {

    private Adventurer player;
    private Monstre monster;
    private Scanner scanner;
    private Random random = new Random();

    public Combat(Adventurer player, Monstre monster) {
        this.player = player;
        this.monster = monster;
        this.scanner = new Scanner(System.in);
    }

    public boolean start() {
        // A implementer plus tard dans LWJGL
        System.out.println("!!! COMBAT !!!");
        System.out.println(player.getName() + " affronte un " + monster.getName() + " !");

        while (player.estVivant() && monster.estVivant()) {
            displayStatus();
            playerTurn();
            if (!monster.estVivant()) break;

            monsterTurn();
        }

        if (player.estVivant()) {
            System.out.println("Vous avez vaincu le " + monster.getName() + " !");
            return true;
        } else {
            System.out.println("Vous avez été vaincu...");
            return false;
        }
    }

    private void playerTurn() {
        System.out.println("C'est votre tour. Que faire ?");
        System.out.println("1. Attaquer");
        System.out.println("2. Utiliser un objet");
        
        int choice = Integer.parseInt(scanner.nextLine());

        if (choice == 1) {
            chooseAttack();
        } else if (choice == 2) {
            useItem();
        } else {
            System.out.println("Choix invalide, vous perdez votre tour !");
        }
    }

    private void chooseAttack() {
        System.out.println("Choisissez votre attaque :");
        List<Attaque> attaques = player.getAttaques();
        for (int i = 0; i < attaques.size(); i++) {
            System.out.println((i + 1) + ". " + attaques.get(i).getNom());
        }
        
        int choice = Integer.parseInt(scanner.nextLine());
        if (choice > 0 && choice <= attaques.size()) {
            attaques.get(choice - 1).executer(player, monster);
        } else {
            System.out.println("Choix invalide !");
            playerTurn(); // On le laisse réessayer
        }
    }

    private void useItem() {
        System.out.println("Quel objet utiliser ?");
        List<Item> potions = player.getInventory().stream()
                .filter(item -> item instanceof PotionDeSoin)
                .collect(Collectors.toList());

        if (potions.isEmpty()) {
            System.out.println("Vous n'avez aucune potion !");
            playerTurn();
            return;
        }

        for (int i = 0; i < potions.size(); i++) {
            System.out.println((i + 1) + ". " + potions.get(i).getName());
        }
        System.out.println("0. Annuler");

        int choice = Integer.parseInt(scanner.nextLine());
        if (choice > 0 && choice <= potions.size()) {
            PotionDeSoin chosenPotion = (PotionDeSoin) potions.get(choice - 1);
            chosenPotion.utiliser(player);
            player.removeItem(chosenPotion); // On retire la potion après usage
        } else {
            playerTurn();
        }
    }
    
    private void displayStatus() {
        System.out.println("---");
        System.out.println("Joueur: " + player.getHealth() + "/" + player.getMaxHealth() + " PV");
        System.out.println("Monstre: " + monster.getHealth() + "/" + monster.getMaxHealth() + " PV");
        System.out.println("---");
    }
    
    private void monsterTurn() {
        List<Attaque> attaques = monster.getAttaques();
        Attaque chosenAttack = attaques.get(random.nextInt(attaques.size()));
        chosenAttack.executer(monster, player);
    }
}
