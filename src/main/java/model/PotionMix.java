package model;

public class PotionMix implements Item {
    private final String name = "Potion Mixte";
    private final String description = "Restaure 15 PV au joueur et inflige 15 dégâts au monstre.";
    private final int soin = 15;
    private final int degats = 15;

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
            System.out.println(joueur.getName() + " récupère " + soin + " PV.");
        }
        if (monstre != null) {
            monstre.perdreVie(degats);
            System.out.println(monstre.getName() + " subit " + degats + " dégâts.");
        }
        System.out.println("La fiole de " + name + " se brise.");
    }
}