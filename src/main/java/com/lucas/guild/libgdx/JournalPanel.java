package com.lucas.guild.libgdx;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.LoreData;
import com.lucas.guild.data.WorldData;
import com.lucas.guild.game.Lore;
import com.lucas.guild.game.WorldEvents;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Quest;

/**
 * Le journal (touche J) : les quêtes en cours, les rumeurs entendues, les textes lus et les lieux
 * visités. Tout ce que le personnage a appris, rangé au même endroit.
 */
class JournalPanel {

    private final Table table;
    private final Skin skin;
    private final Adventurer player;

    JournalPanel(Table table, Skin skin, Adventurer player) {
        this.table = table;
        this.skin = skin;
        this.player = player;
    }

    private static float textWidth() {
        return Math.max(300f, Math.min(720f, Gdx.graphics.getWidth() - 120f));
    }

    private void line(Table out, String text) {
        Label label = UiKit.label(skin, text);
        label.setWrap(true);
        out.add(label).width(textWidth()).left().padLeft(10).padBottom(4).row();
    }

    private void section(Table out, String title) {
        out.add(UiKit.label(skin, title)).left().pad(10).row();
    }

    void rebuild() {
        table.clear();
        table.top().left();
        table.add(UiKit.label(skin, "Journal")).left().pad(10).row();

        Table list = new Table(skin);
        list.top().left();

        WorldData world = GameContent.world();
        section(list, "-- Quêtes en cours --");
        if (player.getActiveQuests().isEmpty()) {
            line(list, "Aucune. Le tableau de la guilde en propose.");
        }
        for (String title : player.getActiveQuests()) {
            Quest quest = null;
            for (Quest q : GameContent.quests()) {
                if (q.getTitle().equals(title)) {
                    quest = q;
                }
            }
            line(list, quest == null ? title : title + " - " + quest.getDescription() + " (" + quest.getGoldReward() + " Po)");
        }
        line(list, "Quêtes terminées : " + player.getDoneQuests().size());

        section(list, "-- Rumeurs entendues --");
        List<LoreData.RumorData> rumors = Lore.heardRumors(player);
        if (rumors.isEmpty()) {
            line(list, "Aucune. Offrez une bière à un aubergiste.");
        }
        for (LoreData.RumorData rumor : rumors) {
            line(list, "- " + rumor.text);
        }

        section(list, "-- Textes lus --");
        List<LoreData.TextData> texts = Lore.readTexts(player);
        if (texts.isEmpty()) {
            line(list, "Aucun. Les panneaux et les stèles se lisent avec E.");
        }
        for (LoreData.TextData text : texts) {
            line(list, text.title + " : " + text.text);
        }

        section(list, "-- Événements connus --");
        boolean anyEvent = false;
        for (String id : player.getVisitedPlaces()) {
            String events = WorldEvents.describeAt(player, id);
            if (events != null && world.place(id) != null) {
                line(list, world.place(id).name + " : " + events);
                anyEvent = true;
            }
        }
        if (!anyEvent) {
            line(list, "Rien de spécial dans les villes que vous connaissez.");
        }

        section(list, "-- Lieux visités --");
        List<String> names = new ArrayList<>();
        for (String id : player.getVisitedPlaces()) {
            if (world.place(id) != null) {
                names.add(world.place(id).name);
            }
        }
        line(list, String.join(", ", names));

        // les textes sont à retour à la ligne automatique : leur hauteur n'est connue qu'après la mise en page,
        // donc la zone qui défile prend toute la hauteur disponible
        final com.badlogic.gdx.scenes.scene2d.ui.ScrollPane pane =
                new com.badlogic.gdx.scenes.scene2d.ui.ScrollPane(list, new com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle());
        pane.setScrollingDisabled(true, false);
        pane.setFadeScrollBars(false);
        pane.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public void enter(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer,
                              com.badlogic.gdx.scenes.scene2d.Actor fromActor) {
                if (event.getStage() != null) {
                    event.getStage().setScrollFocus(pane);
                }
            }
        });
        table.add(pane).height(Math.max(150f, Gdx.graphics.getHeight() - 120f)).left().row();
        table.add(UiKit.label(skin, "J ou Échap : fermer")).left().pad(10).row();
    }
}
