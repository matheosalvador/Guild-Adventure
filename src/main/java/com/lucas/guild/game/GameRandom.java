package com.lucas.guild.game;

import java.util.Random;

/**
 * La source de hasard du jeu (butin, embuscades, événements, éclats...). En jeu, c'est un vrai hasard. Les tests
 * peuvent lui donner une graine : la même suite de tirages revient alors à chaque fois, et deux parcours du banc
 * visuel donnent les mêmes captures.
 */
public final class GameRandom {

    private static Long seed;
    private static long created;

    private GameRandom() {
    }

    /** Un nouveau générateur : imprévisible en jeu, reproductible quand une graine est fixée. */
    public static synchronized Random create() {
        return seed == null ? new Random() : new Random(seed + 7919L * created++);
    }

    /** Pour les tests : fixe la graine ({@code null} = retour au vrai hasard) et repart du premier générateur. */
    public static synchronized void setSeedForTests(Long newSeed) {
        seed = newSeed;
        created = 0;
    }
}
