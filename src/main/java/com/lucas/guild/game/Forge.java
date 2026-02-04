package com.lucas.guild.game;

import com.lucas.guild.model.Arme;
import com.lucas.guild.model.Armure;
import com.lucas.guild.model.Item;

import java.util.LinkedHashMap;
import java.util.Map;
// a implementer commme le reste des fichiers
public class Forge {

    private Map<Item, Integer> itemsForSale = new LinkedHashMap<>();

    public Forge() {
        itemsForSale.put(new Arme("Épée en fer", 20), 75);
        itemsForSale.put(new Armure("Plastron en cuir", 8), 60);
    }

    public void displayInventory() {
        System.out.println("--- Forge du Nain Grincheux ---");
        System.out.println("Articles disponibles :");
        int i = 1;
        for (Map.Entry<Item, Integer> entry : itemsForSale.entrySet()) {
            System.out.println(i + ". " + entry.getKey() + " - " + entry.getValue() + " Or");
            i++;
        }
    }

    public Item getItem(int index) {
        if (index > 0 && index <= itemsForSale.size()) {
            return (Item) itemsForSale.keySet().toArray()[index - 1];
        }
        return null;
    }
    
    public int getPrice(Item item) {
        return itemsForSale.getOrDefault(item, 9999);
    }
}
