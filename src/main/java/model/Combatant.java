package model;

/**
 * Interface définissant les comportements de combat.
 */
public interface Combatant {
    /**
     * Attaque une cible.
     * @param cible L'entité cible de l'attaque.
     */
    void attaquer(Combatant cible);

    /**
     * Subit des dégâts.
     * @param montant Le montant des dégâts subis.
     */
    void subirDegats(int montant);

    /**
     * Vérifie si le combattant est vivant.
     * @return true si vivant, false sinon.
     */
    boolean estVivant();
}
