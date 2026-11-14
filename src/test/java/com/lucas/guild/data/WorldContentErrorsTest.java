package com.lucas.guild.data;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Fichiers du monde cassés (world.json, villes, boutiques) : le message nomme le fichier et le problème. */
class WorldContentErrorsTest {

    @TempDir
    Path tmp;

    @AfterEach
    void restore() {
        GameContent.overrideDataDirForTests(null);
    }

    private void expectError(String file, String from, String to, String... fragments) throws IOException {
        Path src = Paths.get("..", "..", "assets", "data");
        if (!Files.isDirectory(src)) {
            src = Paths.get("assets", "data");
        }
        try (java.util.stream.Stream<Path> files = Files.list(src)) {
            for (Path f : (Iterable<Path>) files::iterator) {
                if (f.toString().endsWith(".json")) {
                    Files.copy(f, tmp.resolve(f.getFileName().toString()));
                }
            }
        }
        Path target = tmp.resolve(file);
        String text = new String(Files.readAllBytes(target), StandardCharsets.UTF_8);
        assertTrue(text.contains(from), "scenario mal ecrit : '" + from + "' introuvable dans " + file);
        Files.write(target, text.replace(from, to).getBytes(StandardCharsets.UTF_8));
        GameContent.overrideDataDirForTests(tmp);
        String message = null;
        try {
            GameContent.validateAll();
        } catch (IllegalStateException e) {
            message = e.getMessage();
        }
        assertTrue(message != null, "aucune erreur alors qu'on en attendait une");
        for (String fragment : fragments) {
            assertTrue(message.contains(fragment), "message sans \"" + fragment + "\" : " + message);
        }
    }

    @Test
    void worldUnknownStartPlace() throws IOException {
        expectError("world.json", "\"startPlace\": \"aethelgard\"", "\"startPlace\": \"atlantide\"", "world.json", "startPlace", "atlantide");
    }

    @Test
    void worldLinkToUnknownPlace() throws IOException {
        expectError("world.json", "[\"portbrume\", \"hautevigne\"]", "[\"portbrume\", \"atlantide\"]", "world.json", "atlantide");
    }

    @Test
    void worldSelfLink() throws IOException {
        expectError("world.json", "[\"portbrume\", \"hautevigne\"]", "[\"portbrume\", \"portbrume\"]", "world.json", "lui-meme");
    }

    @Test
    void worldDuplicatePlace() throws IOException {
        expectError("world.json", "\"id\": \"hautevigne\"", "\"id\": \"portbrume\"", "world.json", "en double");
    }

    @Test
    void worldMissingTownFile() throws IOException {
        expectError("world.json", "town_portbrume.json", "town_fantome.json", "town_fantome.json", "introuvable");
    }

    @Test
    void worldBadSpeed() throws IOException {
        expectError("world.json", "\"walkingKmh\": 5", "\"walkingKmh\": 0", "world.json", "walkingKmh");
    }

    @Test
    void storeWithoutShop() throws IOException {
        expectError("town_portbrume.json", "\"shop\": \"portbrume_store\",", "", "town_portbrume.json", "Comptoir du Port", "shop");
    }

    @Test
    void storeWithUnknownShop() throws IOException {
        expectError("town_portbrume.json", "\"portbrume_store\"", "\"boutique_fantome\"", "town_portbrume.json", "boutique_fantome");
    }

    @Test
    void unknownPartAction() throws IOException {
        expectError("town_hautevigne.json", "\"action\": \"gate\"", "\"action\": \"teleport\"", "town_hautevigne.json", "teleport");
    }

    @Test
    void placeWithTownAndZone() throws IOException {
        expectError("world.json", "\"zoneFile\": \"zone_foret.json\"", "\"zoneFile\": \"zone_foret.json\", \"townFile\": \"town.json\"",
                "world.json", "foret_loups", "townFile", "zoneFile");
    }

    @Test
    void placeWithoutAnyFile() throws IOException {
        expectError("world.json", "\"zoneFile\": \"zone_foret.json\", ", "", "world.json", "foret_loups", "zoneFile");
    }

    @Test
    void zoneMissingFile() throws IOException {
        expectError("world.json", "zone_ruines.json", "zone_fantome.json", "zone_fantome.json", "introuvable");
    }

    @Test
    void zoneUnknownMonster() throws IOException {
        expectError("zone_foret.json", "\"id\": \"wolf\"", "\"id\": \"dragon\"", "zone_foret.json", "dragon", "monsters.json");
    }

    @Test
    void zoneChestUnknownItem() throws IOException {
        expectError("zone_foret.json", "\"healing_potion\"", "\"potion_magique\"", "zone_foret.json", "potion_magique", "items.json");
    }

    @Test
    void zoneWithoutExit() throws IOException {
        expectError("zone_foret.json", "\"exit\"", "\"sortie\"", "zone_foret.json", "exit");
    }

    @Test
    void zoneBadChestRespawn() throws IOException {
        expectError("zone_foret.json", "\"respawnDays\": 7", "\"respawnDays\": 0", "zone_foret.json", "respawnDays");
    }

    @Test
    void monsterDropUnknownItem() throws IOException {
        expectError("monsters.json", "\"wolf_pelt\"", "\"peau_magique\"", "monsters.json", "wolf", "peau_magique");
    }

    @Test
    void monsterDropBadChance() throws IOException {
        expectError("monsters.json", "\"chance\": 80", "\"chance\": 0", "monsters.json", "wolf_pelt", "chance");
    }

    @Test
    void ambushUnknownMonster() throws IOException {
        expectError("world.json", "[\"wolf\", \"goblin\"]", "[\"wolf\", \"dragon\"]", "world.json", "ambush", "dragon");
    }

    @Test
    void difficultyBadAmbushPercent() throws IOException {
        expectError("difficulties.json", "\"ambushPercent\": 20", "\"ambushPercent\": 200", "difficulties.json", "normale", "ambushPercent");
    }

    @Test
    void shopBadPriceFactor() throws IOException {
        expectError("shops.json", "\"priceFactor\": 90", "\"priceFactor\": 0", "shops.json", "portbrume_store", "priceFactor");
    }
}
