package com.lucas.guild.libgdx;

import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

/**
 * Ce qui est propre aux écrans en plein air (ville, zones sauvages) : le soleil qui porte des ombres (une carte
 * d'ombre de 70 m autour du joueur) et le ciel ({@link SkyDome} : astre, étoiles, nuages), dessiné sans ombres.
 * Les intérieurs n'en ont pas : le plafond mettrait toute la pièce dans le noir.
 */
final class SkyAndShadows implements Disposable {

    private final DirectionalShadowLight shadowLight;
    private final ModelBatch shadowBatch;
    private final Array<ModelInstance> skyInstances = new Array<>();
    private final SkyDome skyDome;
    private float clock;

    SkyAndShadows() {
        shadowLight = new DirectionalShadowLight(2048, 2048, 70f, 70f, 1f, 140f);
        shadowLight.set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f);
        shadowBatch = new ModelBatch(new DepthShaderProvider());
        skyDome = new SkyDome(skyInstances);
    }

    /** Le soleil (puis la lune) : la lumière directionnelle à mettre dans l'environnement de l'écran. */
    DirectionalLight sun() {
        return shadowLight;
    }

    /**
     * Calcule les ombres de l'image (si le réglage « Ombres » est allumé) et fait avancer l'horloge du ciel.
     * À appeler avant de dessiner le décor.
     */
    void renderShadows(Environment environment, Array<ModelInstance> instances, Vector3 center, Vector3 viewDirection, float delta) {
        clock += delta;
        boolean shadows = com.lucas.guild.game.Settings.current().shadows;
        environment.shadowMap = shadows ? shadowLight : null;
        if (shadows) {
            shadowLight.begin(center, viewDirection);
            shadowBatch.begin(shadowLight.getCamera());
            shadowBatch.render(instances);
            shadowBatch.end();
            shadowLight.end();
        }
    }

    /** Dessine le ciel : à appeler entre {@code begin} et {@code end} du lot de l'écran, après le décor. */
    void renderSky(ModelBatch batch, Environment environment) {
        batch.render(skyInstances, environment);
    }

    /**
     * Place l'astre, les étoiles et les nuages.
     *
     * @param daylight 0 = nuit noire, 1 = plein jour
     * @param light    direction dans laquelle voyage la lumière
     */
    void updateSky(Vector3 camera, float daylight, float[] light) {
        skyDome.update(camera, daylight, light, clock);
    }

    @Override
    public void dispose() {
        skyDome.dispose();
        skyInstances.clear();
        shadowBatch.dispose();
        shadowLight.dispose();
    }
}
