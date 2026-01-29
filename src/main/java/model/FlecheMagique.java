package model;

/**
 * Sort de Flèche magique.
 * Inflige des dégâts modérés avec un coût et un cooldown réduits.
 */
public class FlecheMagique extends Sort {

    /**
     * Constructeur de la Flèche magique.
     */
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
