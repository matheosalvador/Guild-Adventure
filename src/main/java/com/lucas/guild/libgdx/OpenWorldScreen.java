package com.lucas.guild.libgdx;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.ModelCache;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.collision.btStaticPlaneShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.OpenWorldData;
import com.lucas.guild.data.TownData;
import com.lucas.guild.data.ZoneData;
import com.lucas.guild.game.Chests;
import com.lucas.guild.game.Monster;
import com.lucas.guild.game.Respawns;
import com.lucas.guild.game.Terrain;
import com.lucas.guild.game.WorldLayout;
import com.lucas.guild.model.Item;

/**
 * Le monde ouvert (prototype, voir {@code docs/CADRAGE-MONDE-OUVERT.md}) : un seul terrain où l'on marche
 * d'Aethelgard à la Forêt des Loups sans passer par la carte. La campagne est découpée en carreaux fabriqués autour
 * du joueur ({@link Terrain}) et libérés derrière lui ; la ville et la forêt sont construites d'après leurs fichiers
 * quand on s'en approche, et défaites quand on s'en éloigne. Tout ce qui est créé ici est libéré ici : corps Bullet,
 * formes, modèles fusionnés.
 *
 * <p>Le travail de construction est étalé sur plusieurs images (quelques millisecondes par image) pour qu'aucune
 * image ne dure trop longtemps quand un carreau ou un lieu apparaît.</p>
 */
public class OpenWorldScreen extends BaseWorldScreen {

    /** L'écran noté dans la sauvegarde : la position sauvegardée est alors en coordonnées du monde. */
    public static final String SAVE_SCREEN = "World";

    /** Temps de construction accordé par image, en nanosecondes. */
    private static final long WORK_BUDGET = 4_000_000L;
    /** Marge avant de défaire un lieu ou un carreau qu'on vient de quitter (évite de le refaire en faisant demi-tour). */
    private static final float PLACE_UNLOAD_MARGIN = 60f;
    private static final float[] TREE_SCALE = {0.8f, 1f, 1.25f};
    private static final Color TRUNK = Color.valueOf("5A3A1E");
    private static final Color[] CROWN = {Color.valueOf("2F6B2A"), Color.valueOf("24502A")};

    /** Ce qui est construit ensemble et défait ensemble : un lieu, un carreau, ses corps solides. */
    private final class Group {
        final Array<ModelInstance> visuals = new Array<>();
        final Array<btRigidBody> bodies = new Array<>();
        final Array<Disposable> owned = new Array<>();
        /** Les visuels d'un carreau, fusionnés pour être dessinés en peu d'appels. */
        ModelCache cache;
        boolean cached;

        void clear() {
            for (btRigidBody body : bodies) {
                physics().removeRigidBody(body);
                body.dispose();
            }
            bodies.clear();
            for (Disposable d : owned) {
                d.dispose();
            }
            owned.clear();
            if (!cached) {
                for (ModelInstance m : visuals) {
                    instances.removeValue(m, true);
                }
            }
            visuals.clear();
            if (cache != null) {
                renderables.removeValue(cache, true);
                cache.dispose();
                cache = null;
            }
        }
    }

    /** Un carreau de campagne : ses arbres dessinés, et solides quand le joueur est tout près. */
    private final class Tile {
        final int tx;
        final int tz;
        final List<Terrain.Tree> trees;
        Group visual;
        Group solid;

        Tile(int tx, int tz) {
            this.tx = tx;
            this.tz = tz;
            this.trees = Terrain.trees(layout, tx, tz);
        }
    }

    /** Un lieu posé dans le monde (la ville, la forêt), construit quand on en approche. */
    private final class Place {
        final String id;
        Group group;
        final Map<Monster, String> monsters = new HashMap<>();

        Place(String id) {
            this.id = id;
        }
    }

    /** Un coffre de la forêt visé. */
    private static final class ChestRef {
        final String zoneId;
        final int index;
        final ZoneData.ChestData chest;
        final float x;
        final float z;
        ModelInstance lid;
        Group group;

        ChestRef(String zoneId, int index, ZoneData.ChestData chest, float x, float z) {
            this.zoneId = zoneId;
            this.index = index;
            this.chest = chest;
            this.x = x;
            this.z = z;
        }
    }

