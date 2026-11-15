package com.lucas.guild.libgdx;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

/**
 * De petits éclats qui giclent quand on frappe un monstre ou qu'il meurt : des cubes qui retombent et rétrécissent
 * en une demi-seconde. Purement visuel.
 */
class Sparks implements Disposable {

    private static final class Spark {
        final ModelInstance instance;
        final Vector3 position = new Vector3();
        final Vector3 velocity = new Vector3();
        float life;
        float maxLife;
        float size;

        Spark(ModelInstance instance) {
            this.instance = instance;
        }
    }

    private static final float GRAVITY = 9f;
    private static final float FLOOR = 0.55f;   // le dessus du sol

    private final Array<ModelInstance> sink;
    private final Model cube;
    private final List<Spark> active = new ArrayList<>();
    private final Random random = new Random();

    Sparks(Array<ModelInstance> sink) {
        this.sink = sink;
        this.cube = Shapes.create("box", 1f, 1f, 1f, new Material(ColorAttribute.createDiffuse(Color.WHITE)));
    }

    /** Fait gicler {@code count} éclats de la couleur donnée depuis (x, y, z). */
    void burst(float x, float y, float z, int count, Color color, float speed) {
        for (int i = 0; i < count; i++) {
            ModelInstance instance = new ModelInstance(cube);
            ((ColorAttribute) instance.materials.first().get(ColorAttribute.Diffuse)).color.set(color);
            Spark s = new Spark(instance);
            s.position.set(x, y, z);
            s.velocity.set((random.nextFloat() - 0.5f) * speed, 1.5f + random.nextFloat() * speed * 0.8f, (random.nextFloat() - 0.5f) * speed);
            s.maxLife = s.life = 0.35f + random.nextFloat() * 0.3f;
            s.size = 0.07f + random.nextFloat() * 0.07f;
            sink.add(instance);
            active.add(s);
        }
    }

    void update(float dt) {
        for (int i = active.size() - 1; i >= 0; i--) {
            Spark s = active.get(i);
            s.life -= dt;
            if (s.life <= 0f) {
                sink.removeValue(s.instance, true);
                active.remove(i);
                continue;
            }
            s.velocity.y -= GRAVITY * dt;
            s.position.mulAdd(s.velocity, dt);
            if (s.position.y < FLOOR) {
                s.position.y = FLOOR;
                s.velocity.scl(0.3f, -0.2f, 0.3f);
            }
            float k = s.size * (s.life / s.maxLife);
            s.instance.transform.idt().translate(s.position).scale(k, k, k);
        }
    }

    int count() {
        return active.size();
    }

    void clear() {
        for (Spark s : active) {
            sink.removeValue(s.instance, true);
        }
        active.clear();
    }

    @Override
    public void dispose() {
        clear();
        cube.dispose();
    }
}
