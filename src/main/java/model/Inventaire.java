package model;
// a neutraliser plus tard si inutile
import java.util.ArrayList;
import java.util.List;

public class Inventaire {
    private final List<PotionSoin> potionsSoin = new ArrayList<>();
    private final List<PotionDps> potionsDps = new ArrayList<>();
    private final List<PotionMix> potionsMix = new ArrayList<>();

    public Inventaire() {
    }

    public void ajouterItem(Item item) {
        if (item instanceof PotionSoin) {
            potionsSoin.add((PotionSoin) item);
        } else if (item instanceof PotionDps) {
            potionsDps.add((PotionDps) item);
        } else if (item instanceof PotionMix) {
            potionsMix.add((PotionMix) item);
        }
    }

    public void retirerItem(Item item) {
        if (item instanceof PotionSoin) {
            potionsSoin.remove(item);
        } else if (item instanceof PotionDps) {
            potionsDps.remove(item);
        } else if (item instanceof PotionMix) {
            potionsMix.remove(item);
        }
    }

    public List<Item> getItems() {
        List<Item> allItems = new ArrayList<>();
        allItems.addAll(potionsSoin);
        allItems.addAll(potionsDps);
        allItems.addAll(potionsMix);
        return allItems;
    }

    public boolean estVide() {
        return potionsSoin.isEmpty() && potionsDps.isEmpty() && potionsMix.isEmpty();
    }
}