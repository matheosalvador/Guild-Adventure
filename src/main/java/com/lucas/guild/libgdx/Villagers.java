package com.lucas.guild.libgdx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.NpcsData;
import com.lucas.guild.data.OpenWorldData;
import com.lucas.guild.data.TownData;
import com.lucas.guild.game.NpcTalk;
import com.lucas.guild.game.RouteWalker;
import com.lucas.guild.game.Story;
import com.lucas.guild.game.WorldLayout;

/**
 * Ceux qui se promènent et à qui l'on parle : les habitants d'une ville ou d'un site (« villagers »), posés à
 * {@code (ox, oz)} dans le monde, et les voyageurs qui font la route d'un lieu à l'autre. Ceux qui parlent s'arrêtent
 * et se tournent vers le joueur qui s'approche.
 */
final class Villagers implements Disposable {

    /** Un habitant qui se promène : son modèle, son parcours, et ce qu'il dit (npcs.json) s'il parle. */
    static final class Villager {
        final CharacterModel model;
        final RouteWalker walker;
        /** Ses répliques, ou null s'il ne fait que passer. */
        final NpcsData.VillagerTalkData talk;
        float yaw;
        boolean stopped;
        /** Combien de fois on lui a parlé pendant cette visite : il change de réplique à chaque fois. */
        int asked;

        Villager(CharacterModel model, RouteWalker walker, NpcsData.VillagerTalkData talk) {
            this.model = model;
            this.walker = walker;
            this.talk = talk;
            this.yaw = walker.getYaw();
        }
    }

    /** Un habitant qui parle s'arrête et se tourne vers le joueur à moins de cette distance. */
    private static final float STOP_RANGE = 3f;
    private static final float TURN_SPEED = 240f;

    private final List<Villager> list = new ArrayList<>();
    private final float ox;
    private final float oz;
    private final Vector3 feet = new Vector3();
    private final BoundingBox box = new BoundingBox();
    private final Vector3 hit = new Vector3();

    Villagers(Array<ModelInstance> sink, TownData town, float ox, float oz) {
        this(sink, town.name, town.villagers, ox, oz);
    }

    /** Les habitants {@code villagers} d'un lieu nommé {@code seed} (qui fixe où chacun en est de son parcours). */
    Villagers(Array<ModelInstance> sink, String seed, List<TownData.VillagerData> villagers, float ox, float oz) {
        this.ox = ox;
        this.oz = oz;
        Random random = new Random(seed.hashCode());
        for (TownData.VillagerData data : villagers) {
            add(sink, data, random);
        }
    }

    /** Personne pour l'instant : on les ajoute un par un avec {@link #add(Array, TownData.VillagerData, Random)}. */
    Villagers(float ox, float oz) {
        this.ox = ox;
        this.oz = oz;
    }

    private Villagers() {
        this(0f, 0f);
    }

    /** Ajoute un habitant ; {@code random} (le même pour tout le lieu) fixe où il en est de son parcours. */
    void add(Array<ModelInstance> sink, TownData.VillagerData data, Random random) {
        List<float[]> route = new ArrayList<>();
        for (TownData.RoutePoint point : data.route) {
            route.add(new float[] {point.x, point.z});
        }
        RouteWalker walker = new RouteWalker(route, data.speed);
        walker.advanceMeters(random.nextFloat() * 60f);   // ils ne partent pas tous du même point
        add(sink, data.model, data.talk, walker);
    }

    private void add(Array<ModelInstance> sink, String modelId, String talkId, RouteWalker walker) {
        CharacterModel model = new CharacterModel(sink, GameContent.models(), modelId);
        model.setVisible(true);
        NpcsData.VillagerTalkData talk = talkId != null ? GameContent.npcs().villager(talkId) : null;
        list.add(new Villager(model, walker, talk));
    }

