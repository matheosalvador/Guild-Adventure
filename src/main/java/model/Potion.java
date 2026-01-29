package model;

/**
 * Classe abstraite représentant une potion.
 */
public abstract class Potion {

    private String name;
    private int stock;

    /**
     * Constructeur d'une potion.
     * @param name Le nom de la potion.
     */
    public Potion(String name) {
        this.name = name;
        stock = 10;
    }

    /**
     * Utilise la potion si le stock le permet.
     * @return true si la potion a été utilisée, false sinon.
     */
    public boolean use() {
        if (stock <= 0) return false;

        stock--;
        applyEffect();
        return true;
    }

    /**
     * Applique l'effet de la potion.
     * Doit être implémenté par les sous-classes.
     */
    protected abstract void applyEffect();

    /**
     * Récupère le nom de la potion.
     * @return Le nom.
     */
    public String getName() {
        return name;
    }

    /**
     * Récupère le stock restant.
     * @return Le stock.
     */
    public int getStock() {
        return stock;
    }

    /**
     * Vérifie si le stock est vide.
     * @return true si vide, false sinon.
     */
    public boolean isEmpty() {
        return stock == 0;
    }
}
