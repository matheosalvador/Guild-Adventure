package com.lucas.guild.libgdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.lucas.guild.data.GameContent;

/**
 * Visualiseur de modeles (outil de conception, hors du depot) : affiche un personnage avec un equipement
 * donne depuis plusieurs angles et enregistre des captures. Dans le package libgdx pour voir CharacterModel.
 */
public class ModelViewer extends ApplicationAdapter {

    // nom, armure, arme, sac
    static final String[][] LOOKS = {
        {"nu", null, null, null},
        {"cuir_dague", "leather_armor", "dagger", "pouch"},
        {"mailles_epee", "chain_mail", "short_sword", "backpack"},
        {"plaques_longue", "plate_armor", "long_sword", "travel_bag"},
        {"cuir_hache", "leather_armor", "battle_axe", "backpack"},
    };
    // nom, angle de vue (degres autour du modele), hauteur camera, distance
    static final Object[][] ANGLES = {
        {"face", 0f, 1.1f, 3.2f},
        {"trois_quarts", 330f, 1.2f, 3.4f},
        {"profil", 270f, 1.1f, 3.2f},
        {"dos", 180f, 1.2f, 3.2f},
    };

    final String outDir;
    Array<ModelInstance> instances = new Array<>();
    ModelBatch batch;
    Environment env;
    PerspectiveCamera cam;
    CharacterModel model;
    int step = 0;
    final java.util.List<Runnable> steps = new java.util.ArrayList<>();

    ModelViewer(String outDir) {
        this.outDir = outDir;
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setTitle("viewer");
        cfg.setWindowedMode(640, 640);
        cfg.setInitialVisible(false);
        cfg.useVsync(false);
        new Lwjgl3Application(new ModelViewer(args[0]), cfg);
    }

    @Override
    public void create() {
        batch = new ModelBatch();
        env = new Environment();
        env.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.55f, 0.55f, 0.55f, 1f));
        env.add(new DirectionalLight().set(0.8f, 0.8f, 0.75f, -0.5f, -0.9f, -0.6f));
        cam = new PerspectiveCamera(40, 640, 640);
        cam.near = 0.1f;
        cam.far = 50f;
        model = new CharacterModel(instances, GameContent.models(), "human");
        model.setVisible(true);
        try {
            java.nio.file.Files.createDirectories(java.nio.file.Paths.get(outDir));
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        for (String[] look : LOOKS) {
            for (Object[] angle : ANGLES) {
                final String name = look[0] + "_" + angle[0];
                steps.add(() -> {
                    model.setLook(look[1], look[2], look[3]);
                    model.update(0.016f, 0f, Vector3.Zero, 0f);
                    float a = (Float) angle[1];
                    float h = (Float) angle[2];
                    float d = (Float) angle[3];
                    // le modele regarde vers -Z : a 180 degres, la camera est devant (z negatif)
                    cam.position.set((float) Math.sin(Math.toRadians(a)) * d * -1f, h, (float) Math.cos(Math.toRadians(a)) * d * -1f);
                    cam.lookAt(0, 0.95f, 0);
                    cam.up.set(0, 1, 0);
                    cam.update();
                    draw();
                    capture(name);
                });
            }
        }
        String[] ids = {"blacksmith", "innkeeper", "guildmaster", "merchant", "goblin", "wolf", "giant_rat"};
        final CharacterModel[] holder = new CharacterModel[1];
        for (final String id : ids) {
            for (Object[] angle : ANGLES) {
                if ("dos".equals(angle[0])) continue;
                final String name = "pnj_" + id + "_" + angle[0];
                final Object[] ang = angle;
                steps.add(() -> {
                    if (holder[0] != null) { holder[0].dispose(); }
                    holder[0] = new CharacterModel(instances, GameContent.models(), id);
                    holder[0].setVisible(true);
                    model.setVisible(false);
                    holder[0].update(0.016f, 0f, Vector3.Zero, 0f);
                    float a = (Float) ang[1];
                    float d = id.equals("wolf") || id.equals("giant_rat") ? 3.6f : 3.4f;
                    float h = id.equals("wolf") || id.equals("giant_rat") ? 0.9f : 1.15f;
                    cam.position.set((float) Math.sin(Math.toRadians(a)) * d * -1f, h, (float) Math.cos(Math.toRadians(a)) * d * -1f);
                    cam.lookAt(0, 0.8f, 0);
                    cam.up.set(0, 1, 0);
                    cam.update();
                    draw();
                    capture(name);
                });
            }
        }
        steps.add(() -> { if (holder[0] != null) { holder[0].dispose(); } model.setVisible(true); });
        // marche et coup, de profil
        for (int i = 0; i < 6; i++) {
            final int k = i;
            steps.add(() -> {
                model.setLook("chain_mail", "short_sword", "backpack");
                model.update(0.07f, 4.5f, Vector3.Zero, 0f);
                cam.position.set(3.4f, 1.1f, 0f);
                cam.lookAt(0, 0.95f, 0);
                cam.up.set(0, 1, 0);
                cam.update();
                draw();
                capture("marche_" + k);
            });
        }
        steps.add(() -> model.startAttack());
        for (int i = 0; i < 5; i++) {
            final int k = i;
            steps.add(() -> {
                model.update(0.07f, 0f, Vector3.Zero, 0f);
                cam.position.set(3.4f, 1.2f, 0f);
                cam.lookAt(0, 0.95f, 0);
                cam.up.set(0, 1, 0);
                cam.update();
                draw();
                capture("coup_" + k);
            });
        }
    }

    void draw() {
        Gdx.gl.glClearColor(0.62f, 0.78f, 0.9f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        batch.begin(cam);
        batch.render(instances, env);
        batch.end();
    }

    @Override
    public void render() {
        if (step >= steps.size()) {
            System.out.println("[viewer] FIN");
            Gdx.app.exit();
            return;
        }
        steps.get(step++).run();
    }

    void capture(String name) {
        Pixmap pm = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        PixmapIO.PNG writer = new PixmapIO.PNG((int) (pm.getWidth() * pm.getHeight() * 1.5f));
        try {
            writer.setFlipY(true);
            writer.write(Gdx.files.absolute(outDir + "/" + name + ".png"), pm);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        } finally {
            writer.dispose();
            pm.dispose();
        }
    }
}
