package com.lucas.guild.libgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
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
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
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

    private Stage stage;
    private Skin skin;
    private Table inventoryTable;
    private Table pauseTable;
    
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
        itemObject.modelInstance = itemInstance;
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
        Model enemyModel = modelBuilder.createBox(0.5f, 1f, 2f, new Material(ColorAttribute.createDiffuse(Color.BLUE)), Usage.Position | Usage.Normal);
        disposables.add(enemyModel);
        ModelInstance enemyInstance = new ModelInstance(enemyModel);
        enemyInstance.transform.setTranslation(x, y, z);
        instances.add(enemyInstance);

        Enemy enemyObject = new Enemy(name, health);
        enemyInstance.userData = enemyObject;
        enemyObject.modelInstance = enemyInstance;
        gameObjects.add(enemyObject);

        btCollisionShape enemyShape = new btBoxShape(new Vector3(0.25f, 0.5f, 1f));
        Vector3 localInertia = new Vector3();
        float mass = 1f;
        enemyShape.calculateLocalInertia(mass, localInertia);

        btDefaultMotionState motionState = new btDefaultMotionState(enemyInstance.transform);
        enemyObject.motionState = motionState;
        btRigidBody.btRigidBodyConstructionInfo enemyInfo = new btRigidBody.btRigidBodyConstructionInfo(mass, motionState, enemyShape, localInertia);
        btRigidBody enemyBody = new btRigidBody(enemyInfo);
        enemyBody.setActivationState(Collision.DISABLE_DEACTIVATION);
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
        spriteBatch.end();

        if (isInventoryOpen || isPaused) {
            stage.act(delta);
            stage.draw();
        }
    }

    private void update(float delta) {
        if (!isPaused && !isInventoryOpen) {
            playerController.update();
            dynamicsWorld.stepSimulation(delta, 5, 1/60f);
            playerController.updateCamera();

            for (GameObject go : gameObjects) {
                if (go.body != null && go.motionState != null && go.modelInstance != null) {
                    go.motionState.getWorldTransform(go.modelInstance.transform);
                }
                //if (go instanceof Enemy) {
                //    ((Enemy) go).update(player, playerController.getPlayerBody(), delta);
                //}
            }
            gameClock.update(delta);
        }

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
                toggleInventory();
            } else {
                togglePause();
            }
            return true;
        }

        if (keycode == Input.Keys.I) {
            toggleInventory();
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
                player.removeItem(item);
                updateInventory();
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
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void show() {
        spriteBatch = new SpriteBatch();
        font = new BitmapFont();
        font.setColor(Color.WHITE);
        shapeRenderer = new ShapeRenderer();
        disposables.addAll(spriteBatch, font, shapeRenderer);

        stage = new Stage(new ScreenViewport());
        disposables.add(stage);

        skin = new Skin();
        skin.add("default-font", font);

        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.font = skin.getFont("default-font");
        skin.add("default", textButtonStyle);

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = skin.getFont("default-font");
        skin.add("default", labelStyle);

        setupPauseMenu();
        setupInventory();

        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(stage);
        multiplexer.addProcessor(this);
        Gdx.input.setInputProcessor(multiplexer);
        Gdx.input.setCursorCatched(true);
    }

    private void setupPauseMenu() {
        pauseTable = new Table(skin);
        pauseTable.setFillParent(true);
        pauseTable.setVisible(false);
        stage.addActor(pauseTable);

        TextButton continueButton = new TextButton("Continuer", skin);
        TextButton menuButton = new TextButton("Retour au Menu", skin);

        pauseTable.add(continueButton).pad(10).row();
        pauseTable.add(menuButton).pad(10).row();

        continueButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                togglePause();
            }
        });

        menuButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                game.setScreen(new MainMenuScreen(game));
            }
        });
    }

    private void setupInventory() {
        inventoryTable = new Table(skin);
        inventoryTable.setFillParent(true);
        inventoryTable.setVisible(false);
        stage.addActor(inventoryTable);
        updateInventory();
    }

    private void updateInventory() {
        inventoryTable.clear();
        inventoryTable.add("--- INVENTAIRE ---").row();

        if (player.getInventory().isEmpty()) {
            inventoryTable.add("Vide").row();
        } else {
            for (int i = 0; i < player.getInventory().size(); i++) {
                final int index = i;
                Item item = player.getInventory().get(i);
                TextButton itemButton = new TextButton(item.getName(), skin);
                itemButton.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        useItem(index);
                    }
                });
                inventoryTable.add(itemButton).pad(5).row();
            }
        }
    }

    private void togglePause() {
        isPaused = !isPaused;
        pauseTable.setVisible(isPaused);
        Gdx.input.setCursorCatched(!isPaused);
    }

    private void toggleInventory() {
        isInventoryOpen = !isInventoryOpen;
        inventoryTable.setVisible(isInventoryOpen);
        if (isInventoryOpen) {
            updateInventory();
        }
        Gdx.input.setCursorCatched(!isInventoryOpen);
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
        skin.dispose();
    }
}
