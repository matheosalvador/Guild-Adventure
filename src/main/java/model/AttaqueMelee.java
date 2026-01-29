package model;

/**
 * Représente une attaque de mêlée (corps à corps).
 */
public class AttaqueMelee extends Attaque {

    /**
     * Constructeur d'une attaque de mêlée.
     * @param nom Le nom de l'attaque.
     * @param degats Les dégâts de l'attaque.
     */
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
