package model;

/**
 * Potion de soin qui rend des points de vie au joueur.
 */
public class PotionSoin extends Potion {

    private int soin = 20;
    private Joueur joueur;

    /**
     * Constructeur de la potion de soin.
     * @param joueur Le joueur à soigner.
     */
    public PotionSoin(Joueur joueur) {
        super("Potion de soin");
        this.joueur = joueur;
    }

    @Override
    protected void applyEffect() {
        joueur.ajouterVie(soin);
    }
}
