package com.lucas.guild.model;

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
    /** Or du personnage, en Po. */
    private int gold;

    /** Numéro de jour de la naissance (zéro ou négatif pour un personnage né avant le début de la partie). */
    private int birthDay;

    // Effets de l'âge : recalculés à chaque chargement (refreshAging), donc pas sauvegardés
    private transient double healthFactor = 1.0;
    private transient double speedFactor = 1.0;
    private transient String agingLabel;


    // No-arg constructor for Gson deserialization
    public Adventurer() {
        super();
    }

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

    /** Équipe un objet de l'inventaire (arme, armure, sac). @return null si c'est fait, sinon la raison du refus. */
    public String equip(Item item) {
        return inventory.equip(item);
    }

    /** Retire un équipement. @return null si c'est fait, sinon la raison du refus. */
    public String unequip(Inventaire.Slot slot) {
        return inventory.unequip(slot);
    }

    public int getBonusDegats() {
        int bonus = 0;
        Arme weapon = inventory != null ? inventory.getWeapon() : null;
        if (weapon != null) {
            bonus += weapon.getBonusDegats();
        }
        // On pourrait ajouter d'autres bonus ici (armure, compétences, etc.)
        return bonus;
    }

    /** Réduction de dégâts de l'armure portée. */
    public int getDefense() {
        Armure armor = inventory != null ? inventory.getArmor() : null;
        return armor != null ? armor.getReductionDegats() : 0;
    }

    // ------------------------------------------------------------------ or

    public int getGold() {
        return gold;
    }

    public void addGold(int amount) {
        if (amount > 0) {
            gold += amount;
        }
    }

    /** Dépense de l'or. @return false (rien n'est retiré) si le personnage n'en a pas assez. */
    public boolean spendGold(int amount) {
        if (amount < 0 || amount > gold) {
            return false;
        }
        gold -= amount;
        return true;
    }

    public SkillTree getSkillTree() {
        return skillTree;
    }

    public void setSkillTree(SkillTree skillTree) {
        this.skillTree = skillTree;
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
            health = getMaxHealth();
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

    // ------------------------------------------------------------------ âge

    public int getBirthDay() {
        return birthDay;
    }

    public void setBirthDay(int birthDay) {
        this.birthDay = birthDay;
    }

    /** Âge en années entières au jour {@code currentDay}. */
    public int getAge(int currentDay) {
        return Math.max(0, Math.floorDiv(currentDay - birthDay, GameCalendar.standard().daysPerYear()));
    }

    /** Vrai si le personnage fête un anniversaire ce jour-là. */
    public boolean isBirthday(int currentDay) {
        int sinceBirth = currentDay - birthDay;
        return sinceBirth > 0 && Math.floorMod(sinceBirth, GameCalendar.standard().daysPerYear()) == 0;
    }

    /**
     * Applique les effets de l'âge (vie max, charge, vitesse) au jour {@code currentDay}.
     * À appeler à chaque nouveau jour et après un chargement.
     *
     * @return un message si le personnage vient de passer à un palier de vieillesse plus avancé, sinon null
     */
    public String refreshAging(int currentDay) {
        AgingStage stage = AgingStage.forAge(getAge(currentDay));
        String message = null;
        if (agingLabel != null && !agingLabel.equals(stage.label)) {
            message = "Les années se font sentir : " + stage.label + ".";
        }
        agingLabel = stage.label;
        healthFactor = stage.healthFactor();
        speedFactor = stage.speedFactor();
        if (inventory != null) {
            inventory.setCapacityFactor(stage.carryFactor());
        }
        health = Math.min(health, getMaxHealth());
        return message;
    }

    /** Multiplicateur de vitesse de déplacement : l'âge, puis l'encombrement dû à la charge (1.0 = pleine vitesse). */
    public double getSpeedFactor() {
        double encumbrance = inventory != null ? inventory.getEncumbranceSpeedFactor() : 1.0;
        return speedFactor * encumbrance;
    }

    /** Vie max réelle : la vie de base réduite par l'âge. */
    @Override
    public int getMaxHealth() {
        return Math.max(1, (int) Math.round(maxHealth * healthFactor));
    }

    public void updateVitalSigns() {
        // Pour l'aventurier, la vie ne se régénère pas automatiquement pour l'instant
    }

    @Override
    public void heal(int amount) {
        ajouterVie(amount);
    }
}
