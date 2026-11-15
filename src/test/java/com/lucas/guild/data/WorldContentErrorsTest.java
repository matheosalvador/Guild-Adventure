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
    void readPartWithoutText() throws IOException {
        expectError("town.json", "\"action\": \"read\", \"text\": \"panneau_aethelgard\"},\n    {\"x\": 7, \"z\": 50, \"y\": 1.3",
                "\"action\": \"read\"},\n    {\"x\": 7, \"z\": 50, \"y\": 1.3", "town.json", "read", "text");
    }

    @Test
    void readPartWithUnknownText() throws IOException {
        expectError("town_portbrume.json", "\"panneau_portbrume\"", "\"panneau_fantome\"", "town_portbrume.json", "panneau_fantome", "lore.json");
    }

    @Test
    void zoneReadPartWithUnknownText() throws IOException {
        expectError("zone_ruines.json", "\"stele_ruines\"", "\"stele_fantome\"", "zone_ruines.json", "stele_fantome", "lore.json");
    }

    @Test
    void loreRumorWithoutText() throws IOException {
        expectError("lore.json", "\"text\": \"Des rats gros comme des chiens", "\"texte\": \"Des rats gros comme des chiens", "lore.json", "aeth_rats", "text");
    }

    @Test
    void loreRumorDuplicateId() throws IOException {
        expectError("lore.json", "\"id\": \"aeth_portbrume\"", "\"id\": \"aeth_rats\"", "lore.json", "aeth_rats", "double");
    }

    @Test
    void loreRumorForAnUnknownTown() throws IOException {
        expectError("lore.json", "\"town\": \"portbrume\", \"text\": \"Les marins", "\"town\": \"atlantide\", \"text\": \"Les marins", "lore.json", "atlantide");
    }

    @Test
    void loreRumorForAWildZone() throws IOException {
        expectError("lore.json", "\"town\": \"hautevigne\", \"text\": \"La vieille mine", "\"town\": \"foret_loups\", \"text\": \"La vieille mine", "lore.json", "foret_loups");
    }

    @Test
    void loreTextWithoutTitle() throws IOException {
        expectError("lore.json", "\"title\": \"Panneau de la forêt\"", "\"titre\": \"Panneau de la forêt\"", "lore.json", "panneau_foret", "title");
    }

    @Test
    void eventUnknownItem() throws IOException {
        expectError("events.json", "\"wolf_pelt\"", "\"peau_fantome\"", "events.json", "demande_peaux", "peau_fantome");
    }

    @Test
    void eventUnknownTown() throws IOException {
        expectError("events.json", "\"towns\": [\"hautevigne\"]", "\"towns\": [\"atlantide\"]", "events.json", "filon_retrouve", "atlantide");
    }

    @Test
    void eventInAWildZone() throws IOException {
        expectError("events.json", "\"towns\": [\"hautevigne\"]", "\"towns\": [\"foret_loups\"]", "events.json", "foret_loups");
    }

    @Test
    void eventBadDuration() throws IOException {
        expectError("events.json", "\"days\": 2,", "\"days\": 0,", "events.json", "caravane", "days");
    }

    @Test
    void eventWithoutAnyEffect() throws IOException {
        expectError("events.json", "\"buyChangePercent\": -15", "\"buyChangePercent\": 0", "events.json", "fete_marche", "effet");
    }

    @Test
    void eventDuplicateId() throws IOException {
        expectError("events.json", "\"id\": \"caravane\"", "\"id\": \"fete_marche\"", "events.json", "fete_marche", "double");
    }

    @Test
    void eventBadDailyChance() throws IOException {
        expectError("events.json", "\"dailyChance\": 15", "\"dailyChance\": 200", "events.json", "dailyChance");
    }

    @Test
    void questCollectUnknownItem() throws IOException {
        expectError("quests.json", "\"collectItem\": \"goblin_ear\"", "\"collectItem\": \"oreille_fantome\"", "quests.json", "Trophées de gobelins", "collectItem", "items.json");
    }

    @Test
    void questCollectBadCount() throws IOException {
        expectError("quests.json", "\"collectCount\": 4", "\"collectCount\": 0", "quests.json", "Trophées de gobelins", "collectCount");
    }

    @Test
    void questDeliveryUnknownRecipient() throws IOException {
        expectError("quests.json", "\"target\": \"Comptoir du Port\"", "\"target\": \"Le Chat Perché\"", "quests.json", "Colis pour Port-Brume", "destinataire inconnu", "Le Chat Perché");
    }

    @Test
    void questBoardInUnknownTown() throws IOException {
        expectError("quests.json", "\"town\": \"portbrume\"", "\"town\": \"atlantide\"", "quests.json", "atlantide");
    }

    @Test
    void questBoardInAWildZone() throws IOException {
        expectError("quests.json", "\"town\": \"hautevigne\"", "\"town\": \"foret_loups\"", "quests.json", "foret_loups");
    }

    @Test
    void questPayOnDeliveryWithoutParcel() throws IOException {
        expectError("quests.json", "\"giveItem\": \"Colis scellé\",\n    \"payOnDelivery\": true", "\"payOnDelivery\": true", "quests.json", "Colis pour Port-Brume", "payOnDelivery", "giveItem");
    }

    @Test
    void boardPartIsAKnownAction() throws IOException {
        // « board » est une action valide : le message d'une action inconnue ne la refuse pas
        expectError("town_portbrume.json", "\"action\": \"board\"", "\"action\": \"panneau\"", "town_portbrume.json", "panneau", "board");
    }

    @Test
    void modelUnknownShape() throws IOException {
        expectError("models.json", "\"shape\": \"sphere\"", "\"shape\": \"pyramide\"", "models.json", "shape", "pyramide");
    }

    @Test
    void modelUnknownJoint() throws IOException {
        expectError("models.json", "\"joint\": \"legL\"", "\"joint\": \"jambe\"", "models.json", "articulation inconnue", "jambe");
    }

    @Test
    void modelParentAfterChild() throws IOException {
        expectError("models.json", "\"parent\": \"torso\" }", "\"parent\": \"legR\" }", "models.json", "parent", "avant");
    }

    @Test
    void modelWalkUnknownJoint() throws IOException {
        expectError("models.json", "\"legL\": { \"axis\": \"x\", \"amp\": 28, \"phase\": 0 }", "\"jambeG\": { \"axis\": \"x\", \"amp\": 28, \"phase\": 0 }", "models.json", "walk", "jambeG");
    }

    @Test
    void modelBadAxis() throws IOException {
        expectError("models.json", "\"axis\": \"x\", \"amp\": 28", "\"axis\": \"w\", \"amp\": 28", "models.json", "axis");
    }

    @Test
    void modelBadColor() throws IOException {
        expectError("models.json", "\"#D9A87C\"", "\"#ZZZZZZ\"", "models.json", "color");
    }

    @Test
    void modelZeroSize() throws IOException {
        expectError("models.json", "\"w\": 0.45, \"h\": 0.08, \"d\": 0.29", "\"w\": 0, \"h\": 0.08, \"d\": 0.29", "models.json", "\"w\"");
    }

    @Test
    void modelBadRotation() throws IOException {
        expectError("models.json", "\"rot\": [90, 0, 0]", "\"rot\": [90, 0]", "models.json", "rot");
    }

    @Test
    void modelGearForUnknownItem() throws IOException {
        expectError("models.json", "\"dagger\": {", "\"poignard_fantome\": {", "models.json", "poignard_fantome", "items.json");
    }

    @Test
    void modelArmorColorForUnknownItem() throws IOException {
        expectError("models.json", "\"leather_armor\": \"#6B4A2B\"", "\"armure_fantome\": \"#6B4A2B\"", "models.json", "armure_fantome", "items.json");
    }

    @Test
    void modelVariantUnknownBase() throws IOException {
        expectError("models.json", "\"base\": \"human\"", "\"base\": \"elfe\"", "models.json", "base", "elfe");
    }

    @Test
    void modelVariantOfAVariant() throws IOException {
        expectError("models.json", "\"base\": \"quadruped\"", "\"base\": \"blacksmith\"", "models.json", "variante");
    }

    @Test
    void modelVariantBadRoleColor() throws IOException {
        expectError("models.json", "\"skin\": \"#C98F6B\"", "\"skin\": \"#GGGGGG\"", "models.json", "blacksmith", "couleur");
    }

    @Test
    void modelVariantUnknownGear() throws IOException {
        expectError("models.json", "\"gear\": [\"prop_hammer\"]", "\"gear\": [\"prop_marteau\"]", "models.json", "blacksmith", "prop_marteau");
    }

    @Test
    void modelVariantBadScale() throws IOException {
        expectError("models.json", "\"scale\": [1.18, 0.8, 1.18]", "\"scale\": [1.18, 0]", "models.json", "blacksmith", "scale");
    }

    @Test
    void monsterUnknownModel() throws IOException {
        expectError("monsters.json", "\"model\": \"wolf\"", "\"model\": \"dragon\"", "monsters.json", "wolf", "dragon", "models.json");
    }

    @Test
    void shopBadPriceFactor() throws IOException {
        expectError("shops.json", "\"priceFactor\": 90", "\"priceFactor\": 0", "shops.json", "portbrume_store", "priceFactor");
    }
}
