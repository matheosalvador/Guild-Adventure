package com.lucas.guild.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Adventurer extends Entite {
    
    private String origin;
    private int age = 18;
    private int rank = 0;
    private int experience = 0;
    private int experienceToNextRank = 100;
    private int skillPoints = 0;
    private List<String> learnedSkillNames = new ArrayList<>();
    private double hunger = 100.0;
    private double thirst = 100.0;
    private double energy = 100.0;
    private int gold = 10;
    private Quest currentQuest = null;
    private List<Item> inventory = new ArrayList<>();
    private Arme armeEquipee;
    private Armure armureEquipee;

    public Adventurer(String name, String origin) {
        super(name, 100);
        this.origin = origin;
        applyOriginBonus();
        this.inventory.add(new ObjetDeQuete("Vieux pain"));
        this.inventory.add(new PotionDeSoin("Petite Potion", 30));
        this.apprendreAttaque(new AttaqueSimple("Coup de poing", 5));
        this.armeEquipee = new Arme("Épée rouillée", 10);
        this.armureEquipee = new Armure("Vêtements usés", 2);
    }
    
    public Adventurer() {
        super("Joueur", 100);
    }

    public List<Item> getInventory() {
        return inventory;
    }

    public int getExperience() { return experience; }
    public int getExperienceToNextRank() { return experienceToNextRank; }
    public double getHunger() { return hunger; }
    public double getThirst() { return thirst; }
    public double getEnergy() { return energy; }
    public int getGold() { return gold; }
    public Arme getArmeEquipee() { return armeEquipee; }
    public Armure getArmureEquipee() { return armureEquipee; }
    private void rankUp() {
        rank++;
        experience -= experienceToNextRank;
        experienceToNextRank *= 2;
        this.maxHealth += 20;
        this.health = this.maxHealth;
        this.skillPoints++;
        System.out.println("***********************************");
        System.out.println("MONTÉE DE RANG ! Vous êtes maintenant Rang " + getRankLetter());
        System.out.println("PV Max: " + this.maxHealth + " | Vous gagnez 1 point de compétence !");
        System.out.println("***********************************");
    }
    public void learnSkill(Skill skill) {
        if (skillPoints <= 0) { System.out.println("Pas de points de compétence."); return; }
        if (hasLearnedSkill(skill.getName())) { System.out.println("Compétence déjà connue."); return; }
        if (skill.getPrerequisiteSkillName() != null && !hasLearnedSkill(skill.getPrerequisiteSkillName())) {
            System.out.println("Prérequis non rempli : " + skill.getPrerequisiteSkillName());
            return;
        }
        skillPoints--;
        apprendreAttaque(skill.getAttackToLearn());
        learnedSkillNames.add(skill.getName());
        System.out.println("Compétence apprise : " + skill.getName());
    }
    public boolean hasLearnedSkill(String skillName) {
        if (skillName == null) return true;
        return learnedSkillNames.contains(skillName);
    }
    public String getOrigin() { return origin; }
    public int getSkillPoints() { return skillPoints; }
    public void gainExperience(int amount) {
        this.experience += amount;
        System.out.println(name + " gagne " + amount + " XP !");
        while (experience >= experienceToNextRank) rankUp();
    }
    public String getRankLetter() {
        switch(rank) {
            case 0: return "F"; case 1: return "E"; case 2: return "D"; case 3: return "C";
            case 4: return "B"; case 5: return "A"; case 6: return "S"; default: return "Légende";
        }
    }
    
    @Override
    public void perdreVie(int montant) {
        int reduction = (armureEquipee != null) ? armureEquipee.getReductionDegats() : 0;
        int degatsReels = Math.max(1, montant - reduction);
        this.health = Math.max(0, this.health - degatsReels);
        System.out.println(this.name + " subit " + degatsReels + " dégâts !");
    }

    public void buyItem(Item item, int price) {
        if (gold >= price) {
            gold -= price;
            inventory.add(item);
            System.out.println("Achat : " + item.getName());
        } else {
            System.out.println("Pas assez d'or !");
        }
    }
    public void removeItem(Item item) { inventory.remove(item); }
    public void completeQuest() {
        if (currentQuest == null) { System.out.println("Pas de quête."); return; }
        Item proof = inventory.stream().filter(i -> i instanceof ObjetDeQuete && i.getName().equals(currentQuest.getProofItem())).findFirst().orElse(null);
        if (proof != null) {
            System.out.println("Quête '" + currentQuest.getTitle() + "' terminée !");
            this.gold += currentQuest.getGoldReward();
            gainExperience(currentQuest.getXpReward());
            inventory.remove(proof);
            currentQuest.complete();
            this.currentQuest = null;
        } else {
            System.out.println("Preuve non trouvée.");
        }
    }
    public void findItem(Item item) {
        System.out.println("Trouvé : " + item.getName());
        this.inventory.add(item);
    }
    private void applyOriginBonus() {
        if ("Caserne".equals(origin)) {
            this.maxHealth = 120;
            this.health = 120;
        }
    }
    public boolean sleepInInn() {
        if (gold >= 10) {
            gold -= 10; energy = 100; health = maxHealth;
            System.out.println("Nuit confortable.");
            return true;
        } else {
            System.out.println("Pas assez d'or.");
            return false;
        }
    }
    public void sleepRough() {
        energy = Math.min(100, energy + 40);
        if (Math.random() < 0.3) {
            int stolenGold = Math.min(gold, 15);
            gold -= stolenGold;
            System.out.println(stolenGold + " Or ont disparu !");
        } else {
            System.out.println("Dos en compote.");
        }
    }
    public void eat() {
        if (gold >= 5) { gold -= 5; hunger = Math.min(100, hunger + 50); System.out.println("Repas pris."); } 
        else { System.out.println("Pas assez d'or."); }
    }
    public void drink() {
        if (gold >= 2) { gold -= 2; thirst = Math.min(100, thirst + 60); System.out.println("Bière bue."); } 
        else { System.out.println("Pas assez d'or."); }
    }
    public void acceptQuest(Quest quest) {
        if (this.currentQuest == null) { this.currentQuest = quest; System.out.println("Quête acceptée : " + quest.getTitle()); } 
        else { System.out.println("Quête déjà en cours !"); }
    }
    public void updateVitalSigns() {
        hunger -= 1.5; thirst -= 2.5; energy -= 1.0;
        if (hunger <= 0 || thirst <= 0) { perdreVie(5); }
    }
    public int getRank() { return rank; }
    public Quest getCurrentQuest() { return currentQuest; }
    public void equiper(Item item) {
        if (item instanceof Arme) {
            if (this.armeEquipee != null) inventory.add(this.armeEquipee);
            this.armeEquipee = (Arme) item;
            inventory.remove(item);
            System.out.println("Équipé : " + item.getName());
        } else if (item instanceof Armure) {
            if (this.armureEquipee != null) inventory.add(this.armureEquipee);
            this.armureEquipee = (Armure) item;
            inventory.remove(item);
            System.out.println("Équipé : " + item.getName());
        } else {
            System.out.println("Non équipable.");
        }
    }
    public int getBonusDegats() {
        return (armeEquipee != null) ? armeEquipee.getBonusDegats() : 0;
    }
    @Override
    public String toString() {
        String equipement = "  Équipement: Arme=" + (armeEquipee != null ? armeEquipee.getName() : "Aucune") + 
                            ", Armure=" + (armureEquipee != null ? armureEquipee.getName() : "Aucune");
        return "Adventurer{name='" + name + "', rank=" + getRankLetter() + 
               " (XP: " + experience + "/" + experienceToNextRank + ")" +
               ", hp=" + health + "/" + maxHealth + ", gold=" + gold + 
               ", hunger=" + Math.round(hunger) + ", thirst=" + Math.round(thirst) + 
               ", energy=" + Math.round(energy) + "}\n" + equipement + "\n" +
               "  Inventaire: " + inventory.stream().map(Item::getName).collect(Collectors.toList());
    }
}
