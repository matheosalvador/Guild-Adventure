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
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Guild;
import com.lucas.guild.model.Inventaire;
import com.lucas.guild.model.Item;
import com.lucas.guild.model.Quest;

import java.util.List;

public class GuildScreen extends InputAdapter implements Screen {

    private final MainGame game;
    private final String difficulty;
    private Adventurer player;
    private Guild guild;

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

    private Stage stage;
    private Skin skin;
    private Table dialogTable;
    private Table inventoryTable;
    private Table pauseTable;
    private Table questTable;
    private boolean inDialog = false;
    private boolean isInventoryOpen = false;
    private boolean isPaused = false;
    private boolean isQuestBoardOpen = false;
    private int selectedQuestIndex = 0;

    public GuildScreen(MainGame game, String difficulty) {
        this.game = game;
        this.difficulty = difficulty;
    }

    @Override
    public void show() {
        Bullet.init();
        player = new Adventurer("Lucas", "Guilde");
        guild = new Guild();
        Inventaire inventaire = new Inventaire();
        inventaire.setPoidsMax(difficulty);
        player.setInventory(inventaire);

        setup3DEnvironment();
        setupPhysics();
        setupUI();
        setupInput();

        createGuildHall();

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

        questTable = new Table(skin);
        questTable.setFillParent(true);
        questTable.setVisible(false);
        stage.addActor(questTable);

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

    private void createGuildHall() {
        ModelBuilder modelBuilder = new ModelBuilder();
        
        Material floorMaterial = new Material(ColorAttribute.createDiffuse(Color.BROWN));
        Model floorModel = modelBuilder.createBox(30f, 1f, 30f, floorMaterial, Usage.Position | Usage.Normal);
        disposables.add(floorModel);
        ModelInstance floorInstance = new ModelInstance(floorModel);
        instances.add(floorInstance);
        addStaticBody(floorInstance, new btBoxShape(new Vector3(15f, 0.5f, 15f)), null);

        Material wallMaterial = new Material(ColorAttribute.createDiffuse(Color.TAN));
        Model wallModel = modelBuilder.createBox(1f, 10f, 30f, wallMaterial, Usage.Position | Usage.Normal);
        disposables.add(wallModel);
        
        ModelInstance wall1 = new ModelInstance(wallModel);
        wall1.transform.setTranslation(15f, 5f, 0);
        instances.add(wall1);
        addStaticBody(wall1, new btBoxShape(new Vector3(0.5f, 5f, 15f)), null);

        ModelInstance wall2 = new ModelInstance(wallModel);
        wall2.transform.setTranslation(-15f, 5f, 0);
        instances.add(wall2);
        addStaticBody(wall2, new btBoxShape(new Vector3(0.5f, 5f, 15f)), null);

        Model wallModel2 = modelBuilder.createBox(30f, 10f, 1f, wallMaterial, Usage.Position | Usage.Normal);
        disposables.add(wallModel2);

        ModelInstance wall3 = new ModelInstance(wallModel2);
        wall3.transform.setTranslation(0, 5f, 15f);
        instances.add(wall3);
        addStaticBody(wall3, new btBoxShape(new Vector3(15f, 5f, 0.5f)), null);

        Material doorMaterial = new Material(ColorAttribute.createDiffuse(Color.YELLOW));
        Model doorModel = modelBuilder.createBox(4f, 8f, 0.5f, doorMaterial, Usage.Position | Usage.Normal);
        disposables.add(doorModel);
        ModelInstance doorInstance = new ModelInstance(doorModel);
        doorInstance.transform.setTranslation(0, 4.5f, -14.75f);
        instances.add(doorInstance);
        addStaticBody(doorInstance, new btBoxShape(new Vector3(2f, 4f, 0.25f)), "Door");

        Material npcMaterial = new Material(ColorAttribute.createDiffuse(Color.ROYAL));
        Model npcModel = modelBuilder.createBox(1f, 2f, 1f, npcMaterial, Usage.Position | Usage.Normal);
        disposables.add(npcModel);
        ModelInstance npcInstance = new ModelInstance(npcModel);
        npcInstance.transform.setTranslation(0, 1.5f, 10f);
        instances.add(npcInstance);
        addStaticBody(npcInstance, new btBoxShape(new Vector3(0.5f, 1f, 0.5f)), "GuildMaster");
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

        Gdx.gl.glClearColor(0.2f, 0.15f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(camera);
        modelBatch.render(instances, environment);
        modelBatch.end();

        spriteBatch.begin();
        drawHud();
        spriteBatch.end();

        if (inDialog || isInventoryOpen || isPaused || isQuestBoardOpen) {
            stage.act(delta);
            stage.draw();
        }
    }

    private void update(float delta) {
        if (!inDialog && !isInventoryOpen && !isPaused && !isQuestBoardOpen) {
            playerController.update();
            dynamicsWorld.stepSimulation(delta, 5, 1 / 60f);
            playerController.updateCamera();
        }
    }

    private void drawHud() {
        if (!inDialog && !isInventoryOpen && !isPaused && !isQuestBoardOpen) {
            font.draw(spriteBatch, "+", Gdx.graphics.getWidth() / 2f - 5, Gdx.graphics.getHeight() / 2f + 5);

            btCollisionObject objectInView = playerController.getBodyInView(5f);
            if (objectInView != null) {
                if ("GuildMaster".equals(objectInView.userData)) {
                    String text = "Parler au Maître de Guilde (E)";
                    layout.setText(font, text);
                    font.draw(spriteBatch, layout, Gdx.graphics.getWidth() / 2f - layout.width / 2f, Gdx.graphics.getHeight() / 2f - 30);
                } else if ("Door".equals(objectInView.userData)) {
                    String text = "Sortir (E)";
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

    private void drawQuestBoard() {
        questTable.clear();
        questTable.add("--- Tableau des Quêtes ---").row();
        List<Quest> quests = guild.getQuestsForRank(player.getRank());
        if (quests.isEmpty()) {
            questTable.add("Aucune quête disponible pour votre rang.").row();
        } else {
            for (int i = 0; i < quests.size(); i++) {
                final Quest quest = quests.get(i);
                String buttonText = (i == selectedQuestIndex) ? "> " + quest.getTitle() : quest.getTitle();
                TextButton questButton = new TextButton(buttonText, skin);
                questButton.addListener(new ClickListener() {
                    @Override
                    public void clicked(InputEvent event, float x, float y) {
                        Gdx.app.log("QUETE", quest.toString());
                    }
                });
                questTable.add(questButton).pad(5).row();
            }
        }
        TextButton backButton = new TextButton("Retour", skin);
        backButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                toggleQuestBoard();
                toggleDialog("GuildMaster");
            }
        });
        questTable.add(backButton).pad(10).row();
    }

    @Override
    public boolean keyDown(int keycode) {
        if (isQuestBoardOpen) {
            List<Quest> quests = guild.getQuestsForRank(player.getRank());
            if (keycode == Input.Keys.UP) {
                selectedQuestIndex = Math.max(0, selectedQuestIndex - 1);
                drawQuestBoard();
                return true;
            }
            if (keycode == Input.Keys.DOWN) {
                selectedQuestIndex = Math.min(quests.size() - 1, selectedQuestIndex + 1);
                drawQuestBoard();
                return true;
            }
            if (keycode == Input.Keys.ENTER) {
                if (!quests.isEmpty() && selectedQuestIndex < quests.size()) {
                    Gdx.app.log("QUETE", quests.get(selectedQuestIndex).toString());
                }
                return true;
            }
        }

        if (keycode == Input.Keys.ESCAPE) {
            if (isInventoryOpen) {
                toggleInventory();
            } else if (inDialog) {
                toggleDialog(null);
            } else if (isQuestBoardOpen) {
                toggleQuestBoard();
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
                if ("GuildMaster".equals(objectInView.userData)) {
                    toggleDialog("GuildMaster");
                    return true;
                } else if ("Door".equals(objectInView.userData)) {
                    game.setScreen(new TownScreen(game, difficulty, new Vector3(-20, 2, 5)));
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

        if (inDialog && "GuildMaster".equals(dialogType)) {
            setupGuildMasterDialog();
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

    private void toggleQuestBoard() {
        isQuestBoardOpen = !isQuestBoardOpen;
        questTable.setVisible(isQuestBoardOpen);
        Gdx.input.setCursorCatched(!isQuestBoardOpen);
        if (isQuestBoardOpen) {
            selectedQuestIndex = 0;
            drawQuestBoard();
        }
    }

    private void setupGuildMasterDialog() {
        dialogTable.clear();
        dialogTable.add("Bienvenue à la guilde, aventurier. Que puis-je faire pour vous ?").row();

        TextButton questsButton = new TextButton("Voir les quêtes", skin);
        dialogTable.add(questsButton).pad(10).row();
        questsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                toggleDialog(null);
                toggleQuestBoard();
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
