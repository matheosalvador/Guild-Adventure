package controlleur;

import model.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

public class Gamecontrolleur {

    private GameState gameState;
    private Scanner scanner;
    private transient Random random = new Random();

    private Attaque attaqueMelee;
    private Sort bouleDeFeu;
    private Sort flecheMagique;
    private Sort pariDeGuerison;

    public Gamecontrolleur(GameState gameState) {
        this.gameState = gameState;
        this.scanner = new Scanner(System.in);
        this.attaqueMelee = new AttaqueMelee("Frappe", 15);
        this.bouleDeFeu = new BouleDeFeu();
        this.flecheMagique = new FlecheMagique();
        this.pariDeGuerison = new PariDeGuerison();
    }

    public void demarrerLeJeu() {
        System.out.println("Vous entrez dans la grotte sombre...");
        while (gameState.getJoueur().estVivant() && gameState.getNiveauDuDonjon() <= 5) {
            int niveauIndex = gameState.getNiveauDuDonjon() - 1;
            DungeonMap.DungeonLevel levelData = gameState.dungeonMap.getLevel(niveauIndex);

            if (gameState.getNiveauDuDonjon() == 5) {
                if (handleFinalChoice()) return;
            }

            gameState.dungeonMap.revealNextRooms(niveauIndex);
            gameState.dungeonMap.display(niveauIndex);

            System.out.println("\nOù voulez-vous aller ?");
            System.out.println("1. Continuer sur le chemin principal");
            if (levelData.sideRoomType != null && !levelData.sideRoomVisited) {
                System.out.println("2. Explorer la salle adjacente");
            }
            System.out.print("Choix : ");
            int pathChoice = lireChoixUtilisateur();

            boolean isSideRoom = pathChoice == 2 && levelData.sideRoomType != null && !levelData.sideRoomVisited;
            if (isSideRoom) {
                levelData.sideRoomVisited = true;
                handleRoomEvent(levelData.sideRoomType, true);
            } else {
                levelData.mainRoomVisited = true;
                handleRoomEvent(levelData.mainRoomType, false);
            }

            if (!gameState.getJoueur().estVivant()) {
                System.out.println(AsciiArt.getArt("GAME_OVER"));
                System.out.println(" Votre aventure s'arrête ici. ");
                return;
            }
        }
    }

    private void handleRoomEvent(DungeonMap.RoomType roomType, boolean isSideRoom) {
        switch (roomType) {
            case COMBAT, BOSS:
                if (lancerCombat(gameState.getNiveauDuDonjon())) {
                    if (roomType == DungeonMap.RoomType.BOSS) {
                        System.out.println("\nL'Église vous nomme... le nouveau Roi Démon.");
                        SaveManager.saveNewKing(new PartyData(gameState.getJoueur(), gameState.getAllies()));
                        gameState.incrementerNiveau();
                    } else {
                        offrirRecompenses();
                        if (!isSideRoom) gameState.incrementerNiveau();
                    }
                }
                break;
            case TREASURE:
                System.out.println(AsciiArt.getArt("TREASURE_CHEST"));
                System.out.println("Au centre se trouve un autel mystérieux.");
                System.out.println("Voulez-vous tenter d'activer l'autel ? (o/n)");
                if (scanner.nextLine().equalsIgnoreCase("o")) activateAltar();
                break;
            case EMPTY:
                System.out.println("\nCette salle est vide.");
                if (!isSideRoom) gameState.incrementerNiveau();
                break;
            case SHORTCUT:
                System.out.println("\nVous avez trouvé un raccourci et sautez un niveau !");
                gameState.sauterNiveau();
                break;
        }
    }

    private void activateAltar() {
        System.out.println("Entrez la séquence secrète...");
        String[] konamiCode = {"z", "z", "s", "s", "q", "d", "q", "d", "b", "a"};
        String[] playerInput = new String[10];
        for (int i = 0; i < 10; i++) {
            System.out.print((i + 1) + "/10 > ");
            playerInput[i] = scanner.nextLine();
        }
        if (Arrays.equals(konamiCode, playerInput)) {
            System.out.println("L'autel s'illumine d'une lueur maléfique !");
            gameState.isAltarActivated = true;
        } else {
            System.out.println("Rien ne se passe...");
        }
    }

