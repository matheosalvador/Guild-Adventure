package com.lucas.guild.model;

public class Allie extends Entite implements Combatant {

    private int degats;

    public Allie(String nom, int maxHealth, int degats) {
        super(nom, maxHealth);
        this.degats = degats;
    }

    @Override
    public void attaquer(Combatant cible) {
        if (this.estVivant() && cible instanceof Entite) {
            System.out.println("-> " + this.getName() + " attaque " + ((Entite) cible).getName() + " !");
            cible.subirDegats(this.degats);
        }
    }

    @Override
    public void subirDegats(int montant) {
        this.perdreVie(montant);
        System.out.println("-> " + this.getName() + " a subi " + montant + " dégâts !");
        if (!this.estVivant()) {
            System.out.println("-> " + this.getName() + " a été vaincu !");
        }
    }

    @Override
    public void heal(int maxHealth) {

    }
}