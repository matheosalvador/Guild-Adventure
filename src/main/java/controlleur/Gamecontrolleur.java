package controlleur;
import model.*;
import java.util.Scanner;

public class Gamecontrolleur {

    private Joueur joueur;
    private Monstre monstre;
    private boolean potionUtilisee = false;

    private Scanner scanner;
    private Attaque attaqueMelee;
    private Attaque attaqueMeleeMonstre;
    private PotionSoin potionVie;
    private PotionDps potionAttaque;
    private PotionMix potionMix;

    // Constructeur
    public Gamecontrolleur() {
        // Initialisation des personnages
        this.joueur = new Joueur("Arthur");
        this.monstre = new Monstre("Gobelin");

        // Initialisation du scanner
        this.scanner = new Scanner(System.in);

        // Initialisation des attaques
        this.attaqueMelee = new AttaqueMelee("frappe", 10);
        this.attaqueMeleeMonstre = new AttaqueMelee("coup de griffe", 8);

        // Initialisation des potions
        this.potionVie = new PotionSoin(joueur);
        this.potionAttaque = new PotionDps(monstre);
        this.potionMix = new PotionMix(joueur, monstre);
    }

    public void demarrerJeu() {
        System.out.println("Bienvenue dans le jeu !");
        boolean menuPrincipal = true;

        while (menuPrincipal && joueur.estVivant() && monstre.estVivant()) {
            System.out.println("\n--- Menu principal ---");
            System.out.println("1. Attaque corps à corps");
            System.out.println("2. Utiliser une potion");
            System.out.println("3. Défense (pas encore implémenté)");
            System.out.println("4. Fuir le combat");
            System.out.println("5. Sorts");
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
                    System.out.println(joueur.getName() + " a fui le combat !");
                    menuPrincipal = false;
                    break;

                case 5:
                    utiliserSort();
                    break;

                default:
                    System.out.println("Choix invalide !");
                    break;
            }
            // Fin du tour : régénération de stamina
            joueur.ajouterStamina(5); // régénère 5 stamina par tour

            // Affichage de la stamina après régénération
            System.out.println("\nStamina : " + joueur.getName() + " = " + joueur.getStamina() +
                    "/" + joueur.getMaxStamina());
        }

        System.out.println("\n--- Fin du combat ---");
        if (joueur.estVivant()) {
            System.out.println("Vous avez gagné !");
        } else {
            System.out.println("Vous avez été vaincu !");
        }

        scanner.close();
    }

    // le sous-menu potions
    private void utiliserPotion() {
        boolean sousMenu = true;

        while (sousMenu && !potionUtilisee) {
            System.out.println("\n--- Sous-menu Potions ---");
            System.out.println("1. Potion de soin");
            System.out.println("2. Potion de dégâts sur le monstre");
            System.out.println("3. Potion Mix");
            System.out.println("4. Retour");
            System.out.print("Choix : ");

            int choixItem = scanner.nextInt();
            scanner.nextLine();

            switch (choixItem) {
                case 1:
                    if (potionVie.use()) {
                        System.out.println("Potion de soin utilisée !");
                        potionUtilisee = true;
                    } else {
                        System.out.println("Pas de potion de soin !");
                    }
                    sousMenu = false;
                    break;

                case 2:
                    if (potionAttaque.use()) {
                        System.out.println("Potion de dégâts utilisée sur le monstre !");
                        potionUtilisee = true;
                    } else {
                        System.out.println("Pas de potion de dégâts !");
                    }
                    sousMenu = false;
                    break;

                case 3:
                    if (potionMix.use()) {
                        System.out.println("Potion Mix utilisée !");
                        potionUtilisee = true;
                    } else {
                        System.out.println("Pas de potion Mix !");
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

        // Après utilisation, le monstre riposte si encore vivant a modifier au choix
        if (monstre.estVivant()) {
            attaqueMeleeMonstre.executer(monstre, joueur);
        }
    }

    // le sous-menu sorts
    private void utiliserSort() {
        boolean sousMenuSort = true;

        while (sousMenuSort) {
            System.out.println("\n--- Sous-menu Sorts ---");
            System.out.println("1. Boule de feu (25 dégâts, portée 3)");
            System.out.println("2. Flèche magique (15 dégâts, portée 5)");
            System.out.println("3. Retour");
            System.out.print("Choix : ");

            int choixSort = scanner.nextInt();
            scanner.nextLine();

            switch (choixSort) {
                case 1 -> {
                    AttaqueDistance bouleDeFeu = new AttaqueDistance("Boule de feu", 25, 3);
                    bouleDeFeu.executer(joueur, monstre);
                    sousMenuSort = false;
                }
                case 2 -> {
                    AttaqueDistance flecheMagique = new AttaqueDistance("Flèche magique", 15, 5);
                    flecheMagique.executer(joueur, monstre);
                    sousMenuSort = false;
                }
                case 3 -> sousMenuSort = false;
                default -> System.out.println("Choix invalide !");
            }
        }

        // Monstre riposte avec 50% de chance
        if (monstre.estVivant()) {
            if (Math.random() < 0.5) {
                attaqueMeleeMonstre.executer(monstre, joueur);
            } else {
                System.out.println(monstre.getName() + " a raté son attaque !");
            }
        }
    }
}