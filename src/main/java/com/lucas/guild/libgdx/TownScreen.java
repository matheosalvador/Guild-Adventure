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
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.*;
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
import com.lucas.guild.game.Forge;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Building;
import com.lucas.guild.model.GameClock;
import com.lucas.guild.model.Inventaire;
import com.lucas.guild.model.Item;
import com.lucas.guild.model.Town;

public class TownScreen extends InputAdapter implements Screen {

    private final MainGame game;
    private final Town town;
    private Adventurer player; // Le joueur dans la ville
    private GameClock gameClock;
    private final String difficulty;

    private PerspectiveCamera camera;
    private ModelBatch modelBatch;
    private Environment environment;
    private PlayerController playerController;

    private btDefaultCollisionConfiguration collisionConfig;
    private btCollisionDispatcher dispatcher;
    private btDbvtBroadphase broadphase;
    private btSequentialImpulseConstraintSolver solver;
    private btDiscreteDynamicsWorld dynamicsWorld;

    private final Array<Disposable> disposables = new Array<>();
    private final Array<ModelInstance> instances = new Array<>();
    private final Array<GameObject> gameObjects = new Array<>();

    private SpriteBatch spriteBatch;
    private BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();
    private final StringBuilder hudText = new StringBuilder();

    private Stage stage;
    private Skin skin;
    private Table pauseTable;
    private Table inventoryTable;
    private boolean isPaused = false;
    private boolean isInventoryOpen = false;

    public TownScreen(MainGame game, String difficulty) {
        this.game = game;
        this.difficulty = difficulty;
        this.town = new Town("Aethelgard");
        town.addBuilding(new Forge("La Forge du Nain Grincheux"));
        town.addBuilding(new Building("Guilde des Aventuriers", "Guilde"));
        town.addBuilding(new Building("La Pinte qui Chante", "Taverne"));
    }

    @Override
    public void show() {
        Bullet.init();
        player = new Adventurer("Lucas", "Ville");
        Inventaire inventaire = new Inventaire();
        inventaire.setPoidsMax(difficulty);
        player.setInventory(inventaire);
        gameClock = new GameClock(player);

        setup3DEnvironment();
        setupPhysics();
        setupUI();
        setupInput();

        createGround();
        createBuildings();

        playerController = new PlayerController(camera, dynamicsWorld, player);
    }

