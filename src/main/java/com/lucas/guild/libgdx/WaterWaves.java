package com.lucas.guild.libgdx;

import java.util.ArrayList;
import java.util.List;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;

/** Les surfaces d'eau d'un écran (parties de décor d'animation « water ») : elles ondulent et scintillent doucement. */
final class WaterWaves {

    private static final class Wave {
        final ModelInstance instance;
        final float baseY;
        final Color base;
        final float phase;

        Wave(ModelInstance instance, float baseY, Color base) {
            this.instance = instance;
            this.baseY = baseY;
            this.base = new Color(base);
            this.phase = instance.transform.val[Matrix4.M03] * 0.15f;   // décalée selon la position : pas toutes ensemble
        }
    }

    private final List<Wave> waves = new ArrayList<>();
    private float time;

    void add(ModelInstance instance, float baseY, Color color) {
        waves.add(new Wave(instance, baseY, color));
    }

    void update(float delta) {
        if (waves.isEmpty()) {
            return;
        }
        time += delta;
        for (Wave wave : waves) {
            float s = MathUtils.sin(time * 1.1f + wave.phase);
            wave.instance.transform.val[Matrix4.M13] = wave.baseY + 0.05f * s;
            ColorAttribute diffuse = (ColorAttribute) wave.instance.materials.first().get(ColorAttribute.Diffuse);
            float k = 1f + 0.12f * s;
            diffuse.color.set(Math.min(1f, wave.base.r * k), Math.min(1f, wave.base.g * k), Math.min(1f, wave.base.b * k), 1f);
        }
    }
}
