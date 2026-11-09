package com.lucas.guild.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.ShopData;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Inventaire;
import com.lucas.guild.model.Item;

/**
 * Une boutique (assets/data/shops.json) : ce qu'elle vend, à quel prix, et les règles d'achat
 * et de vente. Pas de dépendance à l'interface : l'écran affiche, cette classe décide.
 *
 * Les méthodes {@link #buy} et {@link #sell} renvoient {@code null} quand c'est fait, sinon la
 * raison du refus, prête à être affichée.
 */
public class Shop {

    private final ShopData data;

    /** @param shopId identifiant dans shops.json (« forge », « guild »...) */
    public Shop(String shopId) {
        this.data = GameContent.shop(shopId);
    }

    public String getTitle() {
        return data.title;
    }

    /** Les objets en vente (des modèles à afficher ; l'achat donne une nouvelle instance). */
    public List<Item> getStock() {
        List<Item> stock = new ArrayList<>();
        for (String id : data.stock) {
            stock.add(GameContent.newItem(id));
        }
        return stock;
    }

    /** Prix payé au joueur quand il revend {@code item} à la boutique. */
    public int sellPrice(Item item) {
        return item.getPrix() * data.sellPercent / 100;
    }

    /** Le joueur achète l'objet {@code wanted} (un élément de {@link #getStock()}). */
    public String buy(Adventurer player, Item wanted) {
        int price = wanted.getPrix();
        if (player.getGold() < price) {
            return "Pas assez d'or : il vous faut " + price + " Po.";
        }
        Item bought = GameContent.newItem(wanted.getId());
        Inventaire inventory = player.getInventaire();
        if (!inventory.peutPorter(bought)) {
            return String.format(Locale.FRANCE, "Trop lourd : il vous manque %.1f kg de capacité.", inventory.poidsManquantPour(bought));
        }
        player.spendGold(price);
        player.addItem(bought);
        return null;
    }

    /** Le joueur revend un objet de son inventaire (un objet équipé doit d'abord être retiré). */
    public String sell(Adventurer player, Item item) {
        if (!player.getInventoryItems().contains(item)) {
            return "Vous ne portez pas cet objet dans votre inventaire.";
        }
        int price = sellPrice(item);
        if (price <= 0) {
            return "La boutique n'en veut pas.";
        }
        player.removeItem(item);
        player.addGold(price);
        return null;
    }
}