    private void setup3DEnvironment() {
        camera = new PerspectiveCamera(70, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0f, 5f, 10f);
        camera.lookAt(0, 0, 0);
        camera.near = 0.1f;
        camera.far = 300f;
        camera.update();

        modelBatch = new ModelBatch();
        disposables.add(modelBatch);

        environment = new Environment();
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
        environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));

        spriteBatch = new SpriteBatch();
        font = new BitmapFont();
        disposables.addAll(spriteBatch, font);
    }

    private void setupPhysics() {
        collisionConfig = new btDefaultCollisionConfiguration();
        dispatcher = new btCollisionDispatcher(collisionConfig);
        broadphase = new btDbvtBroadphase();
        solver = new btSequentialImpulseConstraintSolver();
        dynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfig);
        dynamicsWorld.setGravity(new Vector3(0, -10f, 0));
        disposables.addAll(dynamicsWorld, solver, broadphase, dispatcher, collisionConfig);
    }

    private void setupUI() {
        stage = new Stage(new ScreenViewport());
        disposables.add(stage);

        skin = new Skin();
        skin.add("default-font", font);
        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.font = skin.getFont("default-font");
        skin.add("default", textButtonStyle);
        Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
        skin.add("default", labelStyle);

        // Pause Menu
        pauseTable = new Table();
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

        // Inventory Menu
        inventoryTable = new Table();
        inventoryTable.setFillParent(true);
        inventoryTable.setVisible(false);
        stage.addActor(inventoryTable);
    }

    private void setupInput() {
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(stage);
        multiplexer.addProcessor(this);
        Gdx.input.setInputProcessor(multiplexer);
        Gdx.input.setCursorCatched(true);
    }

    private void createGround() {
        ModelBuilder modelBuilder = new ModelBuilder();
        Material groundMaterial = new Material(ColorAttribute.createDiffuse(Color.GRAY));
        Model groundModel = modelBuilder.createBox(200f, 1f, 200f, groundMaterial, Usage.Position | Usage.Normal);
        disposables.add(groundModel);
        ModelInstance groundInstance = new ModelInstance(groundModel);
        instances.add(groundInstance);

        btCollisionShape groundShape = new btBoxShape(new Vector3(100f, 0.5f, 100f));
        disposables.add(groundShape);
        btRigidBody.btRigidBodyConstructionInfo groundInfo = new btRigidBody.btRigidBodyConstructionInfo(0, null, groundShape, Vector3.Zero);
        disposables.add(groundInfo);
        btRigidBody groundBody = new btRigidBody(groundInfo);
        dynamicsWorld.addRigidBody(groundBody);
    }

    private void createBuildings() {
        createBuilding(new Forge("La Forge du Nain Grincheux"), new Vector3(0, 0, -20), Color.DARK_GRAY);
        createBuilding(new Building("Guilde des Aventuriers", "Guilde"), new Vector3(-20, 0, 0), Color.BROWN);
        createBuilding(new Building("La Pinte qui Chante", "Taverne"), new Vector3(20, 0, 0), Color.TAN);
    }

    private void createBuilding(Building building, Vector3 position, Color color) {
        ModelBuilder modelBuilder = new ModelBuilder();
        Material material = new Material(ColorAttribute.createDiffuse(color));
        Model model = modelBuilder.createBox(10f, 10f, 10f, material, Usage.Position | Usage.Normal);
        disposables.add(model);
        ModelInstance instance = new ModelInstance(model);
        instance.transform.setTranslation(position.add(0, 5.5f, 0));
        instances.add(instance);

        instance.userData = building;

        btCollisionShape shape = new btBoxShape(new Vector3(5f, 5f, 5f));
        disposables.add(shape);
        btRigidBody.btRigidBodyConstructionInfo info = new btRigidBody.btRigidBodyConstructionInfo(0, null, shape, Vector3.Zero);
        disposables.add(info);
        btRigidBody body = new btRigidBody(info);
        body.setWorldTransform(instance.transform);
        body.userData = building;
        dynamicsWorld.addRigidBody(body);
    }

    @Override
    public void render(float delta) {
        update(delta);

        Gdx.gl.glClearColor(0.5f, 0.8f, 1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(camera);
        modelBatch.render(instances, environment);
        modelBatch.end();

        spriteBatch.begin();
        drawHud();
        spriteBatch.end();

        if (isPaused || isInventoryOpen) {
            stage.act(delta);
            stage.draw();
        }
    }

    private void update(float delta) {
        if (!isPaused && !isInventoryOpen) {
            playerController.update();
            dynamicsWorld.stepSimulation(delta, 5, 1 / 60f);
            playerController.updateCamera();
            gameClock.update(delta);
        }
    }

    private void drawHud() {
        if (!isPaused && !isInventoryOpen) {
            font.draw(spriteBatch, "+", Gdx.graphics.getWidth() / 2f - 5, Gdx.graphics.getHeight() / 2f + 5);

            hudText.setLength(0);
            hudText.append("FPS: ").append(Gdx.graphics.getFramesPerSecond()).append("\n");
            hudText.append("Player: ").append(player.getName()).append("\n");
            hudText.append("HP: ").append(player.getHealth()).append(" / ").append(player.getMaxHealth()).append("\n");
            hudText.append("Time: ").append(gameClock.getCurrentHour()).append("h, Day ").append(gameClock.getCurrentDay());
            font.draw(spriteBatch, hudText, 10, Gdx.graphics.getHeight() - 10);

            btCollisionObject objectInView = playerController.getBodyInView(10f);
            if (objectInView != null && objectInView.userData instanceof Building) {
                Building building = (Building) objectInView.userData;
                String text = "Entrer dans: " + building.getName() + " (E)";
                layout.setText(font, text);
                font.draw(spriteBatch, layout, Gdx.graphics.getWidth() / 2f - layout.width / 2f, Gdx.graphics.getHeight() / 2f - 30);
            }
        }
    }

    private void drawInventory() {
        inventoryTable.clear();
        inventoryTable.top().left();
        inventoryTable.add(new Label("Inventaire", skin)).colspan(2).pad(10).row();

        for (Item item : player.getInventory()) {
            inventoryTable.add(new Label(item.getName(), skin)).pad(5);
            inventoryTable.add(new Label(item.getDescription(), skin)).pad(5).row();
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
        if (isPaused || isInventoryOpen) return false;

        if (keycode == Input.Keys.E) {
            btCollisionObject objectInView = playerController.getBodyInView(10f);
            if (objectInView != null && objectInView.userData instanceof Building) {
                Building building = (Building) objectInView.userData;
                if (building instanceof Forge) {
                    game.setScreen(new ForgeScreen(game, difficulty));
                    return true;
                }
            }
        }
        return playerController.keyDown(keycode);
    }

    private void togglePause() {
        isPaused = !isPaused;
        pauseTable.setVisible(isPaused);
        Gdx.input.setCursorCatched(!isPaused);
    }

    private void toggleInventory() {
        isInventoryOpen = !isInventoryOpen;
        inventoryTable.setVisible(isInventoryOpen);
        Gdx.input.setCursorCatched(!isInventoryOpen);
        if (isInventoryOpen) {
            drawInventory();
        }
    }

    @Override
    public boolean keyUp(int keycode) {
        return playerController.keyUp(keycode);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return playerController.touchDown(screenX, screenY, pointer, button);
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
        stage.getViewport().update(width, height, true);
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
        playerController.dispose();
        for (Disposable disposable : disposables) {
            disposable.dispose();
        }
        disposables.clear();
        instances.clear();
        gameObjects.clear();
        skin.dispose();
    }
}
