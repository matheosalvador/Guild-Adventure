package model;
// a neutraliser plus tard si inutile
public interface Item {
    String getName();
    String getDescription();
    void use(Joueur joueur, Monstre monstre);
    double getPoids();
}