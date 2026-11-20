package com.lucas.guild;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.WorldData;
import com.lucas.guild.game.GameSession;
import com.lucas.guild.game.World;

/**
 * Pour les tests sans affichage : marcher d'un lieu à un autre comme dans le monde ouvert, sans le monde ouvert.
 * Le temps de la marche passe (distance de la carte, vitesse du personnage) et le personnage arrive.
 */
final class Trips {

    private Trips() {
    }

    /** Marche du lieu où l'on est jusqu'à {@code placeId}. @return les minutes de jeu passées */
    static int walk(GameSession session, String placeId) {
        WorldData world = GameContent.world();
        String here = session.getPlayer().getTownId();
        int minutes = World.travelMinutes(world.distanceKm(here, placeId), world.walkingKmh, session.getPlayer().getSpeedFactor());
        session.getClock().advanceMinutes(minutes);
        session.getPlayer().setTownId(placeId);
        session.getPlayer().markVisited(placeId);
        return minutes;
    }
}
