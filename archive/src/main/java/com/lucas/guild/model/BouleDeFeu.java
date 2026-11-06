package com.lucas.guild.model;

import java.util.Scanner;

public class BouleDeFeu extends Sort {

    public BouleDeFeu() {
        super("Boule de feu", 25, 20, 3);
    }

    @Override
    public void lancer(Joueur joueur, Entite cible, Scanner scanner) { // Scanner ajouté mais non utilisé
        if (!peutEtreLance(joueur)) {
            System.out.println("Impossible de lancer " + nom + " !");
            return;
        }

        joueur.perdreStamina(coutStamina);
        if (cible instanceof Combatant) {
            ((Combatant) cible).subirDegats(degats);
        } else {
            cible.perdreVie(degats);
        }
        cooldownRestant = cooldownMax;
        System.out.println(joueur.getName() + " lance " + nom + " sur " + cible.getName() + " !");
    }
}