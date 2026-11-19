package com.lucas.guild;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.StoryData;
import com.lucas.guild.data.ZoneData;
import com.lucas.guild.game.Arena;
import com.lucas.guild.game.GameSession;
import com.lucas.guild.game.Lore;
import com.lucas.guild.game.Monster;
import com.lucas.guild.game.QuestLog;
import com.lucas.guild.game.Shop;
import com.lucas.guild.game.Story;
import com.lucas.guild.game.World;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Item;
import com.lucas.guild.model.ObjetDeQuete;
import com.lucas.guild.model.Quest;
import com.lucas.guild.model.QuestType;

/**
 * Joue toute l'histoire, du prologue à l'épilogue, sans interface : seulement les règles ({@code game/}) et les
 * données ({@code story.json} et les autres). Deux parcours : les trois épreuves réussies puis le Sablier brisé ;
 * les trois épreuves ratées (l'histoire continue quand même) puis le Sablier gardé.
 */
class StoryPlaythroughTest {

    private GameSession session;
    private Adventurer player;
    private Story story;
    private QuestLog log;

    private void newGame() {
        session = GameSession.newGame("facile");
        player = session.getPlayer();
        story = new Story(session);
        log = new QuestLog(player);
    }

    // ------------------------------------------------------------------ gestes du joueur

    private void travel(String... places) {
        for (String place : places) {
            assertNotNull(World.travel(session, place), "voyage vers " + place + " depuis " + player.getTownId());
        }
    }

    private StoryData.DialogData dialog(String point, String id) {
        for (StoryData.DialogData d : story.dialogsAt(point)) {
            if (d.id.equals(id)) {
                return d;
            }
        }
        throw new AssertionError("dialogue \"" + id + "\" pas propose a " + point + " ; proposes : "
                + story.dialogsAt(point).stream().map(d -> d.id).collect(java.util.stream.Collectors.toList()));
    }

    private boolean offered(String point, String id) {
        return story.dialogsAt(point).stream().anyMatch(d -> d.id.equals(id));
    }

    /** Lit un dialogue sans choix. */
    private void read(String point, String id) {
        assertNull(story.apply(dialog(point, id)), id);
        story.advance();
    }

    /** Fait un choix dans un dialogue. */
    private void choose(String point, String id, String optionLabel) {
        for (StoryData.OptionData option : story.optionsOf(dialog(point, id))) {
            if (option.label.equals(optionLabel)) {
                assertNull(story.choose(option), id + " / " + optionLabel);
                story.advance();
                return;
            }
        }
        throw new AssertionError("option \"" + optionLabel + "\" pas proposee dans " + id);
    }

    private void pick(String scene, String flag) {
        for (StoryData.PickupData pickup : story.pickupsIn(scene)) {
            if (pickup.flag.equals(flag)) {
                story.pick(pickup);
                story.advance();
                return;
            }
        }
        throw new AssertionError("rien a ramasser pour \"" + flag + "\" dans " + scene);
    }

    private void kill(String monsterId) {
        story.onKill(GameContent.monster(monsterId));
        story.advance();
    }

    /** Fait une quête de bout en bout, quel que soit son type (le joueur doit avoir le rang). */
    private void quest(String title) {
        Quest quest = GameContent.quests().stream().filter(q -> q.getTitle().equals(title)).findFirst().get();
        assertNull(log.accept(quest), "accepter \"" + title + "\" au rang " + player.getRank());
        if (quest.getType() == QuestType.DELIVERY) {
            log.deliver(quest.getTarget(), GameContent.quests());
            if (!quest.isPayOnDelivery()) {
                assertNull(log.turnIn(quest), title);
            }
        } else if (quest.getType() == QuestType.COLLECT) {
            for (int i = 0; i < quest.getCollectCount(); i++) {
                assertTrue(player.addItem(GameContent.newItem(quest.getCollectItem())), "porter " + quest.getCollectItem());
            }
            assertNull(log.turnIn(quest), title);
        } else {
            assertTrue(player.addItem(new ObjetDeQuete(quest.getProofItem())));
            assertNull(log.turnIn(quest), title);
        }
        story.advance();
    }

