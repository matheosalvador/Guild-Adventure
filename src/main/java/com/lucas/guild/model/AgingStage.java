package com.lucas.guild.model;

import java.util.List;

import com.lucas.guild.data.GameContent;

/**
 * Un palier de vieillesse (voir {@code assets/data/aging.json}) : à partir de {@link #fromAge} ans,
 * la vie max, la charge portée et la vitesse du personnage sont multipliées par ces pourcentages.
 */
public class AgingStage {

    public int fromAge;
    public String label;
    public int maxHealthPercent = 100;
    public int carryPercent = 100;
    public int speedPercent = 100;

    private static List<AgingStage> cache;

    /** Tous les paliers, du plus jeune au plus vieux (lus une seule fois). */
    public static synchronized List<AgingStage> all() {
        if (cache == null) {
            cache = GameContent.agingStages();
        }
        return cache;
    }

    /** Le palier qui s'applique à un âge donné. */
    public static AgingStage forAge(int age) {
        AgingStage result = null;
        for (AgingStage stage : all()) {
            if (stage.fromAge <= age) {
                result = stage;
            }
        }
        return result;
    }

    public double healthFactor() {
        return maxHealthPercent / 100.0;
    }

    public double carryFactor() {
        return carryPercent / 100.0;
    }

    public double speedFactor() {
        return speedPercent / 100.0;
    }
}
