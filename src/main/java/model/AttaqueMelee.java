package model;

public class AttaqueMelee extends Attaque {

    public AttaqueMelee(String nom, int degats) {
        super(nom, degats);
    }

    @Override
    public void executer(Entite attaquant, Entite cible) {
        cible.perdreVie(degats);
        System.out.println(attaquant.getName() +
                " frappe " + cible.getName() +
                " avec " + nom + " et inflige " +
                degats + " dégâts !");
    }
}
