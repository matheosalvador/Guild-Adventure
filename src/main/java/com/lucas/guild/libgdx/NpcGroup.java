package com.lucas.guild.libgdx;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.lucas.guild.data.GameContent;

/**
 * Les personnages non joueurs d'un écran : des modèles de models.json posés debout, qui respirent et se tournent
 * vers le joueur quand il approche. Le corps solide qu'on heurte et qu'on vise est posé par l'écran.
 */
final class NpcGroup implements Disposable {

    private static final float NOTICE_RANGE = 9f;
    private static final float TURN_SPEED = 160f;

    private static final class Npc {
        final CharacterModel model;
        final float x;
        final float z;
        float yaw;

        Npc(CharacterModel model, float x, float z) {
            this.model = model;
            this.x = x;
            this.z = z;
        }
    }

    private final Array<ModelInstance> instances;
    private final List<Npc> npcs = new ArrayList<>();
    private final Vector3 feet = new Vector3();

    /** @param instances les instances à dessiner de l'écran : les modèles s'y ajoutent */
    NpcGroup(Array<ModelInstance> instances) {
        this.instances = instances;
    }

    /** Pose un PNJ sur le sol en (x, z), de face (il regarde vers -Z, comme le joueur qui arrive par la porte). */
    void add(String modelId, float x, float z) {
        CharacterModel model = new CharacterModel(instances, GameContent.models(), modelId);
        model.setVisible(true);
        npcs.add(new Npc(model, x, z));
    }

    /** Les PNJ respirent, et se tournent vers le joueur quand il est proche. */
    void update(float delta, Vector3 player) {
        for (Npc npc : npcs) {
            float dx = player.x - npc.x;
            float dz = player.z - npc.z;
            float target = 0f;
            if (Math.hypot(dx, dz) < NOTICE_RANGE) {
                target = (float) Math.toDegrees(Math.atan2(-dx, -dz));
            }
            float diff = ((target - npc.yaw + 540f) % 360f) - 180f;
            float step = TURN_SPEED * delta;
            npc.yaw += Math.max(-step, Math.min(step, diff));
            feet.set(npc.x, 0.5f, npc.z);
            npc.model.update(delta, 0f, feet, npc.yaw);
        }
    }

    @Override
    public void dispose() {
        for (Npc npc : npcs) {
            npc.model.dispose();
        }
        npcs.clear();
    }
}
