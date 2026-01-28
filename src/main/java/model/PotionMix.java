package model;

public class PotionMix extends Potion {

    private int soin = 20;
    private int degats = 30;

    private Joueur joueur;
    private Monstre monstre;

    public PotionMix(Joueur joueur, Monstre monstre) {
        super("Potion Mix");
        this.joueur = joueur;
        this.monstre = monstre;
    }

    @Override
    protected void applyEffect() {
        joueur.ajouterVie(soin);         // soigne le joueur
        monstre.perdreVie(degats);       // inflige des dégâts au monstre
    }
}
