package com.lucas.guild;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.game.GameSession;
import com.lucas.guild.game.GroundItems;
import com.lucas.guild.game.SaveManager;
import com.lucas.guild.model.GroundDrop;

/** Le butin laissé au sol est sauvegardé avec le personnage, et disparaît au bout de quelques jours. */
class GroundItemsSaveTest {

    @AfterEach
    void cleanUp() throws Exception {
        Files.deleteIfExists(Paths.get("save.json"));
    }

    @Test
    void aDropSurvivesSaveAndLoad() throws Exception {
        Files.deleteIfExists(Paths.get("save.json"));
        GameSession s = GameSession.newGame("normale");
        s.getClock().setCurrentDay(5);
        s.getGround().drop("OpenWorld", GameContent.newItem("wolf_pelt"), -890f, -1120.5f);
        s.getGround().drop("ZoneScreen@mine_galeries", GameContent.newItem("dagger"), 3f, 4f);
        assertTrue(new SaveManager().saveGame(s.getPlayer(), s.getClock(), "normale", 0f, 2f, 0f, "World"));

        GameSession back = GameSession.fromSave(new SaveManager().loadGame());
        List<GroundDrop> world = back.getGround().in("OpenWorld");
        assertEquals(1, world.size());
        assertEquals("wolf_pelt", world.get(0).item.getId());
        assertEquals(-1120.5f, world.get(0).z, 1e-3);
        assertEquals(5, world.get(0).day);
        assertEquals("dagger", back.getGround().in("ZoneScreen@mine_galeries").get(0).item.getId(), "le type d'objet (une arme) est garde");
    }

    @Test
    void aDropDisappearsAfterAWeek() {
        GameSession s = GameSession.newGame("normale");
        s.getClock().setCurrentDay(10);
        GroundDrop drop = s.getGround().drop("OpenWorld", GameContent.newItem("wolf_pelt"), 1f, 1f);
        s.getClock().setCurrentDay(10 + GroundItems.LIFETIME_DAYS - 1);
        assertEquals(1, s.getGround().in("OpenWorld").size(), "encore la la veille");
        assertEquals(drop, s.getGround().nearest("OpenWorld", 1.5f, 1f));
        s.getClock().setCurrentDay(10 + GroundItems.LIFETIME_DAYS);
        assertEquals(0, s.getGround().in("OpenWorld").size(), "parti au bout d'une semaine");
        assertEquals(0, s.getPlayer().getGroundItems().size(), "et retire de la sauvegarde");
    }

    @Test
    void anOldSaveHasNothingOnTheGround() {
        GameSession s = GameSession.newGame("normale");
        assertEquals(0, s.getGround().in("OpenWorld").size());
    }
}
