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

/** npcs.json cassé (et le lien des habitants de town.json) : le message nomme le fichier et le problème. Un test par cas. */
class NpcsContentErrorsTest {

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

    private void expectNpcsError(String from, String to, String... fragments) throws IOException {
        expectError("npcs.json", from, to, fragments);
    }

    // ------------------------------------------------------------------ PNJ des bâtiments

    @Test
    void npcMissing() throws IOException {
        expectNpcsError("    {\n      \"id\": \"blacksmith\",\n      \"prompt\": \"Parler au forgeron\",\n"
                + "      \"greeting\": \"Que puis-je faire pour vous ?\",\n      \"options\": [\n"
                + "        { \"action\": \"deliver\", \"label\": \"Remettre la lettre\", \"target\": \"Forgeron\" },\n"
                + "        { \"action\": \"shop\", \"label\": \"Armes et armures\", \"shop\": \"forge\" }\n      ]\n    },\n",
                "", "npcs.json", "PNJ manquant", "blacksmith");
    }

    @Test
    void npcUnknownId() throws IOException {
        expectNpcsError("\"id\": \"blacksmith\"", "\"id\": \"forgeron\"", "npcs.json", "forgeron", "inconnu");
    }

    @Test
    void npcDuplicated() throws IOException {
        expectNpcsError("\"id\": \"blacksmith\"", "\"id\": \"innkeeper\"", "npcs.json", "innkeeper", "en double");
    }

    @Test
    void npcWithoutPrompt() throws IOException {
        expectNpcsError("\"prompt\": \"Parler au forgeron\"", "\"prompt\": \"\"", "npcs.json", "blacksmith", "prompt");
    }

    @Test
    void npcWithoutGreeting() throws IOException {
        expectNpcsError("\"greeting\": \"Que puis-je faire pour vous ?\"", "\"greeting\": \"\"", "npcs.json", "blacksmith", "greeting");
    }

    // ------------------------------------------------------------------ choix

    @Test
    void optionWithoutAction() throws IOException {
        expectNpcsError("\"action\": \"deliver\", \"label\": \"Remettre la lettre\"", "\"label\": \"Remettre la lettre\"",
                "npcs.json", "blacksmith", "choix n°1", "action");
    }

    @Test
    void optionWithUnknownAction() throws IOException {
        expectNpcsError("\"action\": \"deliver\", \"label\": \"Remettre la lettre\"", "\"action\": \"danser\", \"label\": \"Remettre la lettre\"",
                "npcs.json", "inconnue", "danser");
    }

    @Test
    void optionWithoutLabel() throws IOException {
        expectNpcsError("\"label\": \"Armes et armures\", ", "", "npcs.json", "blacksmith", "label");
    }

    @Test
    void negativePrice() throws IOException {
        expectNpcsError("\"price\": 5", "\"price\": -5", "npcs.json", "innkeeper", "price");
    }

    @Test
    void negativeHeal() throws IOException {
        expectNpcsError("\"heal\": 20", "\"heal\": -20", "npcs.json", "innkeeper", "heal");
    }

    @Test
    void serveWithoutText() throws IOException {
        expectNpcsError("\"text\": \"Vous buvez une bière. Ça fait du bien.\", ", "", "npcs.json", "serve", "text");
    }

    @Test
    void talkWithoutText() throws IOException {
        expectNpcsError("\"text\": \"\\\"Finissez des quêtes", "\"texte\": \"\\\"Finissez des quêtes", "npcs.json", "talk", "text");
    }

    @Test
    void rumorWithoutIntro() throws IOException {
        expectNpcsError("\"intro\": \"L'aubergiste se penche vers vous :\",", "", "npcs.json", "rumor", "intro");
    }

    @Test
    void newsWithoutEmptyText() throws IOException {
        expectNpcsError("\"emptyText\": \"Rien de spécial ailleurs", "\"vide\": \"Rien de spécial ailleurs", "npcs.json", "news", "emptyText");
    }

    @Test
    void unknownShop() throws IOException {
        expectNpcsError("\"shop\": \"forge\"", "\"shop\": \"forgee\"", "npcs.json", "Armes et armures", "forgee");
    }

    @Test
    void shopMissingOutsideTheStore() throws IOException {
        expectNpcsError(", \"shop\": \"forge\"", "", "npcs.json", "blacksmith", "\"shop\" manquant");
    }

    @Test
    void optionWithBadCondition() throws IOException {
        expectNpcsError("\"label\": \"Comment monter en rang ?\",", "\"label\": \"Comment monter en rang ?\", \"requires\": [\"drapeau:x\"],",
                "npcs.json", "Comment monter en rang", "drapeau");
    }

    // ------------------------------------------------------------------ habitants

    @Test
    void villagerDuplicated() throws IOException {
        expectNpcsError("\"villagers\": [", "\"villagers\": [\n    { \"id\": \"garde\", \"name\": \"Autre garde\", \"lines\": [{ \"text\": \"Halte.\" }] },",
                "npcs.json", "garde", "en double");
    }

    @Test
    void villagerWithoutName() throws IOException {
        expectNpcsError("\"name\": \"Jorin, porteur d'eau\"", "\"name\": \"\"", "npcs.json", "porteur", "name");
    }

    @Test
    void villagerWithoutLines() throws IOException {
        expectNpcsError("\"name\": \"Tarn, vieux mineur\",\n      \"lines\"", "\"name\": \"Tarn, vieux mineur\",\n      \"autres\"",
                "npcs.json", "vieux_mineur", "lines");
    }

    @Test
    void lineWithoutText() throws IOException {
        expectNpcsError("{ \"text\": \"Quarante ans", "{ \"texte\": \"Quarante ans", "npcs.json", "vieux_mineur", "replique n°1", "text");
    }

    @Test
    void lineInUnknownTown() throws IOException {
        expectNpcsError("\"town\": \"portbrume\"", "\"town\": \"atlantide\"", "npcs.json", "garde", "atlantide");
    }

    @Test
    void lineWithUnknownQuestCondition() throws IOException {
        expectNpcsError("[\"!flag:mandat\"]", "[\"quest:Quete fantome\"]", "npcs.json", "garde", "Quete fantome");
    }

    @Test
    void villagerTalkUnknownInTown() throws IOException {
        expectError("town.json", "\"talk\": \"porteur\"", "\"talk\": \"inconnu\"", "town.json", "talk", "inconnu", "npcs.json");
    }
}
