package model;
// a neutraliser plus tard si inutile
public class PotionSoin implements Item {
    private final String name = "Potion de Soin";
    private final String description = "Restaure 25 points de vie.";
    private final int soin = 25;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void use(Joueur joueur, Monstre monstre) {
        if (joueur != null) {
            joueur.ajouterVie(soin);
            System.out.println(joueur.getName() + " utilise une " + name + " et récupère " + soin + " PV.");
        }
    }
}