package model;
// a neutraliser plus tard si inutile
/**
 * Représente le personnage contrôlé par l'utilisateur.
 * Hérite d'Entite et implémente Combatant, ajoutant des mécaniques spécifiques
 * comme la stamina, un inventaire et la capacité de se défendre.
 */
public class Joueur extends Entite implements Combatant {

    private int stamina;
    private int maxStamina;
    private Inventaire inventaire;
    private boolean enDefense = false;

    /**
     * Constructeur par défaut. Initialise un joueur avec un inventaire vide.
     */
    public Joueur() {
        super();
        this.inventaire = new Inventaire();
    }

    /**
     * Crée un nouveau joueur avec un nom, des statistiques de base et un inventaire.
     *
     * @param name Le nom du joueur.
     */
    public Joueur(String name) {
        super(name, 100);
        this.maxStamina = 100;
        this.stamina = maxStamina;
        this.inventaire = new Inventaire();
    }

    /**
     * Active ou désactive la posture de défense du joueur.
     *
     * @param enDefense true pour activer la défense, false pour la désactiver.
     */
    public void setEnDefense(boolean enDefense) {
        this.enDefense = enDefense;
    }

    /**
     * Vérifie si le joueur est actuellement en posture de défense.
     *
     * @return true si le joueur se défend, sinon false.
     */
    public boolean isEnDefense() {
        return enDefense;
    }

    public Inventaire getInventaire() {
        return inventaire;
    }

    public int getStamina() {
        return stamina;
    }

    public void setStamina(int stamina) {
        this.stamina = stamina;
    }

    public int getMaxStamina() {
        return maxStamina;
    }

    public void setMaxStamina(int maxStamina) {
        this.maxStamina = maxStamina;
    }

    /**
     * Augmente la stamina du joueur sans dépasser le maximum.
     *
     * @param montant La quantité de stamina à ajouter.
     */
    public void ajouterStamina(int montant) {
        if (montant <= 0) return;
        stamina = Math.min(stamina + montant, maxStamina);
    }

    /**
     * Diminue la stamina du joueur sans descendre en dessous de zéro.
     *
     * @param montant La quantité de stamina à retirer.
     */
    public void perdreStamina(int montant) {
        if (montant <= 0) return;
        stamina = Math.max(stamina - montant, 0);
    }

    /**
     * {@inheritDoc}
     * L'attaque de base du joueur. Inflige des dégâts fixes à une cible.
     */
    @Override
    public void attaquer(Combatant cible) {
        int degats = 15; // Dégâts de base
        System.out.println(this.getName() + " attaque !");
        cible.subirDegats(degats);
    }

    /**
     * {@inheritDoc}
     * Si le joueur est en posture de défense, les dégâts subis sont réduits de 50%.
     * Sinon, il subit la totalité des dégâts.
     */
    @Override
    public void subirDegats(int montant) {
        if (enDefense) {
            int degatsReduits = montant / 2;
            this.perdreVie(degatsReduits);
            System.out.println(this.getName() + " se défend et ne subit que " + degatsReduits + " dégâts !");
        } else {
            this.perdreVie(montant);
            System.out.println(this.getName() + " a subi " + montant + " dégâts !");
        }
    }
}