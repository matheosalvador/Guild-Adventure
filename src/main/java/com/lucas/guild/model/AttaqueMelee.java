package com.lucas.guild.model;

/**
 * Représente une attaque de mêlée (corps à corps).
 */
public class AttaqueMelee extends Attaque {

    // No-arg constructor for Gson deserialization
    public AttaqueMelee() {
        super("", 0);
    }

    /**
     * Constructeur d'une attaque de mêlée.
     * @param nom Le nom de l'attaque.
     * @param degats Les dégâts de l'attaque.
     */
    public AttaqueMelee(String nom, int degats) {
        super(nom, degats);
    }

    @Override
    public void executer(Entite attaquant, Entite cible) {
        System.out.println(attaquant.getName() + " utilise " + nom + " sur " + cible.getName() + " !");
        if (cible instanceof Combatant) {
            ((Combatant) cible).subirDegats(degats);
        } else {
            // Fallback pour les entités qui ne seraient pas des combattants
            cible.perdreVie(degats);
            System.out.println(cible.getName() + " a subi " + degats + " dégâts.");
        }
    }
}