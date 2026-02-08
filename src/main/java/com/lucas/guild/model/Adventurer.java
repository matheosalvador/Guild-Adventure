package com.lucas.guild.model;

import com.badlogic.gdx.physics.bullet.collision.gim_bitset;

import java.util.ArrayList;
import java.util.List;

public class Adventurer extends Entite {
    private String location;
    private String origin;
    private int rank; // Changé en int
    private Inventaire inventory;
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
        this.inventory = new Inventaire();
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

    public Inventaire getInventaire() {
        return inventory;
    }

    public List<Item> getInventoryItems() {
        return inventory.getItems();
    }

    public void setInventory(Inventaire inventory) {
        this.inventory = inventory;
    }

    public boolean addItem(Item item) {
        return inventory.ajouterItem(item);
    }

    public void removeItem(Item item) {
        inventory.retirerItem(item);
    }

    public Item findItem(Item itemToFind) {
        for (Item item : inventory.getItems()) {
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

    public List<Attaque> getAttaques() {
        List<Attaque> attaques = new ArrayList<>();
        attaques.add(new AttaqueMelee("Attaque de base", 10 + getBonusDegats()));
        for (Skill skill : skillTree.getSkills()) {
            if (skill.isLearned() && skill.getAttackToLearn() != null) {
                attaques.add(skill.getAttackToLearn());
            }
        }
        return attaques;
    }

    public void updateVitalSigns() {
        // Pour l'aventurier, la vie ne se régénère pas automatiquement pour l'instant
    }

    public gim_bitset getInventory() {
        return null;
    }

    @Override
    public void heal(int maxHealth) {

    }
}
