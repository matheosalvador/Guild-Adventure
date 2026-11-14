package com.lucas.guild.game;

import com.lucas.guild.model.Adventurer;

/**
 * Ce qui arrive quand le joueur tombe à 0 PV : il se réveille à l'auberge, dix pour cent plus
 * pauvre, six heures plus tard, avec la moitié de sa vie. Il ne perd aucun objet.
 */
public final class DeathPenalty {

    /** Part de l'or perdue, en pour cent. */
    public static final int GOLD_LOSS_PERCENT = 10;
    /** Heures passées inconscient. */
    public static final int HOURS_LOST = 6;

    private DeathPenalty() {
    }

    /** Applique la pénalité. @return la phrase à afficher au réveil */
    public static String apply(GameSession session) {
        Adventurer player = session.getPlayer();
        // On se réveille à l'auberge de la dernière ville visitée ; un voyage interrompu est abandonné
        session.clearPendingTravel();
        player.setTownId(player.getHomeTownId());
        int lost = player.getGold() * GOLD_LOSS_PERCENT / 100;
        player.spendGold(lost);

        session.getClock().advanceMinutes(HOURS_LOST * 60);

        player.perdreVie(player.getHealth());
        player.ajouterVie(Math.max(1, player.getMaxHealth() / 2));

        return "Vous vous réveillez à l'auberge, il est " + session.getClock().formatTime()
                + ". On a pris " + lost + " Po pour vos soins.";
    }
}
