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
 * Le ciel des écrans en plein air : le soleil le jour et la lune la nuit (au bout de la lumière directionnelle),
 * des étoiles la nuit et des nuages qui dérivent. Ces objets suivent la caméra, très loin, et ne sont pas
 * éclairés (ils brillent d'eux-mêmes).
 */
class SkyDome implements Disposable {

    private static final float CELESTIAL_DISTANCE = 230f;
    private static final float STAR_DISTANCE = 260f;
    private static final float CLOUD_HEIGHT = 120f;

    private final Array<ModelInstance> sink;
    private final List<Model> models = new ArrayList<>();
    private final ModelInstance celestial;
    private final ColorAttribute celestialEmissive;
    private final List<ModelInstance> stars = new ArrayList<>();
    private final List<float[]> starDirections = new ArrayList<>();
    private final List<ModelInstance> clouds = new ArrayList<>();
    private final List<float[]> cloudSpots = new ArrayList<>();   // x, z (en m, autour de la caméra), taille
    private final Vector3 direction = new Vector3();
    private boolean starsVisible = false;

    SkyDome(Array<ModelInstance> sink) {
        this.sink = sink;
        Random random = new Random(7);

        Model sphere = Shapes.create("sphere", 1f, 1f, 1f, emissiveMaterial(Color.WHITE));
        models.add(sphere);
        celestial = new ModelInstance(sphere);
        celestialEmissive = (ColorAttribute) celestial.materials.first().get(ColorAttribute.Emissive);
        celestial.transform.scale(22f, 22f, 22f);

        Model starModel = Shapes.create("box", 1f, 1f, 1f, emissiveMaterial(Color.WHITE));
        models.add(starModel);
        for (int i = 0; i < 140; i++) {
            ModelInstance star = new ModelInstance(starModel);
            float a = random.nextFloat() * 360f;
            float e = 8f + random.nextFloat() * 80f;   // elevation en degres : pas sous l'horizon
            float cx = (float) (Math.cos(Math.toRadians(e)) * Math.cos(Math.toRadians(a)));
            float cy = (float) Math.sin(Math.toRadians(e));
            float cz = (float) (Math.cos(Math.toRadians(e)) * Math.sin(Math.toRadians(a)));
            starDirections.add(new float[] {cx, cy, cz});
            float size = 0.9f + random.nextFloat() * 1.3f;
            star.transform.scale(size, size, size);
            stars.add(star);
        }

        Model cloudModel = Shapes.create("sphere", 1f, 1f, 1f, emissiveMaterial(Color.WHITE));
        models.add(cloudModel);
        for (int i = 0; i < 14; i++) {
            ModelInstance cloud = new ModelInstance(cloudModel);
            clouds.add(cloud);
            cloudSpots.add(new float[] {(random.nextFloat() - 0.5f) * 520f, (random.nextFloat() - 0.5f) * 520f, 30f + random.nextFloat() * 36f});
        }
        sink.add(celestial);
        for (ModelInstance cloud : clouds) {
            sink.add(cloud);
        }
    }

    private static Material emissiveMaterial(Color color) {
        return new Material(ColorAttribute.createDiffuse(Color.BLACK), ColorAttribute.createEmissive(color));
    }

    /**
     * @param camera    position de la caméra : le ciel la suit
     * @param daylight  0 = nuit noire, 1 = plein jour
     * @param light     direction dans laquelle voyage la lumière (soleil le jour, lune la nuit)
     * @param time      temps écoulé en secondes (fait dériver les nuages)
     */
    void update(Vector3 camera, float daylight, float[] light, float time) {
        // l'astre est à l'opposé de la direction de sa lumière
        direction.set(-light[0], -light[1], -light[2]).nor();
        boolean day = daylight > 0.35f;
        celestialEmissive.color.set(day ? new Color(1f, 0.95f, 0.7f, 1f) : new Color(0.85f, 0.88f, 1f, 1f));
        float size = day ? 24f : 16f;
        celestial.transform.idt().translate(camera.x + direction.x * CELESTIAL_DISTANCE,
                camera.y + Math.max(direction.y, 0.05f) * CELESTIAL_DISTANCE, camera.z + direction.z * CELESTIAL_DISTANCE)
                .scale(size, size, size);

        boolean wantStars = daylight < 0.3f;
        if (wantStars != starsVisible) {
            starsVisible = wantStars;
            for (ModelInstance star : stars) {
                if (wantStars) {
                    sink.add(star);
                } else {
                    sink.removeValue(star, true);
                }
            }
        }
        if (starsVisible) {
            for (int i = 0; i < stars.size(); i++) {
                float[] d = starDirections.get(i);
                float s = stars.get(i).transform.getScaleX();
                stars.get(i).transform.idt().translate(camera.x + d[0] * STAR_DISTANCE, camera.y + d[1] * STAR_DISTANCE,
                        camera.z + d[2] * STAR_DISTANCE).scale(s, s, s);
            }
        }

        // les nuages : blancs le jour, gris sombre la nuit, qui dérivent lentement et reviennent de l'autre côté
        float shade = 0.25f + 0.75f * Math.min(1f, daylight * 1.4f);
        for (int i = 0; i < clouds.size(); i++) {
            float[] spot = cloudSpots.get(i);
            float x = ((spot[0] + time * 3f + 260f) % 520f + 520f) % 520f - 260f;
            ModelInstance cloud = clouds.get(i);
            ((ColorAttribute) cloud.materials.first().get(ColorAttribute.Emissive)).color.set(shade, shade, Math.min(1f, shade + 0.04f), 1f);
            cloud.transform.idt().translate(camera.x + x, CLOUD_HEIGHT, camera.z + spot[1]).scale(spot[2], 7f, spot[2] * 0.55f);
        }
    }

    @Override
    public void dispose() {
        sink.removeValue(celestial, true);
        for (ModelInstance star : stars) {
            sink.removeValue(star, true);
        }
        for (ModelInstance cloud : clouds) {
            sink.removeValue(cloud, true);
        }
        for (Model model : models) {
            model.dispose();
        }
        models.clear();
    }
}
