package model;

/**
 * Sort de Boule de feu.
 * Inflige des dégâts importants mais coûte cher en stamina et a un cooldown.
 */
public class BouleDeFeu extends Sort {

    /**
     * Constructeur de la Boule de feu.
     */
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
