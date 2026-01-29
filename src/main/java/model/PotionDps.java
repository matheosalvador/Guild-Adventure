package model;

/**
 * Potion de dégâts qui inflige des dégâts au monstre.
 */
public class PotionDps extends Potion {

    private int degats = 30;
    private Monstre monstre; // référence directe au monstre

    /**
     * Constructeur de la potion de dégâts.
     * @param monstre Le monstre cible.
     */
    public PotionDps(Monstre monstre) {
        super("Potion de dégâts");
        this.monstre = monstre;
    }

    @Override
    protected void applyEffect() {
        monstre.perdreVie(degats);
    }
}
