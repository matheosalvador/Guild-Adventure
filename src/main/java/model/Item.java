package model;

public interface Item {
    String getName();
    String getDescription();
    void use(Joueur joueur, Monstre monstre);
}