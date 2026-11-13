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
    void shopBadPriceFactor() throws IOException {
        expectError("shops.json", "\"priceFactor\": 90", "\"priceFactor\": 0", "shops.json", "portbrume_store", "priceFactor");
    }
}
