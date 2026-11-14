package com.lucas.guild.libgdx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.TownData;
import com.lucas.guild.data.WorldData;
import com.lucas.guild.data.ZoneData;
import com.lucas.guild.game.Chests;
import com.lucas.guild.game.GameSession;
import com.lucas.guild.game.World;
import com.lucas.guild.model.Item;

/**
 * Une zone sauvage (forêt, ruines...) ou le bout de route d'une embuscade : un terrain ouvert avec
 * des monstres, des coffres, et une borne de route qui ramène à la carte du monde.
 *
 * <p>Tout vient d'un fichier {@code zone_*.json} (voir {@link ZoneData}). Dans une embuscade, la borne
 * ne laisse repartir qu'une fois les agresseurs vaincus.</p>
 */
public class ZoneScreen extends BaseWorldScreen {

    /** Ce qu'on obtient en visant la borne de route. */
    private static final String ROAD = "Road";

    /** Un coffre visé. */
    private static final class ChestRef {
        final int index;

        ChestRef(int index) {
            this.index = index;
        }
    }

    private final Vector3 spawn;
    private final boolean ambush;
    private final Color sky = new Color(0.5f, 0.8f, 1f, 1f);
    private ZoneData zone;
    private String zoneId;
    private final Map<Integer, ModelInstance> chestLids = new HashMap<>();
    private final Random random = new Random();

    /** Une zone sauvage du monde ; {@code spawn} peut être null (on apparaît à l'arrivée de la zone). */
    public ZoneScreen(MainGame game, String difficulty, Vector3 spawn) {
        this(game, difficulty, spawn, false);
    }

    /** Le bout de route d'une embuscade, entre deux villes. */
    public static ZoneScreen ambush(MainGame game, String difficulty) {
        return new ZoneScreen(game, difficulty, null, true);
    }

    private ZoneScreen(MainGame game, String difficulty, Vector3 spawn, boolean ambush) {
        super(game, difficulty);
        this.spawn = spawn;
        this.ambush = ambush;
    }

    @Override
    protected Vector3 getSpawnPoint() {
        return spawn != null ? spawn : new Vector3(zone.arrival.x, zone.arrival.y, zone.arrival.z);
    }

    @Override
    protected Color getBackgroundColor() {
        return sky;
    }

    @Override
    protected void updateEnvironment() {
        applyDayNight(sky);
    }

    @Override
    protected float getAmbientLight() {
        return 0.4f;
    }

    @Override
    protected float getInteractionRange() {
        return 8f;
    }

    @Override
    protected String sceneKey() {
        return ambush ? "ambush@" + player.getTownId() : super.sceneKey();
    }

    // ------------------------------------------------------------------ décor

    @Override
    protected void buildWorld() {
        zoneId = player.getTownId();
        zone = ambush ? GameContent.ambushZone() : GameContent.zone(zoneId);
        String file = ambush ? "world.json (ambush)" : "zone de " + zoneId;
        float half = zone.size / 2f;
        if (ambush && game.getSession().getPendingDestination() != null) {
            townName = "En route vers " + GameContent.world().place(game.getSession().getPendingDestination()).name;
        }

        addBox(zone.size, 1f, zone.size, ColorNames.parse(zone.ground, file, "le sol"), 0, 0, 0, null);
        // les bords du terrain : une haie épaisse, infranchissable
        Color hedge = new Color(0.12f, 0.24f, 0.1f, 1f);
        addBox(zone.size + 4f, 12f, 2f, hedge, 0, 6f, half + 1f, null);
        addBox(zone.size + 4f, 12f, 2f, hedge, 0, 6f, -half - 1f, null);
        addBox(2f, 12f, zone.size + 4f, hedge, half + 1f, 6f, 0, null);
        addBox(2f, 12f, zone.size + 4f, hedge, -half - 1f, 6f, 0, null);

        for (TownData.PartData part : zone.decor) {
            Color color = ColorNames.parse(part.color, file, "le decor");
            float centerY = 0.5f + part.y + part.h / 2f;
            if (part.decal) {
                addVisualBox(part.w, part.h, part.d, color, part.x, centerY, part.z);
            } else {
                addBox(part.w, part.h, part.d, color, part.x, centerY, part.z, null);
            }
        }

        // la borne de route : un poteau et son panneau
        addBox(0.6f, 4f, 0.6f, Color.BROWN, zone.exit.x, 2.5f, zone.exit.z, ROAD);
        addBox(3.2f, 1.2f, 0.3f, Color.TAN, zone.exit.x, 4.4f, zone.exit.z, ROAD);

        if (!ambush) {
            buildChests();
        }

        combat.setBounds(-half + 1.5f, half - 1.5f, -half + 1.5f, half - 1.5f);
        if (ambush) {
            spawnAmbushers();
        } else {
            for (ZoneData.SpawnData m : zone.monsters) {
                combat.spawn(m.id, m.x, m.z);
            }
        }
    }

