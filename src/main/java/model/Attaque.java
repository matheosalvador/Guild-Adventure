package model;

public abstract class Attaque {

    protected String nom;
    protected int degats;

    public Attaque(String nom, int degats) {
        this.nom = nom;
        this.degats = degats;
    }

    public String getNom() {
        return nom;
    }

    public int getDegats() {
        return degats;
    }

    public abstract void executer(Entite attaquant, Entite cible);
}
