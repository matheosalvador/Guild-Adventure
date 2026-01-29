package controlleur;
import model.*;
import java.util.Scanner;

public class Gamecontrolleur {

    // Joueur et Monstre
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
        // Initialisation des personnages
        this.joueur = new Joueur("Arthur");

        this.gestionMonstres = new GestionMonstres();
        this.boss = new Monstre("Gobelin chef");
        gestionMonstres.ajouterMonstre(boss);

        this.scanner = new Scanner(System.in);

        // Initialisation des attaques
        this.attaqueMelee = new AttaqueMelee("frappe", 10);
        this.attaqueMeleeMonstre = new AttaqueMelee("coup de griffe", 8);

        // Initialisation des sorts
        this.bouleDeFeu = new BouleDeFeu();
        this.flecheMagique = new FlecheMagique();

        // Initialisation des potions
        this.potionVie = new PotionSoin();
        this.potionAttaque = new PotionDps();
        this.potionMix = new PotionMix();
    }

    // jeu

    public void demarrerJeu() {
        System.out.println("Bienvenue dans le jeu !");
        joueur.ramasserItem(potionMix);
        joueur.ramasserItem(potionAttaque);
        joueur.ramasserItem(potionVie);
        joueur.ramasserItem(potionMix);
        joueur.ramasserItem(potionAttaque);
        joueur.ramasserItem(potionVie);
        System.out.println("--- Inventaire de départ ---");
        System.out.println(joueur.getInventaire().getItems().toString());
        boolean menuPrincipal = true;

        while (menuPrincipal && joueur.estVivant() && gestionMonstres.resteDesMonstres()) {

            System.out.println("\n--- Menu principal ---");
            System.out.println("1. Attaque corps à corps");
            System.out.println("2. Utiliser une potion");
            System.out.println("3. Défense (pas encore fait)");
            System.out.println("4. Fuir le combat");
            System.out.println("5. Sorts");
            System.out.println("6. Inventaire");
            System.out.println("7. Status");
            System.out.print("Choix : ");

            int choix = scanner.nextInt();
            scanner.nextLine();

            switch (choix) {
                case 1:
                    int coutStaminaMelee = 10;
                    if (joueur.getStamina() >= coutStaminaMelee) {
                        joueur.perdreStamina(coutStaminaMelee);
                        attaqueMelee.executer(joueur, monstre);
                        if (monstre.estVivant()) {
                            attaqueMeleeMonstre.executer(monstre, joueur);
                        }
                    } else {
                        System.out.println("Pas assez de stamina pour attaquer !");
                    }
                    break;

                case 2:
                    utiliserPotion();
                    break;

                case 3:
                    System.out.println("Défense non implémentée pour le moment.");
                    break;

                case 4:
                    joueur.subirDegats(100);
                    System.out.println(joueur.getName() + " a fui le combat !");
                    menuPrincipal = false;
                    break;

                case 5:
                    utiliserSort();
                    break;

                case 6:
                    System.out.println("--- Inventaire Actuel ---");
                    System.out.println(joueur.getInventaire().getItems().toString());
                    break;

                case 7:
                    // --- Affichage du statut du joueur et du monstre ---
                    System.out.println("\n--- Statut Actuel ---");
                    System.out.println(joueur.getName() + " : " + joueur.getHealth() + "/" + joueur.getMaxHealth() + " HP, Stamina : "
                            + joueur.getStamina() + "/" + joueur.getMaxStamina());
                    System.out.println(monstre.getName() + " : " + monstre.getHealth() + "/" + monstre.getMaxHealth() + " HP");
                    System.out.println("Cooldowns : Boule de feu = " + bouleDeFeu.getCooldownRestant() +
                            ", Flèche magique = " + flecheMagique.getCooldownRestant());
                break;

                default:
                    System.out.println("Choix invalide !");
                    break;
            }


            // Fin du tour : régénération de stamina
            joueur.ajouterStamina(5); // régénère 5 stamina par tour
            // Décrémenter cooldown des sorts
            bouleDeFeu.decrementerCooldown();
            flecheMagique.decrementerCooldown();
        }

        System.out.println("\n--- Fin du combat ---");
        if (joueur.estVivant()) {
            System.out.println("Vous avez gagné !");
        } else {
            System.out.println("Vous avez été vaincu !");
        }

        scanner.close();
    }

    // --- Sous-menu potions ---
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
                    if (joueur.getInventaire().contient(potionVie)) {
                        joueur.utiliserItem(potionVie, monstre);
                    } else {
                        System.out.println("Pas de potion de soin !");
                    }
                    sousMenu = false;
                    break;
                case 2:
                    if (joueur.getInventaire().contient(potionAttaque)) {
                        joueur.utiliserItem(potionAttaque, monstre);
                    } else {
                        System.out.println("Pas de potion de attaque !");
                    }
                    sousMenu = false;
                    break;
                case 3:
                    if (joueur.getInventaire().contient(potionMix)) {
                        joueur.utiliserItem(potionMix, monstre);
                    } else {
                        System.out.println("Pas de potion de mix !");
                    }
                    sousMenu = false;
                    break;

                case 4:
                    sousMenu = false;
                    break;

                default:
                    System.out.println("Choix invalide !");
                    break;
            }
        }

        // Monstre riposte après potion
        if (monstre.estVivant()) {
            attaqueMeleeMonstre.executer(monstre, joueur);
        }
    }

    // --- Sous-menu sorts ---
    private void utiliserSort() {
        boolean sousMenuSort = true;

        while (sousMenuSort) {
            System.out.println("\n--- Sous-menu Sorts ---");
            System.out.println("1. Boule de feu (25 dégâts, coût 20, cooldown " + bouleDeFeu.getCooldownRestant() + ")");
            System.out.println("2. Flèche magique (15 dégâts, coût 15, cooldown " + flecheMagique.getCooldownRestant() + ")");
            System.out.println("3. Retour");
            System.out.print("Choix : ");

            int choixSort = scanner.nextInt();
            scanner.nextLine();

            switch (choixSort) {
                case 1:
                    bouleDeFeu.lancer(joueur, monstre);
                    sousMenuSort = false;
                    break;

                case 2:
                    flecheMagique.lancer(joueur, monstre);
                    sousMenuSort = false;
                    break;

                case 3:
                    sousMenuSort = false;
                    break;

                default:
                    System.out.println("Choix invalide !");
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
