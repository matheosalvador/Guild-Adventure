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

/** openworld.json cassé (et le panneau « walk » d'une ville) : le message nomme le fichier et le problème. Un test par cas. */
class OpenWorldContentErrorsTest {

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
        // les fichiers peuvent etre en fins de ligne Windows (CRLF) selon la configuration de git : on compare en LF
        String text = new String(Files.readAllBytes(target), StandardCharsets.UTF_8).replace("\r\n", "\n");
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

    private void expectWorldError(String from, String to, String... fragments) throws IOException {
        expectError("openworld.json", from, to, fragments);
    }

    @Test
    void scaleZero() throws IOException {
        expectWorldError("\"scale\": 100", "\"scale\": 0", "openworld.json", "scale");
    }

    @Test
    void tilesTooSmall() throws IOException {
        expectWorldError("\"tileSize\": 128", "\"tileSize\": 10", "openworld.json", "tileSize");
    }

    @Test
    void loadRadiusZero() throws IOException {
        expectWorldError("\"loadRadius\": 2", "\"loadRadius\": 0", "openworld.json", "loadRadius");
    }

    @Test
    void solidRadiusBeyondLoadRadius() throws IOException {
        expectWorldError("\"solidRadius\": 1", "\"solidRadius\": 3", "openworld.json", "solidRadius");
    }

    @Test
    void viewDistanceTooFar() throws IOException {
        expectWorldError("\"viewDistance\": 270", "\"viewDistance\": 900", "openworld.json", "viewDistance");
    }

    @Test
    void groundMissing() throws IOException {
        expectWorldError("\"ground\": \"#4E7A36\",", "", "openworld.json", "ground");
    }

    @Test
    void respawnDaysZero() throws IOException {
        expectWorldError("\"monsterRespawnDays\": 3", "\"monsterRespawnDays\": 0", "openworld.json", "monsterRespawnDays");
    }

    @Test
    void unknownPlace() throws IOException {
        expectWorldError("{ \"id\": \"aethelgard\", \"half\": 100 }", "{ \"id\": \"atlantide\", \"half\": 100 }",
                "openworld.json", "atlantide", "world.json");
    }

    @Test
    void placeTwice() throws IOException {
        expectWorldError("{ \"id\": \"aethelgard\", \"half\": 100 }", "{ \"id\": \"aethelgard\", \"half\": 100 },\n    { \"id\": \"aethelgard\", \"half\": 100 }",
                "openworld.json", "aethelgard", "en double");
    }

    @Test
    void placeWithoutSize() throws IOException {
        expectWorldError("\"half\": 100", "\"half\": 0", "openworld.json", "aethelgard", "half");
    }

    @Test
    void entranceWithoutWidth() throws IOException {
        expectWorldError("\"width\": 10", "\"width\": 0", "openworld.json", "foret_loups", "width");
    }

    @Test
    void roadFromAPlaceNotInTheWorld() throws IOException {
        expectWorldError("\"from\": \"aethelgard\"", "\"from\": \"portbrume\"", "openworld.json", "route n°1", "from", "portbrume");
    }

    @Test
    void roadWithOnePoint() throws IOException {
        expectWorldError("{ \"x\": 0, \"z\": 62 },\n        { \"x\": -68, \"z\": 70 },\n        { \"x\": -900, \"z\": -1105 },\n        { \"x\": -900, \"z\": -1130 }",
                "{ \"x\": 0, \"z\": 62 }", "openworld.json", "route n°1", "points");
    }

    @Test
    void walkSignToAPlaceOutsideTheWorld() throws IOException {
        expectError("town.json", "\"action\": \"walk\", \"text\": \"foret_loups\"}\n", "\"action\": \"walk\", \"text\": \"portbrume\"}\n",
                "town.json", "portbrume", "openworld.json");
    }
}
