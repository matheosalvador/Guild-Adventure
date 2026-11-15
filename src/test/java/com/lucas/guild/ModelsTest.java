package com.lucas.guild;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.ModelsData;
import com.lucas.guild.model.Item;

/** Modèles 3D (models.json) : le contenu est cohérent et couvre l'équipement du jeu. */
class ModelsTest {

    @Test
    void humanModelIsComplete() {
        ModelsData data = GameContent.models();
        ModelsData.ModelData human = data.models.get("human");
        assertNotNull(human);
        for (String joint : new String[] {"torso", "head", "armL", "armR", "legL", "legR"}) {
            assertTrue(human.joints.containsKey(joint), "articulation " + joint);
        }
        assertTrue(human.parts.size() >= 20, "un vrai corps : " + human.parts.size() + " formes");
        assertTrue(human.parts.stream().anyMatch(p -> "sphere".equals(p.shape)) && human.parts.stream().anyMatch(p -> "cylinder".equals(p.shape)),
                "pas seulement des boites");
        assertFalse(human.walk.isEmpty(), "une marche");
        assertNotNull(human.attack, "un geste de coup");
        assertTrue(human.parts.stream().anyMatch(p -> "armor".equals(p.slot)), "des formes qui prennent la couleur de l'armure");
    }

    @Test
    void everyWeaponArmorAndBagHasALook() {
        ModelsData data = GameContent.models();
        for (Item item : GameContent.items()) {
            String type = item.getClass().getSimpleName();
            if (type.equals("Arme") || type.equals("Sac")) {
                assertTrue(data.gear.containsKey(item.getId()), item.getName() + " : un aspect visible dans models.json");
            }
            if (type.equals("Armure")) {
                assertTrue(data.armorColors.containsKey(item.getId()), item.getName() + " : une couleur d'armure dans models.json");
            }
        }
    }

    @Test
    void gearIsAttachedToTheBodyOfTheHumanModel() {
        ModelsData data = GameContent.models();
        ModelsData.ModelData human = data.models.get("human");
        for (java.util.Map.Entry<String, ModelsData.GearData> e : data.gear.entrySet()) {
            for (ModelsData.PartData part : e.getValue().parts) {
                assertTrue(part.joint == null || human.joints.containsKey(part.joint), e.getKey() + " : articulation " + part.joint);
            }
        }
        // l'arme est tenue par la main droite, portee par le bras droit : elle suit le geste du coup
        assertEquals("handR", data.gear.get("long_sword").parts.get(0).joint);
        assertEquals("armR", human.joints.get("handR").parent);
    }

    @Test
    void everyNpcAndMonsterHasAModel() {
        ModelsData data = GameContent.models();
        for (String id : new String[] {"blacksmith", "innkeeper", "guildmaster", "merchant", "goblin", "wolf", "giant_rat", "quadruped"}) {
            assertNotNull(data.resolved(id), "modele " + id);
        }
        for (com.lucas.guild.data.MonsterData monster : GameContent.monsters().values()) {
            assertNotNull(monster.model, monster.id + " : un modele 3D");
            assertNotNull(data.models.get(monster.model), monster.id + " : modele " + monster.model);
        }
    }

    @Test
    void variantsInheritTheirBaseBody() {
        ModelsData data = GameContent.models();
        ModelsData.ModelData human = data.models.get("human");
        ModelsData.ModelData dwarf = data.resolved("blacksmith");
        assertEquals(human.joints.keySet(), dwarf.joints.keySet(), "meme squelette");
        assertEquals(human.parts.size(), dwarf.parts.size(), "memes formes de base");
        assertFalse(dwarf.extra.isEmpty(), "barbe, tablier...");
        assertNotNull(dwarf.scale, "un nain : plus petit et plus large");
        assertTrue(dwarf.scale[1] < 1f && dwarf.scale[0] > 1f);
        assertEquals(human.walk.keySet(), dwarf.walk.keySet(), "marche heritee");
        assertEquals("#4A4540", dwarf.roles.get("tunic"));
        assertTrue(dwarf.hide.contains("cloak"));

        ModelsData.ModelData goblin = data.resolved("goblin");
        assertTrue(goblin.pose.containsKey("torso"), "le gobelin est voute : sa pose remplace celle de la base");
        assertFalse(human.pose.containsKey("torso"));
    }

    @Test
    void quadrupedHasLegsTailAndABite() {
        ModelsData.ModelData wolf = GameContent.models().resolved("wolf");
        for (String joint : new String[] {"head", "legFL", "legFR", "legBL", "legBR", "tail"}) {
            assertTrue(wolf.joints.containsKey(joint), joint);
        }
        assertNotNull(wolf.attack, "un coup de gueule");
        assertFalse(wolf.idle.isEmpty());
    }
}
