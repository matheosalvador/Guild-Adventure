package model;

public class PotionMix extends Potion {

    private final int soin = 20;
    private final int degats = 30;

    public PotionMix() {
        super("Potion Mix");
    }

    @Override
    public void utiliser(Joueur joueur, Monstre monstre) {
        joueur.ajouterVie(soin);
        monstre.perdreVie(degats);
        System.out.println(joueur.getName() + " récupère " + soin + " PV !");
        System.out.println(monstre.getName() + " subit " + degats + " dégâts !");
    }
}
