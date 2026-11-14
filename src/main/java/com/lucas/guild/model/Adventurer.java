package com.lucas.guild.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.ProfileData;

public class Adventurer extends Entite {
    private String location;
    private String origin;
    private int rank; // Changé en int
    private Inventaire inventory;
    /** Or du personnage, en Po. */
    private int gold;

    /** Numéro de jour de la naissance (zéro ou négatif pour un personnage né avant le début de la partie). */
    private int birthDay;

    /** Classe et origine choisies à la création (classes.json, backgrounds.json) ; null pour une ancienne sauvegarde. */
    private String classId;
    private String backgroundId;

    /** Le lieu (la ville) où se trouve le personnage : identifiant dans world.json ; null pour une ancienne sauvegarde. */
    private String townId;
    /** Les lieux déjà visités. */
    private List<String> visitedPlaces = new ArrayList<>();
    /** La dernière ville où le personnage a mis les pieds : c'est là qu'il se réveille s'il tombe en route. */
    private String homeTownId;
    /** Coffres ouverts : identifiant du coffre -> jour de l'ouverture (ils se remplissent de nouveau plus tard). */
    private java.util.Map<String, Integer> openedChests = new java.util.HashMap<>();

    /** Titres des quêtes acceptées et pas encore rendues, puis des quêtes terminées. */
    private List<String> activeQuests = new ArrayList<>();
    private List<String> doneQuests = new ArrayList<>();

    /** Compétences, gagnées par la pratique. */
    private Skills skills = new Skills();

    // Effets de l'âge : recalculés à chaque chargement (refreshAging), donc pas sauvegardés
    private transient double healthFactor = 1.0;
    private transient double speedFactor = 1.0;
    private transient String agingLabel;
    // Bonus de la classe et de l'origine : recalculés à chaque chargement (refreshBonuses)
    private transient double profileSpeedPercent = 0;
    // Reçoit les messages du personnage (compétence gagnée...) ; branché par la partie en cours
    private transient Consumer<String> notifier;

    /** Points de vie de base d'un nouveau personnage, avant les bonus de classe et d'origine. */
    private static final int BASE_HEALTH = 100;


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
        int fromArmor = armor != null ? armor.getReductionDegats() : 0;
        return fromArmor + (int) Math.floor(getSkillBonus("defenseBonus"));
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

    /** Dégâts d'un coup : 10 de base plus l'arme, augmentés par la compétence Mêlée. */
    public int getAttackDamage() {
        return (int) Math.round((10 + getBonusDegats()) * (1 + getSkillBonus("meleePercent") / 100.0));
    }

    public List<Attaque> getAttaques() {
        List<Attaque> attaques = new ArrayList<>();
        attaques.add(new AttaqueMelee("Attaque de base", getAttackDamage()));
        return attaques;
    }

    // ------------------------------------------------------------------ lieu

    /** Où se trouve le personnage (la ville de départ pour une ancienne sauvegarde ou un lieu supprimé des données). */
    public String getTownId() {
        if (townId == null || GameContent.world().place(townId) == null) {
            townId = GameContent.world().startPlace;
        }
        return townId;
    }

    public void setTownId(String townId) {
        this.townId = townId;
        com.lucas.guild.data.WorldData.PlaceData place = GameContent.world().place(townId);
        if (place != null && place.zoneFile == null) {
            this.homeTownId = townId;
        }
    }

    /** La dernière ville visitée (la ville de départ si le personnage n'a pas encore quitté sa première ville). */
    public String getHomeTownId() {
        com.lucas.guild.data.WorldData world = GameContent.world();
        com.lucas.guild.data.WorldData.PlaceData home = homeTownId != null ? world.place(homeTownId) : null;
        if (home == null || home.zoneFile != null) {
            home = world.place(getTownId());
            if (home == null || home.zoneFile != null) {
                return world.startPlace;
            }
        }
        return home.id;
    }

    /** Les coffres ouverts. Jamais null, même pour une ancienne sauvegarde. */
    public java.util.Map<String, Integer> getOpenedChests() {
        if (openedChests == null) {
            openedChests = new java.util.HashMap<>();
        }
        return openedChests;
    }

    /** Les lieux déjà visités. Jamais null. */
    public List<String> getVisitedPlaces() {
        if (visitedPlaces == null) {
            visitedPlaces = new ArrayList<>();
        }
        return visitedPlaces;
    }

    public void markVisited(String placeId) {
        if (!getVisitedPlaces().contains(placeId)) {
            getVisitedPlaces().add(placeId);
        }
    }

    // ------------------------------------------------------------------ quêtes

    /** Quêtes en cours (titres). Jamais null, même pour une ancienne sauvegarde. */
    public List<String> getActiveQuests() {
        if (activeQuests == null) {
            activeQuests = new ArrayList<>();
        }
        return activeQuests;
    }

    /** Quêtes terminées (titres). Jamais null, même pour une ancienne sauvegarde. */
    public List<String> getDoneQuests() {
        if (doneQuests == null) {
            doneQuests = new ArrayList<>();
        }
        return doneQuests;
    }

    /** Boit la première potion de soin de l'inventaire (touche Q). @return la phrase à afficher */
    public String drinkHealingPotion() {
        for (Item item : inventory.getItems()) {
            if (item.estUtilisableHorsCombat()) {
                return useHealingItem(item);
            }
        }
        return "Vous n'avez pas de potion de soin.";
    }

    // ------------------------------------------------------------------ classe, origine, compétences

    public String getClassId() {
        return classId;
    }

    public String getBackgroundId() {
        return backgroundId;
    }

    /** Libellé de la classe (« Guerrier »), ou null pour un personnage sans classe. */
    public String getClassLabel() {
        ProfileData profile = GameContent.characterClass(classId);
        return profile != null ? profile.label : null;
    }

