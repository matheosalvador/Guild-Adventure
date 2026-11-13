package com.lucas.guild.libgdx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.SkillData;
import com.lucas.guild.game.DebugActions;
import com.lucas.guild.game.DebugMode;
import com.lucas.guild.game.GameSession;
import com.lucas.guild.model.Item;

/**
 * Le panneau du mode debug (F10) : de l'or, des objets, des niveaux de compétence, le temps,
 * l'âge, la vitesse, et des raccourcis vers les bâtiments. Les règles sont dans {@link DebugActions}.
 */
class DebugPanel {

    private static final int PER_ROW = 4;

    private final Table table;
    private final Skin skin;
    private final GameSession session;
    private final DebugActions actions;
    private final MainGame game;
    private final String difficulty;
    private final BaseWorldScreen screen;

    DebugPanel(Table table, Skin skin, MainGame game, BaseWorldScreen screen, GameSession session, String difficulty) {
        this.screen = screen;
        this.table = table;
        this.skin = skin;
        this.game = game;
        this.session = session;
        this.difficulty = difficulty;
        this.actions = new DebugActions(session);
    }

    /** (Re)construit le panneau. {@code message} : résultat de la dernière action, ou null. */
    void rebuild(String message) {
        table.clear();
        table.top().left();

        table.add(UiKit.label(skin, "MODE DEBUG  (F10 ou Échap pour fermer)")).left().pad(8).row();
        if (message != null) {
            table.add(UiKit.message(skin, message)).left().padLeft(10).row();
        }

        Table list = new Table(skin);
        list.top().left();

        section(list, "Personnage");
        buttons(list,
                action("+100 Po", () -> actions.addGold(100)),
                action("+1000 Po", () -> actions.addGold(1000)),
                action("Soigner à fond", actions::healFull),
                action("-20 PV", () -> actions.hurt(20)));
        buttons(list,
                action("Vieillir +10 ans", () -> actions.ageBy(10)),
                action("Vieillir +25 ans", () -> actions.ageBy(25)),
                action("Rajeunir -10 ans", () -> actions.ageBy(-10)));

        section(list, "Temps (" + session.getClock().formatTime() + ", jour " + session.getClock().getCurrentDay() + ")");
        buttons(list,
                action("+1 heure", () -> actions.advanceHours(1)),
                action("+6 heures", () -> actions.advanceHours(6)),
                action("+1 jour", () -> actions.advanceDays(1)),
                action("+1 saison", () -> actions.advanceDays(28)),
                action("+1 an", () -> actions.advanceDays(112)));
        buttons(list,
                action("Aller à 6h", () -> actions.advanceToHour(6)),
                action("Aller à 12h", () -> actions.advanceToHour(12)),
                action("Aller à 19h", () -> actions.advanceToHour(19)),
                action("Aller à 23h", () -> actions.advanceToHour(23)));
        boolean fastTime = session.getClock().getTimeScale() > 1f;
        boolean fastWalk = DebugMode.getSpeedMultiplier() > 1f;
        buttons(list,
                action("Temps x60 : " + (fastTime ? "OUI" : "non"), () -> {
                    session.getClock().setTimeScale(fastTime ? 1f : 60f);
                    return "Temps " + (fastTime ? "normal." : "x60 (une heure de jeu par seconde).");
                }),
                action("Vitesse x3 : " + (fastWalk ? "OUI" : "non"), () -> {
                    DebugMode.setSpeedMultiplier(fastWalk ? 1f : 3f);
                    return "Déplacement " + (fastWalk ? "normal." : "x3.");
                }));

        section(list, "Compétences (+1 niveau)");
        java.util.List<Object[]> row = new java.util.ArrayList<>();
        for (final SkillData def : session.getPlayer().getSkills().definitions()) {
            int level = session.getPlayer().getSkills().getLevel(def.id);
            row.add(action(def.label + " " + level + "/" + def.maxLevel(), () -> actions.skillLevelUp(def.id)));
        }
        buttons(list, row.toArray());

        section(list, "Combat");
        boolean invincible = DebugMode.isInvincible();
        buttons(list,
                action("Rat devant moi", () -> screen.debugSpawn("giant_rat")),
                action("Tuer tout", screen::debugKillAll),
                action("Invincible : " + (invincible ? "OUI" : "non"), () -> {
                    DebugMode.setInvincible(!invincible);
                    return "Invincible : " + (!invincible ? "oui" : "non") + ".";
                }));

        section(list, "Voyager (sans durée)");
        java.util.List<Object[]> places = new java.util.ArrayList<>();
        for (final com.lucas.guild.data.WorldData.PlaceData place : GameContent.world().places) {
            places.add(action(place.name, () -> {
                session.getPlayer().setTownId(place.id);
                session.getPlayer().markVisited(place.id);
                com.lucas.guild.data.TownData.Point3 a = GameContent.town(place.id).arrival;
                return go(new TownScreen(game, difficulty, a != null ? new com.badlogic.gdx.math.Vector3(a.x, a.y, a.z) : new com.badlogic.gdx.math.Vector3(0, 2, 50)));
            }));
        }
        buttons(list, places.toArray());

        section(list, "Aller à");
        buttons(list,
                action("Ville", () -> go(new TownScreen(game, difficulty))),
                action("Forge", () -> go(new ForgeScreen(game, difficulty))),
                action("Guilde", () -> go(new GuildScreen(game, difficulty))),
                action("Auberge", () -> go(new InnScreen(game, difficulty))),
                action("Cave", () -> go(new CellarScreen(game, difficulty))));

        section(list, "Objets");
        java.util.List<Object[]> items = new java.util.ArrayList<>();
        for (final Item item : GameContent.items()) {
            items.add(action("+ " + item.getName(), () -> actions.giveItem(item.getId())));
        }
        buttons(list, items.toArray());

        UiKit.addScrollable(table, list, 1, 90);
    }

    // ------------------------------------------------------------------ construction

    /** Un bouton : son texte et ce qu'il fait (renvoie la phrase affichée en haut). */
    private static Object[] action(String label, java.util.function.Supplier<String> run) {
        return new Object[] {label, run};
    }

    private String go(com.badlogic.gdx.Screen screen) {
        game.setScreen(screen);
        return null;
    }

    private void section(Table list, String title) {
        Label label = UiKit.label(skin, "-- " + title + " --");
        label.setColor(Color.YELLOW);
        list.add(label).left().padTop(10).padLeft(10).row();
    }

    @SuppressWarnings("unchecked")
    private void buttons(Table list, Object... defs) {
        Table row = new Table(skin);
        int n = 0;
        for (Object def : defs) {
            final Object[] d = (Object[]) def;
            final java.util.function.Supplier<String> run = (java.util.function.Supplier<String>) d[1];
            row.add(UiKit.button(skin, (String) d[0], new Runnable() {
                @Override
                public void run() {
                    String result = run.get();
                    if (result != null) {
                        rebuild(result);
                    }
                }
            })).pad(3).left();
            if (++n % PER_ROW == 0) {
                row.row();
            }
        }
        list.add(row).left().padLeft(8).row();
    }
}
