package model;

public class Joueur extends Entite implements Combatable {

    private int stamina;
    private int maxStamina;

    public Joueur(String name) {
        super(name, 100);
        this.maxStamina = 100;
        this.stamina = maxStamina;
    }


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

    public void perdreStamina(int montant) {
        if (montant <= 0) return;
        stamina = Math.max(stamina - montant, 0);
    }

    @Override
    public void attaquer(Combatable cible) {
        int degats = 15;
        System.out.println(this.getName() + " attaque !");
        cible.subirDegats(degats);
    }

    @Override
    public void subirDegats(int montant) {
        this.perdreVie(montant);
        System.out.println(this.getName() + " a subi " + montant + " dégâts !");
    }


}