    /** Libellé de l'origine (« Noble déchu »), ou null pour un personnage sans origine. */
    public String getBackgroundLabel() {
        ProfileData profile = GameContent.background(backgroundId);
        return profile != null ? profile.label : null;
    }

    /**
     * Applique les choix de la création : vie de départ, compétences de départ, bonus.
     * Un identifiant inconnu ou null veut dire « pas de classe » / « pas d'origine ».
     */
    public void chooseProfile(String classId, String backgroundId) {
        ProfileData characterClass = GameContent.characterClass(classId);
        ProfileData background = GameContent.background(backgroundId);
        this.classId = characterClass != null ? classId : null;
        this.backgroundId = background != null ? backgroundId : null;

        int bonusHealth = 0;
        for (ProfileData profile : new ProfileData[] {characterClass, background}) {
            if (profile != null) {
                bonusHealth += profile.healthBonus;
            }
        }
        this.maxHealth = Math.max(10, BASE_HEALTH + bonusHealth);

        refreshBonuses();
        for (ProfileData profile : new ProfileData[] {characterClass, background}) {
            if (profile != null) {
                for (Map.Entry<String, Integer> start : profile.skillStart.entrySet()) {
                    skills.setLevel(start.getKey(), start.getValue());
                }
            }
        }
        refreshBonuses();
        this.health = getMaxHealth();
    }

    /**
     * Recalcule les bonus de la classe, de l'origine et des compétences (vitesse, charge, progression).
     * À appeler après la création, après un chargement et à chaque niveau de compétence gagné.
     */
    public void refreshBonuses() {
        if (skills == null) {
            skills = new Skills();
        }
        Map<String, Integer> practiceBonus = new HashMap<>();
        double carry = 0;
        double speed = 0;
        for (ProfileData profile : new ProfileData[] {GameContent.characterClass(classId), GameContent.background(backgroundId)}) {
            if (profile == null) {
                continue;
            }
            carry += profile.carryBonus;
            speed += profile.speedBonusPercent;
            for (Map.Entry<String, Integer> bonus : profile.practiceBonusPercent.entrySet()) {
                Integer sum = practiceBonus.get(bonus.getKey());
                practiceBonus.put(bonus.getKey(), (sum != null ? sum : 0) + bonus.getValue());
            }
        }
        skills.bind(practiceBonus);
        profileSpeedPercent = speed;
        if (inventory != null) {
            inventory.setBaseBonus(carry + skills.bonus("carryKg"));
        }
        health = Math.min(health, getMaxHealth());
    }

    public Skills getSkills() {
        return skills;
    }

    /** Effet cumulé des compétences (voir {@code SkillData.EFFECTS}), par exemple « buyDiscountPercent ». */
    public double getSkillBonus(String effect) {
        return skills != null ? skills.bonus(effect) : 0;
    }

    /** Le personnage envoie ses messages (compétence gagnée...) à ce récepteur ; {@code null} pour les ignorer. */
    public void setNotifier(Consumer<String> notifier) {
        this.notifier = notifier;
    }

    /**
     * Fait progresser une compétence par la pratique. Un niveau gagné applique tout de suite ses
     * effets (une vie max plus haute soigne d'autant) et envoie un message au joueur.
     *
     * @param amount points de pratique (dans l'unité de la compétence : mètres, Po, potions...)
     */
    public void train(String skillId, double amount) {
        int maxBefore = getMaxHealth();
        String message = skills.train(skillId, amount);
        if (message == null) {
            return;
        }
        refreshBonuses();
        int gain = getMaxHealth() - maxBefore;
        if (gain > 0) {
            health = Math.min(getMaxHealth(), health + gain);
        }
        if (notifier != null) {
            notifier.accept(message);
        }
    }

    /**
     * Utilise une potion de soin de l'inventaire. Refusé si la vie est déjà pleine (on ne gaspille
     * pas la potion). La compétence Soins augmente ce qu'elle rend, et progresse avec l'usage.
     *
     * @return le message à afficher au joueur
     */
    public String useHealingItem(Item item) {
        if (health >= getMaxHealth()) {
            return "Vous êtes déjà en pleine forme.";
        }
        int before = health;
        item.use(this, null);
        removeItem(item);
        int gained = health - before;
        int extra = (int) Math.round(gained * getSkillBonus("healBonusPercent") / 100.0);
        if (extra > 0) {
            ajouterVie(extra);
        }
        String message = item.getName() + " utilisée : +" + (health - before) + " PV.";
        train("healing", 1);
        return message;
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

    /** Palier de vieillesse en toutes lettres, ou null avant le premier calcul. */
    public String getAgingLabel() {
        return agingLabel;
    }

    /**
     * Multiplicateur de vitesse de déplacement : l'âge, l'encombrement dû à la charge, puis les bonus
     * de classe, d'origine et de compétence (1.0 = pleine vitesse).
     */
    public double getSpeedFactor() {
        double encumbrance = inventory != null ? inventory.getEncumbranceSpeedFactor() : 1.0;
        double bonus = Math.max(0.5, 1 + (profileSpeedPercent + getSkillBonus("speedPercent")) / 100.0);
        return speedFactor * encumbrance * bonus;
    }

    /** Vie max réelle : la vie de base et les bonus de compétence, réduits par l'âge. */
    @Override
    public int getMaxHealth() {
        return Math.max(1, (int) Math.round((maxHealth + getSkillBonus("healthBonus")) * healthFactor));
    }

    public void updateVitalSigns() {
        // Pour l'aventurier, la vie ne se régénère pas automatiquement pour l'instant
    }

    @Override
    public void heal(int amount) {
        ajouterVie(amount);
    }
}