    /** Lit jusqu'au bout les textes d'histoire en attente. @return combien, et si le dernier était un épilogue */
    private int readPendingTexts() {
        int n = 0;
        Story.Text text;
        while ((text = story.nextText()) != null) {
            story.markShown(text);
            n++;
            assertTrue(n < 20, "les textes d'histoire tournent en rond");
        }
        return n;
    }

    private ZoneData.DoorData door(String zoneId, String to) {
        return GameContent.zone(zoneId).doors.stream().filter(d -> d.to.equals(to)).findFirst().get();
    }

    private boolean has(String itemId) {
        for (Item item : player.getInventoryItems()) {
            if (itemId.equals(item.getId())) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ les étapes communes aux deux parcours

    /** Du début de la partie au début du chapitre 3 (rang C), en vérifiant chaque porte au passage. */
    private void playUntilTheTrials() {
        newGame();
        story.advance();
        assertEquals("prologue", story.currentChapter().id);
        assertEquals(1, readPendingTexts(), "l'ouverture du prologue");
        assertNotNull(story.lockReason("mine_galeries"), "la mine est fermee au depart");
        assertNotNull(story.lockReason("corvenne_cour"), "le chateau est ferme au depart");
        assertEquals(0, player.getRank());

        // ---- prologue
        int gold = player.getGold();
        quest("Nettoyage de la cave");
        quest("Livraison express");
        assertFalse(story.isDone("prologue"), "il manque la plaque");
        assertFalse(offered("guildmaster@aethelgard", "odran_plaque"), "rien a montrer avant d'avoir trouve la plaque");
        pick("CellarScreen@aethelgard", "plaque_found");
        read("guildmaster@aethelgard", "odran_plaque");
        assertTrue(story.isDone("prologue"));
        assertEquals(1, player.getRank(), "rang E");
        assertEquals(gold + 20 + 10 + 30, player.getGold(), "les deux quetes puis la recompense du prologue");
        assertEquals(2, readPendingTexts(), "la conclusion du prologue, l'ouverture du chapitre 1");
        assertEquals("chapitre1", story.currentChapter().id);

        // ---- chapitre 1 : la tour vide
        travel("ruines_gobelines");
        Lore.learn(player, Lore.textKey("stele_ruines"));
        kill("goblin_chief");
        assertTrue(story.has("registre_found"));
        travel("aethelgard");
        read("guildmaster@aethelgard", "odran_registre");
        assertTrue(story.isDone("chapitre1"));
        assertEquals(1, player.getRank(), "toujours rang E : le rang D demande 4 quetes");
        assertNull(story.currentChapter(), "le chapitre 2 attend le rang D");
        assertTrue(story.journalLines().get(0).contains("Rang D"), story.journalLines().toString());
        assertTrue(offered("guildmaster@aethelgard", "odran_mandat_attente"));
        assertFalse(offered("guildmaster@aethelgard", "odran_mandat"));
        quest("Chasse aux gobelins");
        quest("Colis pour Port-Brume");
        assertEquals(2, player.getRank(), "4 quetes et le chapitre 1 : rang D");
        assertEquals("chapitre2", story.currentChapter().id);

        // ---- chapitre 2 : la mine
        read("guildmaster@aethelgard", "odran_mandat");
        assertNull(story.lockReason("mine_galeries"), "le mandat ouvre la mine");
        travel("hautevigne");
        read("innkeeper@hautevigne", "garin_mine");
        travel("mine_galeries");
        assertFalse(story.holds(door("mine_galeries", "mine_chambre").requires), "la grille est fermee sans la cle");
        pick("ZoneScreen@mine_galeries", "mine_key");
        assertTrue(story.holds(door("mine_galeries", "mine_chambre").requires));
        kill("sergeant_holt");
        assertFalse(story.holds(GameContent.zone("mine_chambre").monsters.get(0).requires), "le gardien ne revient pas");
        travel("hautevigne", "aethelgard");
        read("guildmaster@aethelgard", "odran_lettre");
        assertTrue(story.isDone("chapitre2"));
        assertEquals(2, player.getRank(), "le rang C demande 7 quetes");
        assertFalse(offered("fosse_master", "force_depart"), "pas d'epreuve avant le chapitre 3");
        quest("Pli pour Hautevigne");
        quest("Ailes pour l'apothicaire");
        quest("Lames pour le port");
        assertEquals(3, player.getRank(), "7 quetes et le chapitre 2 : rang C");
        assertEquals("chapitre3", story.currentChapter().id);
        readPendingTexts();
    }

    /** Du troisième sceau à la reddition d'Aldric. */
    private void playTheCastle() {
        assertTrue(story.isDone("chapitre3"), "trois sceaux, intacts ou feles : le chapitre 3 est fini");
        assertNotNull(story.lockReason("corvenne_cour"), "le chateau demande aussi le rang B");
        quest("Les os gravés");
        quest("Crocs pour les amulettes");
        quest("Pli pour la Guilde");
        assertEquals(4, player.getRank(), "10 quetes et les trois sceaux : rang B");
        assertNull(story.lockReason("corvenne_cour"));
        assertEquals("chapitre4", story.currentChapter().id);

        travel("aethelgard", "ruines_gobelines", "corvenne_cour");
        assertEquals(1, story.enter("ZoneScreen@corvenne_cour", 0f, 38f).size(), "le texte de la herse");
        assertEquals(0, story.enter("ZoneScreen@corvenne_cour", 0f, 38f).size(), "une seule fois");
        assertFalse(story.holds(door("corvenne_salle", "corvenne_crypte").requires), "la crypte est fermee tant que Varn tient");
        kill("captain_varn");
        assertTrue(story.holds(door("corvenne_salle", "corvenne_crypte").requires));
        assertTrue(offered("sablier", "sablier_garde"), "Aldric barre l'acces au Sablier");
    }

    // ------------------------------------------------------------------ parcours 1

    @Test
    void wholeStoryWithIntactSealsThenBreakingTheHourglass() {
        playUntilTheTrials();

        // ---- Sagesse : le journal doit contenir les indices
        travel("foret_loups");
        assertTrue(offered("cairn", "cairn_mince"), "journal trop mince : pas d'enigme, pas d'echec");
        Lore.learn(player, Lore.rumorKey("haute_vin"));
        Lore.learn(player, Lore.rumorKey("haute_mine"));
        Lore.learn(player, Lore.textKey("panneau_hautevigne"));
        story.advance();
        assertFalse(offered("cairn", "cairn_mince"));
        choose("cairn", "cairn_enigme", "Hautevigne");
        assertEquals(4, story.optionsOf(dialog("cairn", "cairn_enigme2")).size(), "quatre reponses, \"Le temps\" une seule fois");
        choose("cairn", "cairn_enigme2", "Le temps");
        assertTrue(story.has("seal_sagesse_intact"));

        // ---- Force : les trois vagues de la Fosse
        travel("aethelgard", "portbrume", "fosse_debardeurs");
        choose("fosse_master", "force_depart", "Je descends");
        ZoneData.ArenaData arenaData = GameContent.zone("fosse_debardeurs").arena;
        assertTrue(story.has(arenaData.startFlag));
        Arena arena = new Arena(arenaData);
        assertEquals(3, arena.start().size(), "premiere vague : trois loups");
        assertNull(arena.update(2), "tant qu'il reste des monstres, rien");
        assertNotNull(arena.update(0), "deuxieme vague");
        assertNotNull(arena.update(0), "troisieme vague");
        assertNull(arena.update(0));
        assertTrue(arena.isWon());
        arena.record(story);
        story.advance();
        assertTrue(story.has("seal_force_intact"));
        assertFalse(story.has(arenaData.startFlag));

        // ---- Endurance : 18 kg de Hautevigne a Port-Brume en moins de 14 h
        travel("portbrume", "hautevigne");
        choose("innkeeper@hautevigne", "endurance_depart", "Je le prends, le compte commence");
        assertTrue(has("oath_ore"));
        travel("portbrume");
        read("innkeeper@portbrume", "endurance_remise");
        assertFalse(has("oath_ore"));
        assertTrue(story.has("seal_endurance_intact"));

        playTheCastle();
        assertEquals(15, story.boonsIn("corvenne_crypte")[0], "sceau de Force intact : +15 % de degats au chateau");
        assertEquals(20, story.boonsIn("corvenne_crypte")[1], "sceau d'Endurance intact : +20 PV au chateau");
        assertEquals(0, story.boonsIn("aethelgard")[0], "nulle part ailleurs");

        // ---- Aldric : trois phases, puis il se rend
        Monster aldric = Monster.spawn("lord_aldric", 0, 0);
        aldric.setConditions(story::holds);
        while (aldric.getHealth() * 100 >= 60 * aldric.getMaxHealth()) {
            aldric.takeDamage(20);
        }
        assertEquals(0, aldric.getPhase());
        assertEquals("ageless_soldier", aldric.pollStartedPhase().summon);
        int health = aldric.getHealth();
        for (int i = 0; i < 60 * 30; i++) {
            aldric.update(1 / 60f, 500f, 500f);
        }
        assertEquals(health, aldric.getHealth(), "sceau de Sagesse intact : le Sablier ne le soigne plus");
        while (aldric.getPhase() < 1) {
            aldric.takeDamage(20);
        }
        assertEquals(30, aldric.getDamage(), "derniere phase : il frappe plus lourd");
        while (!aldric.hasYielded()) {
            aldric.takeDamage(20);
        }
        assertTrue(aldric.getHealth() > 0, "il se rend, il ne meurt pas");
        story.onYield(aldric.data);
        story.advance();

        // ---- le choix
        int gold = player.getGold();
        int ageBefore = player.getAge(session.getClock().getCurrentDay());
        readPendingTexts();           // les textes des chapitres 3 et 4, lus en route
        choose("sablier", "sablier_choix", "Briser le Sablier");
        assertTrue(story.isDone("chapitre4"));
        assertEquals(gold + 600, player.getGold(), "la recompense de la fin");
        Story.Text outro = story.nextText();
        assertFalse(outro.ending, "d'abord la conclusion du chapitre");
        story.markShown(outro);
        Story.Text epilogue = story.nextText();
        assertTrue(epilogue.ending, "puis l'epilogue, sur son ecran");
        assertTrue(epilogue.body.contains("vignes"));
        story.markShown(epilogue);
        assertNull(story.nextText());

        // ---- le monde apres : plus sur, Hautevigne moins chere, et toujours jouable
        assertEquals("Protecteur du pays", story.title());
        assertEquals(8, story.ambushPercent(20), "embuscades : 40 % de la chance normale");
        Adventurer other = GameSession.newGame("facile").getPlayer();
        other.setTownId("hautevigne");
        player.setTownId("hautevigne");
        Item potion = GameContent.newItem("healing_potion");
        assertTrue(new Shop("hautevigne_store").buyPrice(player, potion) < new Shop("hautevigne_store").buyPrice(other, potion),
                "Hautevigne redevient abordable pour celui qui a brise le Sablier");
        assertFalse(story.guildClosed());
        assertFalse(player.isAgeFrozen());
        session.getClock().advanceMinutes(113 * 24 * 60);
        assertEquals(ageBefore + 1, player.getAge(session.getClock().getCurrentDay()), "il vieillit toujours");
        quest("Vider la mine");
        assertEquals("L'histoire est terminée. Le monde, lui, continue.", story.journalLines().get(0));
    }

    // ------------------------------------------------------------------ parcours 2

    @Test
    void failingEveryTrialDoesNotBlockTheStoryThenKeepingTheHourglass() {
        playUntilTheTrials();

        // ---- Sagesse : mauvaise reponse
        Lore.learn(player, Lore.rumorKey("haute_vin"));
        Lore.learn(player, Lore.rumorKey("haute_mine"));
        Lore.learn(player, Lore.textKey("panneau_hautevigne"));
        story.advance();
        choose("cairn", "cairn_enigme", "Port-Brume");
        choose("cairn", "cairn_enigme2", "Le temps");
        assertTrue(story.has("seal_sagesse_cracked"), "le temps, mais pas a la bonne ville : sceau fele");
        assertTrue(offered("cairn", "cairn_attente"), "on ne retente pas tout de suite");

        // ---- Force : on tombe a la deuxieme vague
        choose("fosse_master", "force_depart", "Je descends");
        Arena arena = new Arena(GameContent.zone("fosse_debardeurs").arena);
        arena.start();
        arena.update(0);
        assertTrue(arena.fail());
        arena.record(story);
        story.advance();
        assertTrue(story.has("seal_force_cracked"));

        // ---- Endurance : on arrive trop tard
        travel("hautevigne");
        choose("innkeeper@hautevigne", "endurance_depart", "Je le prends, le compte commence");
        session.getClock().advanceMinutes(6 * 60);   // six heures perdues en route
        travel("portbrume");
        assertFalse(offered("innkeeper@portbrume", "endurance_remise"));
        read("innkeeper@portbrume", "endurance_retard");
        assertTrue(story.has("seal_endurance_cracked"));

        assertTrue(story.isDone("chapitre3"), "trois sceaux feles : l'histoire continue");

        // ---- on peut retenter trois jours plus tard, et reussir
        assertTrue(offered("fosse_master", "force_attente"));
        assertFalse(offered("fosse_master", "force_retour"));
        session.getClock().advanceMinutes(3 * 24 * 60);
        assertTrue(offered("fosse_master", "force_retour"));
        choose("fosse_master", "force_retour", "Je redescends");
        Arena second = new Arena(GameContent.zone("fosse_debardeurs").arena);
        second.start();
        second.update(0);
        second.update(0);
        second.update(0);
        assertTrue(second.isWon());
        second.record(story);
        story.advance();
        assertTrue(story.has("seal_force_intact"));
        assertFalse(story.has("seal_force_cracked"));

        playTheCastle();
        assertEquals(15, story.boonsIn("corvenne_cour")[0], "le sceau de Force regagne donne son avantage");
        assertEquals(0, story.boonsIn("corvenne_cour")[1], "le sceau d'Endurance est reste fele");

        // ---- Aldric se soigne, cette fois : le sceau de Sagesse est fele
        Monster aldric = Monster.spawn("lord_aldric", 0, 0);
        aldric.setConditions(story::holds);
        while (aldric.getPhase() < 0) {
            aldric.takeDamage(20);
        }
        int health = aldric.getHealth();
        for (int i = 0; i < 60 * 13; i++) {
            aldric.update(1 / 60f, 500f, 500f);
        }
        assertEquals(health + 40, aldric.getHealth(), "40 PV repris en 12 secondes");
        while (!aldric.hasYielded()) {
            aldric.takeDamage(20);
        }
        story.onYield(aldric.data);
        story.advance();

        // ---- garder le Sablier
        int gold = player.getGold();
        int day = session.getClock().getCurrentDay();
        int age = player.getAge(day);
        choose("sablier", "sablier_choix", "Garder le Sablier");
        assertEquals(gold, player.getGold(), "pas d'or pour cette fin");
        readPendingTexts();
        assertTrue(story.guildClosed(), "la Guilde ne lui confie plus rien");
        assertNull(story.title());
        assertEquals(20, story.ambushPercent(20), "les routes restent dangereuses");
        assertTrue(player.isAgeFrozen());
        session.getClock().advanceMinutes(5 * 112 * 24 * 60);
        assertEquals(age, player.getAge(session.getClock().getCurrentDay()), "cinq ans plus tard, il a le meme age");
        // le monde reste jouable
        travel("ruines_gobelines", "aethelgard");
        quest("Breloques volées");
    }
}
