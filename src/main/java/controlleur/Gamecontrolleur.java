package controlleur;

import model.*;
import java.util.Scanner;

public class Gamecontrolleur {

    /* =========================
       === ATTRIBUTS PRINCIPAUX ===
       ========================= */

    // Personnages
    private Joueur joueur;
    private GestionMonstres gestionMonstres;
    private Monstre boss;

    // Attaques
    private Attaque attaqueMeleeJoueur;
    private Attaque attaqueMeleeMonstre;

    // Sorts
    private Sort bouleDeFeu;
    private Sort flecheMagique;

    // Potions
    private PotionSoin potionVie;
    private PotionDps potionAttaque;
    private PotionMix potionMix;

    // Outils
    private Scanner scanner;

    /* =========================
       === CONSTRUCTEUR ===
       ========================= */

    public Gamecontrolleur() {
        initialiserPersonnages();
        initialiserAttaques();
        initialiserSorts();
        initialiserPotions();
        scanner = new Scanner(System.in);
    }

    /* =========================
       === INITIALISATIONS ===
       ========================= */

    private void initialiserPersonnages() {
        joueur = new Joueur("Arthur");

        gestionMonstres = new GestionMonstres();
        boss = new Monstre("Gobelin chef");
        gestionMonstres.ajouterMonstre(boss);
    }

    private void initialiserAttaques() {
        attaqueMeleeJoueur = new AttaqueMelee("Frappe", 10);
        attaqueMeleeMonstre = new AttaqueMelee("Coup de griffe", 8);
    }

    private void initialiserSorts() {
        bouleDeFeu = new BouleDeFeu();
        flecheMagique = new FlecheMagique();
    }

    private void initialiserPotions() {
        potionVie = new PotionSoin();
        potionAttaque = new PotionDps();
        potionMix = new PotionMix();
    }

    /* =========================
       === JEU PRINCIPAL ===
       ========================= */

    public void demarrerJeu() {
        afficherIntroduction();
        donnerInventaireDepart();

        boolean jeuActif = true;

        while (jeuActif && joueur.estVivant() && gestionMonstres.resteDesMonstres()) {

            afficherMenuPrincipal();
            int choix = lireChoix();

            switch (choix) {
                case 1 -> attaquerCorpsACorps();
                case 2 -> utiliserPotion();
                case 3 -> System.out.println("Défense non implémentée.");
                case 4 -> jeuActif = fuirCombat();
                case 5 -> utiliserSort();
                case 6 -> afficherInventaire();
                case 7 -> afficherStatus();
                default -> System.out.println("Choix invalide !");
            }

            finDeTour();
        }

        afficherFinCombat();
        scanner.close();
    }

    /* =========================
       === ACTIONS DU JOUEUR ===
       ========================= */

    private void attaquerCorpsACorps() {
        int coutStamina = 10;

        if (joueur.getStamina() < coutStamina) {
            System.out.println("Pas assez de stamina !");
            return;
        }

        joueur.perdreStamina(coutStamina);
        attaqueMeleeJoueur.executer(joueur, boss);

        if (boss.estVivant()) {
            attaqueMeleeMonstre.executer(boss, joueur);
        }
    }

    private boolean fuirCombat() {
        joueur.subirDegats(100);
        System.out.println(joueur.getName() + " a fui le combat !");
        return false;
    }

    /* =========================
       === POTIONS ===
       ========================= */

    private void utiliserPotion() {
        afficherMenuPotions();
        int choix = lireChoix();

        switch (choix) {
            case 1 -> utiliserPotionSiDisponible(potionVie, "Potion de soin");
            case 2 -> utiliserPotionSiDisponible(potionAttaque, "Potion d'attaque");
            case 3 -> utiliserPotionSiDisponible(potionMix, "Potion mix");
            case 4 -> { return; }
            default -> System.out.println("Choix invalide !");
        }

        if (boss.estVivant()) {
            attaqueMeleeMonstre.executer(boss, joueur);
        }
    }

    private void utiliserPotionSiDisponible(Item potion, String nom) {
        if (joueur.getInventaire().contient(potion)) {
            joueur.utiliserItem(potion, boss);
        } else {
            System.out.println("Pas de " + nom + " !");
        }
    }

    /* =========================
       === SORTS ===
       ========================= */

    private void utiliserSort() {
        afficherMenuSorts();
        int choix = lireChoix();

        switch (choix) {
            case 1 -> bouleDeFeu.lancer(joueur, boss);
            case 2 -> flecheMagique.lancer(joueur, boss);
            case 3 -> { return; }
            default -> System.out.println("Choix invalide !");
        }

        gestionMonstres.supprimerMonstresMorts();
        boss.decrementerCooldownInvocation();
    }

    /* =========================
       === AFFICHAGES ===
       ========================= */

    private void afficherIntroduction() {
        System.out.println("Bienvenue dans le jeu !");
    }

    private void donnerInventaireDepart() {
        joueur.ramasserItem(potionVie);
        joueur.ramasserItem(potionAttaque);
        joueur.ramasserItem(potionMix);
        joueur.ramasserItem(potionVie);
        joueur.ramasserItem(potionAttaque);
        joueur.ramasserItem(potionMix);

        System.out.println("--- Inventaire de départ ---");
        afficherInventaire();
    }

    private void afficherMenuPrincipal() {
        System.out.println("""
                \n--- Menu principal ---
                1. Attaque corps à corps
                2. Utiliser une potion
                3. Défense
                4. Fuir
                5. Sorts
                6. Inventaire
                7. Status
                """);
        System.out.print("Choix : ");
    }

    private void afficherMenuPotions() {
        System.out.println("""
                \n--- Potions ---
                1. Potion de soin
                2. Potion de dégâts
                3. Potion mix
                4. Retour
                """);
        System.out.print("Choix : ");
    }

    private void afficherMenuSorts() {
        System.out.println("""
                \n--- Sorts ---
                1. Boule de feu (CD : """ + bouleDeFeu.getCooldownRestant() + """
                )
                2. Flèche magique (CD : """ + flecheMagique.getCooldownRestant() + """
                )
                3. Retour
                """);
        System.out.print("Choix : ");
    }

    private void afficherInventaire() {
        System.out.println(joueur.getInventaire().getItems());
    }

    private void afficherStatus() {
        System.out.println("\n--- Status ---");
        System.out.println(joueur.getName() + " : " +
                joueur.getHealth() + "/" + joueur.getMaxHealth() + " HP | Stamina : " +
                joueur.getStamina() + "/" + joueur.getMaxStamina());

        System.out.println(boss.getName() + " : " +
                boss.getHealth() + "/" + boss.getMaxHealth() + " HP");
    }

    private void afficherFinCombat() {
        System.out.println("\n--- Fin du combat ---");
        System.out.println(joueur.estVivant() ? "Victoire !" : "Défaite...");
    }

    /* =========================
       === UTILITAIRES ===
       ========================= */

    private int lireChoix() {
        int choix = scanner.nextInt();
        scanner.nextLine();
        return choix;
    }

    private void finDeTour() {
        joueur.ajouterStamina(5);
        bouleDeFeu.decrementerCooldown();
        flecheMagique.decrementerCooldown();
    }
}
