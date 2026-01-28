package model;

public class FlecheMagique extends Sort {

    public FlecheMagique() {
        super("Flèche magique", 15, 15, 2);
    }

    @Override
    public void lancer(Joueur joueur, Entite cible) {
        if (!peutEtreLance(joueur)) {
            System.out.println("Impossible de lancer " + nom + " (stamina insuffisante ou cooldown) !");
            return;
        }

        joueur.perdreStamina(coutStamina);
        cible.perdreVie(degats);
        cooldownRestant = cooldownMax;
        System.out.println(joueur.getName() + " lance " + nom + " sur " + cible.getName() +
                " et inflige " + degats + " dégâts !");
    }
}
