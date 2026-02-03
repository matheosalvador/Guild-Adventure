package com.lucas.guild.libgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.collision.btDbvtBroadphase;
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.lucas.guild.model.Adventurer;

public class GameScreen implements Screen {

    private final PerspectiveCamera camera;
    private final ModelBatch modelBatch;
    private final Model groundModel;
    private final ModelInstance groundInstance;
    private final Environment environment;
    private final PlayerController playerController;

    // Bullet Physics
    private final btDefaultCollisionConfiguration collisionConfig;
    private final btCollisionDispatcher dispatcher;
    private final btDbvtBroadphase broadphase;
    private final btSequentialImpulseConstraintSolver solver;
    private final btDiscreteDynamicsWorld dynamicsWorld;
    private final btCollisionShape groundShape;
    private final btCollisionObject groundObject;

    private final SpriteBatch spriteBatch;
    private final BitmapFont font;
    private final Adventurer player;

    public GameScreen(MainGame game) {
        Bullet.init();

        camera = new PerspectiveCamera(70, Gdx.graphics.getWidth(), Gdx.graphics.getHeight()); // Adjusted FOV
        camera.position.set(10f, 10f, 10f);
        camera.lookAt(0, 0, 0);
        camera.near = 0.1f; // Adjusted near plane
        camera.far = 300f;
        camera.update();

        modelBatch = new ModelBatch();

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        // Physics World Initialization
        collisionConfig = new btDefaultCollisionConfiguration();
        dispatcher = new btCollisionDispatcher(collisionConfig);
        broadphase = new btDbvtBroadphase();
        solver = new btSequentialImpulseConstraintSolver();
        dynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfig);
        dynamicsWorld.setGravity(new Vector3(0, -10f, 0));

        // Ground Creation
        ModelBuilder modelBuilder = new ModelBuilder();
        Texture cobblestoneTexture = new Texture(Gdx.files.internal("coblestone.png"));
        cobblestoneTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        Material groundMaterial = new Material(TextureAttribute.createDiffuse(cobblestoneTexture));
        final int textureRepeat = 50;
        groundModel = modelBuilder.createBox(200f, 1f, 200f, groundMaterial, Usage.Position | Usage.Normal | Usage.TextureCoordinates);
        groundInstance = new ModelInstance(groundModel);
        groundInstance.materials.get(0).get(TextureAttribute.class, TextureAttribute.Diffuse).scaleU = textureRepeat;
        groundInstance.materials.get(0).get(TextureAttribute.class, TextureAttribute.Diffuse).scaleV = textureRepeat;

        groundShape = new btBoxShape(new Vector3(100f, 0.5f, 100f));
        groundObject = new btCollisionObject();
        groundObject.setCollisionShape(groundShape);
        groundObject.setWorldTransform(groundInstance.transform);
        dynamicsWorld.addCollisionObject(groundObject);

        playerController = new PlayerController(camera, dynamicsWorld);
        Gdx.input.setInputProcessor(playerController);
        Gdx.input.setCursorCatched(true); // Capture the mouse cursor

        // Initialize UI elements
        spriteBatch = new SpriteBatch();
        font = new BitmapFont();
        font.setColor(Color.WHITE);

        player = new Adventurer("Lucas", "Caserne");
    }

    @Override
    public void render(float delta) {
        // Release cursor on ESC
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.input.setCursorCatched(!Gdx.input.isCursorCatched());
        }

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        playerController.update(delta);
        dynamicsWorld.stepSimulation(delta, 5, 1/60f);

        modelBatch.begin(camera);
        modelBatch.render(groundInstance, environment);
        playerController.render(modelBatch, environment);
        modelBatch.end();

        spriteBatch.begin();
        font.draw(spriteBatch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 10, Gdx.graphics.getHeight() - 10);
        font.draw(spriteBatch, "Player: " + player.getName(), 10, Gdx.graphics.getHeight() - 30);
        spriteBatch.end();
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }

    @Override
    public void show() {
        Gdx.input.setCursorCatched(true);
    }

    @Override
    public void hide() {
        Gdx.input.setCursorCatched(false);
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        modelBatch.dispose();
        groundModel.dispose();
        playerController.dispose();
        
        dynamicsWorld.removeCollisionObject(groundObject);
        groundObject.dispose();
        groundShape.dispose();

        dynamicsWorld.dispose();
        solver.dispose();
        broadphase.dispose();
        dispatcher.dispose();
        collisionConfig.dispose();
        spriteBatch.dispose();
        font.dispose();
    }
}
