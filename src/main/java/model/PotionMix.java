package model;

/**
 * Potion mixte qui soigne le joueur et inflige des dégâts au monstre.
 */
public class PotionMix extends Potion {

    private int soin = 20;
    private int degats = 30;

    private Joueur joueur;
    private Monstre monstre;

    /**
     * Constructeur de la potion mixte.
     * @param joueur Le joueur à soigner.
     * @param monstre Le monstre à attaquer.
     */
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
