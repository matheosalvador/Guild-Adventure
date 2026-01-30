package model;

/**
 * Représente une entité de base dans le jeu, comme un joueur, un monstre ou un allié.
 * Possède des attributs fondamentaux tels que le nom et les points de vie.
 */
public abstract class Entite {

    protected String name;
    protected int health;
    protected int maxHealth;

    /**
     * Constructeur pour créer une entité avec un nom et des points de vie maximum.
     * La santé actuelle est initialisée au maximum.
     *
     * @param name      Le nom de l'entité.
     * @param maxHealth La quantité maximale de points de vie.
     */
    protected Entite(String name, int maxHealth) {
        this.name = name;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
    }

    public Entite() {

    }

    public String getName() {
        return name;
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return maxHealth;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Augmente les points de vie de l'entité sans dépasser le maximum.
     *
     * @param montant La quantité de vie à ajouter.
     */
    public void ajouterVie(int montant) {
        if (montant <= 0) return;
        health = Math.min(health + montant, maxHealth);
    }

    /**
     * Diminue les points de vie de l'entité sans descendre en dessous de zéro.
     *
     * @param montant La quantité de vie à retirer.
     */
    public void perdreVie(int montant) {
        if (montant <= 0) return;
        health = Math.max(health - montant, 0);
    }

    /**
     * Vérifie si l'entité est toujours en vie.
     *
     * @return true si les points de vie sont supérieurs à zéro, sinon false.
     */
    public boolean estVivant() {
        return health > 0;
    }
}