package com.lucas.guild.model;

import java.util.Scanner;

public abstract class Sort {

    protected String nom;
    protected int degats;
    protected int coutStamina;
    protected int cooldownMax;
    protected int cooldownRestant;

    public Sort(String nom, int degats, int coutStamina, int cooldown) {
        this.nom = nom;
        this.degats = degats;
        this.coutStamina = coutStamina;
        this.cooldownMax = cooldown;
        this.cooldownRestant = 0;
    }

    public String getNom() { return nom; }
    public int getDegats() { return degats; }
    public int getCoutStamina() { return coutStamina; }
    public int getCooldownRestant() { return cooldownRestant; }

    public boolean peutEtreLance(Joueur joueur) {
        return joueur.getStamina() >= coutStamina && cooldownRestant == 0;
    }

    public void decrementerCooldown() {
        if (cooldownRestant > 0) cooldownRestant--;
    }

    // La méthode lancer accepte maintenant un Scanner pour les sorts interactifs
    public abstract void lancer(Joueur joueur, Entite cible, Scanner scanner);
}