package controlleur;

import model.*;
import java.util.Scanner;
import model.GameState;
import service.SaveManager;

public class Gamecontrolleur {

    private Joueur joueur;
    private Monstre monstre;
    private boolean potionUtilisee = false;

    // Sorts
    private Sort bouleDeFeu;
    private Sort flecheMagique;

    private Scanner scanner;
    private Attaque attaqueMelee;
    private Attaque attaqueMeleeMonstre;
    private PotionSoin potionVie;
    private PotionDps potionAttaque;
    private PotionMix potionMix;

    // Constructeur
    public Gamecontrolleur() {
        this.scanner = new Scanner(System.in);

        // Charger sauvegarde si disponible
        GameState loadedState = SaveManager.load();
        if (loadedState != null) {
            System.out.println("Partie chargée : " + loadedState.getPlayerName());
            this.joueur = new Joueur(loadedState.getPlayerName());
            this.joueur.setHealth(loadedState.getHealth());
            this.joueur.setLevel(loadedState.getLevel());
        } else {
            System.out.print("Entrez le nom du joueur : ");
            String name = "Arthur"; // Par défaut si pas d'entrée dans console
            this.joueur = new Joueur(name);
            System.out.println("Nouvelle partie pour " + name);
        }

        // Initialisation du monstre (toujours un Gobelin pour le moment)
        this.monstre = new Monstre("Gobelin");

        // Initialisation des attaques
        this.attaqueMelee = new AttaqueMelee("frappe", 10);
        this.attaqueMeleeMonstre = new AttaqueMelee("coup de griffe", 8);

        // Initialisation des sorts
        this.bouleDeFeu = new BouleDeFeu();
        this.flecheMagique = new FlecheMagique();

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
            System.out.println("3. Défense (pas encore fait)");
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
            joueur.ajouterStamina(5);

            // Décrémenter cooldown des sorts
            bouleDeFeu.decrementerCooldown();
            flecheMagique.decrementerCooldown();

            // --- Affichage du statut ---
            System.out.println("\n--- Statut après ce tour ---");
            System.out.println(joueur.getName() + " : " + joueur.getHealth() + "/" + joueur.getMaxHealth() +
                    " HP, Stamina : " + joueur.getStamina() + "/" + joueur.getMaxStamina());
            System.out.println(monstre.getName() + " : " + monstre.getHealth() + "/" + monstre.getMaxHealth() + " HP");
            System.out.println("Cooldowns : Boule de feu = " + bouleDeFeu.getCooldownRestant() +
                    ", Flèche magique = " + flecheMagique.getCooldownRestant());

            // --- SAUVEGARDE AUTOMATIQUE ---
            GameState currentState = new GameState(joueur.getName(), joueur.getLevel(), joueur.getHealth());
            SaveManager.save(currentState);
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

        if (monstre.estVivant()) {
            if (Math.random() < 0.5) {
                attaqueMeleeMonstre.executer(monstre, joueur);
            } else {
                System.out.println(monstre.getName() + " a raté son attaque !");
            }
        }
    }
}
