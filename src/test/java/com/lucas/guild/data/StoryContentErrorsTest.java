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

/**
 * Fichiers de l'histoire cassés (story.json, et ce que l'histoire a ajouté aux lieux, aux zones, aux monstres et aux
 * rumeurs) : le message nomme le fichier et le problème. Un test par cas d'erreur.
 */
class StoryContentErrorsTest {

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

    // ------------------------------------------------------------------ story.json : rangs et conditions

    @Test
    void rankOutOfSequence() throws IOException {
        expectError("story.json", "{ \"rank\": 2, \"label\": \"D\"", "{ \"rank\": 3, \"label\": \"D\"", "story.json", "rang", "rank");
    }

    @Test
    void rankWithoutLabel() throws IOException {
        expectError("story.json", "\"rank\": 1, \"label\": \"E\"", "\"rank\": 1, \"label\": \"\"", "story.json", "label");
    }

    @Test
    void conditionOfUnknownType() throws IOException {
        expectError("story.json", "[\"flag:done:chapitre1\", \"quests:4\"]", "[\"drapeau:done:chapitre1\", \"quests:4\"]", "story.json", "condition inconnue", "drapeau");
    }

    @Test
    void conditionExpectingANumber() throws IOException {
        expectError("story.json", "\"quests:4\"", "\"quests:beaucoup\"", "story.json", "attend un nombre");
    }

    @Test
    void conditionSinceWithoutMinutes() throws IOException {
        expectError("story.json", "\"!since:seal_force_cracked:4320\"", "\"!since:seal_force_cracked\"", "story.json", "drapeau:minutes");
    }

    @Test
    void conditionOnUnknownQuest() throws IOException {
        expectError("story.json", "\"quest:Nettoyage de la cave\"", "\"quest:Quete fantome\"", "story.json", "quete inconnue", "Quete fantome");
    }

    @Test
    void conditionOnUnknownLore() throws IOException {
        expectError("story.json", "\"lore:text:stele_ruines\"", "\"lore:text:stele_fantome\"", "story.json", "savoir inconnu", "stele_fantome");
    }

    @Test
    void conditionOnUnknownItem() throws IOException {
        expectError("story.json", "\"!item:oath_ore\"", "\"!item:objet_fantome\"", "story.json", "objet inconnu", "objet_fantome");
    }

    // ------------------------------------------------------------------ story.json : chapitres

    @Test
    void chapterDuplicateId() throws IOException {
        expectError("story.json", "\"id\": \"chapitre1\"", "\"id\": \"prologue\"", "story.json", "chapitre", "en double");
    }

    @Test
    void chapterWithoutTitle() throws IOException {
        expectError("story.json", "\"title\": \"Prologue : Le rang F\",", "", "story.json", "prologue", "title");
    }

    @Test
    void chapterWithoutIntro() throws IOException {
        expectError("story.json", "\"intro\": \"", "\"introduction\": \"", "story.json", "prologue", "intro");
    }

    @Test
    void chapterWithoutOutro() throws IOException {
        expectError("story.json", "\"outro\": \"", "\"conclusion\": \"", "story.json", "prologue", "outro");
    }

    @Test
    void chapterWithoutObjectives() throws IOException {
        expectError("story.json", "\"objectives\": [", "\"objectifs\": [", "story.json", "prologue", "aucun objectif");
    }

    @Test
    void chapterNegativeGold() throws IOException {
        expectError("story.json", "\"goldReward\": 30,", "\"goldReward\": -30,", "story.json", "prologue", "goldReward");
    }

    @Test
    void objectiveWithoutText() throws IOException {
        expectError("story.json", "{ \"text\": \"Montrer la plaque au maître de guilde\", \"done\"", "{ \"done\"", "story.json", "objectif", "text");
    }

    @Test
    void objectiveWithoutCondition() throws IOException {
        expectError("story.json", "\"done\": [\"flag:plaque_shown\"] }", "\"done\": [] }", "story.json", "objectif", "done");
    }

