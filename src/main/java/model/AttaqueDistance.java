package model;

public class AttaqueDistance extends Attaque {

    private int portee;

    public AttaqueDistance(String nom, int degats, int portee) {
        super(nom, degats);
        this.portee = portee;
    }

    @Override
    public void executer(Entite attaquant, Entite cible) {
        cible.perdreVie(degats);
        System.out.println(attaquant.getName() +
                " lance " + nom + " sur " + cible.getName() +
                " et inflige " + degats + " dégâts !");
    }
}
