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
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
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
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
import com.badlogic.gdx.physics.bullet.linearmath.btDefaultMotionState;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.lucas.guild.model.Adventurer;

public class GameScreen implements Screen {

    private final PerspectiveCamera camera;
    private final ModelBatch modelBatch;
    private final Environment environment;
    private final PlayerController playerController;

    // Bullet Physics
    private final btDefaultCollisionConfiguration collisionConfig;
    private final btCollisionDispatcher dispatcher;
    private final btDbvtBroadphase broadphase;
    private final btSequentialImpulseConstraintSolver solver;
    private final btDiscreteDynamicsWorld dynamicsWorld;
    
    private final Array<Disposable> disposables = new Array<>();
    private final Array<ModelInstance> instances = new Array<>();

    private final SpriteBatch spriteBatch;
    private final BitmapFont font;
    private final Adventurer player;
    private GameObject objectInView = null;
    private final GlyphLayout layout = new GlyphLayout(); // For text measurement

    public GameScreen(MainGame game) {
        Bullet.init();

        camera = new PerspectiveCamera(70, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(10f, 10f, 10f);
        camera.lookAt(0, 0, 0);
        camera.near = 0.1f;
        camera.far = 300f;
        camera.update();

        modelBatch = new ModelBatch();
        disposables.add(modelBatch);

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
        disposables.addAll(collisionConfig, dispatcher, broadphase, solver, dynamicsWorld);

        // Create world objects
        createGround();
        createBox(5, 1.5f, 5);

        playerController = new PlayerController(camera, dynamicsWorld);
        disposables.add(playerController);
        Gdx.input.setInputProcessor(playerController);
        Gdx.input.setCursorCatched(true);

        // Initialize UI elements
        spriteBatch = new SpriteBatch();
        font = new BitmapFont();
        font.setColor(Color.WHITE);
        disposables.add(spriteBatch, font);

        player = new Adventurer("Lucas", "Caserne");
    }

    private void createGround() {
        ModelBuilder modelBuilder = new ModelBuilder();
        Texture cobblestoneTexture = new Texture(Gdx.files.internal("coblestone.png"));
        disposables.add(cobblestoneTexture);
        cobblestoneTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
        Material groundMaterial = new Material(TextureAttribute.createDiffuse(cobblestoneTexture));
        final int textureRepeat = 50;
        Model groundModel = modelBuilder.createBox(200f, 1f, 200f, groundMaterial, Usage.Position | Usage.Normal | Usage.TextureCoordinates);
        disposables.add(groundModel);
        ModelInstance groundInstance = new ModelInstance(groundModel);
        instances.add(groundInstance);

        btCollisionShape groundShape = new btBoxShape(new Vector3(100f, 0.5f, 100f));
        disposables.add(groundShape);
        btRigidBody.btRigidBodyConstructionInfo groundInfo = new btRigidBody.btRigidBodyConstructionInfo(0, null, groundShape, Vector3.Zero);
        btRigidBody groundBody = new btRigidBody(groundInfo);
        disposables.add(groundBody);
        dynamicsWorld.addRigidBody(groundBody);
    }

    private void createBox(float x, float y, float z) {
        ModelBuilder modelBuilder = new ModelBuilder();
        Model boxModel = modelBuilder.createBox(1f, 1f, 1f, new Material(ColorAttribute.createDiffuse(Color.ORANGE)), Usage.Position | Usage.Normal);
        disposables.add(boxModel);
        ModelInstance boxInstance = new ModelInstance(boxModel);
        boxInstance.transform.setTranslation(x, y, z);
        instances.add(boxInstance);

        // Associate a GameObject with the instance
        boxInstance.userData = new GameObject("Box");

        btCollisionShape boxShape = new btBoxShape(new Vector3(0.5f, 0.5f, 0.5f));
        disposables.add(boxShape);
        Vector3 localInertia = new Vector3();
        boxShape.calculateLocalInertia(0, localInertia); // Mass is 0, so it's static

        btDefaultMotionState motionState = new btDefaultMotionState(boxInstance.transform);
        disposables.add(motionState);
        btRigidBody.btRigidBodyConstructionInfo boxInfo = new btRigidBody.btRigidBodyConstructionInfo(0, motionState, boxShape, localInertia);
        btRigidBody boxBody = new btRigidBody(boxInfo);
        boxBody.userData = boxInstance.userData; // Link the same GameObject to the physics body
        disposables.add(boxBody);
        dynamicsWorld.addRigidBody(boxBody);
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.input.setCursorCatched(!Gdx.input.isCursorCatched());
        }

        playerController.update(delta);
        objectInView = playerController.getObjectInView(3f);

        dynamicsWorld.stepSimulation(delta, 5, 1/60f);

        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(camera);
        modelBatch.render(instances, environment);
        modelBatch.end();

        spriteBatch.begin();
        // Draw crosshair
        font.getData().setScale(2);
        layout.setText(font, "+");
        font.draw(spriteBatch, layout, Gdx.graphics.getWidth() / 2f - layout.width / 2f, Gdx.graphics.getHeight() / 2f + layout.height / 2f);
        font.getData().setScale(1);

        // Draw HUD
        font.draw(spriteBatch, "FPS: " + Gdx.graphics.getFramesPerSecond(), 10, Gdx.graphics.getHeight() - 10);
        font.draw(spriteBatch, "Player: " + player.getName(), 10, Gdx.graphics.getHeight() - 30);
        if (objectInView != null && objectInView.isActive) {
            layout.setText(font, "Appuyer sur E pour interagir");
            font.draw(spriteBatch, layout, Gdx.graphics.getWidth() / 2f - layout.width / 2f, Gdx.graphics.getHeight() / 2f - 30);
        }
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
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void dispose() {
        for (Disposable disposable : disposables) {
            disposable.dispose();
        }
        disposables.clear();
    }
}