    private final Vector3 spawn;
    private final Color sky = new Color(0.5f, 0.8f, 1f, 1f);
    private WorldLayout layout;
    private OpenWorldData data;
    private final Map<Long, Tile> tiles = new HashMap<>();
    private final List<Place> places = new ArrayList<>();
    private final Array<RenderableProvider> renderables = new Array<>();
    private final ArrayDeque<Runnable> work = new ArrayDeque<>();
    private final Group roads = new Group();
    private final Map<String, btBoxShape> trunkShapes = new HashMap<>();
    private btStaticPlaneShape groundShape;
    private btRigidBody groundBody;
    private btRigidBody.btRigidBodyConstructionInfo groundInfo;
    private final Matrix4 tmp = new Matrix4();
    private String currentPlace;

    /** Statistiques du banc de test : la plus longue image de construction, et le nombre de carreaux faits. */
    long slowestWorkNanos;
    int tilesBuilt;

    /** @param spawn la position du joueur, en coordonnées du monde */
    public OpenWorldScreen(MainGame game, String difficulty, Vector3 spawn) {
        super(game, difficulty);
        this.spawn = spawn;
    }

    // ------------------------------------------------------------------ réglages de l'écran

    @Override
    protected Vector3 getSpawnPoint() {
        return spawn;
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
    protected boolean hasSky() {
        return true;
    }

    @Override
    protected boolean hasFog() {
        return true;
    }

    @Override
    protected float getViewDistance() {
        return GameContent.openWorld().viewDistance;
    }

    @Override
    protected float getAmbientLight() {
        return 0.4f;
    }

    @Override
    protected float getInteractionRange() {
        return 10f;
    }

    @Override
    protected boolean isOpenWorld() {
        return true;
    }

    /** Les objets jetés restent là où on les a jetés, dans tout le monde ouvert. */
    @Override
    protected String sceneKey() {
        return "OpenWorld";
    }

    @Override
    protected Array<RenderableProvider> extraRenderables() {
        return renderables;
    }

    // ------------------------------------------------------------------ construction

    @Override
    protected void buildWorld() {
        layout = WorldLayout.standard();
        data = layout.data();
        // un seul sol solide pour tout le monde : pas de jointure entre les carreaux où le joueur buterait
        groundShape = new btStaticPlaneShape(new Vector3(0, 1, 0), 0.5f);
        groundInfo = new btRigidBody.btRigidBodyConstructionInfo(0, null, groundShape, Vector3.Zero);
        groundBody = new btRigidBody(groundInfo);
        physics().addRigidBody(groundBody);

        buildRoads();
        for (OpenWorldData.PlaceData p : data.places) {
            places.add(new Place(p.id));
        }
        // au départ, tout ce qui est autour du joueur est construit d'un coup (l'écran n'est pas encore affiché)
        updateStreaming(spawn.x, spawn.z);
        while (!work.isEmpty()) {
            work.poll().run();
        }
        updateCurrentPlace(spawn.x, spawn.z);
    }

    private void buildRoads() {
        Color color = ColorNames.parse(data.road, "openworld.json", "la route");
        for (OpenWorldData.RoadData road : data.roads) {
            for (int i = 0; i + 1 < road.points.size(); i++) {
                OpenWorldData.PointData a = road.points.get(i);
                OpenWorldData.PointData b = road.points.get(i + 1);
                float dx = b.x - a.x;
                float dz = b.z - a.z;
                float length = (float) Math.hypot(dx, dz) + data.roadWidth;   // déborde un peu : les tronçons se rejoignent
                float yaw = (float) Math.toDegrees(Math.atan2(dx, dz));
                visual(roads, "box", data.roadWidth, 0.1f, length, color, (a.x + b.x) / 2f, 0.53f, (a.z + b.z) / 2f, new float[] {0, yaw, 0});
            }
        }
    }

    /** Une forme visible : dans les instances de l'écran, ou gardée pour la fusion du carreau. */
    private ModelInstance visual(Group g, String shape, float w, float h, float d, Color color, float x, float y, float z, float[] rot) {
        ModelInstance instance = new ModelInstance(shapeModel(shape, w, h, d, color));
        instance.transform.setTranslation(x, y, z);
        if (rot != null) {
            instance.transform.rotate(Vector3.Y, rot[1]).rotate(Vector3.X, rot[0]).rotate(Vector3.Z, rot[2]);
        }
        g.visuals.add(instance);
        if (!g.cached) {
            instances.add(instance);
        }
        return instance;
    }

    /** Un corps solide immobile ; {@code shape} est libéré avec le groupe si {@code owned}. */
    private btRigidBody solid(Group g, btCollisionShape shape, boolean owned, float x, float y, float z, Object userData) {
        btRigidBody.btRigidBodyConstructionInfo info = new btRigidBody.btRigidBodyConstructionInfo(0, null, shape, Vector3.Zero);
        btRigidBody body = new btRigidBody(info);
        body.setWorldTransform(tmp.setToTranslation(x, y, z));
        body.userData = userData;
        physics().addRigidBody(body);
        g.bodies.add(body);
        g.owned.add(info);
        if (owned) {
            g.owned.add(shape);
        }
        return body;
    }

    /** Une boîte visible et solide. */
    private void box(Group g, float w, float h, float d, Color color, float x, float y, float z, Object userData) {
        visual(g, "box", w, h, d, color, x, y, z, null);
        solid(g, new btBoxShape(new Vector3(w / 2f, h / 2f, d / 2f)), true, x, y, z, userData);
    }

    /** Une partie de décor d'un fichier de lieu, posée au point (ox, oz) du monde (même règle que les autres écrans). */
    private void part(Group g, TownData.PartData part, float ox, float oz, String file, Object userData) {
        Color color = ColorNames.parse(part.color, file, "le decor");
        float x = ox + part.x;
        float z = oz + part.z;
        float centerY = 0.5f + part.y + part.h / 2f;
        if (part.decal || !"box".equals(part.shape) || part.rot != null) {
            visual(g, part.shape, part.w, part.h, part.d, color, x, centerY, z, part.rot);
        } else {
            box(g, part.w, part.h, part.d, color, x, centerY, z, userData);
        }
    }

    // ------------------------------------------------------------------ carreaux

    private static long tileKey(int tx, int tz) {
        return ((long) tx << 32) ^ (tz & 0xffffffffL);
    }

    /** Ce qu'il faut construire et défaire autour du joueur : carreaux, corps des arbres, lieux. */
    private void updateStreaming(float x, float z) {
        int ptx = layout.tileOf(x);
        int ptz = layout.tileOf(z);
        int r = data.loadRadius;
        for (int tx = ptx - r; tx <= ptx + r; tx++) {
            for (int tz = ptz - r; tz <= ptz + r; tz++) {
                long key = tileKey(tx, tz);
                if (!tiles.containsKey(key)) {
                    final Tile tile = new Tile(tx, tz);
                    tiles.put(key, tile);
                    work.add(() -> buildTile(tile));
                }
            }
        }
        for (Iterator<Tile> it = tiles.values().iterator(); it.hasNext(); ) {
            Tile tile = it.next();
            int ring = Math.max(Math.abs(tile.tx - ptx), Math.abs(tile.tz - ptz));
            if (ring > r + 1) {
                unloadTile(tile);
                it.remove();
            } else if (ring <= data.solidRadius && tile.solid == null) {
                final Tile t = tile;
                t.solid = new Group();
                work.add(() -> solidifyTile(t));
            } else if (ring > data.solidRadius + 1 && tile.solid != null) {
                tile.solid.clear();
                tile.solid = null;
            }
        }
        for (Place place : places) {
            float distance = layout.distanceToPlace(place.id, x, z);
            if (place.group == null && distance < data.placeLoadDistance) {
                loadPlace(place);
            } else if (place.group != null && distance > data.placeLoadDistance + PLACE_UNLOAD_MARGIN) {
                unloadPlace(place);
            }
        }
    }

    private void buildTile(Tile tile) {
        if (tiles.get(tileKey(tile.tx, tile.tz)) != tile) {
            return;   // défait avant d'être construit
        }
        Group g = new Group();
        g.cached = true;
        float size = data.tileSize;
        float cx = (tile.tx + 0.5f) * size;
        float cz = (tile.tz + 0.5f) * size;
        // le sol dessiné est un peu plus bas que le sol solide (0,5) : sous une ville ou une forêt, le leur le recouvre
        visual(g, "box", size, 1f, size, ColorNames.parse(data.ground, "openworld.json", "le sol"), cx, -0.05f, cz, null);
        for (Terrain.Tree tree : tile.trees) {
            float s = TREE_SCALE[tree.size];
            float trunkH = (tree.kind == 0 ? 5.8f : 3.5f) * s;
            float trunkW = (tree.kind == 0 ? 0.8f : 1f) * s;
            visual(g, "box", trunkW, trunkH, trunkW, TRUNK, tree.x, 0.5f + trunkH / 2f, tree.z, null);
            if (tree.kind == 0) {
                visual(g, "sphere", 4.2f * s, 4.5f * s, 4.2f * s, CROWN[0], tree.x, 0.5f + trunkH - 0.5f * s + 2.25f * s, tree.z, null);
            } else {
                visual(g, "cone", 3.8f * s, 8.4f * s, 3.8f * s, CROWN[1], tree.x, 0.5f + trunkH + 4.2f * s, tree.z, null);
            }
        }
        g.cache = new ModelCache();
        g.cache.begin();
        g.cache.add(g.visuals);
        g.cache.end();
        renderables.add(g.cache);
        tile.visual = g;
        tilesBuilt++;
    }

    /** Les troncs du carreau deviennent solides (les formes sont partagées par taille d'arbre). */
    private void solidifyTile(Tile tile) {
        if (tile.solid == null || tiles.get(tileKey(tile.tx, tile.tz)) != tile) {
            return;
        }
        for (Terrain.Tree tree : tile.trees) {
            float s = TREE_SCALE[tree.size];
            float trunkH = (tree.kind == 0 ? 5.8f : 3.5f) * s;
            float trunkW = (tree.kind == 0 ? 0.8f : 1f) * s;
            String key = tree.kind + "/" + tree.size;
            btBoxShape shape = trunkShapes.get(key);
            if (shape == null) {
                shape = new btBoxShape(new Vector3(trunkW / 2f, trunkH / 2f, trunkW / 2f));
                trunkShapes.put(key, shape);
            }
            solid(tile.solid, shape, false, tree.x, 0.5f + trunkH / 2f, tree.z, null);
        }
    }

    private void unloadTile(Tile tile) {
        if (tile.visual != null) {
            tile.visual.clear();
            tile.visual = null;
        }
        if (tile.solid != null) {
            tile.solid.clear();
            tile.solid = null;
        }
    }

    // ------------------------------------------------------------------ lieux

    private void loadPlace(final Place place) {
        final Group g = new Group();
        place.group = g;
        final float[] o = layout.origin(place.id);
        com.lucas.guild.data.WorldData.PlaceData info = GameContent.world().place(place.id);
        if (info.townFile != null) {
            final TownData town = GameContent.town(place.id);
            final String file = info.townFile;
            work.add(() -> {
                if (place.group == g) {
                    Color ground = town.ground != null ? ColorNames.parse(town.ground, file, "le sol") : Color.GRAY;
                    visual(g, "box", 200f, 1f, 200f, ground, o[0], 0f, o[1], null);
                }
            });
            for (final TownData.BuildingData b : town.buildings) {
                work.add(() -> {
                    if (place.group != g) {
                        return;
                    }
                    TownScreen.TownBuilding building = new TownScreen.TownBuilding(b.name, b.interior, b.shop);
                    box(g, 10f, 10f, 10f, ColorNames.parse(b.color, file, "batiment \"" + b.name + "\""), o[0] + b.x, 5.5f, o[1] + b.z, building);
                    for (TownData.PartData part : b.parts) {
                        part(g, part, o[0] + b.x, o[1] + b.z, file, building);
                    }
                });
            }
            for (final TownData.PartData part : town.decor) {
                // la porte de la ville est ouverte (on passe à pied) ; le panneau « à pied » n'a plus de sens ici
                if ("gate".equals(part.action) || "walk".equals(part.action)) {
                    continue;
                }
                work.add(() -> {
                    if (place.group == g) {
                        part(g, part, o[0], o[1], file, "board".equals(part.action) ? null : decorUserData(part));
                    }
                });
            }
        } else {
            loadZone(place, g, o);
        }
    }

    private void loadZone(final Place place, final Group g, final float[] o) {
        final ZoneData zone = GameContent.zone(place.id);
        final String file = "zone de " + place.id;
        final OpenWorldData.PlaceData where = data.place(place.id);
        final float half = zone.size / 2f;
        work.add(() -> {
            if (place.group != g) {
                return;
            }
            visual(g, "box", zone.size, 1f, zone.size, ColorNames.parse(zone.ground, file, "le sol"), o[0], 0f, o[1], null);
            Color border = zone.wall != null ? ColorNames.parse(zone.wall, file, "les murs") : new Color(0.12f, 0.24f, 0.1f, 1f);
            hedge(g, border, o, half, true, half + 1f, where.entrance);
            hedge(g, border, o, half, true, -half - 1f, where.entrance);
            hedge(g, border, o, half, false, half + 1f, where.entrance);
            hedge(g, border, o, half, false, -half - 1f, where.entrance);
        });
        for (final TownData.PartData part : zone.decor) {
            work.add(() -> {
                if (place.group == g) {
                    part(g, part, o[0], o[1], file, decorUserData(part));
                }
            });
        }
        work.add(() -> {
            if (place.group != g) {
                return;
            }
            int today = gameClock.getCurrentDay();
            for (int i = 0; i < zone.chests.size(); i++) {
                ZoneData.ChestData chest = zone.chests.get(i);
                ChestRef ref = new ChestRef(place.id, i, chest, o[0] + chest.x, o[1] + chest.z);
                ref.group = g;
                box(g, 1.4f, 0.8f, 0.9f, new Color(0.45f, 0.28f, 0.12f, 1f), ref.x, 0.9f, ref.z, ref);
                if (Chests.isFull(player, Chests.key(place.id, i), chest, today)) {
                    ref.lid = visual(g, "box", 1.4f, 0.3f, 0.9f, Color.GOLDENROD, ref.x, 1.45f, ref.z, null);
                }
            }
            // les monstres de la forêt y restent ; un monstre tué ne revient qu'après quelques jours
            combat.setBounds(o[0] - half + 1.5f, o[0] + half - 1.5f, o[1] - half + 1.5f, o[1] + half - 1.5f);
            for (int i = 0; i < zone.monsters.size(); i++) {
                ZoneData.SpawnData m = zone.monsters.get(i);
                String key = Respawns.key(place.id, i);
                if (story().holds(m.requires) && Respawns.isAlive(player, key, today, data.monsterRespawnDays)) {
                    place.monsters.put(combat.spawn(m.id, o[0] + m.x, o[1] + m.z), key);
                }
            }
        });
    }

    /**
     * Un côté de la haie de la zone : {@code alongX} = un côté parallèle à X (au z donné), sinon parallèle à Z (au
     * x donné). L'entrée de la route, si elle est de ce côté, y laisse une ouverture.
     */
    private void hedge(Group g, Color color, float[] o, float half, boolean alongX, float at, OpenWorldData.EntranceData entrance) {
        float full = 2f * half + 4f;
        boolean open = entrance != null && (alongX ? Math.abs(entrance.z - at) < 2f : Math.abs(entrance.x - at) < 2f);
        if (!open) {
            if (alongX) {
                box(g, full, 12f, 2f, color, o[0], 6f, o[1] + at, null);
            } else {
                box(g, 2f, 12f, full, color, o[0] + at, 6f, o[1], null);
            }
            return;
        }
        float gapCenter = alongX ? entrance.x : entrance.z;
        float lo = -full / 2f;
        float hi = full / 2f;
        float gapLo = gapCenter - entrance.width / 2f;
        float gapHi = gapCenter + entrance.width / 2f;
        for (float[] seg : new float[][] {{lo, gapLo}, {gapHi, hi}}) {
            float len = seg[1] - seg[0];
            if (len <= 0.5f) {
                continue;
            }
            float mid = (seg[0] + seg[1]) / 2f;
            if (alongX) {
                box(g, len, 12f, 2f, color, o[0] + mid, 6f, o[1] + at, null);
            } else {
                box(g, 2f, 12f, len, color, o[0] + at, 6f, o[1] + mid, null);
            }
        }
    }

    private void unloadPlace(Place place) {
        if (!place.monsters.isEmpty()) {
            combat.clearAll();   // seuls les monstres de ce lieu sont dans le monde (prototype : un seul lieu sauvage)
            place.monsters.clear();
        }
        place.group.clear();
        place.group = null;
    }

    @Override
    protected void onMonsterKilled(Monster monster) {
        for (Place place : places) {
            String key = place.monsters.remove(monster);
            if (key != null) {
                Respawns.killed(player, key, gameClock.getCurrentDay());
            }
        }
    }

    // ------------------------------------------------------------------ à chaque image

    @Override
    protected void afterUpdate(float delta) {
        Vector3 p = playerPosition();
        game.getSession().setWorldPosition(p.x, p.z);
        updateCurrentPlace(p.x, p.z);
        updateStreaming(p.x, p.z);
        long start = System.nanoTime();
        long end = start + WORK_BUDGET;
        while (!work.isEmpty() && System.nanoTime() < end) {
            work.poll().run();
        }
        slowestWorkNanos = Math.max(slowestWorkNanos, System.nanoTime() - start);
    }

    /** Le lieu où l'on est : le HUD l'affiche, et les règles du jeu (boutiques, auberge, carte) en dépendent. */
    private void updateCurrentPlace(float x, float z) {
        String here = layout.placeAt(x, z);
        if (here == null) {
            townName = "En chemin";
            currentPlace = null;
            return;
        }
        if (!here.equals(currentPlace)) {
            currentPlace = here;
            player.setTownId(here);
            player.markVisited(here);
            townName = GameContent.world().place(here).name;
        }
    }

    /** Pour le banc de test : nombre de carreaux construits en ce moment, et de travaux en attente. */
    int loadedTiles() {
        int n = 0;
        for (Tile t : tiles.values()) {
            if (t.visual != null) {
                n++;
            }
        }
        return n;
    }

    int pendingWork() {
        return work.size();
    }

    // ------------------------------------------------------------------ interactions

    @Override
    protected String getInteractionPrompt(Object userData) {
        if (userData instanceof TownScreen.TownBuilding) {
            return "Entrer dans: " + ((TownScreen.TownBuilding) userData).name + " (E)";
        }
        if (userData instanceof ChestRef) {
            return ((ChestRef) userData).lid != null ? "Ouvrir le coffre (E)" : "Coffre vide";
        }
        return null;
    }

    @Override
    protected boolean onInteract(Object userData) {
        if (userData instanceof TownScreen.TownBuilding) {
            Vector3 p = playerPosition();
            saveManager.saveGame(player, gameClock, difficulty, p.x, p.y, p.z, SAVE_SCREEN);
            game.setScreen(TownScreen.interiorScreen(game, difficulty, (TownScreen.TownBuilding) userData));
            return true;
        }
        if (userData instanceof ChestRef) {
            openChest((ChestRef) userData);
            return true;
        }
        return false;
    }

    private void openChest(ChestRef ref) {
        List<Item> overflow = new ArrayList<>();
        String message = Chests.open(player, Chests.key(ref.zoneId, ref.index), ref.chest, gameClock.getCurrentDay(), overflow);
        if (message == null) {
            gameClock.postMessage("Le coffre est vide.");
            return;
        }
        gameClock.postMessage(message);
        if (ref.lid != null) {
            instances.removeValue(ref.lid, true);
            ref.group.visuals.removeValue(ref.lid, true);
            ref.lid = null;
        }
        float dx = 1.2f;
        for (Item item : overflow) {
            dropOnGround(item, ref.x + dx, ref.z + 1.2f);
            dx += 0.7f;
        }
    }

    // ------------------------------------------------------------------ touches : carte, sauvegarde

    @Override
    protected boolean onGlobalKeyDown(int keycode) {
        if (keycode == Input.Keys.F5) {
            Vector3 p = playerPosition();
            if (!saveManager.saveGame(player, gameClock, difficulty, p.x, p.y, p.z, SAVE_SCREEN)) {
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
        if (keycode == Input.Keys.M && !isOverlayOpen()) {
            game.setScreen(new WorldMapScreen(game, difficulty));   // la carte montre où l'on est
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ fin

    @Override
    public void dispose() {
        // nos corps d'abord, tant que le monde physique existe
        work.clear();
        for (Tile tile : tiles.values()) {
            unloadTile(tile);
        }
        tiles.clear();
        for (Place place : places) {
            if (place.group != null) {
                place.group.clear();
                place.group = null;
            }
        }
        roads.clear();
        if (groundBody != null) {
            physics().removeRigidBody(groundBody);
            groundBody.dispose();
            groundInfo.dispose();
            groundShape.dispose();
            groundBody = null;
        }
        for (btBoxShape shape : trunkShapes.values()) {
            shape.dispose();
        }
        trunkShapes.clear();
        super.dispose();
    }
}