    /**
     * Les voyageurs d'openworld.json : chacun fait l'aller-retour sur la route entre ses deux lieux. Où il en est
     * dépend seulement de l'heure de jeu ({@code gameMinutes}) : on le retrouve au même endroit après un chargement.
     */
    static Villagers travelers(Array<ModelInstance> sink, List<OpenWorldData.TravelerData> travelers, WorldLayout layout, long gameMinutes) {
        Villagers v = new Villagers();
        for (OpenWorldData.TravelerData t : travelers) {
            List<OpenWorldData.PointData> road = layout.road(t.from, t.to).points;
            List<float[]> route = new ArrayList<>();
            for (OpenWorldData.PointData p : road) {
                route.add(new float[] {p.x, p.z});
            }
            for (int i = road.size() - 2; i >= 1; i--) {
                route.add(new float[] {road.get(i).x, road.get(i).z});   // le retour, par la même route
            }
            float loop = 0f;
            for (int i = 0; i < route.size(); i++) {
                float[] a = route.get(i);
                float[] b = route.get((i + 1) % route.size());
                loop += (float) Math.hypot(b[0] - a[0], b[1] - a[1]);
            }
            RouteWalker walker = new RouteWalker(route, t.speed);
            // une seconde réelle = une minute de jeu : il a marché « speed » mètres par minute de jeu
            walker.advanceMeters((float) ((gameMinutes * (double) t.speed) % loop));
            v.add(sink, t.model, t.talk, walker);
        }
        return v;
    }

    private float x(Villager v) {
        return ox + v.walker.getX();
    }

    private float z(Villager v) {
        return oz + v.walker.getZ();
    }

    /** Ce qu'un habitant peut dire en ce moment, ici (vide s'il ne fait que passer). */
    static List<String> linesOf(Villager v, String townId, Story story) {
        return v.talk == null ? Collections.<String>emptyList() : NpcTalk.lines(v.talk, townId, story);
    }

    /**
     * À chaque image : ils marchent (sauf si un menu est ouvert), ceux qui parlent attendent le joueur et le regardent,
     * comme celui à qui l'on parle.
     */
    void update(float delta, Vector3 player, boolean frozen, Villager talkingTo) {
        for (Villager v : list) {
            float dx = player.x - x(v);
            float dz = player.z - z(v);
            v.stopped = v.talk != null && Math.hypot(dx, dz) < STOP_RANGE;
            if (!frozen && !v.stopped) {
                v.walker.update(delta);
            }
            float target = v.stopped || v == talkingTo ? (float) Math.toDegrees(Math.atan2(-dx, -dz)) : v.walker.getYaw();
            float diff = ((target - v.yaw + 540f) % 360f) - 180f;
            float step = TURN_SPEED * delta;
            v.yaw += Math.max(-step, Math.min(step, diff));
            boolean walking = !frozen && !v.stopped;
            v.model.update(frozen ? 0f : delta, walking ? v.walker.getSpeed() : 0f, feet.set(x(v), 0.5f, z(v)), v.yaw);
        }
    }

    /** L'habitant qui a quelque chose à dire visé par le rayon, le plus proche, ou null. */
    Villager aimed(Vector3 eye, Vector3 direction, float maxDistance, String townId, Story story) {
        Ray ray = new Ray(eye, direction);
        Villager best = null;
        float bestDistance = maxDistance;
        for (Villager v : list) {
            if (linesOf(v, townId, story).isEmpty()) {
                continue;
            }
            float x = x(v);
            float z = z(v);
            box.set(new Vector3(x - 0.45f, 0.5f, z - 0.45f), new Vector3(x + 0.45f, 2.5f, z + 0.45f));
            if (Intersector.intersectRayBounds(ray, box, hit)) {
                float distance = hit.dst(eye);
                if (distance <= bestDistance) {
                    bestDistance = distance;
                    best = v;
                }
            }
        }
        return best;
    }

    @Override
    public void dispose() {
        for (Villager v : list) {
            v.model.dispose();
        }
        list.clear();
    }
}
