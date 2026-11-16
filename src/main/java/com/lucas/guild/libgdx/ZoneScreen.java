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
import com.lucas.guild.game.Arena;
import com.lucas.guild.game.Chests;
import com.lucas.guild.game.GameRandom;
import com.lucas.guild.game.GameSession;
import com.lucas.guild.game.World;
import com.lucas.guild.model.Item;

/**
 * Une zone : une zone sauvage (forêt, ruines...), un lieu fermé (galerie de mine, salle de château) ou le bout de
 * route d'une embuscade. Un terrain avec des monstres, des coffres, une borne de route qui ramène à la carte du
 * monde, et parfois des portes vers d'autres zones ou une arène.
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

    /** Une porte visée. */
    private static final class DoorRef {
        final ZoneData.DoorData door;

        DoorRef(ZoneData.DoorData door) {
            this.door = door;
        }
    }

    private final Vector3 spawn;
    private final boolean ambush;
    private final Color sky = new Color(0.5f, 0.8f, 1f, 1f);
    private ZoneData zone;
    private String zoneId;
    private final Map<Integer, ModelInstance> chestLids = new HashMap<>();
    private final Random random = GameRandom.create();
    /** L'arène de la zone, s'il y en a une (une épreuve de l'histoire). */
    private Arena arena;

    /** Une zone du monde ; {@code spawn} peut être null (on apparaît à l'arrivée de la zone). */
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

    /** Les données de la zone, lues à la première demande (l'éclairage en a besoin avant la construction du décor). */
    private ZoneData zone() {
        if (zone == null) {
            zoneId = game.getSession().getPlayer().getTownId();
            zone = ambush ? GameContent.ambushZone() : GameContent.zone(zoneId);
        }
        return zone;
    }

    @Override
    protected Vector3 getSpawnPoint() {
        return spawn != null ? spawn : new Vector3(zone().arrival.x, zone().arrival.y, zone().arrival.z);
    }

    @Override
    protected Color getBackgroundColor() {
        return sky;
    }

    @Override
    protected void updateEnvironment() {
        if (!zone().indoor) {
            applyDayNight(sky);
        }
    }

    @Override
    protected boolean hasSky() {
        return !zone().indoor;
    }

    @Override
    protected float getAmbientLight() {
        if (zone().ambient > 0) {
            return zone().ambient;
        }
        return zone().indoor ? 0.3f : 0.4f;
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
        zone();
        String file = ambush ? "world.json (ambush)" : "zone de " + zoneId;
        float half = zone.size / 2f;
        if (ambush && game.getSession().getPendingDestination() != null) {
            townName = "En route vers " + GameContent.world().place(game.getSession().getPendingDestination()).name;
        }

        addBox(zone.size, 1f, zone.size, ColorNames.parse(zone.ground, file, "le sol"), 0, 0, 0, null);
        // les bords du terrain : une haie épaisse dehors, des murs dans un lieu fermé ; infranchissables
        Color border = zone.wall != null ? ColorNames.parse(zone.wall, file, "les murs")
                : zone.indoor ? new Color(0.23f, 0.23f, 0.25f, 1f) : new Color(0.12f, 0.24f, 0.1f, 1f);
        if (zone.indoor) {
            sky.set(ColorNames.parse(zone.background, file, "le fond"));
        }
        addBox(zone.size + 4f, 12f, 2f, border, 0, 6f, half + 1f, null);
        addBox(zone.size + 4f, 12f, 2f, border, 0, 6f, -half - 1f, null);
        addBox(2f, 12f, zone.size + 4f, border, half + 1f, 6f, 0, null);
        addBox(2f, 12f, zone.size + 4f, border, -half - 1f, 6f, 0, null);

        for (TownData.PartData part : zone.decor) {
            addDecorPart(part, 0, 0, ColorNames.parse(part.color, file, "le decor"), decorUserData(part));
        }

        // la borne de route : un poteau et son panneau
        if (zone.exit != null) {
            addBox(0.6f, 4f, 0.6f, Color.BROWN, zone.exit.x, 2.5f, zone.exit.z, ROAD);
            addBox(3.2f, 1.2f, 0.3f, Color.TAN, zone.exit.x, 4.4f, zone.exit.z, ROAD);
        }
        // les portes vers d'autres zones : un cadre et son battant
        for (ZoneData.DoorData door : zone.doors) {
            DoorRef ref = new DoorRef(door);
            Color wood = ColorNames.parse(door.color, file, "une porte");
            addBox(3.2f, 4.2f, 0.7f, wood, door.x, 2.6f, door.z, ref);
            addVisualBox(2.2f, 3.4f, 0.8f, new Color(wood).mul(0.55f, 0.55f, 0.55f, 1f), door.x, 2.2f, door.z);
        }

        if (!ambush) {
            buildChests();
        }

        combat.setBounds(-half + 1.5f, half - 1.5f, -half + 1.5f, half - 1.5f);
        if (ambush) {
            spawnAmbushers();
        } else {
            for (ZoneData.SpawnData m : zone.monsters) {
                if (story().holds(m.requires)) {
                    combat.spawn(m.id, m.x, m.z);
                }
            }
            if (zone.arena != null) {
                arena = new Arena(zone.arena);
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

    // ------------------------------------------------------------------ arène

    private void spawnWave(List<ZoneData.SpawnData> wave) {
        for (ZoneData.SpawnData m : wave) {
            combat.spawn(m.id, m.x, m.z);
        }
        gameClock.postMessage("Vague " + arena.waveNumber() + " sur " + arena.waveCount() + " !");
    }

    /** L'arène : elle commence quand l'histoire le demande, les vagues se suivent, la dernière vaincue donne la victoire. */
    @Override
    protected void afterUpdate(float delta) {
        if (arena == null) {
            return;
        }
        if (!arena.isRunning()) {
            if (!arena.isWon() && story().has(zone.arena.startFlag)) {
                spawnWave(arena.start());
            }
            return;
        }
        List<ZoneData.SpawnData> next = arena.update(combat.count());
        if (next != null) {
            spawnWave(next);
        } else if (arena.isWon()) {
            String message = arena.record(story());
            if (message != null) {
                gameClock.postMessage(message);
            }
        }
    }

    /** Le joueur tombe ou s'en va pendant l'arène : l'épreuve est perdue (l'histoire continue). */
    private void failArena() {
        if (arena != null && arena.fail()) {
            String message = arena.record(story());
            if (message != null) {
                gameClock.postMessage(message);
            }
        }
    }

    @Override
    protected void onPlayerDeath() {
        failArena();
        super.onPlayerDeath();
    }

    // ------------------------------------------------------------------ interactions

    @Override
    protected String getInteractionPrompt(Object userData) {
        if (ROAD.equals(userData)) {
            if (ambush) {
                return combat.count() > 0 ? "Des ennemis vous barrent la route !" : "Reprendre la route (E)";
            }
            return arena != null && arena.isRunning() ? "Quitter l'arène : l'épreuve sera perdue (E)" : "Prendre la route (E)";
        }
        if (userData instanceof ChestRef) {
            return chestLids.containsKey(((ChestRef) userData).index) ? "Ouvrir le coffre (E)" : "Coffre vide";
        }
        if (userData instanceof DoorRef) {
            ZoneData.DoorData door = ((DoorRef) userData).door;
            return story().holds(door.requires) ? door.label + " (E)" : door.lockedText;
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
        if (userData instanceof DoorRef) {
            goThrough(((DoorRef) userData).door);
            return true;
        }
        return false;
    }

    private void takeTheRoad() {
        GameSession session = game.getSession();
        if (!ambush) {
            failArena();
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

    /** Franchit une porte : on passe dans l'autre zone, avec une sauvegarde automatique à l'arrivée. */
    private void goThrough(ZoneData.DoorData door) {
        if (!story().holds(door.requires)) {
            gameClock.postMessage(door.lockedText);
            return;
        }
        failArena();
        player.setTownId(door.to);
        player.markVisited(door.to);
        TownData.Point3 a = door.spawn != null ? door.spawn : GameContent.arrival(door.to);
        saveManager.saveGame(player, gameClock, difficulty, a.x, a.y, a.z, "Zone");
        game.setScreen(new ZoneScreen(game, difficulty, new Vector3(a.x, a.y, a.z)));
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
            if (arena != null && arena.isRunning()) {
                gameClock.postMessage("Impossible de sauvegarder pendant l'épreuve.");
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
