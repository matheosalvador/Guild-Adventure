package model;

import java.util.ArrayList;
import java.util.List;

public class Inventaire {

    private final List<Item> items = new ArrayList<>();

    public void ajouterItem(Item item) {
        items.add(item);
    }

    public void retirerItem(Item item) {
        items.remove(item);
    }

    public boolean contient(Item item) {
        return items.contains(item);
    }

    public List<Item> getItems() {
        return List.copyOf(items);
    }
}
