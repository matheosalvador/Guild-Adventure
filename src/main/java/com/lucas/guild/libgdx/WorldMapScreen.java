package com.lucas.guild.libgdx;

import java.util.List;
import java.util.Locale;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.TownData;
import com.lucas.guild.data.WorldData;
import com.lucas.guild.game.GameSession;
import com.lucas.guild.game.SaveManager;
import com.lucas.guild.game.World;
import com.lucas.guild.model.Adventurer;

/**
 * La carte du monde, atteinte par la porte de la ville : les lieux et les routes à gauche, les
 * routes qui partent d'ici à droite avec leur distance et leur durée. Choisir une route fait
 * passer le temps et amène devant la porte de la ville d'arrivée.
 *
 * <p>Les lieux sont placés d'après leurs coordonnées (km) de world.json.</p>
 */
public class WorldMapScreen implements Screen {

    private static final float MAP_X = 30f;
    private static final float MAP_Y = 80f;
    private static final float MAP_SIZE = 400f;

    private final MainGame game;
    private final String difficulty;

    private Stage stage;
    private Skin skin;
    private BitmapFont font;
    private ShapeRenderer shapes;
    private SpriteBatch batch;
    private WorldData world;
    private Adventurer player;

    public WorldMapScreen(MainGame game, String difficulty) {
        this.game = game;
        this.difficulty = difficulty;
    }

    @Override
    public void show() {
        Gdx.input.setCursorCatched(false);
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        font = new BitmapFont();
        shapes = new ShapeRenderer();
        batch = new SpriteBatch();
        skin = new Skin();
        skin.add("default-font", font);
        TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle();
        buttonStyle.font = font;
        skin.add("default", buttonStyle);
        skin.add("default", new Label.LabelStyle(font, Color.WHITE));

        GameSession session = game.getSession();
        player = session.getPlayer();
        world = GameContent.world();

        String here = player.getTownId();
        Table side = new Table(skin);
        side.setFillParent(true);
        side.right().padRight(25);
        stage.addActor(side);

        side.add(UiKit.label(skin, "Carte du monde")).pad(5).row();
        side.add(UiKit.label(skin, "Vous êtes à " + world.place(here).name)).pad(3).row();
        side.add(UiKit.label(skin, session.getClock().getDate() + " - " + session.getClock().formatTime())).pad(3).row();
        side.add(UiKit.label(skin, "-- Routes --")).pad(10).row();

        List<World.Route> routes = World.routesFrom(player);
        for (final World.Route route : routes) {
            String text = String.format(Locale.FRANCE, "%s - %.0f km - %s", route.place.name, route.km, World.formatDuration(route.minutes));
            int arrival = (session.getClock().getCurrentHour() * 60 + session.getClock().getCurrentMinute() + route.minutes) / 60 % 24;
            if (arrival < 6 || arrival >= 20) {
                text += " (arrivée de nuit)";
            }
            side.add(UiKit.button(skin, text, new Runnable() {
                @Override
                public void run() {
                    travel(route.place.id);
                }
            })).pad(5).row();
        }
        side.add(UiKit.button(skin, "Retour à la ville", new Runnable() {
            @Override
            public void run() {
                backToTown();
            }
        })).padTop(25).row();

        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    backToTown();
                    return true;
                }
                return false;
            }
        });
    }

    private void backToTown() {
        game.setScreen(new TownScreen(game, difficulty, arrivalOf(player.getTownId())));
    }

    private void travel(String placeId) {
        GameSession session = game.getSession();
        String message = World.travel(session, placeId);
        if (message == null) {
            return;
        }
        session.getClock().postMessage(message);
        Vector3 spawn = arrivalOf(placeId);
        // sauvegarde automatique à l'arrivée, devant la porte de la nouvelle ville
        new SaveManager().saveGame(player, session.getClock(), difficulty, spawn.x, spawn.y, spawn.z, "Town");
        game.setScreen(new TownScreen(game, difficulty, spawn));
    }

    /** Où on apparaît en arrivant dans la ville du lieu {@code placeId} : devant sa porte. */
    private static Vector3 arrivalOf(String placeId) {
        TownData.Point3 a = GameContent.town(placeId).arrival;
        return a != null ? new Vector3(a.x, a.y, a.z) : new Vector3(0, 2, 50);
    }

    // ------------------------------------------------------------------ dessin

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.12f, 0.14f, 0.18f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        drawMap();
        stage.act(Math.min(delta, 1 / 30f));
        stage.draw();
    }

    private float[] project(WorldData.PlaceData p) {
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (WorldData.PlaceData q : world.places) {
            minX = Math.min(minX, q.x);
            maxX = Math.max(maxX, q.x);
            minZ = Math.min(minZ, q.z);
            maxZ = Math.max(maxZ, q.z);
        }
        float span = Math.max(Math.max(maxX - minX, maxZ - minZ), 1f);
        float scale = (MAP_SIZE - 120f) / span;
        // le nord (z négatif) est en haut de la carte
        float x = MAP_X + 60f + (p.x - minX) * scale;
        float y = MAP_Y + MAP_SIZE - 60f - (p.z - minZ) * scale;
        return new float[] {x, y};
    }

    private void drawMap() {
        shapes.setProjectionMatrix(stage.getCamera().combined);
        batch.setProjectionMatrix(stage.getCamera().combined);
        String here = player.getTownId();

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.18f, 0.24f, 0.2f, 1f);
        shapes.rect(MAP_X, MAP_Y, MAP_SIZE, MAP_SIZE);
        shapes.end();

        Gdx.gl.glLineWidth(3f);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        for (List<String> link : world.links) {
            float[] a = project(world.place(link.get(0)));
            float[] b = project(world.place(link.get(1)));
            boolean fromHere = link.contains(here);
            shapes.setColor(fromHere ? Color.GOLD : Color.GRAY);
            shapes.line(a[0], a[1], b[0], b[1]);
        }
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (WorldData.PlaceData p : world.places) {
            float[] xy = project(p);
            shapes.setColor(p.id.equals(here) ? Color.YELLOW : player.getVisitedPlaces().contains(p.id) ? Color.WHITE : Color.GRAY);
            shapes.circle(xy[0], xy[1], p.id.equals(here) ? 11f : 8f);
        }
        shapes.end();

        batch.begin();
        for (WorldData.PlaceData p : world.places) {
            float[] xy = project(p);
            boolean known = player.getVisitedPlaces().contains(p.id) || isNeighborOfHere(p.id);
            font.draw(batch, known ? p.name : "???", xy[0] - 30f, xy[1] - 16f);
        }
        batch.end();
    }

    private boolean isNeighborOfHere(String id) {
        for (WorldData.PlaceData n : world.neighbors(player.getTownId())) {
            if (n.id.equals(id)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() {
        dispose();
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        if (stage != null) {
            stage.dispose();
            skin.dispose();
            font.dispose();
            shapes.dispose();
            batch.dispose();
            stage = null;
        }
    }
}