    private void buildChests() {
        int today = gameClock.getCurrentDay();
        for (int i = 0; i < zone.chests.size(); i++) {
            ZoneData.ChestData chest = zone.chests.get(i);
            addBox(1.4f, 0.8f, 0.9f, new Color(0.45f, 0.28f, 0.12f, 1f), chest.x, 0.9f, chest.z, new ChestRef(i));
            if (Chests.isFull(player, Chests.key(zoneId, i), chest, today)) {
                chestLids.put(i, addVisualBox(1.4f, 0.3f, 0.9f, Color.GOLDENROD, chest.x, 1.45f, chest.z));
            }
        }
    }

    /** Les agresseurs d'une embuscade : entre « min » et « max », tirés parmi ceux du monde, devant le joueur. */
    private void spawnAmbushers() {
        WorldData.AmbushData config = GameContent.world().ambush;
        int count = config.min + random.nextInt(config.max - config.min + 1);
        for (int i = 0; i < count; i++) {
            String id = config.monsters.get(random.nextInt(config.monsters.size()));
            float x = zone.arrival.x + (random.nextFloat() - 0.5f) * 8f;
            float z = zone.arrival.z - 8f - i * 2.5f;
            combat.spawn(id, x, z);
        }
    }

    // ------------------------------------------------------------------ interactions

    @Override
    protected String getInteractionPrompt(Object userData) {
        if (ROAD.equals(userData)) {
            if (ambush) {
                return combat.count() > 0 ? "Des ennemis vous barrent la route !" : "Reprendre la route (E)";
            }
            return "Prendre la route (E)";
        }
        if (userData instanceof ChestRef) {
            return chestLids.containsKey(((ChestRef) userData).index) ? "Ouvrir le coffre (E)" : "Coffre vide";
        }
        return null;
    }

    @Override
    protected boolean onInteract(Object userData) {
        if (ROAD.equals(userData)) {
            takeTheRoad();
            return true;
        }
        if (userData instanceof ChestRef) {
            openChest(((ChestRef) userData).index);
            return true;
        }
        return false;
    }

    private void takeTheRoad() {
        GameSession session = game.getSession();
        if (!ambush) {
            game.setScreen(new WorldMapScreen(game, difficulty));
            return;
        }
        if (combat.count() > 0) {
            gameClock.postMessage("Des ennemis vous barrent la route !");
            return;
        }
        String message = World.finishTravel(session);
        if (message != null) {
            gameClock.postMessage(message);
        }
        TownData.Point3 a = GameContent.arrival(player.getTownId());
        saveManager.saveGame(player, gameClock, difficulty, a.x, a.y, a.z, "Town");
        game.setScreen(game.screenAt(player.getTownId(), difficulty));
    }

    private void openChest(int index) {
        ZoneData.ChestData chest = zone.chests.get(index);
        List<Item> overflow = new ArrayList<>();
        String message = Chests.open(player, Chests.key(zoneId, index), chest, gameClock.getCurrentDay(), overflow);
        if (message == null) {
            gameClock.postMessage("Le coffre est vide.");
            return;
        }
        gameClock.postMessage(message);
        ModelInstance lid = chestLids.remove(index);
        if (lid != null) {
            instances.removeValue(lid, true);
        }
        float dx = 1.2f;
        for (Item item : overflow) {
            dropOnGround(item, chest.x + dx, chest.z + 1.2f);
            dx += 0.7f;
        }
    }

    // ------------------------------------------------------------------ sauvegarde rapide

    @Override
    protected boolean onGlobalKeyDown(int keycode) {
        if (keycode == Input.Keys.F5) {
            if (ambush) {
                gameClock.postMessage("Impossible de sauvegarder pendant une embuscade.");
                return true;
            }
            Vector3 p = playerController.getPlayerBody().getCenterOfMassPosition();
            boolean saved = saveManager.saveGame(player, gameClock, difficulty, p.x, p.y, p.z, "Zone");
            if (!saved) {
                Gdx.app.error("SAVE", "ÉCHEC de la sauvegarde (voir la console)");
            }
            return true;
        }
        if (keycode == Input.Keys.F9) {
            if (game.loadSavedGame()) {
                game.setScreen(game.resumeScreen());
            }
            return true;
        }
        return false;
    }
}
