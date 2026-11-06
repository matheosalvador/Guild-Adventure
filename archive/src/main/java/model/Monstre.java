package model;
// a neutraliser plus tard si inutile
public class Monstre extends Entite implements Combatant {

    private int degats;

    public Monstre(String name, int maxHealth, int degats) {
        super(name, maxHealth);
        this.degats = degats;
    }

    @Override
    public void attaquer(Combatant cible) {
        if (this.estVivant()) {
            System.out.println(this.getName() + " attaque " + ((Entite)cible).getName() + " !");
            cible.subirDegats(this.degats);
        }
    }

    @Override
    public void subirDegats(int montant) {
        this.perdreVie(montant);
        System.out.println(this.getName() + " a subi " + montant + " points de dégâts.");

        if (!this.estVivant()) {
            System.out.println(this.getName() + " est vaincu !");
        }
    }
}