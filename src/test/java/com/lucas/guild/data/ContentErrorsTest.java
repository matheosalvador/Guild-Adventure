package com.lucas.guild.data;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Un fichier de donnees casse doit faire echouer le lancement avec un message qui NOMME le fichier
 * et le probleme. Chaque test copie assets/data dans un dossier temporaire, abime un fichier,
 * puis verifie le message.
 */
class ContentErrorsTest {

    @TempDir
    Path tmp;

    @AfterEach
    void restore() {
        GameContent.overrideDataDirForTests(null);
    }

    /** Copie assets/data, remplace {@code from} par {@code to} dans {@code file}, puis valide tout. */
    private String validateAfter(String file, String from, String to) throws IOException {
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
        if ("<DELETE>".equals(to)) {
            Files.delete(target);
        } else if ("<ALL>".equals(from)) {
            Files.write(target, to.getBytes(StandardCharsets.UTF_8));
        } else if (!from.isEmpty()) {
            String text = new String(Files.readAllBytes(target), StandardCharsets.UTF_8);
            assertTrue(text.contains(from), "scenario mal ecrit : '" + from + "' introuvable dans " + file);
            Files.write(target, text.replace(from, to).getBytes(StandardCharsets.UTF_8));
        }
        GameContent.overrideDataDirForTests(tmp);
        try {
            GameContent.validateAll();
            return null;
        } catch (IllegalStateException e) {
            return e.getMessage();
        }
    }

    private void expectError(String file, String from, String to, String... fragments) throws IOException {
        String message = validateAfter(file, from, to);
        assertTrue(message != null, "aucune erreur alors qu'on en attendait une");
        for (String fragment : fragments) {
            assertTrue(message.contains(fragment), "message sans \"" + fragment + "\" : " + message);
        }
    }

    @Test
    void validDataPasses() throws IOException {
        assertTrue(validateAfter("skills.json", "", "") == null);
    }

    // ---------------------------------------------------------------- skills.json

    @Test
    void skillDuplicateId() throws IOException {
        expectError("skills.json", "\"id\": \"vigor\"", "\"id\": \"healing\"", "skills.json", "en double");
    }

    @Test
    void skillNoThresholds() throws IOException {
        expectError("skills.json", "\"thresholds\": [4, 15, 40, 90, 180]", "\"thresholds\": []", "skills.json", "vigor", "thresholds");
    }

    @Test
    void skillThresholdsNotIncreasing() throws IOException {
        expectError("skills.json", "[4, 15, 40, 90, 180]", "[4, 15, 10, 90, 180]", "skills.json", "vigor", "croissante");
    }

    @Test
    void skillUnknownEffect() throws IOException {
        expectError("skills.json", "\"healthBonus\": 5", "\"flyPercent\": 5", "skills.json", "vigor", "flyPercent", "connus");
    }

    @Test
    void skillNoEffects() throws IOException {
        expectError("skills.json", "\"effects\": { \"healthBonus\": 5 }", "\"effects\": {}", "skills.json", "vigor", "effects");
    }

    @Test
    void skillZeroEffect() throws IOException {
        expectError("skills.json", "\"healthBonus\": 5", "\"healthBonus\": 0", "skills.json", "vigor", "non nulle");
    }

    @Test
    void skillBadNumber() throws IOException {
        expectError("skills.json", "[4, 15, 40, 90, 180]", "[4, \"beaucoup\"]", "skills.json", "invalide");
    }

    @Test
    void skillNoLabel() throws IOException {
        expectError("skills.json", "\"label\": \"Vigueur\",", "", "skills.json", "vigor", "label");
    }

    // ---------------------------------------------------------------- classes.json / backgrounds.json

    @Test
    void classUnknownItem() throws IOException {
        expectError("classes.json", "\"short_sword\"", "\"epee_magique\"", "classes.json", "warrior", "epee_magique", "items.json");
    }

    @Test
    void classUnknownSkillStart() throws IOException {
        expectError("classes.json", "\"athletics\": 1", "\"flying\": 1", "classes.json", "scout", "flying", "skillStart");
    }

    @Test
    void classSkillStartTooHigh() throws IOException {
        expectError("classes.json", "\"athletics\": 1", "\"athletics\": 9", "classes.json", "scout", "niveau entre 1 et 5");
    }

    @Test
    void classUnknownPracticeSkill() throws IOException {
        expectError("classes.json", "\"athletics\": 50", "\"flying\": 50", "classes.json", "scout", "flying", "practiceBonusPercent");
    }

    @Test
    void classPracticeMinus100() throws IOException {
        expectError("classes.json", "\"haggling\": -30", "\"haggling\": -100", "classes.json", "warrior", "> -100");
    }

    @Test
    void classDuplicateId() throws IOException {
        expectError("classes.json", "\"id\": \"scout\"", "\"id\": \"warrior\"", "classes.json", "en double");
    }

    @Test
    void classNoLabel() throws IOException {
        expectError("classes.json", "\"label\": \"Marchand\",", "", "classes.json", "merchant", "label");
    }

    @Test
    void classesEmpty() throws IOException {
        expectError("classes.json", "<ALL>", "[]", "classes.json", "au moins une");
    }

    @Test
    void backgroundsMissing() throws IOException {
        expectError("backgrounds.json", "", "<DELETE>", "backgrounds.json", "introuvable");
    }

    @Test
    void backgroundsBadJson() throws IOException {
        expectError("backgrounds.json", "\"goldBonus\": 50", "\"goldBonus\": lots", "backgrounds.json", "invalide");
    }

    // ---------------------------------------------------------------- items, difficultes

    @Test
    void itemDuplicateId() throws IOException {
        expectError("items.json", "\"id\": \"dagger\"", "\"id\": \"healing_potion\"", "items.json", "en double");
    }

    @Test
    void difficultyNegativeGold() throws IOException {
        expectError("difficulties.json", "\"startingGold\": 100", "\"startingGold\": -1", "difficulties.json", "facile", "startingGold");
    }

    @Test
    void difficultyUnknownStartingItem() throws IOException {
        expectError("difficulties.json", "\"healing_potion\", \"healing_potion\"", "\"epee_magique\"", "difficulties.json", "epee_magique");
    }

    // ---------------------------------------------------------------- town.json

    @Test
    void townPartWithZeroWidth() throws IOException {
        expectError("town.json", "\"w\": 12,", "\"w\": 0,", "town.json", "Forge", "\"w\"");
    }

    @Test
    void townPartWithoutColor() throws IOException {
        expectError("town.json", "\"color\": \"#2B2B2B\"", "\"colour\": \"#2B2B2B\"", "town.json", "color");
    }

    @Test
    void townHasDecor() {
        GameContent.overrideDataDirForTests(null);
        TownData town = GameContent.town();
        assertTrue(town.decor.size() > 50, "la ville a un decor (rues, maisons, arbres...)");
        assertTrue(town.buildings.stream().allMatch(b -> !b.parts.isEmpty()), "chaque batiment a des details");
    }

    // ---------------------------------------------------------------- quests.json

    @Test
    void questDuplicateTitle() throws IOException {
        expectError("quests.json", "\"title\": \"Livraison express\"", "\"title\": \"Nettoyage de la cave\"", "quests.json", "en double");
    }

    @Test
    void questNegativeReward() throws IOException {
        expectError("quests.json", "\"goldReward\": 20", "\"goldReward\": -5", "quests.json", "goldReward");
    }

    @Test
    void questWithoutProof() throws IOException {
        expectError("quests.json", "\"proofItem\": \"Queue de rat\"", "\"proofItem\": \"\"", "quests.json", "proofItem");
    }
}
