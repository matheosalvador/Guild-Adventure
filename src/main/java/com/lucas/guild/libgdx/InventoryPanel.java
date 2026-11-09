package com.lucas.guild.libgdx;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Inventaire;
import com.lucas.guild.model.Item;

/**
 * Le panneau d'inventaire (touche I) : or, charge, équipement porté et objets transportés,
 * avec les actions possibles (utiliser, équiper, retirer, jeter).
 */
class InventoryPanel {

    private final Table table;
    private final Skin skin;
    private final Adventurer player;

    InventoryPanel(Table table, Skin skin, Adventurer player) {
        this.table = table;
        this.skin = skin;
        this.player = player;
    }

    /** (Re)construit le panneau. {@code message} est affiché en bas (résultat de la dernière action), ou null. */
    void rebuild(String message) {
        table.clear();
        table.top().left();
        Inventaire inventory = player.getInventaire();

        table.add(UiKit.label(skin, "Inventaire")).colspan(4).left().pad(10).row();
        table.add(UiKit.label(skin, "Or : " + player.getGold() + " Po")).colspan(4).left().padLeft(10).row();

        table.add(UiKit.label(skin, UiKit.loadLine(player))).colspan(4).left().padLeft(10).row();
        double ageFactor = inventory.getCapacityFactor();
        if (inventory.getBag() != null || ageFactor < 0.995) {
            String detail = String.format(Locale.FRANCE, "(base %.1f kg + sac %.1f kg", inventory.getPoidsDeBase(), inventory.getCapaciteSac());
            if (ageFactor < 0.995) {
                detail += String.format(Locale.FRANCE, ", × %.0f %% à cause de l'âge", ageFactor * 100);
            }
            table.add(UiKit.label(skin, detail + ")")).colspan(4).left().padLeft(10).row();
        }
        double speed = player.getSpeedFactor();
        if (speed < 0.995) {
            table.add(UiKit.label(skin, String.format(Locale.FRANCE, "Vitesse de déplacement : %.0f %%", speed * 100)))
                    .colspan(4).left().padLeft(10).row();
        }

        // --- équipement porté
        table.add(UiKit.label(skin, "-- Équipement --")).colspan(4).left().pad(10).row();
        equipmentRow(Inventaire.Slot.WEAPON, inventory.getWeapon());
        equipmentRow(Inventaire.Slot.ARMOR, inventory.getArmor());
        equipmentRow(Inventaire.Slot.BAG, inventory.getBag());

        // --- objets transportés, regroupés
        table.add(UiKit.label(skin, "-- Objets --")).colspan(4).left().pad(10).row();
        List<Item> items = inventory.getItems();
        if (items.isEmpty()) {
            table.add(UiKit.label(skin, "Rien.")).colspan(4).left().padLeft(10).row();
        }
        for (Map.Entry<String, List<Item>> stack : UiKit.stacks(items).entrySet()) {
            itemRow(stack.getValue());
        }

        if (message != null) {
            table.add(UiKit.message(skin, message)).colspan(4).left().pad(10).row();
        }
    }

    private void equipmentRow(final Inventaire.Slot slot, Item equipped) {
        String text = slot.getLabel() + " : " + (equipped != null ? equipped.getName() + " (" + UiKit.kg(equipped.getPoids()) + ")" : "aucun(e)");
        table.add(UiKit.label(skin, text)).left().pad(3).padLeft(10);
        if (equipped != null) {
            table.add(UiKit.button(skin, "Retirer", new Runnable() {
                @Override
                public void run() {
                    rebuild(player.unequip(slot));
                }
            })).pad(3);
        }
        table.row();
    }

    private void itemRow(List<Item> stack) {
        final Item item = stack.get(0);
        String count = stack.size() > 1 ? " x" + stack.size() : "";
        String text = item.getName() + count + " - " + item.getDescription() + " (" + UiKit.kg(item.getPoids() * stack.size()) + ")";
        table.add(UiKit.label(skin, text)).left().pad(3).padLeft(10);

        if (item.estUtilisableHorsCombat()) {
            table.add(UiKit.button(skin, "Utiliser", new Runnable() {
                @Override
                public void run() {
                    rebuild(player.useHealingItem(item));
                }
            })).pad(3);
        } else if (isEquipment(item)) {
            table.add(UiKit.button(skin, "Équiper", new Runnable() {
                @Override
                public void run() {
                    rebuild(player.equip(item));
                }
            })).pad(3);
        } else {
            table.add(UiKit.label(skin, "")).pad(3);
        }

        table.add(UiKit.button(skin, "Jeter", new Runnable() {
            @Override
            public void run() {
                player.removeItem(item);
                rebuild(item.getName() + " jeté(e).");
            }
        })).pad(3);
        table.row();
    }

    private static boolean isEquipment(Item item) {
        return item instanceof com.lucas.guild.model.Arme
                || item instanceof com.lucas.guild.model.Armure
                || item instanceof com.lucas.guild.model.Sac;
    }
}
