package com.lucas.guild.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
     * Une embuscade attend-elle le joueur sur cette route ? Seulement entre deux villes (les zones
     * sauvages sont déjà dangereuses), si le monde en prévoit, selon la difficulté de la partie.
     */
    public static boolean shouldAmbush(GameSession session, String placeId, Random random) {
        WorldData world = GameContent.world();
        WorldData.PlaceData from = world.place(session.getPlayer().getTownId());
        WorldData.PlaceData to = world.place(placeId);
        if (world.ambush == null || from == null || to == null || from.zoneFile != null || to.zoneFile != null) {
            return false;
        }
        if (DebugMode.isForceAmbush()) {
            return true;
        }
        return random.nextInt(100) < GameContent.ambushPercent(session.getDifficulty());
    }

    /**
     * Début d'un voyage coupé par une embuscade : la moitié du trajet passe, le reste attend que le
     * joueur ait vaincu ses agresseurs ({@link #finishTravel}).
     *
     * @return la phrase à afficher, ou {@code null} si aucune route ne mène à ce lieu
     */
    public static String startAmbushedTravel(GameSession session, String placeId) {
        for (Route route : routesFrom(session.getPlayer())) {
            if (route.place.id.equals(placeId)) {
                int before = (route.minutes / 2) / 5 * 5;
                session.getClock().advanceMinutes(before);
                session.setPendingTravel(placeId, route.minutes - before);
                return "Embuscade ! On vous attaque sur la route.";
            }
        }
        return null;
    }

    /** La fin du trajet interrompu : le temps restant passe et le joueur arrive. @return la phrase d'arrivée, ou null s'il n'y a pas de voyage en cours */
    public static String finishTravel(GameSession session) {
        String placeId = session.getPendingDestination();
        if (placeId == null) {
            return null;
        }
        int minutes = session.getPendingMinutes();
        session.clearPendingTravel();
        Adventurer player = session.getPlayer();
        session.getClock().advanceMinutes(minutes);
        player.setTownId(placeId);
        player.markVisited(placeId);
        return "Vous arrivez à " + GameContent.world().place(placeId).name + ". Il est " + session.getClock().formatTime() + ".";
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
