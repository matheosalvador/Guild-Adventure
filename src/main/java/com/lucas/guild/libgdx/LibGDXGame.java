package com.lucas.guild.libgdx;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.*;
import com.lucas.guild.model.Adventurer;

public class LibGDXGame extends ApplicationAdapter {
    // 3D
    private ModelBatch modelBatch;
    private Environment environment;
    private PerspectiveCamera camera;
    private Model groundModel;
    private ModelInstance groundInstance;
    private Model playerModel;
    private ModelInstance playerInstance;

    // 2D (HUD)
    private SpriteBatch spriteBatch;
    private BitmapFont font;

    // Game Logic
    private Adventurer player;
    private PlayerController playerController;

    // Physics
    private btCollisionConfiguration collisionConfig;
    private btDispatcher dispatcher;
    private btBroadphaseInterface broadphase;
    private btConstraintSolver solver;
    private btDynamicsWorld dynamicsWorld;
    private btRigidBody groundBody;
    private btRigidBody playerBody;

    @Override
    public void create() {
        Bullet.init(); // Initialize Bullet physics

        // --- 3D Setup ---
        modelBatch = new ModelBatch();
        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
        
        camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 1f;
        camera.far = 300f;
        
        // --- Physics World Setup ---
        collisionConfig = new btDefaultCollisionConfiguration();
        dispatcher = new btCollisionDispatcher(collisionConfig);
        broadphase = new btDbvtBroadphase();
        solver = new btSequentialImpulseConstraintSolver();
        dynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfig);
        dynamicsWorld.setGravity(new Vector3(0, -10f, 0));

        // --- Create Physics Objects ---
        ModelBuilder modelBuilder = new ModelBuilder();

        // Ground
        groundModel = modelBuilder.createBox(200f, 1f, 200f, new Material(ColorAttribute.createDiffuse(Color.FOREST)), Usage.Position | Usage.Normal);
        groundInstance = new ModelInstance(groundModel);
        btCollisionShape groundShape = new btBoxShape(new Vector3(100f, 0.5f, 100f));
        btRigidBody.btRigidBodyConstructionInfo groundInfo = new btRigidBody.btRigidBodyConstructionInfo(0, null, groundShape, Vector3.Zero);
        groundBody = new btRigidBody(groundInfo);
        dynamicsWorld.addRigidBody(groundBody);

        // Player
        float playerHeight = 1.8f;
        float playerRadius = 0.4f;
        playerModel = modelBuilder.createCapsule(playerRadius, playerHeight, 16, new Material(ColorAttribute.createDiffuse(Color.BLUE)), Usage.Position | Usage.Normal);
        playerInstance = new ModelInstance(playerModel);
        btCollisionShape playerShape = new btCapsuleShape(playerRadius, playerHeight);
        Vector3 localInertia = new Vector3();
        playerShape.calculateLocalInertia(80f, localInertia); // Mass of 80kg
        btRigidBody.btRigidBodyConstructionInfo playerInfo = new btRigidBody.btRigidBodyConstructionInfo(80f, null, playerShape, localInertia);
        playerBody = new btRigidBody(playerInfo);
        playerBody.setWorldTransform(playerInstance.transform.setTranslation(0, 10, 0));
        playerBody.setCollisionFlags(playerBody.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_CHARACTER_OBJECT);
        playerBody.setActivationState(Collision.DISABLE_DEACTIVATION);
        dynamicsWorld.addRigidBody(playerBody);

        // --- Controller & Input ---
        playerController = new PlayerController((Camera) camera, (btDiscreteDynamicsWorld) dynamicsWorld);
        Gdx.input.setInputProcessor(new InputMultiplexer(playerController));
        Gdx.input.setCursorCatched(true); // Capture the mouse cursor

        // --- 2D (HUD) Setup ---
        spriteBatch = new SpriteBatch();
        font = new BitmapFont();
        font.setColor(Color.WHITE);

        // --- Game Logic Setup ---
        player = new Adventurer("Lucas", "Caserne");
        
        Gdx.app.log("LibGDXGame", "Game with Physics created!");
    }

    @Override
    public void render() {
        // --- Logic Update ---
        playerController.update(Gdx.graphics.getDeltaTime());
        dynamicsWorld.stepSimulation(Gdx.graphics.getDeltaTime(), 5, 1f/60f);

        playerBody.getWorldTransform(playerInstance.transform);

        // --- 3D Rendering ---
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0.3f, 0.3f, 0.3f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(camera);
        modelBatch.render(groundInstance, environment);
        modelBatch.render(playerInstance, environment);
        modelBatch.end();

        // --- 2D Rendering (HUD) ---
        spriteBatch.begin();
        font.draw(spriteBatch, "HP: " + player.getHealth() + " | Gold: " + player.getGold(), 10, Gdx.graphics.getHeight() - 10);
        spriteBatch.end();
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            Gdx.input.setCursorCatched(!Gdx.input.isCursorCatched());
        }
    }

    @Override
    public void dispose() {
        dynamicsWorld.removeRigidBody(playerBody);
        dynamicsWorld.removeRigidBody(groundBody);
        playerBody.dispose();
        groundBody.dispose();
        
        modelBatch.dispose();
        groundModel.dispose();
        playerModel.dispose();
        spriteBatch.dispose();
        font.dispose();
        
        dynamicsWorld.dispose();
        solver.dispose();
        broadphase.dispose();
        dispatcher.dispose();
        collisionConfig.dispose();

        Gdx.app.log("LibGDXGame", "Game disposed!");
    }
}
