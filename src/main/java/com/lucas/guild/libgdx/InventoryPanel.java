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
    /** Où s'écrivent les lignes : la zone qui défile pendant la construction, sinon le panneau. */
    private Table out;
    /** Ce que devient un objet jeté (le poser au sol devant le joueur) ; sans lui, l'objet disparaît. */
    private java.util.function.Consumer<Item> dropHandler;

    void setDropHandler(java.util.function.Consumer<Item> dropHandler) {
        this.dropHandler = dropHandler;
    }

    InventoryPanel(Table table, Skin skin, Adventurer player) {
        this.table = table;
        this.skin = skin;
        this.player = player;
        this.out = table;
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

        Table list = new Table(skin);
        out = list;
        // --- équipement porté
        out.add(UiKit.label(skin, "-- Équipement --")).colspan(4).left().pad(10).row();
        equipmentRow(Inventaire.Slot.WEAPON, inventory.getWeapon());
        equipmentRow(Inventaire.Slot.ARMOR, inventory.getArmor());
        equipmentRow(Inventaire.Slot.BAG, inventory.getBag());

        // --- objets transportés, regroupés
        out.add(UiKit.label(skin, "-- Objets --")).colspan(4).left().pad(10).row();
        List<Item> items = inventory.getItems();
        if (items.isEmpty()) {
            out.add(UiKit.label(skin, "Rien.")).colspan(4).left().padLeft(10).row();
        }
        for (Map.Entry<String, List<Item>> stack : UiKit.stacks(items).entrySet()) {
            itemRow(stack.getValue());
        }

        out = table;
        UiKit.addScrollable(table, list, 4, 230);

        if (message != null) {
            table.add(UiKit.message(skin, message)).colspan(4).left().pad(10).row();
        }
    }

    private void equipmentRow(final Inventaire.Slot slot, Item equipped) {
        String text = slot.getLabel() + " : " + (equipped != null ? equipped.getName() + " (" + UiKit.kg(equipped.getPoids()) + ")" : "aucun(e)");
        out.add(UiKit.label(skin, text)).left().pad(3).padLeft(10);
        if (equipped != null) {
            out.add(UiKit.button(skin, "Retirer", new Runnable() {
                @Override
                public void run() {
                    rebuild(player.unequip(slot));
                }
            })).pad(3);
        }
        out.row();
    }

    private void itemRow(List<Item> stack) {
        final Item item = stack.get(0);
        String count = stack.size() > 1 ? " x" + stack.size() : "";
        String text = item.getName() + count + " - " + item.getDescription() + " (" + UiKit.kg(item.getPoids() * stack.size()) + ")";
        out.add(UiKit.label(skin, text)).left().pad(3).padLeft(10);

        if (item.estUtilisableHorsCombat()) {
            out.add(UiKit.button(skin, "Utiliser", new Runnable() {
                @Override
                public void run() {
                    rebuild(player.useHealingItem(item));
                }
            })).pad(3);
        } else if (isEquipment(item)) {
            out.add(UiKit.button(skin, "Équiper", new Runnable() {
                @Override
                public void run() {
                    rebuild(player.equip(item));
                }
            })).pad(3);
        } else {
            out.add(UiKit.label(skin, "")).pad(3);
        }

        out.add(UiKit.button(skin, "Jeter", new Runnable() {
            @Override
            public void run() {
                player.removeItem(item);
                if (dropHandler != null) {
                    dropHandler.accept(item);
                    rebuild(item.getName() + " posé(e) au sol.");
                } else {
                    rebuild(item.getName() + " jeté(e).");
                }
            }
        })).pad(3);
        out.row();
    }

    private static boolean isEquipment(Item item) {
        return item instanceof com.lucas.guild.model.Arme
                || item instanceof com.lucas.guild.model.Armure
                || item instanceof com.lucas.guild.model.Sac;
    }
}
