package model;

public abstract class Item {
    private final String nom;

    public Item(String nom) {
        this.nom = nom;
    }

    public String getNom() {
        return nom;
    }

    @Override
    public String toString() {
        return getNom();
    }

    public abstract void utiliser(Joueur joueur, Monstre monstre);
}