    private boolean handleFinalChoice() {
        System.out.println("\nVous êtes devant la porte du Roi Démon. Que faites-vous ?");
        System.out.println("1. Combattre");
        System.out.println("2. Tenter de fuir");
        if (gameState.isAltarActivated) System.out.println("3. Proposer vos services au Roi Démon");
        System.out.print("Choix : ");
        int choix = lireChoixUtilisateur();
        switch (choix) {
            case 2:
                if (random.nextDouble() < 0.5) {
                    System.out.println("Fuite réussie ! Mais l'Église vous exécute pour votre échec.");
                } else {
                    System.out.println("Fuite impossible ! Le combat est inévitable !");
                    return false;
                }
                return true;
            case 3:
                if (gameState.isAltarActivated) {
                    System.out.println("Le Roi Démon accepte votre allégeance.");
                    SaveManager.saveTraitor(new PartyData(gameState.getJoueur(), gameState.getAllies()));
                    return true;
                }
            default:
                System.out.println("Vous ouvrez la porte, prêt pour le combat final !");
                return false;
        }
    }

    private boolean lancerCombat(int niveau) {
        List<Monstre> monstres = genererVague(niveau);
        afficherSceneDeCombat(monstres);

        while (gameState.getJoueur().estVivant() && !monstres.isEmpty()) {
            tourDuJoueur(monstres);
            if (monstres.isEmpty()) break;
            tourDesAllies(monstres);
            if (monstres.isEmpty()) break;
            tourDesMonstres(monstres);
            finDeTour();
        }
        return gameState.getJoueur().estVivant();
    }

    private void afficherSceneDeCombat(List<Monstre> monstres) {
        System.out.println("\n==================================================");
        // Affichage du joueur et de ses alliés
        System.out.println("\n--- VOTRE GROUPE ---");
        System.out.println(AsciiArt.getArt(gameState.getJoueur().getName()));
        for (Allie allie : gameState.getAllies()) {
            System.out.println(AsciiArt.getArt(allie.getName()));
        }

        // Affichage des monstres
        System.out.println("\n--- ENNEMIS ---");
        for (Monstre monstre : monstres) {
            String artKey = monstre.getName().split(" ")[0];
            if (monstre.getName().contains("ROI DÉMON") || monstre.getName().contains("Le Nouveau Roi")) {
                artKey = "DEMON_KING";
            }
            System.out.println(AsciiArt.getArt(artKey));
        }
        System.out.println("==================================================\n");
    }

    private void tourDuJoueur(List<Monstre> monstres) {
        gameState.getJoueur().setEnDefense(false);
        afficherStatut(monstres);
        System.out.println("\n--- Actions ---");
        System.out.println("1. Attaquer");
        System.out.println("2. Inventaire");
        System.out.println("3. Sorts");
        System.out.println("4. Se défendre");
        System.out.print("Choix : ");
        int choix = lireChoixUtilisateur();
        switch (choix) {
            case 1 -> attaquer(monstres);
            case 2 -> utiliserItem(monstres);
            case 3 -> utiliserSort(monstres);
            case 4 -> seDefendre();
            default -> System.out.println("Choix invalide, vous perdez votre tour !");
        }
        monstres.removeIf(m -> !m.estVivant());
    }

    private void tourDesAllies(List<Monstre> monstres) {
        gameState.getAllies().removeIf(a -> !a.estVivant());
        if (gameState.getAllies().isEmpty()) return;
        System.out.println("\n--- Tour des alliés ---");
        for (Allie allie : gameState.getAllies()) {
            if (!monstres.isEmpty()) {
                allie.attaquer(monstres.get(random.nextInt(monstres.size())));
            }
        }
        monstres.removeIf(m -> !m.estVivant());
    }

    private void tourDesMonstres(List<Monstre> monstres) {
        System.out.println("\n--- Tour des monstres ---");
        List<Combatant> cibles = new ArrayList<>();
        cibles.add(gameState.getJoueur());
        cibles.addAll(gameState.getAllies());
        for (Monstre monstre : monstres) {
            if (!cibles.isEmpty()) {
                monstre.attaquer(cibles.get(random.nextInt(cibles.size())));
            }
        }
    }

