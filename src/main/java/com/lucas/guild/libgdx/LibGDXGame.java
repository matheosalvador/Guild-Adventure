package com.lucas.guild.libgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
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
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.*;
import com.badlogic.gdx.physics.bullet.linearmath.btDefaultMotionState;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Item;
import com.lucas.guild.model.PotionDeSoin;
import model.GameClock;

public class LibGDXGame extends InputAdapter implements Screen {

    private final MainGame game;
    private final PerspectiveCamera camera;
    private final ModelBatch modelBatch;
    private final Environment environment;
    private final PlayerController playerController;

    private final btDefaultCollisionConfiguration collisionConfig;
    private final btCollisionDispatcher dispatcher;
    private final btDbvtBroadphase broadphase;
    private final btSequentialImpulseConstraintSolver solver;
    private final btDiscreteDynamicsWorld dynamicsWorld;
    
    private final Array<Disposable> disposables = new Array<>();
    private final Array<ModelInstance> instances = new Array<>();
    private final Array<GameObject> gameObjects = new Array<>();

    private SpriteBatch spriteBatch;
    private BitmapFont font;
    private ShapeRenderer shapeRenderer;
    
    private final Adventurer player;
    private final GameClock gameClock;
    private final GlyphLayout layout = new GlyphLayout();
    private final StringBuilder hudText = new StringBuilder();
    
    private boolean isInventoryOpen = false;
    private boolean isPaused = false;

    public LibGDXGame(MainGame game) {
        this.game = game;
        Bullet.init();

        player = new Adventurer("Lucas", "Caserne");
        gameClock = new GameClock(player);

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

        collisionConfig = new btDefaultCollisionConfiguration();
        dispatcher = new btCollisionDispatcher(collisionConfig);
        broadphase = new btDbvtBroadphase();
        solver = new btSequentialImpulseConstraintSolver();
        dynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfig);
        dynamicsWorld.setGravity(new Vector3(0, -10f, 0));

        createGround();
        createItem(5, 1.5f, 5, new PotionDeSoin("Potion de Soin", 25));
        createEnemy(-5, 1.5f, 5, "Slime", 50);

