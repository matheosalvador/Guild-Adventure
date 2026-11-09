package com.lucas.guild.libgdx;

import java.util.List;
import java.util.Map;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.lucas.guild.game.Shop;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Item;

/**
 * Le panneau d'une boutique : ce qu'elle vend (acheter) et ce que le joueur peut lui revendre.
 * Les règles sont dans {@link Shop} ; ce panneau ne fait qu'afficher et relayer les clics.
 */
class ShopPanel {

    private final Table table;
    private final Skin skin;
    private final Adventurer player;
    private final Shop shop;
    private final Runnable onBack;

    ShopPanel(Table table, Skin skin, Adventurer player, Shop shop, Runnable onBack) {
        this.table = table;
        this.skin = skin;
        this.player = player;
        this.shop = shop;
        this.onBack = onBack;
    }

    /** (Re)construit le panneau. {@code message} : résultat de la dernière action (refus...), ou null. */
    void rebuild(String message) {
        table.clear();

        table.add(UiKit.label(skin, shop.getTitle())).colspan(5).pad(10).row();
        table.add(UiKit.label(skin, "Or : " + player.getGold() + " Po    " + UiKit.loadLine(player))).colspan(5).padBottom(10).row();

        // --- acheter
        table.add(UiKit.label(skin, "-- Acheter --")).colspan(5).pad(5).row();
        for (final Item wanted : shop.getStock()) {
            table.add(UiKit.label(skin, wanted.getName())).left().pad(3);
            table.add(UiKit.label(skin, wanted.getDescription())).left().pad(3);
            table.add(UiKit.label(skin, UiKit.kg(wanted.getPoids()))).pad(3);
            table.add(UiKit.label(skin, shop.buyPrice(player, wanted) + " Po")).pad(3);
            table.add(UiKit.button(skin, "Acheter", new Runnable() {
                @Override
                public void run() {
                    String error = shop.buy(player, wanted);
                    rebuild(error != null ? error : wanted.getName() + " acheté(e).");
                }
            })).pad(3);
            table.row();
        }

        // --- vendre (objets transportés, pas l'équipement porté)
        table.add(UiKit.label(skin, "-- Vendre --")).colspan(5).pad(5).row();
        boolean any = false;
        for (Map.Entry<String, List<Item>> stack : UiKit.stacks(player.getInventoryItems()).entrySet()) {
            final Item item = stack.getValue().get(0);
            int price = shop.sellPrice(player, item);
            if (price <= 0) {
                continue;
            }
            any = true;
            String count = stack.getValue().size() > 1 ? " x" + stack.getValue().size() : "";
            table.add(UiKit.label(skin, item.getName() + count)).left().pad(3);
            table.add(UiKit.label(skin, "")).pad(3);
            table.add(UiKit.label(skin, UiKit.kg(item.getPoids()))).pad(3);
            table.add(UiKit.label(skin, price + " Po")).pad(3);
            table.add(UiKit.button(skin, "Vendre", new Runnable() {
                @Override
                public void run() {
                    String error = shop.sell(player, item);
                    rebuild(error != null ? error : item.getName() + " vendu(e).");
                }
            })).pad(3);
            table.row();
        }
        if (!any) {
            table.add(UiKit.label(skin, "Vous n'avez rien à vendre.")).colspan(5).pad(3).row();
        }

        table.add(UiKit.button(skin, "Retour", onBack)).colspan(5).pad(10).row();

        if (message != null) {
            table.add(UiKit.message(skin, message)).colspan(5).pad(5).row();
        }
    }
}
