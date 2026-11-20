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

/** openworld.json cassé : le message nomme le fichier et le problème. Un test par cas. */
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
        expectWorldError("{ \"id\": \"aethelgard\", \"half\": 70 }", "{ \"id\": \"atlantide\", \"half\": 70 }",
                "openworld.json", "atlantide", "world.json");
    }

    @Test
    void placeTwice() throws IOException {
        expectWorldError("{ \"id\": \"aethelgard\", \"half\": 70 }", "{ \"id\": \"aethelgard\", \"half\": 70 },\n    { \"id\": \"aethelgard\", \"half\": 70 }",
                "openworld.json", "aethelgard", "en double");
    }

    @Test
    void placeWithoutSize() throws IOException {
        expectWorldError("{ \"id\": \"aethelgard\", \"half\": 70 }", "{ \"id\": \"aethelgard\", \"half\": 0 }", "openworld.json", "aethelgard", "half");
    }

    @Test
    void entranceWithoutWidth() throws IOException {
        expectWorldError("\"width\": 10", "\"width\": 0", "openworld.json", "foret_loups", "width");
    }

    @Test
    void zoneWithoutEntrance() throws IOException {
        expectWorldError("{ \"id\": \"ruines_gobelines\", \"half\": 72, \"entrance\": { \"x\": 0, \"z\": 71, \"width\": 10 } }",
                "{ \"id\": \"ruines_gobelines\", \"half\": 72 }", "openworld.json", "ruines_gobelines", "entrance");
    }

    @Test
    void placeOfTheMapMissingFromTheWorld() throws IOException {
        expectWorldError("    { \"id\": \"hautevigne\", \"half\": 70 },\n", "", "openworld.json", "hautevigne", "ni dans");
    }

    @Test
    void portalWithoutDoor() throws IOException {
        expectWorldError("\"color\": \"#0B0B0D\", \"action\": \"enter\"", "\"color\": \"#0B0B0D\"", "openworld.json", "mine_galeries", "enter");
    }

    @Test
    void portalOnATown() throws IOException {
        expectWorldError("\"id\": \"fosse_debardeurs\", \"half\": 12", "\"id\": \"portbrume\", \"half\": 12", "openworld.json", "portbrume", "places");
    }

    @Test
    void portalWithAnUnknownAction() throws IOException {
        expectWorldError("\"color\": \"#0B0B0D\", \"action\": \"enter\"", "\"color\": \"#0B0B0D\", \"action\": \"entrer\"", "openworld.json", "mine_galeries", "entrer");
    }

    @Test
    void roadBetweenPlacesThatAreNotLinked() throws IOException {
        expectWorldError("\"roads\": []", "\"roads\": [ { \"from\": \"foret_loups\", \"to\": \"portbrume\", \"via\": [] } ]",
                "openworld.json", "route n°1", "pas relies");
    }

    @Test
    void siteWithAnUnknownMonster() throws IOException {
        expectWorldError("{ \"id\": \"bandit\", \"x\": -3, \"z\": 0 }", "{ \"id\": \"brigand\", \"x\": -3, \"z\": 0 }",
                "openworld.json", "camp_bandits", "brigand", "monsters.json");
    }

    @Test
    void siteNamedLikeAPlaceOfTheMap() throws IOException {
        expectWorldError("\"id\": \"tour_guet\"", "\"id\": \"aethelgard\"", "openworld.json", "aethelgard", "en double");
    }

    @Test
    void siteMonsterOutsideTheSite() throws IOException {
        expectWorldError("{ \"id\": \"wolf\", \"x\": -6, \"z\": -5 }", "{ \"id\": \"wolf\", \"x\": -60, \"z\": -5 }",
                "openworld.json", "tour_guet", "hors du site");
    }

    @Test
    void gatheringOfAnUnknownKind() throws IOException {
        expectWorldError("\"kind\": \"herb\"", "\"kind\": \"fleur\"", "openworld.json", "gathering", "fleur");
    }

    @Test
    void gatheringAnUnknownItem() throws IOException {
        expectWorldError("\"item\": \"healing_herb\"", "\"item\": \"herbe_magique\"", "openworld.json", "gathering", "herbe_magique", "items.json");
    }

    @Test
    void travelerOnARoadThatDoesNotExist() throws IOException {
        expectWorldError("\"from\": \"portbrume\", \"to\": \"hautevigne\", \"speed\": 1.1", "\"from\": \"foret_loups\", \"to\": \"portbrume\", \"speed\": 1.1",
                "openworld.json", "voyageur", "foret_loups");
    }

    @Test
    void travelerWithoutLines() throws IOException {
        expectWorldError("\"talk\": \"colporteur\"", "\"talk\": \"bavard\"", "openworld.json", "voyageur", "bavard", "npcs.json");
    }

    @Test
    void roadFromAnUnknownPlace() throws IOException {
        expectWorldError("\"roads\": []", "\"roads\": [ { \"from\": \"atlantide\", \"to\": \"portbrume\" } ]",
                "openworld.json", "route n°1", "from", "atlantide");
    }
}
