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
import com.badlogic.gdx.math.Matrix4;
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
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.GameClock;
import com.lucas.guild.model.Inventaire;
import com.lucas.guild.model.Item;

public class InnScreen extends InputAdapter implements Screen {

    private final MainGame game;
    private final String difficulty;
    private Adventurer player;
    private GameClock gameClock;

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

    private SpriteBatch spriteBatch;
    private BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();
    private final StringBuilder hudText = new StringBuilder();

    private Stage stage;
    private Skin skin;
    private Table dialogTable;
    private Table inventoryTable;
    private Table pauseTable;
    private boolean inDialog = false;
    private boolean isInventoryOpen = false;
    private boolean isPaused = false;

    public InnScreen(MainGame game, String difficulty) {
        this.game = game;
        this.difficulty = difficulty;
    }

    @Override
    public void show() {
        Bullet.init();
        player = new Adventurer("Lucas", "Auberge");
        Inventaire inventaire = new Inventaire();
        inventaire.setPoidsMax(difficulty);
        player.setInventory(inventaire);
        gameClock = new GameClock(player);

        setup3DEnvironment();
        setupPhysics();
        setupUI();
        setupInput();

        createInn();

        playerController = new PlayerController(camera, dynamicsWorld, player, new Vector3(0, 2, -8));
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
        environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.6f, 0.6f, 0.6f, 1f));
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

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = skin.getFont("default-font");
        skin.add("default", labelStyle);

        dialogTable = new Table(skin);
        dialogTable.setFillParent(true);
        dialogTable.setVisible(false);
        stage.addActor(dialogTable);

        inventoryTable = new Table(skin);
        inventoryTable.setFillParent(true);
        inventoryTable.setVisible(false);
        stage.addActor(inventoryTable);

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

    private void setupInput() {
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(stage);
        multiplexer.addProcessor(this);
        Gdx.input.setInputProcessor(multiplexer);
        Gdx.input.setCursorCatched(true);
    }

    private void createInn() {
        ModelBuilder modelBuilder = new ModelBuilder();

        // --- Rez-de-chaussée ---
        Material floorMaterial = new Material(ColorAttribute.createDiffuse(Color.MAROON));
        Model floorModel = modelBuilder.createBox(20f, 1f, 20f, floorMaterial, Usage.Position | Usage.Normal);
        disposables.add(floorModel);
        ModelInstance floorInstance = new ModelInstance(floorModel);
        instances.add(floorInstance);
        addStaticBody(floorInstance, new btBoxShape(new Vector3(10f, 0.5f, 10f)), null);

        Material wallMaterial = new Material(ColorAttribute.createDiffuse(Color.TAN));
        Model wallModel = modelBuilder.createBox(1f, 10f, 20f, wallMaterial, Usage.Position | Usage.Normal);
        disposables.add(wallModel);
        
        ModelInstance wall1 = new ModelInstance(wallModel);
        wall1.transform.setTranslation(10f, 5f, 0);
        instances.add(wall1);
        addStaticBody(wall1, new btBoxShape(new Vector3(0.5f, 5f, 10f)), null);

        ModelInstance wall2 = new ModelInstance(wallModel);
        wall2.transform.setTranslation(-10f, 5f, 0);
        instances.add(wall2);
        addStaticBody(wall2, new btBoxShape(new Vector3(0.5f, 5f, 10f)), null);

        Model wallModel2 = modelBuilder.createBox(20f, 10f, 1f, wallMaterial, Usage.Position | Usage.Normal);
        disposables.add(wallModel2);

        ModelInstance wall3 = new ModelInstance(wallModel2);
        wall3.transform.setTranslation(0, 5f, 10f);
        instances.add(wall3);
        addStaticBody(wall3, new btBoxShape(new Vector3(10f, 5f, 0.5f)), null);

        Material doorMaterial = new Material(ColorAttribute.createDiffuse(Color.YELLOW));
        Model doorModel = modelBuilder.createBox(4f, 8f, 0.5f, doorMaterial, Usage.Position | Usage.Normal);
        disposables.add(doorModel);
        ModelInstance doorInstance = new ModelInstance(doorModel);
        doorInstance.transform.setTranslation(0, 4.5f, -9.75f);
        instances.add(doorInstance);
        addStaticBody(doorInstance, new btBoxShape(new Vector3(2f, 4f, 0.25f)), "Door");

        Material npcMaterial = new Material(ColorAttribute.createDiffuse(Color.ORANGE));
        Model npcModel = modelBuilder.createBox(1f, 2f, 1f, npcMaterial, Usage.Position | Usage.Normal);
        disposables.add(npcModel);
        ModelInstance npcInstance = new ModelInstance(npcModel);
        npcInstance.transform.setTranslation(0, 1.5f, 5f);
        instances.add(npcInstance);
        addStaticBody(npcInstance, new btBoxShape(new Vector3(0.5f, 1f, 0.5f)), "Innkeeper");

        // --- Étage ---
        // Création du sol de l'étage en plusieurs parties pour laisser un trou pour l'escalier
        Model floor2PartModel = modelBuilder.createBox(12f, 1f, 20f, floorMaterial, Usage.Position | Usage.Normal);
        disposables.add(floor2PartModel);
        ModelInstance floor2Part1 = new ModelInstance(floor2PartModel);
        floor2Part1.transform.setTranslation(4, 10f, 0);
        instances.add(floor2Part1);
        addStaticBody(floor2Part1, new btBoxShape(new Vector3(6f, 0.5f, 10f)), null);

        Model floor2Part2Model = modelBuilder.createBox(8f, 1f, 12f, floorMaterial, Usage.Position | Usage.Normal);
        disposables.add(floor2Part2Model);
        ModelInstance floor2Part2 = new ModelInstance(floor2Part2Model);
        floor2Part2.transform.setTranslation(-6, 10f, 4);
        instances.add(floor2Part2);
        addStaticBody(floor2Part2, new btBoxShape(new Vector3(4f, 0.5f, 6f)), null);


        Material bedMaterial = new Material(ColorAttribute.createDiffuse(Color.RED));
        Model bedModel = modelBuilder.createBox(4f, 2f, 8f, bedMaterial, Usage.Position | Usage.Normal);
        disposables.add(bedModel);
        ModelInstance bedInstance = new ModelInstance(bedModel);
        bedInstance.transform.setTranslation(5, 11.5f, 5);
        instances.add(bedInstance);
        addStaticBody(bedInstance, new btBoxShape(new Vector3(2f, 1f, 4f)), "Bed");

        // Murs de l'étage
        Model wallUpperModel = modelBuilder.createBox(1f, 10f, 20f, wallMaterial, Usage.Position | Usage.Normal);
        disposables.add(wallUpperModel);
        ModelInstance wallUpper1 = new ModelInstance(wallUpperModel);
        wallUpper1.transform.setTranslation(10f, 15f, 0);
        instances.add(wallUpper1);
        addStaticBody(wallUpper1, new btBoxShape(new Vector3(0.5f, 5f, 10f)), null);

        ModelInstance wallUpper2 = new ModelInstance(wallUpperModel);
        wallUpper2.transform.setTranslation(-10f, 15f, 0);
        instances.add(wallUpper2);
        addStaticBody(wallUpper2, new btBoxShape(new Vector3(0.5f, 5f, 10f)), null);

        Model wallUpperModel2 = modelBuilder.createBox(20f, 10f, 1f, wallMaterial, Usage.Position | Usage.Normal);
        disposables.add(wallUpperModel2);
        ModelInstance wallUpper3 = new ModelInstance(wallUpperModel2);
        wallUpper3.transform.setTranslation(0, 15f, 10f);
        instances.add(wallUpper3);
        addStaticBody(wallUpper3, new btBoxShape(new Vector3(10f, 5f, 0.5f)), null);

        ModelInstance wallUpper4 = new ModelInstance(wallUpperModel2);
        wallUpper4.transform.setTranslation(0, 15f, -10f);
        instances.add(wallUpper4);
        addStaticBody(wallUpper4, new btBoxShape(new Vector3(10f, 5f, 0.5f)), null);

        // Toit
        Model roofModel = modelBuilder.createBox(20f, 1f, 20f, floorMaterial, Usage.Position | Usage.Normal);
        disposables.add(roofModel);
        ModelInstance roofInstance = new ModelInstance(roofModel);
        roofInstance.transform.setTranslation(0, 20f, 0);
        instances.add(roofInstance);
        addStaticBody(roofInstance, new btBoxShape(new Vector3(10f, 0.5f, 10f)), null);


        // Escalier
        Material stepMaterial = new Material(ColorAttribute.createDiffuse(Color.BROWN));
        Model stepModel = modelBuilder.createBox(4f, 1f, 2f, stepMaterial, Usage.Position | Usage.Normal);
        disposables.add(stepModel);
        for (int i = 0; i < 10; i++) {
            ModelInstance step = new ModelInstance(stepModel);
            step.transform.setTranslation(-8, 1f + i, 8f - i * 2f);
            instances.add(step);
            addStaticBody(step, new btBoxShape(new Vector3(2f, 0.5f, 1f)), null);
        }
    }

    private void addStaticBody(ModelInstance instance, btCollisionShape shape, Object userData) {
        disposables.add(shape);
        btRigidBody.btRigidBodyConstructionInfo info = new btRigidBody.btRigidBodyConstructionInfo(0, null, shape, Vector3.Zero);
        disposables.add(info);
        btRigidBody body = new btRigidBody(info);
        body.setWorldTransform(instance.transform);
        body.userData = userData;
        dynamicsWorld.addRigidBody(body);
    }

    @Override
    public void render(float delta) {
        update(delta);

        Gdx.gl.glClearColor(0.3f, 0.2f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(camera);
        modelBatch.render(instances, environment);
        modelBatch.end();

        spriteBatch.begin();
        drawHud();
        spriteBatch.end();

        if (inDialog || isInventoryOpen || isPaused) {
            stage.act(delta);
            stage.draw();
        }
    }

    private void update(float delta) {
        if (!inDialog && !isInventoryOpen && !isPaused) {
            playerController.update();
            dynamicsWorld.stepSimulation(delta, 5, 1 / 60f);
            playerController.updateCamera();
            gameClock.update(delta);
        }
    }

    private void drawHud() {
        if (!inDialog && !isInventoryOpen && !isPaused) {
            font.draw(spriteBatch, "+", Gdx.graphics.getWidth() / 2f - 5, Gdx.graphics.getHeight() / 2f + 5);

            hudText.setLength(0);
            hudText.append("FPS: ").append(Gdx.graphics.getFramesPerSecond()).append("\n");
            hudText.append("Player: ").append(player.getName()).append("\n");
            hudText.append("HP: ").append(player.getHealth()).append(" / ").append(player.getMaxHealth()).append("\n");
            hudText.append("Time: ").append(gameClock.getCurrentHour()).append("h, Day ").append(gameClock.getCurrentDay());
            font.draw(spriteBatch, hudText, 10, Gdx.graphics.getHeight() - 10);

            btCollisionObject objectInView = playerController.getBodyInView(5f);
            if (objectInView != null) {
                if ("Innkeeper".equals(objectInView.userData)) {
                    String text = "Parler à l'aubergiste (E)";
                    layout.setText(font, text);
                    font.draw(spriteBatch, layout, Gdx.graphics.getWidth() / 2f - layout.width / 2f, Gdx.graphics.getHeight() / 2f - 30);
                } else if ("Door".equals(objectInView.userData)) {
                    String text = "Sortir (E)";
                    layout.setText(font, text);
                    font.draw(spriteBatch, layout, Gdx.graphics.getWidth() / 2f - layout.width / 2f, Gdx.graphics.getHeight() / 2f - 30);
                } else if ("Bed".equals(objectInView.userData)) {
                    String text = "Dormir (10 Po) (E)";
                    layout.setText(font, text);
                    font.draw(spriteBatch, layout, Gdx.graphics.getWidth() / 2f - layout.width / 2f, Gdx.graphics.getHeight() / 2f - 30);
                }
            }
        }
    }

    private void drawInventory() {
        inventoryTable.clear();
        inventoryTable.top().left();
        Inventaire inventaire = player.getInventaire();
        inventoryTable.add(new Label("Inventaire", skin)).colspan(2).pad(10).row();
        inventoryTable.add(new Label("Poids: " + inventaire.getPoidsActuel() + " / " + inventaire.getPoidsMax() + " kg", skin)).colspan(2).pad(10).row();

        for (Item item : player.getInventoryItems()) {
            inventoryTable.add(new Label(item.getName(), skin)).pad(5);
            inventoryTable.add(new Label(item.getDescription(), skin)).pad(5).row();
        }
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.ESCAPE) {
            if (isInventoryOpen) {
                toggleInventory();
            } else if (inDialog) {
                toggleDialog(null);
            } else {
                togglePause();
            }
            return true;
        }
        if (keycode == Input.Keys.I) {
            toggleInventory();
            return true;
        }
        if (inDialog || isInventoryOpen || isPaused) return false;

        if (keycode == Input.Keys.E) {
            btCollisionObject objectInView = playerController.getBodyInView(5f);
            if (objectInView != null) {
                if ("Innkeeper".equals(objectInView.userData)) {
                    toggleDialog("Innkeeper");
                    return true;
                } else if ("Door".equals(objectInView.userData)) {
                    game.setScreen(new TownScreen(game, difficulty, new Vector3(20, 2, 5))); // Position de sortie
                    return true;
                } else if ("Bed".equals(objectInView.userData)) {
                    // Logique pour dormir
                    Gdx.app.log("Auberge", "Le joueur va dormir");
                    gameClock.sleepUntilNextMorning();
                    player.heal(player.getMaxHealth()); // Soin complet
                    return true;
                }
            }
        }

        return playerController.keyDown(keycode);
    }

    private void toggleDialog(String dialogType) {
        inDialog = !inDialog;
        dialogTable.setVisible(inDialog);
        Gdx.input.setCursorCatched(!inDialog);

        if (inDialog && "Innkeeper".equals(dialogType)) {
            setupInnkeeperDialog();
        } else {
            dialogTable.clear();
        }
    }

    private void toggleInventory() {
        isInventoryOpen = !isInventoryOpen;
        inventoryTable.setVisible(isInventoryOpen);
        Gdx.input.setCursorCatched(!isInventoryOpen);
        if (isInventoryOpen) {
            drawInventory();
        }
    }

    private void togglePause() {
        isPaused = !isPaused;
        pauseTable.setVisible(isPaused);
        Gdx.input.setCursorCatched(!isPaused);
    }

    private void setupInnkeeperDialog() {
        dialogTable.clear();
        dialogTable.add("Bienvenue à La Pinte qui Chante ! Que désirez-vous ?").row();

        TextButton foodButton = new TextButton("Manger un repas (5 Po)", skin);
        dialogTable.add(foodButton).pad(10).row();
        foodButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("Auberge", "Le joueur mange un repas.");
                player.heal(20); // Soigne un peu
                toggleDialog(null);
            }
        });

        TextButton drinkButton = new TextButton("Boire une bière (2 Po)", skin);
        dialogTable.add(drinkButton).pad(10).row();
        drinkButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("Auberge", "Le joueur boit une bière.");
                toggleDialog(null);
            }
        });

        TextButton leaveButton = new TextButton("Partir", skin);
        dialogTable.add(leaveButton).pad(10).row();
        leaveButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                toggleDialog(null);
            }
        });
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
    }
}
