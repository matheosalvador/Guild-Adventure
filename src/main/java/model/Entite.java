package model;

/**
 * Classe abstraite représentant une entité vivante dans le jeu (Joueur, Monstre).
 */
public abstract class Entite {

    protected String name;
    protected int health;
    protected int maxHealth;

    /**
     * Constructeur protégé pour les entités.
     * @param name Nom de l'entité.
     * @param maxHealth Santé maximale de l'entité.
     */
    protected Entite(String name, int maxHealth) {
        this.name = name;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
    }

    /**
     * Récupère le nom de l'entité.
     * @return Le nom.
     */
    public String getName() {
        return name;
    }

    /**
     * Récupère la santé actuelle.
     * @return La santé actuelle.
     */
    public int getHealth() {
        return health;
    }

    /**
     * Récupère la santé maximale.
     * @return La santé maximale.
     */
    public int getMaxHealth() {
        return maxHealth;
    }

    /**
     * Définit le nom de l'entité.
     * @param name Le nouveau nom.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Ajoute des points de vie à l'entité.
     * @param montant Le montant de vie à ajouter.
     */
    public void ajouterVie(int montant) {
        if (montant <= 0) return;
        health = Math.min(health + montant, maxHealth);
    }

    /**
     * Retire des points de vie à l'entité.
     * @param montant Le montant de vie à retirer.
     */
    public void perdreVie(int montant) {
        if (montant <= 0) return;
        health = Math.max(health - montant, 0);
    }

    /**
     * Vérifie si l'entité est vivante.
     * @return true si la santé est supérieure à 0, false sinon.
     */
    public boolean estVivant() {
        return health > 0;
    }

    /**
     * Définit la santé de l'entité.
     * @param health La nouvelle santé.
     */
    public abstract void setHealth(int health);

    /**
     * Définit le niveau de l'entité.
     * @param level Le nouveau niveau.
     */
    public abstract void setLevel(int level);

    /**
     * Récupère le niveau de l'entité.
     * @return Le niveau.
     */
    public abstract int getLevel();
}
