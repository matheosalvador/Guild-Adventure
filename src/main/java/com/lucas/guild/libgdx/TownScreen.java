package com.lucas.guild.libgdx;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.TownData;

/**
 * La ville. Le sol est fixe ; les bâtiments (nom, position, couleur, intérieur à ouvrir)
 * viennent de {@code assets/data/town.json}. On entre dans un bâtiment en le visant et en appuyant sur E.
 */
public class TownScreen extends BaseWorldScreen {

    /** Identifiants d'intérieur reconnus (voir {@link #interiorScreen(String)}). */
    private static final Set<String> KNOWN_INTERIORS = new HashSet<>(Arrays.asList("forge", "guild", "inn", "store"));

    /** Un bâtiment de la ville, avec l'intérieur qu'il ouvre. */
    private static final class TownBuilding {
        final String name;
        final String interior;
        final String shop;

        TownBuilding(String name, String interior, String shop) {
            this.name = name;
            this.interior = interior;
            this.shop = shop;
        }
    }

    /** Ce qu'on obtient en visant la porte de la ville (action « gate » de town.json). */
    private static final String GATE = "Gate";
    /** Ce qu'on obtient en visant le tableau d'annonces de la ville (action « board »). */
    private static final String BOARD = "Board";

    private com.badlogic.gdx.scenes.scene2d.ui.Table boardTable;
    private QuestBoardPanel questBoard;

    private final Vector3 spawnPoint;

    /** Couleur du ciel, mise à jour à chaque image selon l'heure. */
    private final Color sky = new Color(0.5f, 0.8f, 1f, 1f);

    public TownScreen(MainGame game, String difficulty) {
        this(game, difficulty, new Vector3(0, 2, 0));
    }

    public TownScreen(MainGame game, String difficulty, Vector3 spawnPoint) {
        super(game, difficulty);
        this.spawnPoint = spawnPoint;
    }

    @Override
    protected Vector3 getSpawnPoint() {
        return spawnPoint;
    }

    @Override
    protected Color getBackgroundColor() {
        return sky;
    }

    /** Jour et nuit : le ciel, le soleil (puis la lune) et la lumière ambiante suivent l'heure et la saison. */
    @Override
    protected void updateEnvironment() {
        applyDayNight(sky);
    }

