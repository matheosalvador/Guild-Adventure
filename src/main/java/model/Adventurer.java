package model;

import java.util.ArrayList;
import java.util.List;

public class Adventurer {
    // Stats d'identité
    private String name;
    private String origin; // Caserne, Académie, Église
    private int age = 18;
    private int rank = 0; // 0 = Rang F, 1 = E, etc.
    
    // Besoins vitaux (0 à 100)
    private double hunger = 100.0;
    private double thirst = 100.0;
    private double energy = 100.0;
    private int hp = 100;
    
    // Économie et Quêtes
    private int gold = 10;
    private Quest currentQuest = null;
    private List<String> inventory = new ArrayList<>();

    public Adventurer(String name, String origin) {
        this.name = name;
        this.origin = origin;
        applyOriginBonus();
        this.inventory.add("Vieux pain"); // Equipement de départ
    }

    private void applyOriginBonus() {
        if (origin.equals("Caserne")) {
            this.hp += 20; // Plus robuste
        } else if (origin.equals("Académie")) {
            // Bonus de mana ou intelligence ici
        }
    }

    public void acceptQuest(Quest quest) {
        if (this.currentQuest == null) {
            this.currentQuest = quest;
            System.out.println(name + " accepte la quête : " + quest.getTitle());
        } else {
            System.out.println("Vous avez déjà une quête en cours !");
        }
    }

    public void completeQuest() {
        if (currentQuest != null && inventory.contains(currentQuest.getProofItem())) {
            System.out.println("Quête '" + currentQuest.getTitle() + "' terminée !");
            this.gold += currentQuest.getGoldReward();
            // On pourrait aussi ajouter la réputation ici
            inventory.remove(currentQuest.getProofItem());
            currentQuest.complete();
            this.currentQuest = null;
        } else if (currentQuest != null) {
            System.out.println("Vous n'avez pas la preuve requise (" + currentQuest.getProofItem() + ") !");
        } else {
            System.out.println("Vous n'avez pas de quête en cours.");
        }
    }
    
    // Pour les tests, on va simuler la trouvaille d'un objet
    public void findItem(String item) {
        System.out.println(name + " a trouvé : " + item);
        this.inventory.add(item);
    }

    // Méthode appelée à chaque heure qui passe
    public void updateVitalSigns() {
        hunger -= 1.5;
        thirst -= 2.5;
        energy -= 1.0;
        
        if (hunger <= 0 || thirst <= 0) {
            hp -= 5;
            System.out.println(name + " est en train de mourir de faim ou de soif !");
        }
    }

    public int getRank() {
        return rank;
    }

    @Override
    public String toString() {
        return "Adventurer{" +
                "name='" + name + '\'' +
                ", rank=" + rank +
                ", hp=" + hp +
                ", gold=" + gold +
                ", hunger=" + Math.round(hunger) +
                ", thirst=" + Math.round(thirst) +
                ", energy=" + Math.round(energy) +
                ", quest=" + (currentQuest != null ? currentQuest.getTitle() : "aucune") +
                ", inventory=" + inventory +
                '}';
    }
}
