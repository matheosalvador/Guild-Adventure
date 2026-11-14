package com.lucas.guild.game;

import java.util.ArrayList;
import java.util.List;

import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Arme;
import com.lucas.guild.model.Armure;
import com.lucas.guild.data.CalendarData;
import com.lucas.guild.data.DifficultyData;
import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.ProfileData;
import com.lucas.guild.model.GameCalendar;
import com.lucas.guild.model.GameClock;
import com.lucas.guild.model.GameData;
import com.lucas.guild.model.Inventaire;
import com.lucas.guild.model.Item;
import com.lucas.guild.model.Sac;

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
    private static final String DEFAULT_NAME = "Aventurier";

    private final Adventurer player;
    private final GameClock clock;
    private final String difficulty;

    // Position de reprise (en coordonnées de la ville), seulement si elle vient d'une sauvegarde
    private final boolean hasSavedPosition;
    private final float savedX;
    private final float savedY;
    private final float savedZ;

    /** Les objets au sol (en mémoire, pas sauvegardés). */
    private final GroundItems ground = new GroundItems();
    /** Voyage coupé par une embuscade : le lieu où l'on va et les minutes qu'il reste (pas sauvegardé). */
    private String pendingDestination;
    private int pendingMinutes;

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

    /** Nouvelle partie sans création de personnage : un aventurier sans classe ni origine. */
    public static GameSession newGame(String difficulty) {
        return newGame(difficulty, DEFAULT_NAME, null, null);
    }

    /**
     * Nouvelle partie avec le personnage créé par le joueur : un seul joueur, créé ici, pour toute la session.
     *
     * @param classId      classe choisie (classes.json), ou null
     * @param backgroundId origine choisie (backgrounds.json), ou null
     */
    public static GameSession newGame(String difficulty, String name, String classId, String backgroundId) {
        String diff = normalizeDifficulty(difficulty);
        String playerName = name != null && !name.trim().isEmpty() ? name.trim() : DEFAULT_NAME;

        Adventurer player = new Adventurer(playerName, "Ville");
        player.setTownId(GameContent.world().startPlace);
        player.markVisited(player.getTownId());
        player.chooseProfile(classId, backgroundId);
        Inventaire inventaire = new Inventaire();
        inventaire.applyDifficulty(diff);
        player.setInventory(inventaire);
        player.refreshBonuses(); // la charge portable dépend de la classe et de l'origine

        // Or et objets de départ : la difficulté (difficulties.json), puis la classe et l'origine
        DifficultyData settings = GameContent.difficulty(diff);
        ProfileData characterClass = GameContent.characterClass(classId);
        ProfileData background = GameContent.background(backgroundId);
        int gold = settings.startingGold;
        List<String> itemIds = new ArrayList<>(settings.startingItems);
        for (ProfileData profile : new ProfileData[] {characterClass, background}) {
            if (profile != null) {
                gold += profile.goldBonus;
                itemIds.addAll(profile.startingItems);
            }
        }
        player.addGold(Math.max(0, gold));
        for (String itemId : itemIds) {
            Item item = GameContent.newItem(itemId);
            if (!player.addItem(item)) {
                continue; // trop lourd pour la charge de départ : on ne le donne pas
            }
            // sac, arme et armure de départ sont portés tout de suite (si l'emplacement est libre)
            boolean wearIt = (item instanceof Sac && inventaire.getBag() == null)
                    || (item instanceof Arme && inventaire.getWeapon() == null)
                    || (item instanceof Armure && inventaire.getArmor() == null);
            if (wearIt) {
                player.equip(item);
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
        player.setNotifier(clock::postMessage);

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

        // Bonus de classe, d'origine et de compétence (charge portable...) : pas sauvegardés, on les recalcule.
        player.refreshBonuses();
        player.markVisited(player.getTownId());   // le lieu de la sauvegarde (ou celui de départ, pour une ancienne)

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
        player.setNotifier(clock::postMessage);

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

    public GroundItems getGround() {
        return ground;
    }

    public void setPendingTravel(String placeId, int minutesLeft) {
        this.pendingDestination = placeId;
        this.pendingMinutes = minutesLeft;
    }

    public String getPendingDestination() {
        return pendingDestination;
    }

    public int getPendingMinutes() {
        return pendingMinutes;
    }

    public void clearPendingTravel() {
        this.pendingDestination = null;
        this.pendingMinutes = 0;
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