    @Override
    protected boolean hasSky() {
        return true;
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
    protected boolean announcesEvents() {
        return true;
    }

    /** Un habitant qui se promène : son modèle et son parcours. */
    private static final class Villager {
        final CharacterModel model;
        final com.lucas.guild.game.RouteWalker walker;

        Villager(CharacterModel model, com.lucas.guild.game.RouteWalker walker) {
            this.model = model;
            this.walker = walker;
        }
    }

    private final java.util.List<Villager> villagers = new java.util.ArrayList<>();
    private final Vector3 villagerFeet = new Vector3();

    private void buildVillagers(TownData town) {
        java.util.Random random = new java.util.Random(town.name.hashCode());
        for (TownData.VillagerData data : town.villagers) {
            java.util.List<float[]> route = new java.util.ArrayList<>();
            for (TownData.RoutePoint point : data.route) {
                route.add(new float[] {point.x, point.z});
            }
            com.lucas.guild.game.RouteWalker walker = new com.lucas.guild.game.RouteWalker(route, data.speed);
            walker.advanceMeters(random.nextFloat() * 60f);   // ils ne partent pas tous du même point
            CharacterModel model = new CharacterModel(instances, GameContent.models(), data.model);
            model.setVisible(true);
            disposables.add(model);
            villagers.add(new Villager(model, walker));
        }
    }

    @Override
    protected void afterUpdate(float delta) {
        boolean frozen = isOverlayOpen();
        for (Villager v : villagers) {
            if (!frozen) {
                v.walker.update(delta);
            }
            v.model.update(frozen ? 0f : delta, frozen ? 0f : v.walker.getSpeed(), villagerFeet.set(v.walker.getX(), 0.5f, v.walker.getZ()), v.walker.getYaw());
        }
    }

    // ------------------------------------------------------------------ décor

    @Override
    protected void buildWorld() {
        TownData town = GameContent.town(player.getTownId());
        Color ground = town.ground != null ? parseColor(town.ground, "le sol") : Color.GRAY;
        addBox(200f, 1f, 200f, ground, 0, 0, 0, null); // sol

        for (TownData.BuildingData data : town.buildings) {
            addBuilding(data);
        }
        buildVillagers(town);
        for (TownData.PartData part : town.decor) {
            addPart(part, 0, 0, "decor", "gate".equals(part.action) ? GATE : "board".equals(part.action) ? BOARD : decorUserData(part));
        }
    }

    /** Couleur par son nom LibGDX (« BROWN ») ou en hexadécimal (« #5B8C3A »). */
    private static Color parseColor(String name, String where) {
        return ColorNames.parse(name, "town.json", where);
    }

    /** Une boîte du décor ; (ox, oz) est le centre du bâtiment pour ses détails, {@code userData} ce qu'on obtient en la visant. */
    private void addPart(TownData.PartData part, float ox, float oz, String where, Object userData) {
        addDecorPart(part, ox, oz, parseColor(part.color, where), userData);
    }

    private void addBuilding(TownData.BuildingData data) {
        Color color = parseColor(data.color, "batiment \"" + data.name + "\"");
        if (!KNOWN_INTERIORS.contains(data.interior)) {
            throw new IllegalStateException("town.json : batiment \"" + data.name + "\" : interieur inconnu \"" + data.interior
                    + "\" (attendu : " + KNOWN_INTERIORS + ")");
        }
        TownBuilding building = new TownBuilding(data.name, data.interior, data.shop);
        addBox(10f, 10f, 10f, color, data.x, 5.5f, data.z, building).userData = building;
        for (TownData.PartData part : data.parts) {
            addPart(part, data.x, data.z, "batiment \"" + data.name + "\" (details)", building); // viser la porte ou l'enseigne ouvre aussi le batiment
        }
    }

    // ------------------------------------------------------------------ interactions

    @Override
    protected String getInteractionPrompt(Object userData) {
        if (userData instanceof TownBuilding) {
            return "Entrer dans: " + ((TownBuilding) userData).name + " (E)";
        }
        if (GATE.equals(userData)) {
            return "Prendre la route (E)";
        }
        if (BOARD.equals(userData)) {
            return "Lire le tableau d'annonces (E)";
        }
        return null;
    }

    @Override
    protected boolean onInteract(Object userData) {
        if (GATE.equals(userData)) {
            game.setScreen(new WorldMapScreen(game, difficulty));
            return true;
        }
        if (BOARD.equals(userData)) {
            questBoard.toggle();
            return true;
        }
        if (!(userData instanceof TownBuilding)) {
            return false;
        }
        game.setScreen(interiorScreen((TownBuilding) userData));
        return true;
    }

    /** L'écran d'intérieur correspondant à un identifiant de town.json. */
    private com.badlogic.gdx.Screen interiorScreen(TownBuilding building) {
        switch (building.interior) {
            case "forge":
                return new ForgeScreen(game, difficulty);
            case "guild":
                return new GuildScreen(game, difficulty);
            case "inn":
                return new InnScreen(game, difficulty);
            case "store":
                return new StoreScreen(game, difficulty, building.name, building.shop);
            default:
                throw new IllegalStateException("interieur inconnu : " + building.interior);
        }
    }

    // ------------------------------------------------------------------ tableau d'annonces

    @Override
    protected void setupExtraUI() {
        boardTable = new com.badlogic.gdx.scenes.scene2d.ui.Table(skin);
        boardTable.setFillParent(true);
        boardTable.setVisible(false);
        stage.addActor(boardTable);
        questBoard = new QuestBoardPanel(boardTable, skin, player, player.getTownId(), "Tableau d'annonces", dialogTable.getBackground(),
                () -> questBoard.toggle());
    }

    @Override
    protected boolean isExtraOverlayOpen() {
        return questBoard != null && questBoard.isOpen();
    }

    @Override
    protected boolean closeExtraOverlay() {
        if (isExtraOverlayOpen()) {
            questBoard.toggle();
            return true;
        }
        return false;
    }

    @Override
    protected boolean onOverlayKeyDown(int keycode) {
        return questBoard != null && questBoard.handleKey(keycode);
    }

    // ------------------------------------------------------------------ sauvegarde rapide

    @Override
    protected boolean onGlobalKeyDown(int keycode) {
        // Quick Save (F5) — toujours disponible, même en pause
        if (keycode == Input.Keys.F5) {
            Vector3 playerPos = playerController.getPlayerBody().getCenterOfMassPosition();
            boolean saved = saveManager.saveGame(player, gameClock, difficulty, playerPos.x, playerPos.y, playerPos.z, "Town");
            if (saved) {
                Gdx.app.log("SAVE", "Partie sauvegardée ! Items: " + player.getInventoryItems().size());
            } else {
                Gdx.app.error("SAVE", "ÉCHEC de la sauvegarde (voir la console)");
            }
            return true;
        }

        // Quick Load (F9) — recharge la sauvegarde disque dans la session puis relance la ville
        if (keycode == Input.Keys.F9) {
            if (game.loadSavedGame()) {
                Gdx.app.log("SAVE", "Sauvegarde chargée : " + game.getSession().getPlayer().getName());
                game.setScreen(game.resumeScreen());
            } else {
                Gdx.app.error("SAVE", "Aucune sauvegarde utilisable à charger");
            }
            return true;
        }
        return false;
    }
}
