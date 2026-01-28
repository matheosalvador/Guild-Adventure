package model;

public class PotionSoin extends Potion {

    private int soin = 20;
    private Joueur joueur;

    public PotionSoin(Joueur joueur) {
        super("Potion de soin");
        this.joueur = joueur;
    }

    @Override
    protected void applyEffect() {
        joueur.ajouterVie(soin);
    }
}
