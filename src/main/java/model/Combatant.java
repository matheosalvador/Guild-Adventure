package model;

public interface Combatant {
    void attaquer(Combatant cible);
    void subirDegats(int montant);
    boolean estVivant();
}
