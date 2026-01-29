package model;

/**
 * Représente un monstre dans le jeu.
 */
public class Monstre extends Entite implements Combatant {

    /**
     * Constructeur du monstre.
     * @param name Le nom du monstre.
     */
    public Monstre(String name) {
        super(name, 200);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void setHealth(int health) {
        this.health = Math.max(0, Math.min(health, maxHealth));
    }

    @Override
    public void setLevel(int level) {
        // Pas de niveau pour le monstre pour l'instant
    }

    @Override
    public int getLevel() {
        return 0;
    }

    /**
     * Définit la santé du monstre (surcharge avec Integer).
     * @param health La nouvelle santé.
     */
    public void setHealth(Integer health) {
        if (health != null) {
            this.health = Math.max(0, Math.min(health, maxHealth));
        }
    }


    @Override
    public void attaquer(Combatant cible) {
        if (this.estVivant()) {
            int degats = 10;
            System.out.println(this.getName() + " attaque sa cible !");
            cible.subirDegats(degats);
        }
    }

    @Override
    public void subirDegats(int montant) {
        this.perdreVie(montant);
        System.out.println(this.getName() + " a subi " + montant + " points de dégâts.");

        if (!this.estVivant()) {
            System.out.println(this.getName() + " a échoué !");
        }
    }
}
