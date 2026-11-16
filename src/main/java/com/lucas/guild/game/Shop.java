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

    private final String shopId;
    private final ShopData data;

    /** @param shopId identifiant dans shops.json (« forge », « guild »...) */
    public Shop(String shopId) {
        this.shopId = shopId;
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

    /** Prix de l'objet dans cette boutique : le catalogue, ajusté par le {@code priceFactor} de la boutique (de la ville). */
    private int shopPrice(Item item) {
        return (int) Math.round(item.getPrix() * data.priceFactor / 100.0);
    }

    /** Le même prix pour ce personnage : une fin de l'histoire peut avoir changé le {@code priceFactor} de la boutique. */
    private int shopPrice(Adventurer player, Item item) {
        int factor = new Story(player, null).priceFactor(shopId, data.priceFactor);
        return (int) Math.round(item.getPrix() * factor / 100.0);
    }

    /** Prix d'achat de l'objet ici, avec les événements en cours dans la ville (fête, pénurie...). */
    private int eventPrice(Adventurer player, Item item) {
        int percent = WorldEvents.buyPercent(player, player.getTownId(), item);
        int price = shopPrice(player, item);
        return percent == 100 ? price : (int) Math.round(price * percent / 100.0);
    }

    /** Une phrase sur les événements qui jouent sur les prix de cette ville, ou null s'il n'y en a pas. */
    public String eventLine(Adventurer player) {
        return WorldEvents.describeAt(player, player.getTownId());
    }

    /** Prix de base payé quand on revend {@code item} à la boutique (sans le talent du vendeur). */
    public int sellPrice(Item item) {
        return shopPrice(item) * data.sellPercent / 100;
    }

    /** Prix que {@code player} paie pour {@code item} : le prix affiché, réduit par sa compétence Marchandage. */
    public int buyPrice(Adventurer player, Item item) {
        int price = eventPrice(player, item);
        if (price <= 0) {
            return 0;
        }
        int discount = (int) Math.round(player.getSkillBonus("buyDiscountPercent"));
        // arrondi au plus proche : une réduction de 2 % ne doit pas faire perdre 1 Po sur un objet à 10 Po
        return Math.max(1, (int) Math.round(price * (100 - Math.min(discount, 90)) / 100.0));
    }

    /** Prix que {@code player} reçoit pour {@code item} : la part de base, augmentée par son Marchandage. */
    public int sellPrice(Adventurer player, Item item) {
        int percent = data.sellPercent + (int) Math.round(player.getSkillBonus("sellBonusPercent"));
        return (int) ((long) shopPrice(player, item) * Math.min(percent, 100) * WorldEvents.sellPercent(player, player.getTownId(), item) / 10000L);
    }

    /** Le joueur achète l'objet {@code wanted} (un élément de {@link #getStock()}). */
    public String buy(Adventurer player, Item wanted) {
        int price = buyPrice(player, wanted);
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
        player.train("haggling", price);
        return null;
    }

    /** Le joueur revend un objet de son inventaire (un objet équipé doit d'abord être retiré). */
    public String sell(Adventurer player, Item item) {
        if (!player.getInventoryItems().contains(item)) {
            return "Vous ne portez pas cet objet dans votre inventaire.";
        }
        int price = sellPrice(player, item);
        if (price <= 0) {
            return "La boutique n'en veut pas.";
        }
        player.removeItem(item);
        player.addGold(price);
        player.train("haggling", price);
        return null;
    }
}