    private void attaquer(List<Monstre> monstres) {
        Monstre cible = choisirCible(monstres);
        if (cible == null) return;
        if (gameState.getJoueur().getStamina() >= 20) {
            gameState.getJoueur().perdreStamina(20);
            attaqueMelee.executer(gameState.getJoueur(), cible);
        } else {
            System.out.println("Pas assez de stamina !");
        }
    }

    private void utiliserItem(List<Monstre> monstres) {
        Inventaire inventaire = gameState.getJoueur().getInventaire();
        if (inventaire.estVide()) {
            System.out.println("L'inventaire est vide.");
            return;
        }
        System.out.println("\n--- Inventaire ---");
        List<Item> items = inventaire.getItems();
        for (int i = 0; i < items.size(); i++) System.out.println((i + 1) + ". " + items.get(i).getName());
        System.out.println((items.size() + 1) + ". Retour");
        System.out.print("Choix : ");
        int choix = lireChoixUtilisateur() - 1;
        if (choix >= 0 && choix < items.size()) {
            Item item = items.get(choix);
            if (item instanceof PotionSoin) {
                choisirCiblePourSoin();
            } else {
                Monstre cibleMonstre = null;
                if (item instanceof PotionDps || item instanceof PotionMix) {
                    cibleMonstre = choisirCible(monstres);
                    if (cibleMonstre == null) return;
                }
                item.use(gameState.getJoueur(), cibleMonstre);
            }
            inventaire.retirerItem(item);
        }
    }

    private void choisirCiblePourSoin() {
        System.out.println("Qui soigner ?");
        List<Entite> cibles = new ArrayList<>();
        cibles.add(gameState.getJoueur());
        cibles.addAll(gameState.getAllies());
        for (int i = 0; i < cibles.size(); i++) {
            Entite e = cibles.get(i);
            System.out.println((i + 1) + ". " + e.getName() + " (" + e.getHealth() + "/" + e.getMaxHealth() + " PV)");
        }
        System.out.print("Choix : ");
        int choix = lireChoixUtilisateur() - 1;
        if (choix >= 0 && choix < cibles.size()) {
            cibles.get(choix).ajouterVie(25);
            System.out.println(cibles.get(choix).getName() + " récupère 25 PV !");
        }
    }

    private void utiliserSort(List<Monstre> monstres) {
        System.out.println("\n--- Sorts ---");
        System.out.println("1. Boule de Feu (20 ST, CD:" + bouleDeFeu.getCooldownRestant() + ")");
        System.out.println("2. Flèche Magique (15 ST, CD:" + flecheMagique.getCooldownRestant() + ")");
        System.out.println("3. Pari de Guérison (25 ST, CD:" + pariDeGuerison.getCooldownRestant() + ")");
        System.out.println("4. Retour");
        System.out.print("Choix : ");
        int choix = lireChoixUtilisateur();
        Sort sortChoisi = null;
        switch (choix) {
            case 1: sortChoisi = bouleDeFeu; break;
            case 2: sortChoisi = flecheMagique; break;
            case 3: sortChoisi = pariDeGuerison; break;
            default: return;
        }
        if (sortChoisi.peutEtreLance(gameState.getJoueur())) {
            Monstre cible = null;
            if (sortChoisi instanceof BouleDeFeu || sortChoisi instanceof FlecheMagique) {
                cible = choisirCible(monstres);
                if (cible == null) return;
            }
            sortChoisi.lancer(gameState.getJoueur(), cible, scanner);
        } else {
            System.out.println("Impossible de lancer " + sortChoisi.getNom() + " !");
        }
    }

    private void seDefendre() {
        gameState.getJoueur().setEnDefense(true);
        System.out.println(gameState.getJoueur().getName() + " lève son bouclier.");
    }

    private void finDeTour() {
        if (gameState.getJoueur().isEnDefense()) gameState.getJoueur().ajouterStamina(25);
        else gameState.getJoueur().ajouterStamina(5);
        bouleDeFeu.decrementerCooldown();
        flecheMagique.decrementerCooldown();
        pariDeGuerison.decrementerCooldown();
    }

