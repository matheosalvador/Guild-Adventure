package model;

public abstract class Potion {

    private String name;
    private int stock;

    public Potion(String name) {
        this.name = name;
        stock = 10;
    }

    public boolean use() {
        if (stock <= 0) return false;

        stock--;
        applyEffect();
        return true;
    }

    // Chaque potion définit son effet directement
    protected abstract void applyEffect();

    public String getName() {
        return name;
    }

    public int getStock() {
        return stock;
    }

    public boolean isEmpty() {
        return stock == 0;
    }
}
