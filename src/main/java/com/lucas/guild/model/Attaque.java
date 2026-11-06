package com.lucas.guild.model;

/**
 * Classe abstraite représentant une attaque.
 */
public abstract class Attaque {

    protected String nom;
    protected int degats;

    /**
     * Constructeur d'une attaque.
     * @param nom Le nom de l'attaque.
     * @param degats Les dégâts de base de l'attaque.
     */
    public Attaque(String nom, int degats) {
        this.nom = nom;
        this.degats = degats;
    }

    /**
     * Récupère le nom de l'attaque.
     * @return Le nom.
     */
    public String getNom() {
        return nom;
    }

    /**
     * Récupère les dégâts de l'attaque.
     * @return Les dégâts.
     */
    public int getDegats() {
        return degats;
    }

    /**
     * Exécute l'attaque.
     * @param attaquant L'entité qui attaque.
     * @param cible L'entité cible.
     */
    public abstract void executer(Entite attaquant, Entite cible);
}
