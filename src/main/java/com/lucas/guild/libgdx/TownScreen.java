package com.lucas.guild.libgdx;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Colors;
import com.badlogic.gdx.math.Vector3;
import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.TownData;
import com.lucas.guild.game.GameSession;
import com.lucas.guild.model.Building;
import com.lucas.guild.model.DayNightCycle;

/**
 * La ville. Le sol est fixe ; les bâtiments (nom, position, couleur, intérieur à ouvrir)
 * viennent de {@code assets/data/town.json}. On entre dans un bâtiment en le visant et en appuyant sur E.
 */
public class TownScreen extends BaseWorldScreen {

    /** Identifiants d'intérieur reconnus (voir {@link #interiorScreen(String)}). */
    private static final Set<String> KNOWN_INTERIORS = new HashSet<>(Arrays.asList("forge", "guild", "inn", "store"));

    /** Un bâtiment de la ville, avec l'intérieur qu'il ouvre. */
    private static class TownBuilding extends Building {
        final String interior;
        final String shop;

        TownBuilding(String name, String type, String interior, String shop) {
            super(name, type);
            this.interior = interior;
            this.shop = shop;
        }
    }

    /** Ce qu'on obtient en visant la porte de la ville (action « gate » de town.json). */
    private static final String GATE = "Gate";

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
        DayNightCycle.Light light = DayNightCycle.compute(
                gameClock.getTimeOfDay(), gameClock.getSunrise(), gameClock.getSunset(), getAmbientLight());

        sky.set(light.sky[0], light.sky[1], light.sky[2], 1f);

        ambientLight.color.set(light.ambient[0], light.ambient[1], light.ambient[2], 1f);
        sunLight.color.set(light.lightColor[0], light.lightColor[1], light.lightColor[2], 1f);
        sunLight.direction.set(light.lightDirection[0], light.lightDirection[1], light.lightDirection[2]).nor();
    }

    @Override
    protected float getAmbientLight() {
        return 0.4f;
    }

    @Override
    protected float getInteractionRange() {
        return 10f;
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
        for (TownData.PartData part : town.decor) {
            addPart(part, 0, 0, "decor", "gate".equals(part.action) ? GATE : null);
        }
    }

    /** Couleur par son nom LibGDX (« BROWN ») ou en hexadécimal (« #5B8C3A »). */
    private static Color parseColor(String name, String where) {
        return ColorNames.parse(name, "town.json", where);
    }

    /** Une boîte du décor ; (ox, oz) est le centre du bâtiment pour ses détails, {@code userData} ce qu'on obtient en la visant. */
    private void addPart(TownData.PartData part, float ox, float oz, String where, Object userData) {
        Color color = parseColor(part.color, where);
        float x = ox + part.x;
        float z = oz + part.z;
        float centerY = 0.5f + part.y + part.h / 2f;   // le sol est une boîte d'1 m dont le dessus est à 0,5
        if (part.decal) {
            addVisualBox(part.w, part.h, part.d, color, x, centerY, z);
        } else {
            addBox(part.w, part.h, part.d, color, x, centerY, z, userData);
        }
    }

    private void addBuilding(TownData.BuildingData data) {
        Color color = parseColor(data.color, "batiment \"" + data.name + "\"");
        if (!KNOWN_INTERIORS.contains(data.interior)) {
            throw new IllegalStateException("town.json : batiment \"" + data.name + "\" : interieur inconnu \"" + data.interior
                    + "\" (attendu : " + KNOWN_INTERIORS + ")");
        }
        TownBuilding building = new TownBuilding(data.name, data.type, data.interior, data.shop);
        addBox(10f, 10f, 10f, color, data.x, 5.5f, data.z, building).userData = building;
        for (TownData.PartData part : data.parts) {
            addPart(part, data.x, data.z, "batiment \"" + data.name + "\" (details)", building); // viser la porte ou l'enseigne ouvre aussi le batiment
        }
    }

    // ------------------------------------------------------------------ interactions

    @Override
    protected String getInteractionPrompt(Object userData) {
        if (userData instanceof Building) {
            return "Entrer dans: " + ((Building) userData).getName() + " (E)";
        }
        if (GATE.equals(userData)) {
            return "Prendre la route (E)";
        }
        return null;
    }

    @Override
    protected boolean onInteract(Object userData) {
        if (GATE.equals(userData)) {
            game.setScreen(new WorldMapScreen(game, difficulty));
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
                return new StoreScreen(game, difficulty, building.getName(), building.shop);
            default:
                throw new IllegalStateException("interieur inconnu : " + building.interior);
        }
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
                GameSession loaded = game.getSession();
                Vector3 pos = loaded.hasSavedPosition()
                        ? new Vector3(loaded.getSavedX(), loaded.getSavedY(), loaded.getSavedZ())
                        : new Vector3(0, 2, 0);
                Gdx.app.log("SAVE", "Sauvegarde chargée : " + loaded.getPlayer().getName());
                game.setScreen(new TownScreen(game, loaded.getDifficulty(), pos));
            } else {
                Gdx.app.error("SAVE", "Aucune sauvegarde utilisable à charger");
            }
            return true;
        }
        return false;
    }
}
