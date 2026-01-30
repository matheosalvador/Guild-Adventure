package model;

import java.util.ArrayList;
import java.util.List;

/**
 * Représente l'état complet d'une partie en cours
 * Cet objet contient toutes les informations nécessaires pour sauvegarder et charger la progression,
 * y compris le joueur, ses alliés, sa position dans le donjon et l'état des secrets.
 */
public class GameState {

    private final Joueur joueur;
    private final List<Allie> allies;
    private int niveauDuDonjon;
    public final DungeonMap dungeonMap;
    public boolean isAltarActivated = false;

    /**
     * Crée un nouvel état de jeu pour une nouvelle partie.
     *
     * @param joueur         Le personnage du joueur qui commence l'aventure.
     * @param niveauDuDonjon Le niveau de départ (généralement 1).
     */
    public GameState(Joueur joueur, int niveauDuDonjon) {
        this.joueur = joueur;
        this.allies = new ArrayList<>();
        this.niveauDuDonjon = niveauDuDonjon;
        this.dungeonMap = new DungeonMap(5); // Le donjon a une taille fixe de 5 niveaux.
    }

    public Joueur getJoueur() {
        return joueur;
    }

    public List<Allie> getAllies() {
        return allies;
    }

    public int getNiveauDuDonjon() {
        return niveauDuDonjon;
    }

    /**
     * Fait avancer le joueur au niveau suivant du donjon.
     */
    public void incrementerNiveau() {
        this.niveauDuDonjon++;
    }

    /**
     * Fait avancer le joueur de deux niveaux, simulant un raccourci.
     */
    public void sauterNiveau() {
        this.niveauDuDonjon += 2;
    }
}