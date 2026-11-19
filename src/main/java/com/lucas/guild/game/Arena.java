package com.lucas.guild.game;

import java.util.List;

import com.lucas.guild.data.ZoneData;

/**
 * Le déroulement d'une arène (zone_*.json, « arena ») : des vagues de monstres qui se suivent ; quand la dernière
 * est vaincue, c'est gagné. Sans dépendance à l'interface : l'écran fait apparaître les monstres qu'on lui rend.
 */
public final class Arena {

    private final ZoneData.ArenaData data;
    /** Numéro de la vague en cours (-1 : pas commencée). */
    private int wave = -1;
    private boolean won;
    private boolean failed;

    public Arena(ZoneData.ArenaData data) {
        this.data = data;
    }

    /** Vrai entre le début de la première vague et la victoire ou l'échec. */
    public boolean isRunning() {
        return wave >= 0 && !won && !failed;
    }

    public boolean isWon() {
        return won;
    }

    /** La vague en cours, comptée à partir de 1 (0 si l'arène n'a pas commencé). */
    public int waveNumber() {
        return wave + 1;
    }

    public int waveCount() {
        return data.waves.size();
    }

    /** Commence l'arène. @return les monstres de la première vague */
    public List<ZoneData.SpawnData> start() {
        wave = 0;
        won = false;
        failed = false;
        return data.waves.get(0);
    }

    /**
     * À appeler à chaque image pendant l'arène, avec le nombre de monstres encore debout.
     *
     * @return les monstres de la vague suivante s'il faut la faire apparaître, sinon null (regarder alors {@link #isWon()})
     */
    public List<ZoneData.SpawnData> update(int alive) {
        if (!isRunning() || alive > 0) {
            return null;
        }
        if (wave + 1 >= data.waves.size()) {
            won = true;
            return null;
        }
        wave++;
        return data.waves.get(wave);
    }

    /** Le joueur est tombé ou a quitté l'arène : l'épreuve est perdue. Sans effet si elle n'est pas en cours. */
    public boolean fail() {
        if (!isRunning()) {
            return false;
        }
        failed = true;
        return true;
    }

    /**
     * Inscrit le résultat de l'arène dans l'histoire : le drapeau de départ est retiré, ceux de la victoire ou de
     * l'échec sont posés.
     *
     * @return la phrase à afficher, ou null
     */
    public String record(Story story) {
        story.unset(data.startFlag);
        for (String flag : won ? data.winSets : data.failSets) {
            story.set(flag);
        }
        return won ? data.winMessage : data.failMessage;
    }
}
