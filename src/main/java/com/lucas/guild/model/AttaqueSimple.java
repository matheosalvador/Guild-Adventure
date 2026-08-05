package com.lucas.guild.model;

public class AttaqueSimple extends Attaque {

    // No-arg constructor for Gson deserialization
    public AttaqueSimple() {
        super("", 0);
    }

    public AttaqueSimple(String nom, int degats) {
        super(nom, degats);
    }

    @Override
    public void executer(Entite attaquant, Entite cible) {
        int totalDamage = this.degats;
        
        // Si l'attaquant est le joueur, on ajoute le bonus de son arme
        if (attaquant instanceof Adventurer) {
            totalDamage += ((Adventurer) attaquant).getBonusDegats();
        }
        
        System.out.println(attaquant.getName() + " utilise " + this.nom + " !");
        // La méthode perdreVie de la cible appliquera la réduction de l'armure
        cible.perdreVie(totalDamage);
    }
}