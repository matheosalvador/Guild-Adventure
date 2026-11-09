package com.lucas.guild.game;

import com.lucas.guild.model.Adventurer;
import com.lucas.guild.data.CalendarData;
import com.lucas.guild.data.DifficultyData;
import com.lucas.guild.data.GameContent;
import com.lucas.guild.model.GameCalendar;
import com.lucas.guild.model.GameClock;
import com.lucas.guild.model.GameData;
import com.lucas.guild.model.Inventaire;
import com.lucas.guild.model.Item;
import com.lucas.guild.model.Sac;
import com.lucas.guild.model.Skill;
import com.lucas.guild.model.SkillTree;

/**
 * État de la partie en cours, gardé en mémoire et partagé par tous les écrans
 * (ville, forge, guilde, auberge).
 *
 * Les écrans ne lisent plus le disque : seule la sauvegarde explicite (F5 / sortie
 * d'un bâtiment) écrit {@code save.json}, et seul « Continuer » (ou F9) le relit.
 * Cette classe ne dépend pas de LibGDX pour rester testable.
 */
public class GameSession {

    private static final String DEFAULT_DIFFICULTY = "normale";

    private final Adventurer player;
    private final GameClock clock;
    private final String difficulty;

    // Position de reprise (en coordonnées de la ville), seulement si elle vient d'une sauvegarde
    private final boolean hasSavedPosition;
    private final float savedX;
    private final float savedY;
    private final float savedZ;

    private GameSession(Adventurer player, GameClock clock, String difficulty,
                        boolean hasSavedPosition, float x, float y, float z) {
        this.player = player;
        this.clock = clock;
        this.difficulty = difficulty;
        this.hasSavedPosition = hasSavedPosition;
        this.savedX = x;
        this.savedY = y;
        this.savedZ = z;
    }

    /** Nouvelle partie : un seul joueur, créé ici, pour toute la session. */
    public static GameSession newGame(String difficulty) {
        String diff = normalizeDifficulty(difficulty);

        Adventurer player = new Adventurer("Lucas", "Caserne");
        Inventaire inventaire = new Inventaire();
        inventaire.applyDifficulty(diff);
        player.setInventory(inventaire);

        // Or et objets de départ selon la difficulté (difficulties.json)
        DifficultyData settings = GameContent.difficulty(diff);
        player.addGold(settings.startingGold);
        for (String itemId : settings.startingItems) {
            Item item = GameContent.newItem(itemId);
            if (player.addItem(item) && item instanceof Sac) {
                player.equip(item); // un sac de départ est porté tout de suite
            }
        }

        // Pour le test : on apprend "Attaque Puissante" (ancien comportement de la forge).
        // A retirer quand le système de compétences sera en place.
        for (Skill skill : player.getSkillTree().getSkills()) {
            if ("Attaque Puissante".equals(skill.getName())) {
                skill.learn();
                break;
            }
        }

        // Date, heure et âge de départ (calendar.json)
        GameCalendar calendar = GameCalendar.standard();
        CalendarData.NewGameData start = calendar.newGame();
        GameClock clock = new GameClock(player);
        clock.setCurrentDay(start.day);
        clock.setCurrentHour(start.hour);
        clock.setCurrentMinute(start.minute);
        player.setBirthDay(start.day - start.startingAge * calendar.daysPerYear());
        player.refreshAging(start.day);

        return new GameSession(player, clock, diff, false, 0, 0, 0);
    }

    /** Reconstruit une session depuis une sauvegarde, en réparant les champs manquants. */
    public static GameSession fromSave(GameData data) {
        String diff = normalizeDifficulty(data.difficulty);
        Adventurer player = data.playerData;

        if (player.getInventaire() == null) {
            player.setInventory(new Inventaire());
        }
        // Anciens formats d'inventaire (une liste par potion) -> liste unique ; poids et encombrement selon la difficulté
        player.getInventaire().migrateLegacy();
        player.getInventaire().applyDifficulty(diff);

        if (player.getSkillTree() == null) {
            player.setSkillTree(new SkillTree(player.getLocation()));
        }

        GameClock clock = new GameClock(player);
        clock.setCurrentHour(Math.max(0, Math.min(23, data.currentHour)));
        clock.setCurrentMinute(data.currentMinute);
        clock.setCurrentDay(Math.max(1, data.currentDay));

        // Les sauvegardes d'avant la version 2 n'ont pas de date de naissance : on donne l'âge de départ.
        GameCalendar calendar = GameCalendar.standard();
        if (data.version < 2) {
            player.setBirthDay(clock.getCurrentDay() - calendar.newGame().startingAge * calendar.daysPerYear());
        }
        // Les sauvegardes d'avant la version 3 n'ont pas d'or : on donne celui de départ.
        if (data.version < 3 && player.getGold() == 0) {
            player.addGold(GameContent.difficulty(diff).startingGold);
        }
        // Les effets de l'âge ne sont pas sauvegardés : on les recalcule.
        player.refreshAging(clock.getCurrentDay());

        // currentScreen n'est renseigné que par les sauvegardes qui contiennent une position
        boolean hasPosition = data.currentScreen != null;
        return new GameSession(player, clock, diff, hasPosition, data.playerX, data.playerY, data.playerZ);
    }

    private static String normalizeDifficulty(String difficulty) {
        if ("facile".equals(difficulty) || "normale".equals(difficulty) || "realiste".equals(difficulty)) {
            return difficulty;
        }
        return DEFAULT_DIFFICULTY;
    }

    public Adventurer getPlayer() {
        return player;
    }

    public GameClock getClock() {
        return clock;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public boolean hasSavedPosition() {
        return hasSavedPosition;
    }

    public float getSavedX() {
        return savedX;
    }

    public float getSavedY() {
        return savedY;
    }

    public float getSavedZ() {
        return savedZ;
    }
}
