package model;

public interface Combatable {
    void attaquer(Combatable cible);
    void subirDegats(int montant);
    boolean estVivant();
}