package com.lucas.guild.model;

import java.util.ArrayList;
import java.util.List;

public abstract class Entite {

    protected String name;
    protected int health;
    protected int maxHealth;
    protected List<Attaque> attaques = new ArrayList<>();

    protected Entite(String name, int maxHealth) {
        this.name = name;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
    }
    
    public void apprendreAttaque(Attaque attaque) {
        this.attaques.add(attaque);
    }

    public List<Attaque> getAttaques() {
        return attaques;
    }

    public String getName() { return name; }
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }

    public void perdreVie(int montant) {
        if (montant <= 0) return;
        health = Math.max(health - montant, 0);
    }
    
    public void ajouterVie(int montant) {
        if (montant <= 0) return;
        health = Math.min(health + montant, maxHealth);
    }

    public boolean estVivant() {
        return health > 0;
    }

    public abstract void updateVitalSigns();
}
