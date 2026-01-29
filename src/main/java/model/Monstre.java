package model;

public class Monstre extends Entite implements Combatant {

    public Monstre(String name) {
        super(name, 200);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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


    public void setHealth(Integer health) {
        this.health = health;
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