    // ------------------------------------------------------------------ story.json : dialogues

    @Test
    void dialogDuplicateId() throws IOException {
        expectError("story.json", "\"id\": \"odran_registre\"", "\"id\": \"odran_plaque\"", "story.json", "dialogue", "en double");
    }

    @Test
    void dialogAtUnknownNpc() throws IOException {
        expectError("story.json", "\"id\": \"durgan_lames\", \"at\": \"blacksmith@aethelgard\"", "\"id\": \"durgan_lames\", \"at\": \"boulanger@aethelgard\"",
                "story.json", "durgan_lames", "\"at\" inconnu", "boulanger");
    }

    @Test
    void dialogAtAPlaceThatIsNotATown() throws IOException {
        expectError("story.json", "\"id\": \"garin_mine\", \"at\": \"innkeeper@hautevigne\"", "\"id\": \"garin_mine\", \"at\": \"innkeeper@foret_loups\"",
                "story.json", "garin_mine", "\"at\" inconnu");
    }

    @Test
    void dialogWithoutLabel() throws IOException {
        expectError("story.json", "\"label\": \"Montrer la plaque de soldat\",", "", "story.json", "odran_plaque", "label");
    }

    @Test
    void dialogWithoutText() throws IOException {
        expectError("story.json", "\"text\": \"Odran lit.", "\"texte\": \"Odran lit.", "story.json", "odran_lettre", "text");
    }

    @Test
    void optionWithoutLabel() throws IOException {
        expectError("story.json", "{ \"label\": \"Je descends\", ", "{ ", "story.json", "force_depart", "option", "label");
    }

    @Test
    void optionLeadingToUnknownDialog() throws IOException {
        expectError("story.json", "\"next\": \"cairn_enigme2\"", "\"next\": \"nulle_part\"", "story.json", "cairn_enigme", "next", "nulle_part");
    }

    @Test
    void dialogGivingUnknownItem() throws IOException {
        expectError("story.json", "\"gives\": [\"oath_ore\"], \"sets\": [\"endurance_started\"]", "\"gives\": [\"objet_fantome\"], \"sets\": [\"endurance_started\"]",
                "story.json", "endurance_depart", "objet inconnu", "objet_fantome");
    }

    @Test
    void pointWithoutName() throws IOException {
        expectError("story.json", "\"cairn\": \"Le Cairn des Anciens\"", "\"cairn\": \"\"", "story.json", "point", "cairn");
    }

    // ------------------------------------------------------------------ story.json : objets, déclencheurs, règles

    @Test
    void pickupWithoutFlag() throws IOException {
        expectError("story.json", "\"name\": \"Plaque de soldat\", \"flag\": \"plaque_found\",", "\"name\": \"Plaque de soldat\",", "story.json", "objet a ramasser", "flag");
    }

    @Test
    void pickupWithoutScene() throws IOException {
        expectError("story.json", "{ \"scene\": \"CellarScreen@aethelgard\", ", "{ ", "story.json", "objet a ramasser", "scene");
    }

    @Test
    void triggerDuplicateId() throws IOException {
        expectError("story.json", "\"id\": \"corvenne_varn\"", "\"id\": \"corvenne_herse\"", "story.json", "declencheur", "en double");
    }

    @Test
    void triggerWithoutRadius() throws IOException {
        expectError("story.json", "\"radius\": 8,", "\"radius\": 0,", "story.json", "corvenne_herse", "radius");
    }

    @Test
    void ruleSettingNothing() throws IOException {
        expectError("story.json", "\"sets\": [\"sagesse_ready\"] }", "\"sets\": [] }", "story.json", "regle", "sets");
    }

    // ------------------------------------------------------------------ story.json : fins, avantages, effets

    @Test
    void endingDuplicateId() throws IOException {
        expectError("story.json", "\"id\": \"garder\"", "\"id\": \"briser\"", "story.json", "fin", "en double");
    }

