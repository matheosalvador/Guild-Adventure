package model;

public class PotionDps extends Potion {

    private final int degats = 30;

    public PotionDps() {
        super("Potion de dégâts");
    }

    @Override
    public void utiliser(Joueur joueur, Monstre monstre) {
        monstre.perdreVie(degats);
        System.out.println(monstre.getName() + " subit " + degats + " dégâts !");
    }
}
