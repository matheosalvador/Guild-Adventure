package model;

public class PotionSoin extends Potion {

    private final int soin = 20;

    public PotionSoin() {
        super("Potion de soin");
    }

    @Override
    public void utiliser(Joueur joueur, Monstre monstre) {
        joueur.ajouterVie(soin);
        System.out.println(joueur.getName() + " récupère " + soin + " PV !");
    }
}
