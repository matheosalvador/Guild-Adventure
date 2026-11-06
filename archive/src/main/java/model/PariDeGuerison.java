package model;
// a neutraliser plus tard si inutile
import java.util.Random;
import java.util.Scanner;

public class PariDeGuerison extends Sort {

    private transient final Random random = new Random(); // Ignoré par Gson

    public PariDeGuerison() {
        super("Pari de Guérison", 0, 25, 4); // Pas de dégâts, coût stamina, cooldown
    }

    @Override
    public void lancer(Joueur joueur, Entite cible, Scanner scanner) {
        if (!peutEtreLance(joueur)) {
            System.out.println("Impossible de lancer " + nom + " (stamina ou cooldown) !");
            return;
        }

        joueur.perdreStamina(coutStamina);
        cooldownRestant = cooldownMax;

        int prediction = random.nextInt(31); // 0 à 30
        System.out.println("\n--- Pari de Guérison ---");
        System.out.println("Une prédiction mystique apparaît : [" + prediction + "]");
        System.out.println("Appuyez sur [Entrée] pour tenter votre chance et arrêter le flux magique !");
        scanner.nextLine(); // Attend l'entrée du joueur

        int resultat = random.nextInt(31); // 0 à 30
        int difference = Math.abs(prediction - resultat);
        int soinDeBase = 30 - difference; // Plus on est proche, plus le soin de base est élevé

        System.out.println("Le flux s'est arrêté à : [" + resultat + "]");
        System.out.println("Différence : " + difference);

        if (difference == 0) {
            System.out.println("Incroyable ! Harmonie parfaite ! Vous recevez un soin critique !");
            joueur.ajouterVie(60); // Soin critique pour un résultat parfait
            System.out.println(joueur.getName() + " récupère 60 PV !");
        } else if (difference <= 5) {
            System.out.println("Belle intuition ! Le soin est puissant.");
            joueur.ajouterVie(soinDeBase + 10); // Bonus pour un bon résultat
            System.out.println(joueur.getName() + " récupère " + (soinDeBase + 10) + " PV.");
        } else if (difference <= 15) {
            System.out.println("Un soin modeste mais bienvenu.");
            joueur.ajouterVie(soinDeBase);
            System.out.println(joueur.getName() + " récupère " + soinDeBase + " PV.");
        } else {
            System.out.println("Le flux magique est instable... Le sort échoue presque.");
            joueur.ajouterVie(5); // Soin minimal en cas d'échec
            System.out.println(joueur.getName() + " ne récupère que 5 PV.");
        }
    }
}