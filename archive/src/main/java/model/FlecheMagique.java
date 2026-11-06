package model;
// a neutraliser plus tard si inutile
import java.util.Scanner;

public class FlecheMagique extends Sort {

    public FlecheMagique() {
        super("Flèche magique", 15, 15, 2);
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