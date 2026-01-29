package model;

/**
 * Classe abstraite représentant un sort magique.
 */
public abstract class Sort {

    protected String nom;
    protected int degats;
    protected int coutStamina;
    protected int cooldownMax;
    protected int cooldownRestant;

    /**
     * Constructeur d'un sort.
     * @param nom Le nom du sort.
     * @param degats Les dégâts du sort.
     * @param coutStamina Le coût en stamina.
     * @param cooldown Le temps de recharge (en tours).
     */
    public Sort(String nom, int degats, int coutStamina, int cooldown) {
        this.nom = nom;
        this.degats = degats;
        this.coutStamina = coutStamina;
        this.cooldownMax = cooldown;
        this.cooldownRestant = 0;
    }

    /**
     * Récupère le nom du sort.
     * @return Le nom.
     */
    public String getNom() { return nom; }

    /**
     * Récupère les dégâts du sort.
     * @return Les dégâts.
     */
    public int getDegats() { return degats; }

    /**
     * Récupère le coût en stamina.
     * @return Le coût.
     */
    public int getCoutStamina() { return coutStamina; }

    /**
     * Récupère le cooldown restant.
     * @return Le nombre de tours restants.
     */
    public int getCooldownRestant() { return cooldownRestant; }

    /**
     * Vérifie si le sort peut être lancé.
     * @param joueur Le joueur qui veut lancer le sort.
     * @return true si possible, false sinon.
     */
    public boolean peutEtreLance(Joueur joueur) {
        return joueur.getStamina() >= coutStamina && cooldownRestant == 0;
    }

    /**
     * Décrémente le cooldown du sort (à appeler à chaque tour).
     */
    public void decrementerCooldown() {
        if (cooldownRestant > 0) cooldownRestant--;
    }

    /**
     * Lance le sort sur une cible.
     * @param joueur Le joueur qui lance le sort.
     * @param cible La cible du sort.
     */
    public abstract void lancer(Joueur joueur, Entite cible);
}
