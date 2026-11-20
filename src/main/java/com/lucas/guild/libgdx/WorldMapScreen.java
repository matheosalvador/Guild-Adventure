package com.lucas.guild.libgdx;

import java.util.List;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.WorldData;
import com.lucas.guild.game.GameSession;
import com.lucas.guild.game.SaveManager;
import com.lucas.guild.game.World;
import com.lucas.guild.model.Adventurer;

/**
 * La carte du monde (touche M dans le monde ouvert) : les lieux et les routes à gauche, sans rien qui dise où l'on
 * est (seuls les lieux déjà atteints ont leur nom) ; à droite, le voyage rapide vers les lieux déjà atteints, qui
 * fait passer le temps et amène à leur porte.
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

        Table side = new Table(skin);
        side.setFillParent(true);
        side.right().padRight(25);
        stage.addActor(side);

        side.add(UiKit.label(skin, "Carte du monde")).pad(5).row();
        side.add(UiKit.label(skin, session.getClock().getDate() + " - " + session.getClock().formatTime())).pad(3).row();
        side.add(UiKit.label(skin, "-- Voyage rapide (lieux déjà atteints) --")).pad(10).row();

        List<World.Route> routes = World.fastTravelRoutes(session, com.lucas.guild.game.WorldLayout.standard());
        if (routes.isEmpty()) {
            side.add(UiKit.message(skin, "Aucun autre lieu atteint à pied pour l'instant.")).pad(5).row();
        }
        for (final World.Route route : routes) {
            if (route.lockedReason != null) {
                // un lieu fermé par l'histoire : on le voit, avec la raison, mais on ne peut pas y aller
                Label closed = UiKit.label(skin, route.place.name + " - fermé");
                closed.setColor(Color.GRAY);
                side.add(closed).pad(5).padBottom(0).row();
                Label reason = UiKit.message(skin, route.lockedReason);
                reason.setWrap(true);
                reason.setAlignment(com.badlogic.gdx.utils.Align.center);
                side.add(reason).width(330f).padBottom(5).row();
                continue;
            }
            // seulement le nom : la distance et la durée se comptent depuis là où l'on est, elles trahiraient la position
            side.add(UiKit.button(skin, route.place.name, new Runnable() {
                @Override
                public void run() {
                    travel(route.place.id);
                }
            })).pad(5).row();
        }
        side.add(UiKit.button(skin, "Retour sur le chemin", new Runnable() {
            @Override
            public void run() {
                backToTheRoad();
            }
        })).padTop(25).row();

        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ESCAPE) {
                    backToTheRoad();
                    return true;
                }
                return false;
            }
        });
    }

    /** On reprend là où l'on s'était arrêté, dans le monde ouvert. */
    private void backToTheRoad() {
        GameSession session = game.getSession();
        game.setScreen(new OpenWorldScreen(game, difficulty, new com.badlogic.gdx.math.Vector3(session.getWorldX(), 2f, session.getWorldZ())));
    }

    /** Le voyage rapide : le temps passe, on arrive à la porte du lieu (les embuscades se font sur la route, à pied). */
    private void travel(String placeId) {
        GameSession session = game.getSession();
        String message = World.fastTravel(session, com.lucas.guild.game.WorldLayout.standard(), placeId);
        if (message == null) {
            return;
        }
        session.getClock().postMessage(message);
        float[] w = MainGame.arrivalPoint(placeId);
        session.setWorldPosition(w[0], w[1]);
        new SaveManager().saveGame(player, session.getClock(), difficulty, w[0], 2f, w[1], OpenWorldScreen.SAVE_SCREEN);
        game.setScreen(game.arrivalIn(placeId, difficulty));
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
        return project(p.x, p.z);
    }

    /** Un point de la carte (en km) sur l'écran. */
    private float[] project(float px, float pz) {
        float minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, minZ = Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (WorldData.PlaceData q : world.places) {
            if (q.hidden) {
                continue;
            }
            minX = Math.min(minX, q.x);
            maxX = Math.max(maxX, q.x);
            minZ = Math.min(minZ, q.z);
            maxZ = Math.max(maxZ, q.z);
        }
        float span = Math.max(Math.max(maxX - minX, maxZ - minZ), 1f);
        float scale = (MAP_SIZE - 120f) / span;
        // le nord (z négatif) est en haut de la carte
        float x = MAP_X + 60f + (px - minX) * scale;
        float y = MAP_Y + MAP_SIZE - 60f - (pz - minZ) * scale;
        return new float[] {x, y};
    }

    private void drawMap() {
        shapes.setProjectionMatrix(stage.getCamera().combined);
        batch.setProjectionMatrix(stage.getCamera().combined);
        // rien ne montre où l'on est : ni point, ni lieu ni routes mis en avant

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        shapes.setColor(0.18f, 0.24f, 0.2f, 1f);
        shapes.rect(MAP_X, MAP_Y, MAP_SIZE, MAP_SIZE);
        shapes.end();

        Gdx.gl.glLineWidth(3f);
        shapes.begin(ShapeRenderer.ShapeType.Line);
        for (List<String> link : world.links) {
            float[] a = project(world.place(link.get(0)));
            float[] b = project(world.place(link.get(1)));
            shapes.setColor(Color.GRAY);
            shapes.line(a[0], a[1], b[0], b[1]);
        }
        shapes.end();

        shapes.begin(ShapeRenderer.ShapeType.Filled);
        for (WorldData.PlaceData p : world.places) {
            if (p.hidden) {
                continue;   // un étage de donjon : pas sur la carte
            }
            float[] xy = project(p);
            boolean zone = WorldData.isZone(p);
            shapes.setColor(player.getVisitedPlaces().contains(p.id) ? (zone ? Color.LIME : Color.WHITE)
                    : zone ? Color.FOREST : Color.GRAY);
            if (zone) {
                shapes.rect(xy[0] - 7f, xy[1] - 7f, 14f, 14f);   // une zone sauvage : un carré ; une ville : un rond
            } else {
                shapes.circle(xy[0], xy[1], 8f);
            }
        }
        shapes.end();

        batch.begin();
        for (WorldData.PlaceData p : world.places) {
            if (p.hidden) {
                continue;
            }
            float[] xy = project(p);
            boolean known = player.getVisitedPlaces().contains(p.id);
            // un lieu collé à une ville (sa mine, son arène) : son nom s'écrit au-dessus, pour ne pas recouvrir celui de la ville
            float labelY = isNextToATown(p) ? xy[1] + 26f : xy[1] - 16f;
            font.draw(batch, known ? p.name : "???", xy[0] - 30f, labelY);
        }
        batch.end();
    }

    /** Une zone à moins de 4 km d'une ville : sur la carte, les deux points se touchent presque. */
    private boolean isNextToATown(WorldData.PlaceData place) {
        if (!WorldData.isZone(place)) {
            return false;
        }
        for (WorldData.PlaceData other : world.places) {
            if (!WorldData.isZone(other) && world.distanceKm(place.id, other.id) < 4.0) {
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
