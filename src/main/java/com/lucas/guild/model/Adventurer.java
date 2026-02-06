package com.lucas.guild.model;

import java.util.ArrayList;
import java.util.List;

public class Adventurer extends Entite {
    private String location;
    private String origin;
    private int rank; // Changé en int
    private List<Item> inventory;
    private SkillTree skillTree;
    private int skillPoints;
    private int experience;
    private int level;
    private Arme equippedWeapon;
    private Armure equippedArmor;


    public Adventurer(String name, String location) {
        super(name, 100); // Initial HP
        this.location = location;
        this.origin = "Village de départ"; // Exemple
        this.rank = 0; // 0 = Débutant / Rang F
        this.inventory = new ArrayList<>();
        this.skillTree = new SkillTree(location); // Utilise la location comme classe
        this.skillPoints = 0;
        this.experience = 0;
        this.level = 1;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getOrigin() {
        return origin;
    }

    public int getRank() {
        return rank;
    }

    public List<Item> getInventory() {
        return inventory;
    }

    public void addItem(Item item) {
        inventory.add(item);
    }

    public void removeItem(Item item) {
        inventory.remove(item);
    }

    public Item findItem(Item itemToFind) {
        for (Item item : inventory) {
            if (item.equals(itemToFind)) {
                return item;
            }
        }
        return null;
    }

    public int getBonusDegats() {
        int bonus = 0;
        if (equippedWeapon != null) {
            bonus += equippedWeapon.getBonusDegats();
        }
        // On pourrait ajouter d'autres bonus ici (armure, compétences, etc.)
        return bonus;
    }

    public SkillTree getSkillTree() {
        return skillTree;
    }

    public boolean hasLearnedSkill(String skillName) {
        for (Skill skill : skillTree.getSkills()) {
            if (skill.getName().equals(skillName) && skill.isLearned()) {
                return true;
            }
        }
        return false;
    }

    public int getSkillPoints() {
        return skillPoints;
    }

    public int getExperience() {
        return experience;
    }

    public void addExperience(int amount) {
        this.experience += amount;
        // Logique de montée de niveau simple
        if (experience >= level * 100) {
            level++;
            experience = 0;
            maxHealth += 10;
            health = maxHealth;
            skillPoints++;
            System.out.println(name + " est monté au niveau " + level + " !");
        }
    }

    public int getLevel() {
        return level;
    }

    @Override
    public void updateVitalSigns() {
        // Pour l'aventurier, la vie ne se régénère pas automatiquement pour l'instant
    }
}
