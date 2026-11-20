package com.lucas.guild.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.WorldData;
import com.lucas.guild.model.Adventurer;

/**
 * Les déplacements dans le monde ouvert, sans interface : le voyage rapide vers les lieux déjà atteints, la durée
 * d'un trajet, et les embuscades sur la route.
 *
 * <p>La durée vient de la distance (en km, d'après les positions de world.json) et de la vitesse
 * du personnage : trop chargé, trop âgé, on voyage plus lentement.</p>
 */
public final class World {

    /** Un voyage rapide possible. */
    public static final class Route {
        public final WorldData.PlaceData place;
        public final double km;
        public final int minutes;
        /** Pourquoi la route est fermée à ce personnage (histoire), ou null si elle est ouverte. */
        public final String lockedReason;

        Route(WorldData.PlaceData place, double km, int minutes, String lockedReason) {
            this.place = place;
            this.km = km;
            this.minutes = minutes;
            this.lockedReason = lockedReason;
        }
    }

    private World() {
    }

    /** Durée d'un trajet de {@code km}, en minutes (arrondie aux 5 minutes), pour un personnage de vitesse {@code speedFactor}. */
    public static int travelMinutes(double km, double walkingKmh, double speedFactor) {
        double hours = km / (walkingKmh * Math.max(0.2, speedFactor));
        return (int) Math.max(5, Math.round(hours * 60 / 5.0) * 5);
    }

    /**
     * Monde ouvert : le voyage rapide, vers les lieux déjà atteints (sauf celui où l'on est), depuis l'endroit où se
     * trouve le joueur. La durée est celle de la marche en ligne droite, comme sur la carte.
     */
    public static List<Route> fastTravelRoutes(GameSession session, WorldLayout layout) {
        WorldData world = GameContent.world();
        Adventurer player = session.getPlayer();
        float[] km = layout.toMap(session.getWorldX(), session.getWorldZ());
        String here = layout.placeAt(session.getWorldX(), session.getWorldZ());
        Story story = new Story(player, null);
        List<Route> routes = new ArrayList<>();
        for (WorldData.PlaceData place : world.places) {
            if (place.hidden || place.id.equals(here) || !player.getVisitedPlaces().contains(place.id)) {
                continue;
            }
            double distance = Math.hypot(place.x - km[0], place.z - km[1]);
            routes.add(new Route(place, distance, travelMinutes(distance, world.walkingKmh, player.getSpeedFactor()), story.lockReason(place.id)));
        }
        return routes;
    }

    /** Le voyage rapide vers {@code placeId} : le temps passe, le joueur arrive. @return la phrase d'arrivée, ou null si ce voyage n'est pas proposé */
    public static String fastTravel(GameSession session, WorldLayout layout, String placeId) {
        for (Route route : fastTravelRoutes(session, layout)) {
            if (route.place.id.equals(placeId) && route.lockedReason == null) {
                session.getClock().advanceMinutes(route.minutes);
                session.getPlayer().setTownId(placeId);
                return "Vous arrivez à " + route.place.name + " après " + formatDuration(route.minutes)
                        + " de marche. Il est " + session.getClock().formatTime() + ".";
            }
        }
        return null;
    }

    /** « 6 h 20 » ou « 45 min ». */
    public static String formatDuration(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        if (h == 0) {
            return m + " min";
        }
        return m == 0 ? h + " h" : h + " h " + String.format("%02d", m);
    }

    /**
     * Une embuscade surgit-elle sur la route ? Tiré à chaque étape de marche hors des lieux, si le monde en prévoit,
     * selon la difficulté de la partie (et les effets de l'histoire, qui peuvent rendre les routes plus sûres).
     */
    public static boolean rollRoadAmbush(GameSession session, Random random) {
        if (GameContent.world().ambush == null) {
            return false;
        }
        if (DebugMode.isForceAmbush()) {
            return true;
        }
        int percent = new Story(session).ambushPercent(GameContent.ambushPercent(session.getDifficulty()));
        return random.nextInt(100) < percent;
    }
}