        playerController = new PlayerController(camera, dynamicsWorld, player);
    }

    private void createGround() {
        ModelBuilder modelBuilder = new ModelBuilder();
        Material groundMaterial = new Material(ColorAttribute.createDiffuse(Color.FOREST));
        Model groundModel = modelBuilder.createBox(200f, 1f, 200f, groundMaterial, Usage.Position | Usage.Normal);
        disposables.add(groundModel);
        ModelInstance groundInstance = new ModelInstance(groundModel);
        instances.add(groundInstance);

        btCollisionShape groundShape = new btBoxShape(new Vector3(100f, 0.5f, 100f));
        btRigidBody.btRigidBodyConstructionInfo groundInfo = new btRigidBody.btRigidBodyConstructionInfo(0, null, groundShape, Vector3.Zero);
        btRigidBody groundBody = new btRigidBody(groundInfo);
        dynamicsWorld.addRigidBody(groundBody);
    }

    private void createItem(float x, float y, float z, PotionDeSoin item) {
        ModelBuilder modelBuilder = new ModelBuilder();
        Model itemModel = modelBuilder.createCylinder(0.5f, 1f, 0.5f, 16, new Material(ColorAttribute.createDiffuse(Color.RED)), Usage.Position | Usage.Normal);
        disposables.add(itemModel);
        ModelInstance itemInstance = new ModelInstance(itemModel);
        itemInstance.transform.setTranslation(x, y, z);
        instances.add(itemInstance);

        GameObject itemObject = new GameObject(item.getName(), item);
        itemInstance.userData = itemObject;
        gameObjects.add(itemObject);

        btCollisionShape itemShape = new btCylinderShape(new Vector3(0.25f, 0.5f, 0.25f));
        Vector3 localInertia = new Vector3();
        float mass = 0.5f;
        itemShape.calculateLocalInertia(mass, localInertia);

        btDefaultMotionState motionState = new btDefaultMotionState(itemInstance.transform);
        itemObject.motionState = motionState;
        btRigidBody.btRigidBodyConstructionInfo itemInfo = new btRigidBody.btRigidBodyConstructionInfo(mass, motionState, itemShape, localInertia);
        btRigidBody itemBody = new btRigidBody(itemInfo);
        itemBody.userData = itemObject;
        itemObject.body = itemBody;
        dynamicsWorld.addRigidBody(itemBody);
    }

    private void createEnemy(float x, float y, float z, String name, int health) {
        ModelBuilder modelBuilder = new ModelBuilder();
        Model enemyModel = modelBuilder.createBox(1f, 1f, 1f, new Material(ColorAttribute.createDiffuse(Color.BLUE)), Usage.Position | Usage.Normal);
        disposables.add(enemyModel);
        ModelInstance enemyInstance = new ModelInstance(enemyModel);
        enemyInstance.transform.setTranslation(x, y, z);
        instances.add(enemyInstance);

        Enemy enemyObject = new Enemy(name, health);
        enemyInstance.userData = enemyObject;
        gameObjects.add(enemyObject);

        btCollisionShape enemyShape = new btBoxShape(new Vector3(0.5f, 0.5f, 0.5f));
        Vector3 localInertia = new Vector3();
        float mass = 1f;
        enemyShape.calculateLocalInertia(mass, localInertia);

        btDefaultMotionState motionState = new btDefaultMotionState(enemyInstance.transform);
        enemyObject.motionState = motionState;
        btRigidBody.btRigidBodyConstructionInfo enemyInfo = new btRigidBody.btRigidBodyConstructionInfo(mass, motionState, enemyShape, localInertia);
        btRigidBody enemyBody = new btRigidBody(enemyInfo);
        enemyBody.userData = enemyObject;
        enemyObject.body = enemyBody;
        dynamicsWorld.addRigidBody(enemyBody);
    }

    @Override
    public void render(float delta) {
        update(delta);
        
        Gdx.gl.glClearColor(0.3f, 0.6f, 0.8f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(camera);
        modelBatch.render(instances, environment);
        modelBatch.end();

        drawHealthBars();

        spriteBatch.begin();
        drawHud();
        if (isInventoryOpen) drawInventory();
        if (isPaused) drawPauseMenu();
        spriteBatch.end();
    }

    private void update(float delta) {
        if (!isPaused && !isInventoryOpen) {
            playerController.update();
            dynamicsWorld.stepSimulation(delta, 5, 1/60f);
            playerController.updateCamera();

            for (GameObject go : gameObjects) {
                if (go instanceof Enemy) {
                    ((Enemy) go).update(player, playerController.getPlayerBody(), delta);
                }
            }
        }
        
        gameClock.update(delta);

        for (int i = gameObjects.size - 1; i >= 0; i--) {
            GameObject go = gameObjects.get(i);
            if (!go.isActive) {
                dynamicsWorld.removeCollisionObject(go.body);
                go.body.dispose();
                if (go.motionState != null) go.motionState.dispose();
                gameObjects.removeIndex(i);
                
                for (int j = instances.size - 1; j >= 0; j--) {
                    if (instances.get(j).userData == go) {
                        instances.removeIndex(j);
                        break;
                    }
                }
            }
        }
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.ESCAPE) {
            if (isInventoryOpen) {
                isInventoryOpen = false;
                Gdx.input.setCursorCatched(true);
            } else {
                isPaused = !isPaused;
                Gdx.input.setCursorCatched(!isPaused);
            }
            return true;
        }

        if (isPaused) {
            if (keycode == Input.Keys.C) {
                isPaused = false;
                Gdx.input.setCursorCatched(true);
            }
            if (keycode == Input.Keys.M) {
                game.setScreen(new MainMenuScreen(game));
            }
            return true;
        }

        if (keycode == Input.Keys.I) {
            isInventoryOpen = !isInventoryOpen;
            Gdx.input.setCursorCatched(!isInventoryOpen);
            return true;
        }

        if (isInventoryOpen) {
            if (keycode == Input.Keys.NUM_1) useItem(0);
            return true;
        }

        return playerController.keyDown(keycode);
    }

    @Override
    public boolean keyUp(int keycode) {
        return playerController.keyUp(keycode);
    }
    
    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (!isPaused && !isInventoryOpen) {
            return playerController.touchDown(screenX, screenY, pointer, button);
        }
        return false;
    }

    private void useItem(int index) {
        if (index >= 0 && index < player.getInventory().size()) {
            Item item = player.getInventory().get(index);
            if (item instanceof PotionDeSoin) {
                ((PotionDeSoin) item).utiliser(player);
                player.getInventory().remove(item);
            }
        }
    }

    private void drawHud() {
        font.draw(spriteBatch, "+", Gdx.graphics.getWidth() / 2f - 5, Gdx.graphics.getHeight() / 2f + 5);
        hudText.setLength(0);
        hudText.append("FPS: ").append(Gdx.graphics.getFramesPerSecond()).append("\n");
        hudText.append("Player: ").append(player.getName()).append("\n");
        hudText.append("HP: ").append(player.getHealth()).append(" / ").append(player.getMaxHealth()).append("\n");
        hudText.append("Time: ").append(gameClock.getCurrentHour()).append("h, Day ").append(gameClock.getCurrentDay());
        font.draw(spriteBatch, hudText, 10, Gdx.graphics.getHeight() - 10);

        if (!isInventoryOpen && !isPaused) {
            btCollisionObject objectInView = playerController.getBodyInView(3f);
            if (objectInView != null && objectInView.userData instanceof GameObject) {
                GameObject go = (GameObject) objectInView.userData;
                if (go.isActive) {
                    String text = (go.item != null) ? "Ramasser " + go.item.getName() : "Interagir";
                    layout.setText(font, text + " (E)");
                    font.draw(spriteBatch, layout, Gdx.graphics.getWidth() / 2f - layout.width / 2f, Gdx.graphics.getHeight() / 2f - 30);
                }
            }
        }
    }

    private void drawInventory() {
        spriteBatch.end();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.5f);
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        spriteBatch.begin();

        hudText.setLength(0);
        hudText.append("--- INVENTAIRE ---\n\n");
        if (player.getInventory().isEmpty()) {
            hudText.append("Vide");
        } else {
            int i = 1;
            for (Item item : player.getInventory()) {
                hudText.append("[").append(i++).append("] ").append(item.getName()).append("\n");
            }
        }
        layout.setText(font, hudText);
        font.draw(spriteBatch, hudText, Gdx.graphics.getWidth() / 2f - layout.width / 2f, Gdx.graphics.getHeight() * 0.8f);
    }
    
    private void drawPauseMenu() {
        spriteBatch.end();
        Gdx.gl.glEnable(GL20.GL_BLEND);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0, 0, 0, 0.5f);
        shapeRenderer.rect(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        spriteBatch.begin();

        hudText.setLength(0);
        hudText.append("PAUSE\n\n");
        hudText.append("[C] pour Continuer\n");
        hudText.append("[M] pour retourner au Menu Principal");
        layout.setText(font, hudText);
        font.draw(spriteBatch, hudText, Gdx.graphics.getWidth() / 2f - layout.width / 2f, Gdx.graphics.getHeight() * 0.6f);
    }

    private void drawHealthBars() {
        shapeRenderer.setProjectionMatrix(camera.combined);
        for (ModelInstance instance : instances) {
            if (instance.userData instanceof Enemy) {
                Enemy enemy = (Enemy) instance.userData;
                if (enemy.isActive && enemy.health < enemy.maxHealth) {
                    Vector3 position = new Vector3();
                    instance.transform.getTranslation(position);
                    
                    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                    shapeRenderer.setColor(Color.RED);
                    shapeRenderer.rect(position.x - 0.5f, position.y + 1.2f, 1, 0.1f);
                    shapeRenderer.setColor(Color.GREEN);
                    shapeRenderer.rect(position.x - 0.5f, position.y + 1.2f, (float)enemy.health / enemy.maxHealth, 0.1f);
                    shapeRenderer.end();
                }
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
    }

    @Override
    public void show() {
        spriteBatch = new SpriteBatch();
        font = new BitmapFont();
        font.setColor(Color.WHITE);
        shapeRenderer = new ShapeRenderer();
        disposables.addAll(spriteBatch, font, shapeRenderer);
        Gdx.input.setInputProcessor(this);
        Gdx.input.setCursorCatched(true);
    }

    @Override
    public void hide() {
        Gdx.input.setCursorCatched(false);
        dispose();
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void dispose() {
        for (GameObject go : gameObjects) {
            dynamicsWorld.removeCollisionObject(go.body);
            go.body.dispose();
            if (go.motionState != null) go.motionState.dispose();
        }
        gameObjects.clear();
        
        playerController.dispose();
        
        dynamicsWorld.dispose();
        solver.dispose();
        broadphase.dispose();
        dispatcher.dispose();
        collisionConfig.dispose();

        for (Disposable disposable : disposables) {
            disposable.dispose();
        }
        disposables.clear();
        instances.clear();
    }
}
