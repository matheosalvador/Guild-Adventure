package model;

public class Monstre {

    private String name;
    private Integer health;
    private boolean isAlive;

    public Monstre(String name) {
        this.name = name;
        this.health = 100;
        isAlive = true;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getHealth() {
        return health;
    }

    public void setHealth(Integer health) {
        this.health = health;
    }

    public boolean estVivant() {
        return estVivant();
    }

    public void setAlive(boolean alive) {
        isAlive = alive;
    }
}

