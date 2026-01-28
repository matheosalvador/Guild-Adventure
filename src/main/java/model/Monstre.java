package model;

public class Monstre extends Entite implements Combatable {

    public Monstre(String name) {
        super(name, 200);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public void setHealth(Integer health) {
        this.health = health;
    }


    @Override
    public void attaquer(Combatable cible) {
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