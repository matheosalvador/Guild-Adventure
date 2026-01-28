package controlleur;
import model.*;
import java.util.Scanner;

public class Gamecontrolleur {
    private Joueur joueur;
    private Monstre monstre;
    private boolean potionUtilisee = false;

    private Scanner scanner;

    public Gamecontrolleur(Joueur joueur, Monstre monstre, Scanner scanner) {
        this.joueur = joueur;
        this.monstre = monstre;
        this.scanner = scanner;
    }

    public void demarrerJeu() {
        boolean menuPrincipal = true; // contrôle du menu principal

        while (menuPrincipal && joueur.isAlive() && monstre.estVivant()) {
            int choix = scanner.nextInt();
            scanner.nextLine();

            switch (choix) {
                case 1:
                    /*
                    joueur.attaquer(monstre);
                     */
                    if (monstre.estVivant()) {
                        /*
                        monstre.attaquer(joueur);
                         */
                    }
                    break;

                case 2:
                    boolean sousMenu = true; // contrôle du sous-menu des potions

                    while (sousMenu && !potionUtilisee) {
                        int choixItem = scanner.nextInt();
                        scanner.nextLine();
                        switch (choixItem) {
                            case 1:
                                /*
                                if (item.abstract.potionVie.getpotionrestant>0){
                                    item.abstract.potionVie(joueur);
                                    sousMenu = false; // quitte le sous-menu
                                    potionUtilisee = true;
                                } else {
                                    System.out.println("Pas de potion de soin dans votre sac");
                                }
                                */
                                break;

                            case 2:
                                /*
                                if (item.abstract.potionDps.getpotionrestant>0){
                                    item.abstract.potionDps(joueur);
                                    sousMenu = false;
                                    potionUtilisee = true;
                                } else {
                                    System.out.println("Pas de potion de degats dans votre sac");
                                }
                                */
                                break;

                            case 3:
                                /*
                                if (item.abstract.potionMixte.getpotionrestant>0){
                                    item.abstract.potionMixte(joueur);
                                    sousMenu = false;
                                    potionUtilisee = true;
                                } else {
                                    System.out.println("Pas de potion mixte dans votre sac");
                                }
                                */
                                break;

                            case 4:
                                sousMenu = false; // permet de quitter le sous-menu manuellement
                                break;

                            default:
                                System.out.println("Choix invalide");
                                break;
                        }
                    }
                    break;
                case 3:
                    /*
                    joueur.nextdefense(monstre);
                     */
                    break;
                case 4:
                    /*
                    joueur.fuite():
                     */
                    break;
                case 5:
                    /*
                    Sous-menu futur a faire pour les sorts
                    joeur.sort(monstre):
                     */
                    break;



                default:
                    System.out.println("Choix invalide");
                    break;

            }
        }
    }
}
