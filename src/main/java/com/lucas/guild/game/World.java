package com.lucas.guild.game;

import java.util.ArrayList;
import java.util.List;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.WorldData;
import com.lucas.guild.model.Adventurer;

/**
 * Les déplacements dans le monde : quelles routes partent d'un lieu, combien de temps dure un
 * trajet, et le trajet lui-même. Sans dépendance à l'interface.
 *
 * <p>La durée vient de la distance (en km, d'après les positions de world.json) et de la vitesse
 * du personnage : trop chargé, trop âgé, on voyage plus lentement.</p>
 */
public final class World {

    /** Une route possible depuis le lieu courant. */
    public static final class Route {
        public final WorldData.PlaceData place;
        public final double km;
        public final int minutes;

        Route(WorldData.PlaceData place, double km, int minutes) {
            this.place = place;
            this.km = km;
            this.minutes = minutes;
        }
    }

    private World() {
    }

    /** Durée d'un trajet de {@code km}, en minutes (arrondie aux 5 minutes), pour un personnage de vitesse {@code speedFactor}. */
    public static int travelMinutes(double km, double walkingKmh, double speedFactor) {
        double hours = km / (walkingKmh * Math.max(0.2, speedFactor));
        return (int) Math.max(5, Math.round(hours * 60 / 5.0) * 5);
    }

    /** Les routes qui partent du lieu où se trouve le joueur. */
    public static List<Route> routesFrom(Adventurer player) {
        WorldData world = GameContent.world();
        String here = player.getTownId();
        List<Route> routes = new ArrayList<>();
        for (WorldData.PlaceData neighbor : world.neighbors(here)) {
            double km = world.distanceKm(here, neighbor.id);
            routes.add(new Route(neighbor, km, travelMinutes(km, world.walkingKmh, player.getSpeedFactor())));
        }
        return routes;
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
     * Voyage jusqu'à un lieu relié par une route : le temps passe, le joueur arrive et le lieu est
     * noté comme visité.
     *
     * @return la phrase à afficher à l'arrivée, ou {@code null} si aucune route ne mène à ce lieu
     */
    public static String travel(GameSession session, String placeId) {
        Adventurer player = session.getPlayer();
        for (Route route : routesFrom(player)) {
            if (route.place.id.equals(placeId)) {
                session.getClock().advanceMinutes(route.minutes);
                player.setTownId(placeId);
                player.markVisited(placeId);
                return "Vous arrivez à " + route.place.name + " après " + formatDuration(route.minutes)
                        + " de marche. Il est " + session.getClock().formatTime() + ".";
            }
        }
        return null;
    }
}