    @Test
    void endingWithoutTrigger() throws IOException {
        expectError("story.json", "\"title\": \"Le Sablier gardé\",\n      \"requires\": [\"flag:ending_keep\"],", "\"title\": \"Le Sablier gardé\",", "story.json", "garder", "requires");
    }

    @Test
    void boonInUnknownPlace() throws IOException {
        expectError("story.json", "\"places\": [\"corvenne_cour\", \"corvenne_salle\", \"corvenne_crypte\"], \"damagePercent\": 15",
                "\"places\": [\"nulle_part\"], \"damagePercent\": 15", "story.json", "avantage", "lieu inconnu", "nulle_part");
    }

    @Test
    void boonWithoutEffect() throws IOException {
        expectError("story.json", "\"damagePercent\": 15 }", "\"damagePercent\": 0 }", "story.json", "avantage", "aucun effet");
    }

    @Test
    void effectOnUnknownShop() throws IOException {
        expectError("story.json", "\"hautevigne_store\": 100", "\"boutique_fantome\": 100", "story.json", "effet", "boutique_fantome");
    }

    @Test
    void effectPriceFactorOutOfRange() throws IOException {
        expectError("story.json", "\"hautevigne_store\": 100", "\"hautevigne_store\": 5", "story.json", "effet", "priceFactor");
    }

    @Test
    void effectAmbushPercentOutOfRange() throws IOException {
        expectError("story.json", "\"ambushPercent\": 40", "\"ambushPercent\": 900", "story.json", "effet", "ambushPercent");
    }

    // ------------------------------------------------------------------ world.json : lieux verrouillés et cachés

    @Test
    void hiddenTown() throws IOException {
        expectError("world.json", "\"townFile\": \"town_portbrume.json\", \"x\": 30, \"z\": -14 }", "\"townFile\": \"town_portbrume.json\", \"x\": 30, \"z\": -14, \"hidden\": true }",
                "world.json", "portbrume", "hidden");
    }

    @Test
    void roadToAHiddenPlace() throws IOException {
        expectError("world.json", "[\"hautevigne\", \"mine_galeries\"]", "[\"hautevigne\", \"mine_chambre\"]", "world.json", "cache", "mine_chambre");
    }

    @Test
    void placeWithMalformedCondition() throws IOException {
        expectError("world.json", "\"requires\": [\"flag:mandat\"]", "\"requires\": [\"mandat\"]", "world.json", "mine_galeries", "condition inconnue");
    }

    @Test
    void lockedStartPlace() throws IOException {
        expectError("world.json", "\"townFile\": \"town.json\", \"x\": 0, \"z\": 0 }", "\"townFile\": \"town.json\", \"x\": 0, \"z\": 0, \"requires\": [\"flag:x\"] }",
                "world.json", "startPlace");
    }

    // ------------------------------------------------------------------ zones : portes, arène, points d'histoire

    @Test
    void doorToUnknownPlace() throws IOException {
        expectError("zone_mine_galeries.json", "\"to\": \"mine_chambre\"", "\"to\": \"nulle_part\"", "zone_mine_galeries.json", "porte", "nulle_part");
    }

    @Test
    void lockedDoorWithoutReason() throws IOException {
        expectError("zone_corvenne_salle.json", "\"lockedText\": \"La porte de la crypte est fermée à clé.\",", "", "zone_corvenne_salle.json", "porte", "lockedText");
    }

    @Test
    void doorWithoutLabel() throws IOException {
        expectError("zone_mine_chambre.json", "\"label\": \"Remonter vers les galeries\",", "", "zone_mine_chambre.json", "porte", "label");
    }

    @Test
    void zoneWithNeitherRoadNorDoor() throws IOException {
        expectError("zone_fosse.json", " \"exit\": {\n  \"x\": 0,\n  \"z\": 20.5\n },", "", "zone_fosse.json", "exit");
    }

    @Test
    void arenaWithoutStartFlag() throws IOException {
        expectError("zone_fosse.json", "\"startFlag\": \"force_running\",", "", "zone_fosse.json", "arena", "startFlag");
    }

