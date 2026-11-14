package com.lucas.guild.libgdx;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Item;

/** Petits outils partagés par les panneaux d'interface (inventaire, boutique). */
final class UiKit {

    private UiKit() {
    }

    /**
     * Met un contenu dans une zone qui défile à la molette quand il dépasse l'écran
     * (listes longues : boutiques, inventaire). La molette agit quand la souris est dessus.
     *
     * @param reservedHeight hauteur d'écran gardée pour le reste du panneau (titre, boutons...)
     */
    static void addScrollable(Table parent, Table content, int colspan, float reservedHeight) {
        final ScrollPane pane = new ScrollPane(content, new ScrollPane.ScrollPaneStyle());
        pane.setScrollingDisabled(true, false);
        pane.setFadeScrollBars(false);
        pane.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                if (event.getStage() != null) {
                    event.getStage().setScrollFocus(pane);
                }
            }
        });
        float max = Math.max(150f, Gdx.graphics.getHeight() - reservedHeight);
        // hauteur = celle du contenu, plafonnée : un contenu court n'est pas étiré
        float height = Math.min(max, content.getPrefHeight());
        parent.add(pane).colspan(colspan).height(height).row();
    }

    static Label label(Skin skin, String text) {
        return new Label(text, skin);
    }

    /** Un message d'information ou d'erreur, en couleur. */
    static Label message(Skin skin, String text) {
        Label label = new Label(text, skin);
        label.setColor(Color.ORANGE);
        return label;
    }

    static TextButton button(Skin skin, String text, final Runnable action) {
        TextButton button = new TextButton(text, skin);
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });
        return button;
    }

    /** « 6,5 / 30,0 kg ». */
    static String weight(double current, double max) {
        return String.format(Locale.FRANCE, "%.1f / %.1f kg", current, max);
    }

    static String kg(double value) {
        return String.format(Locale.FRANCE, "%.1f kg", value);
    }

    /** Regroupe les objets identiques (même identifiant, ou même nom) pour n'afficher qu'une ligne « x3 ». */
    static Map<String, List<Item>> stacks(List<Item> items) {
        Map<String, List<Item>> stacks = new LinkedHashMap<>();
        for (Item item : items) {
            String key = item.getId() != null ? item.getId() : item.getName();
            List<Item> stack = stacks.get(key);
            if (stack == null) {
                stack = new ArrayList<>();
                stacks.put(key, stack);
            }
            stack.add(item);
        }
        return stacks;
    }

    /** « Charge : 6,5 / 30,0 kg » avec le détail base + sac quand un sac est porté. */
    static String loadLine(Adventurer player) {
        return "Charge : " + weight(player.getInventaire().getPoidsActuel(), player.getInventaire().getPoidsMax());
    }
}
