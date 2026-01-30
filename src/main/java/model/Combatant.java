package model;

/**
 * Définit le contrat pour toute entité capable de participer à un combat.
 * Les combattants peuvent attaquer, subir des dégâts et être vérifiés pour leur état de vie.
 */
public interface Combatant {

    /**
     * Exécute une action d'attaque contre une autre entité combattante.
     *
     * @param cible Le combattant qui subira l'attaque.
     */
    void attaquer(Combatant cible);

    /**
     * Applique une quantité de dégâts au combattant.
     * La logique de réduction (défense, armure, etc.) est gérée à l'intérieur de cette méthode.
     *
     * @param montant La quantité de dégâts bruts à infliger.
     */
    void subirDegats(int montant);

    /**
     * Vérifie si le combattant est toujours en vie.
     *
     * @return true si le combattant est en vie, sinon false.
     */
    boolean estVivant();
}