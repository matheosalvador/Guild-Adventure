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

    /**
     * Un générateur qui dépend seulement d'un endroit (un carreau du monde ouvert) : le même endroit donne toujours
     * le même tirage, en jeu comme en test (le terrain fabriqué ne change pas d'une visite à l'autre).
     */
    public static Random forPlace(long a, long b) {
        return new Random(0x5DEECE66DL ^ (a * 73856093L) ^ (b * 19349663L));
    }

    /** Pour les tests : fixe la graine ({@code null} = retour au vrai hasard) et repart du premier générateur. */
    public static synchronized void setSeedForTests(Long newSeed) {
        seed = newSeed;
        created = 0;
    }
}
