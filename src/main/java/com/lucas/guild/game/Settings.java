package com.lucas.guild.game;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

/**
 * Les réglages du joueur (menu Options), enregistrés dans {@code settings.json} à côté de la sauvegarde mais séparés
 * d'elle : ils valent pour toutes les parties. Un fichier absent, illisible ou aux valeurs hors limites ne plante
 * jamais : on prend les valeurs par défaut (ou les limites).
 */
public final class Settings {

    public static final String FILE = "settings.json";
    public static final float MIN_SENSITIVITY = 0.2f;
    public static final float MAX_SENSITIVITY = 3f;
    public static final float SENSITIVITY_STEP = 0.1f;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** Multiplie la vitesse à laquelle la souris tourne la vue (1 = la vitesse d'origine). */
    public float mouseSensitivity = 1f;
    /** La vue au lancement du jeu : 3e personne, ou 1re (la touche V bascule toujours pendant la partie). */
    public boolean thirdPerson = false;
    /** Ombres portées en plein air. */
    public boolean shadows = true;
    public boolean fullscreen = false;

    private static Settings current;

    /** Les réglages en cours, lus dans {@code settings.json} à la première demande. */
    public static synchronized Settings current() {
        if (current == null) {
            current = load(path());
        }
        return current;
    }

    /** Remplace les réglages en cours (tests). */
    public static synchronized void setCurrent(Settings settings) {
        current = settings;
    }

    /** {@code settings.json} dans le dossier du jeu. */
    public static Path path() {
        return Paths.get(FILE).toAbsolutePath();
    }

    /** Lit les réglages ; un fichier absent ou illisible donne les réglages par défaut, une valeur hors limites est ramenée dans les limites. */
    public static Settings load(Path file) {
        if (!Files.isRegularFile(file)) {
            return new Settings();
        }
        try {
            String json = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
            Settings settings = GSON.fromJson(json, Settings.class);
            if (settings == null) {
                return new Settings();
            }
            settings.sensitivityWithinLimits();
            return settings;
        } catch (IOException | JsonParseException | NumberFormatException | IllegalStateException e) {
            System.err.println(FILE + " illisible, reglages par defaut (" + e.getMessage() + ")");
            return new Settings();
        }
    }

    /** Enregistre dans {@code settings.json}. @return false si l'écriture a échoué (le jeu continue avec les réglages en mémoire). */
    public boolean save() {
        return save(path());
    }

    /** Enregistre dans {@code file} (d'abord un fichier temporaire, puis remplacement : jamais de fichier à moitié écrit). */
    public boolean save(Path file) {
        try {
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.write(tmp, GSON.toJson(this).getBytes(StandardCharsets.UTF_8));
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException | RuntimeException e) {
            System.err.println(FILE + " : enregistrement impossible (" + e.getMessage() + ")");
            return false;
        }
    }

    /** Augmente ({@code steps} > 0) ou baisse la sensibilité de la souris, par pas de 0,1, sans sortir des limites. */
    public void changeSensitivity(int steps) {
        mouseSensitivity += steps * SENSITIVITY_STEP;
        sensitivityWithinLimits();
    }

    /** La sensibilité telle qu'affichée : « 1,0 ». */
    public String sensitivityLabel() {
        int tenths = Math.round(mouseSensitivity * 10f);
        return (tenths / 10) + "," + (tenths % 10);
    }

    private void sensitivityWithinLimits() {
        if (Float.isNaN(mouseSensitivity) || Float.isInfinite(mouseSensitivity)) {
            mouseSensitivity = 1f;
        }
        float bounded = Math.max(MIN_SENSITIVITY, Math.min(MAX_SENSITIVITY, mouseSensitivity));
        mouseSensitivity = Math.round(bounded * 10f) / 10f;   // pas de 0,1 : pas de 1,0000001 après plusieurs clics
    }
}
