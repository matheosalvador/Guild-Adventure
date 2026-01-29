package model;

public abstract class Entite {

    protected String name;
    protected int health;
    protected int maxHealth;

    protected Entite(String name, int maxHealth) {
        this.name = name;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
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


    public void ajouterVie(int montant) {
        if (montant <= 0) return;
        health = Math.min(health + montant, maxHealth);
    }

    public void perdreVie(int montant) {
        if (montant <= 0) return;
        health = Math.max(health - montant, 0);
    }

    public boolean estVivant() {
        return health > 0;
    }

    public abstract void setHealth(int health);

    public abstract void setLevel(int level);

    public abstract int getLevel();
}
