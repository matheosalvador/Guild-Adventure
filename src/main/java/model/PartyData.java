package model;
// a neutraliser plus tard si inutile
import java.util.List;

public class PartyData {
    private Joueur joueur;
    private List<Allie> allies;

    public PartyData(Joueur joueur, List<Allie> allies) {
        this.joueur = joueur;
        this.allies = allies;
    }

    public Joueur getJoueur() {
        return joueur;
    }

    public List<Allie> getAllies() {
        return allies;
    }
}