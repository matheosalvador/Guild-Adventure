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
import com.lucas.guild.data.TownData;
import com.lucas.guild.game.NpcTalk;
import com.lucas.guild.game.RouteWalker;
import com.lucas.guild.game.Story;

/**
 * Les habitants d'une ville qui se promènent (town*.json, « villagers ») : leur parcours, leur modèle, et ceux qui
 * parlent s'arrêtent et se tournent vers le joueur qui s'approche. Sert à la ville et au monde ouvert, où la ville est
 * posée à {@code (ox, oz)}.
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
        this.ox = ox;
        this.oz = oz;
        Random random = new Random(town.name.hashCode());
        for (TownData.VillagerData data : town.villagers) {
            List<float[]> route = new ArrayList<>();
            for (TownData.RoutePoint point : data.route) {
                route.add(new float[] {point.x, point.z});
            }
            RouteWalker walker = new RouteWalker(route, data.speed);
            walker.advanceMeters(random.nextFloat() * 60f);   // ils ne partent pas tous du même point
            CharacterModel model = new CharacterModel(sink, GameContent.models(), data.model);
            model.setVisible(true);
            NpcsData.VillagerTalkData talk = data.talk != null ? GameContent.npcs().villager(data.talk) : null;
            list.add(new Villager(model, walker, talk));
        }
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