    @Test
    void arenaWithUnknownMonster() throws IOException {
        expectError("zone_fosse.json", "\"id\": \"skeleton_miner\"", "\"id\": \"monstre_fantome\"", "zone_fosse.json", "arena", "monstre_fantome");
    }

    @Test
    void decorPointingToUnknownStoryPoint() throws IOException {
        expectError("zone_fosse.json", "\"text\": \"fosse_master\"", "\"text\": \"point_fantome\"", "zone_fosse.json", "point d'histoire inconnu", "point_fantome");
    }

    @Test
    void storyActionWithoutPoint() throws IOException {
        expectError("zone_corvenne_crypte.json", "\"action\": \"story\",\n   \"text\": \"sablier\"", "\"action\": \"story\"", "zone_corvenne_crypte.json", "story", "text");
    }

    @Test
    void spawnWithMalformedCondition() throws IOException {
        expectError("zone_mine_chambre.json", "\"!flag:letter_found\"", "\"letter_found\"", "zone_mine_chambre.json", "sergeant_holt", "condition inconnue");
    }

    @Test
    void zoneAmbientOutOfRange() throws IOException {
        expectError("zone_mine_chambre.json", "\"ambient\": 0.22", "\"ambient\": 3", "zone_mine_chambre.json", "ambient");
    }

    // ------------------------------------------------------------------ monsters.json : phases et reddition

    @Test
    void monsterYieldingWithoutFlag() throws IOException {
        expectError("monsters.json", "\"yieldPercent\": 5, \"yieldFlag\": \"aldric_yielded\",", "\"yieldPercent\": 5,", "monsters.json", "lord_aldric", "yieldFlag");
    }

    @Test
    void monsterWindUpLongerThanItsCooldown() throws IOException {
        expectError("monsters.json", "\"aggroRange\": 22, \"attackRange\": 2.0, \"attackCooldown\": 1.4,",
                "\"aggroRange\": 22, \"attackRange\": 2.0, \"attackCooldown\": 1.4, \"windUp\": 1.5,", "monsters.json", "sergeant_holt", "windUp");
    }

    @Test
    void monsterPhaseFasterThanItsWindUp() throws IOException {
        expectError("monsters.json", "\"speed\": 3.0, \"attackCooldown\": 1.6,", "\"speed\": 3.0, \"attackCooldown\": 0.4,", "monsters.json", "lord_aldric", "phase", "attackCooldown");
    }

    @Test
    void monsterPhasesNotDecreasing() throws IOException {
        expectError("monsters.json", "{ \"below\": 25, \"damage\": 30", "{ \"below\": 80, \"damage\": 30", "monsters.json", "lord_aldric", "phase", "below");
    }

    @Test
    void monsterPhaseSummoningUnknownMonster() throws IOException {
        expectError("monsters.json", "\"summon\": \"ageless_soldier\"", "\"summon\": \"monstre_fantome\"", "monsters.json", "lord_aldric", "renfort inconnu", "monstre_fantome");
    }

    @Test
    void monsterPhaseHealWithoutAmount() throws IOException {
        expectError("monsters.json", "\"healEvery\": 12, \"healAmount\": 40,", "\"healEvery\": 12,", "monsters.json", "lord_aldric", "healEvery");
    }

    @Test
    void monsterNegativeFlyHeight() throws IOException {
        expectError("monsters.json", "\"flyHeight\": 0.9", "\"flyHeight\": -1", "monsters.json", "bat", "flyHeight");
    }

    // ------------------------------------------------------------------ lore.json : rumeurs liées à l'histoire

    @Test
    void rumorWithMalformedCondition() throws IOException {
        expectError("lore.json", "\"requires\": [\"flag:done:prologue\"], \"text\": \"La tour", "\"requires\": [\"prologue\"], \"text\": \"La tour",
                "lore.json", "aeth_tour", "condition inconnue");
    }
}
