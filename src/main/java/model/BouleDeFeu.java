package model;

public class BouleDeFeu extends Sort {

    public BouleDeFeu() {
        super("Boule de feu", 25, 20, 3); // dégâts, coût stamina, cooldown
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
