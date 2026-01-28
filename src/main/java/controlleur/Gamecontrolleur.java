package controlleur;

import model.*;
import java.util.Scanner;

public class Gamecontrolleur {

    private Joueur joueur;
    private GestionMonstres gestionMonstres;
    private Monstre boss;

    // Sorts
    private Sort bouleDeFeu;
    private Sort flecheMagique;

    private Scanner scanner;
    private Attaque attaqueMelee;
    private Attaque attaqueMeleeMonstre;

    // Potions
    private PotionSoin potionVie;
    private PotionDps potionAttaque;
    private PotionMix potionMix;

    // Constructeur
    public Gamecontrolleur() {

        this.joueur = new Joueur("Arthur");

        this.gestionMonstres = new GestionMonstres();
        this.boss = new Monstre("Gobelin chef");
        gestionMonstres.ajouterMonstre(boss);

        this.scanner = new Scanner(System.in);

        this.attaqueMelee = new AttaqueMelee("frappe", 10);
        this.attaqueMeleeMonstre = new AttaqueMelee("coup de griffe", 8);

        this.bouleDeFeu = new BouleDeFeu();
        this.flecheMagique = new FlecheMagique();

        // Potions (avec stock interne)
        this.potionVie = new PotionSoin(joueur);
        this.potionAttaque = new PotionDps(boss);
        this.potionMix = new PotionMix(joueur, boss);
    }

    // jeu

    public void demarrerJeu() {

        System.out.println(" Combat contre le Gobelin Chef !");
        boolean menuPrincipal = true;

        while (menuPrincipal && joueur.estVivant() && gestionMonstres.resteDesMonstres()) {

            System.out.println("\n--- Menu principal ---");
            System.out.println("1. Attaque corps à corps");
            System.out.println("2. Utiliser une potion");
            System.out.println("3. Sorts");
            System.out.println("4. Fuir");
            System.out.print("Choix : ");

            int choix = scanner.nextInt();
            scanner.nextLine();

            switch (choix) {
                case 1 -> attaqueJoueur();
                case 2 -> utiliserPotion();
                case 3 -> utiliserSort();
                case 4 -> menuPrincipal = false;
                default -> System.out.println("Choix invalide !");
            }

            joueur.ajouterStamina(5);
            bouleDeFeu.decrementerCooldown();
            flecheMagique.decrementerCooldown();

            tourDesMonstres();
            afficherEtat();
        }

        System.out.println("\n--- Fin du combat ---");
        System.out.println(joueur.estVivant() ? " Victoire !" : " Défaite...");
        scanner.close();
    }

    //joueur

    private void attaqueJoueur() {

        if (joueur.getStamina() < 10) {
            System.out.println("Pas assez de stamina !");
            return;
        }

        joueur.perdreStamina(10);
        attaqueMelee.executer(joueur, boss);
        gestionMonstres.supprimerMonstresMorts();
    }

    // potions

    private void utiliserPotion() {

        boolean sousMenu = true;

        while (sousMenu) {

            System.out.println("\n--- Sous-menu Potions ---");
            System.out.println("1. Potion de soin");
            System.out.println("2. Potion de dégâts");
            System.out.println("3. Potion Mix");
            System.out.println("4. Retour");
            System.out.print("Choix : ");

            int choix = scanner.nextInt();
            scanner.nextLine();

            switch (choix) {

                case 1:
                    if (!potionVie.use()) {
                        System.out.println(" Plus de potion de soin !");
                    }
                    sousMenu = false;
                    break;

                case 2:
                    if (!potionAttaque.use()) {
                        System.out.println(" Plus de potion de dégâts !");
                    }
                    sousMenu = false;
                    break;

                case 3:
                    if (!potionMix.use()) {
                        System.out.println(" Plus de potion Mix !");
                    }
                    sousMenu = false;
                    break;

                case 4:
                    sousMenu = false;
                    break;

                default:
                    System.out.println("Choix invalide !");
            }
        }
    }

    // Sorts

    private void utiliserSort() {

        System.out.println("\n--- Sous-menu Sorts ---");
        System.out.println("1. Boule de feu (CD: " + bouleDeFeu.getCooldownRestant() + ")");
        System.out.println("2. Flèche magique (CD: " + flecheMagique.getCooldownRestant() + ")");
        System.out.println("3. Retour");
        System.out.print("Choix : ");

        int choix = scanner.nextInt();
        scanner.nextLine();

        if (choix == 1) {
            bouleDeFeu.lancer(joueur, boss);
        } else if (choix == 2) {
            flecheMagique.lancer(joueur, boss);
        }
    }

    // Monstre

    private void tourDesMonstres() {

        if (boss.estVivant() && boss.peutInvoquer() && Math.random() < 0.3) {
            boss.invoquer(gestionMonstres);
        }

        for (Monstre m : gestionMonstres.getMonstres()) {
            if (m.estVivant()) {
                attaqueMeleeMonstre.executer(m, joueur);
            }
        }

        gestionMonstres.supprimerMonstresMorts();
        boss.decrementerCooldownInvocation();
    }

    // Affichage

    private void afficherEtat() {

        System.out.println("\n--- État du combat ---");
        System.out.println(joueur.getName() + " : " + joueur.getHealth() + " HP");

        for (Monstre m : gestionMonstres.getMonstres()) {
            System.out.println(m.getName() + " : " + m.getHealth() + " HP");
        }
    }
}
