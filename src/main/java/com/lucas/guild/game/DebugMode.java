package com.lucas.guild.game;

/**
 * Le mode debug : un panneau d'outils de test (touche F10 en jeu). Il est éteint par défaut.
 *
 * <p>Pour l'allumer : lancer avec {@code run-debug.bat}, ou ajouter l'option {@code --debug} au
 * lancement, ou {@code -Dguild.debug=true} à la machine virtuelle, ou définir la variable
 * d'environnement {@code GUILD_DEBUG=1}.</p>
 */
public final class DebugMode {

    private static boolean enabled = "true".equalsIgnoreCase(System.getProperty("guild.debug"))
            || "1".equals(System.getenv("GUILD_DEBUG"));

    /** Multiplicateur de la vitesse de déplacement du joueur (1 = normal). */
    private static float speedMultiplier = 1f;

    /** Le joueur ne perd plus de vie (combat). */
    private static boolean invincible = false;

    /** Chaque voyage entre villes est coupé par une embuscade. */
    private static boolean forceAmbush = false;

    private DebugMode() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) {
            speedMultiplier = 1f;
            invincible = false;
            forceAmbush = false;
        }
    }

    public static boolean isForceAmbush() {
        return enabled && forceAmbush;
    }

    public static void setForceAmbush(boolean value) {
        forceAmbush = value;
    }

    /** Invincible : seulement si le mode debug est allumé. */
    public static boolean isInvincible() {
        return enabled && invincible;
    }

    public static void setInvincible(boolean value) {
        invincible = value;
    }

    /** Allume le mode debug si {@code --debug} figure parmi les arguments du programme. */
    public static void enableFromArgs(String[] args) {
        if (args == null) {
            return;
        }
        for (String arg : args) {
            if ("--debug".equals(arg)) {
                enabled = true;
            }
        }
    }

    /** Vitesse de déplacement : 1 hors debug, quoi qu'on ait réglé. */
    public static float getSpeedMultiplier() {
        return enabled ? speedMultiplier : 1f;
    }

    public static void setSpeedMultiplier(float value) {
        speedMultiplier = value;
    }
}
