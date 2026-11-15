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
}