    private List<Monstre> genererVague(int niveau) {
        List<Monstre> vague = new ArrayList<>();
        if (niveau == 5) {
            PartyData newKing = SaveManager.loadNewKing();
            if (newKing != null) {
                System.out.println("Un nouveau Roi Démon se dresse devant vous...");
                vague.add(new Monstre(newKing.getJoueur().getName() + " (Le Nouveau Roi)", 500, 30));
                newKing.getAllies().forEach(a -> vague.add(new Monstre(a.getName() + " (Garde Royal)", 150, 20)));
                return vague;
            }
            vague.add(new Monstre("ROI DÉMON", 400, 25));
            PartyData traitor = SaveManager.loadTraitor();
            if (traitor != null) {
                System.out.println("Un traître se joint au combat !");
                vague.add(new Monstre(traitor.getJoueur().getName() + " (Le Traître)", 200, 20));
                traitor.getAllies().forEach(a -> vague.add(new Monstre(a.getName() + " (Sbire)", 100, 15)));
            }
        } else {
            switch (niveau) {
                case 1: vague.add(new Monstre("Gobelin", 60, 8)); break;
                case 2:
                    vague.add(new Monstre("Gobelin", 60, 8));
                    vague.add(new Monstre("Orque", 90, 12));
                    break;
                case 3:
                    vague.add(new Monstre("Orque", 90, 12));
                    vague.add(new Monstre("Orque", 90, 12));
                    break;
                case 4:
                    vague.add(new Monstre("Troll", 150, 18));
                    vague.add(new Monstre("Orque", 90, 12));
                    break;
            }
        }
        return vague;
    }

    private void offrirRecompenses() {
        System.out.println("\n--- Récompenses ---");
        System.out.println("1. Une Potion de Soin.");
        System.out.println("2. Recruter un 'Loup Fidèle' (10 dégâts).");
        System.out.println("3. Recruter un 'Mercenaire Aguerri' (20 dégâts).");
        System.out.print("Choix : ");
        int choix = lireChoixUtilisateur();
        switch (choix) {
            case 1:
                gameState.getJoueur().getInventaire().ajouterItem(new PotionSoin());
                System.out.println("Vous recevez une Potion de Soin.");
                break;
            case 2:
                gameState.getAllies().add(new Allie("Loup Fidèle", 80, 10));
                System.out.println("Le Loup Fidèle se joint à vous !");
                break;
            case 3:
                gameState.getAllies().add(new Allie("Mercenaire Aguerri", 120, 20));
                System.out.println("Le Mercenaire Aguerri se joint à votre cause !");
                break;
            default:
                System.out.println("Vous ne prenez rien.");
                break;
        }
    }

    private Monstre choisirCible(List<Monstre> monstres) {
        if (monstres.isEmpty()) return null;
        if (monstres.size() == 1) return monstres.get(0);
        System.out.println("\n--- Choisir une cible ---");
        for (int i = 0; i < monstres.size(); i++) {
            Monstre m = monstres.get(i);
            System.out.println((i + 1) + ". " + m.getName() + " (" + m.getHealth() + "/" + m.getMaxHealth() + " PV)");
        }
        System.out.print("Cible : ");
        int choix = lireChoixUtilisateur() - 1;
        if (choix >= 0 && choix < monstres.size()) return monstres.get(choix);
        System.out.println("Cible invalide.");
        return null;
    }

    private void afficherStatut(List<Monstre> monstres) {
        System.out.println("\n--------------------");
        Joueur joueur = gameState.getJoueur();
        String defenseStatus = joueur.isEnDefense() ? " (En défense)" : "";
        System.out.println(joueur.getName() + " : " + joueur.getHealth() + "/" + joueur.getMaxHealth() + " HP | " + joueur.getStamina() + "/" + joueur.getMaxStamina() + " ST" + defenseStatus);
        if (!gameState.getAllies().isEmpty()) {
            System.out.println("--- Alliés ---");
            for (Allie allie : gameState.getAllies()) {
                System.out.println("- " + allie.getName() + " : " + allie.getHealth() + "/" + allie.getMaxHealth() + " HP");
            }
        }
        System.out.println("--- Ennemis ---");
        for (Monstre m : monstres) {
            System.out.println("- " + m.getName() + " : " + m.getHealth() + "/" + m.getMaxHealth() + " HP");
        }
        System.out.println("--------------------");
    }

    private int lireChoixUtilisateur() {
        try {
            return Integer.parseInt(scanner.nextLine());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}