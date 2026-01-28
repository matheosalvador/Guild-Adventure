package model;

public class PotionDps extends Potion {

    private int degats = 30;
    private Monstre monstre; // référence directe au monstre

    public PotionDps(Monstre monstre) {
        super("Potion de dégâts");
        this.monstre = monstre;
    }

    @Override
    public void applyEffect() {
        monstre.perdreVie(degats);
    }
}
