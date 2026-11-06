package com.lucas.guild.game;

import com.lucas.guild.model.Item;
import com.lucas.guild.model.PotionDeSoin;

import java.util.ArrayList;
import java.util.List;

public class Boutique {

    private List<Item> itemsForSale = new ArrayList<>();

    public Boutique() {
        // On remplit les étagères de la boutique
        itemsForSale.add(new PotionDeSoin("Petite Potion", 30));
        itemsForSale.add(new PotionDeSoin("Grande Potion", 80));
    }

    public void displayInventory() {
        System.out.println("--- Boutique d'Objets ---");
        System.out.println("Articles disponibles :");
        for (int i = 0; i < itemsForSale.size(); i++) {
            Item item = itemsForSale.get(i);
            // potentiel rpprix a implementé dans LWJGL
            int price = (item.getName().equals("Grande Potion")) ? 25 : 10;
            System.out.println((i + 1) + ". " + item.getName() + " - " + price + " Or");
        }
    }

    public Item getItem(int index) {
        if (index >= 0 && index < itemsForSale.size()) {
            return itemsForSale.get(index);
        }
        return null;
    }
    
    public int getPrice(Item item) {
        if (item.getName().equals("Grande Potion")) {
            return 25;
        }
        return 10;
    }
}
