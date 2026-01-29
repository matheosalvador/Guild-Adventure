package model;

/**
 * Représente le joueur dans le jeu.
 * Le joueur possède de la stamina en plus de sa santé.
 */
public class Joueur extends Entite implements Combatant {

    private int stamina;
    private int maxStamina;
    private final Inventaire inventaire;

    /**
     * Constructeur du joueur.
     * @param name Le nom du joueur.
     */
    public Joueur(String name) {
        super(name, 100); // santé max = 100 par défaut
        this.stamina = 100;
        this.maxStamina = 100;
        this.stamina = maxStamina;
        this.inventaire = new Inventaire();
    }

    public void ramasserItem(Item item){
        inventaire.ajouterItem(item);
        System.out.println(getName() + " ramasse " + item.getNom());
    }

    public void utiliserItem(Item item, Monstre monstre) {
        item.utiliser(this, monstre);
        inventaire.retirerItem(item);
    }


    public Inventaire getInventaire() {
        return inventaire; }

    public int getStamina() {

        return stamina;
    }

    public int getMaxStamina() {

        return maxStamina;
    }

    public void ajouterStamina(int montant) {
        if (montant <= 0) return;
        stamina = Math.min(stamina + montant, maxStamina);
    }

    /**
     * Retire de la stamina au joueur.
     * @param montant Le montant de stamina à retirer.
     */
    public void perdreStamina(int montant) {
        if (montant <= 0) return;
        stamina = Math.max(stamina - montant, 0);
    }

    // --- Combat ---
    @Override
    public void attaquer(Combatant cible) {
        int degats = 15;
        System.out.println(this.getName() + " attaque !");
        cible.subirDegats(degats);
    }

    @Override
    public void subirDegats(int montant) {
        this.perdreVie(montant);
        System.out.println(this.getName() + " a subi " + montant + " dégâts !");
    }

    @Override
    public void setHealth(int health) {

    }

    @Override
    public void setLevel(int level) {

    }

    @Override
    public int getLevel() {
        return 0;
    }
}


