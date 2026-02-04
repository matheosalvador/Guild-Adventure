package model;
// a neutraliser plus tard si inutile
/**
 * Représente une attaque à distance.
 */
public class AttaqueDistance extends Attaque {

    private int portee;

    /**
     * Constructeur d'une attaque à distance.
     * @param nom Le nom de l'attaque.
     * @param degats Les dégâts de l'attaque.
     * @param portee La portée de l'attaque.
     */
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
