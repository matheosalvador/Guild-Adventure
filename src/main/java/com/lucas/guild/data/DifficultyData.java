package com.lucas.guild.data;

import java.util.ArrayList;
import java.util.List;

/** Une difficulté telle qu'écrite dans {@code difficulties.json}. */
public class DifficultyData {

    public String label;
    /** Poids que le personnage porte sans sac, en kg. */
    public double maxWeight;
    /** Part de la charge max (0 à 1) à partir de laquelle la vitesse baisse. */
    public double encumbranceStart = 0.5;
    /** Vitesse, en %, quand la charge est au maximum. */
    public int minSpeedPercent = 100;
    public int startingGold;
    /** Identifiants (items.json) des objets de départ. */
    public List<String> startingItems = new ArrayList<>();
}
