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
import com.lucas.guild.model.Skill;

public class ForgeScreen extends InputAdapter implements Screen {

    private final MainGame game;
    private Adventurer player;

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
    private boolean inDialog = false;

    public ForgeScreen(MainGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        Bullet.init();
        player = new Adventurer("Lucas", "Caserne");
        // Pour le test, on apprend la compétence "Attaque Puissante"
        for (Skill skill : player.getSkillTree().getSkills()) {
            if ("Attaque Puissante".equals(skill.getName())) {
                skill.learn();
                break;
            }
        }


        setup3DEnvironment();
        setupPhysics();
        setupUI();
        setupInput();

        createForge();

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
    }

    private void setupInput() {
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(stage);
        multiplexer.addProcessor(this);
        Gdx.input.setInputProcessor(multiplexer);
        Gdx.input.setCursorCatched(true);
    }

    private void createForge() {
        ModelBuilder modelBuilder = new ModelBuilder();
        
        // Sol
        Material floorMaterial = new Material(ColorAttribute.createDiffuse(Color.DARK_GRAY));
        Model floorModel = modelBuilder.createBox(20f, 1f, 20f, floorMaterial, Usage.Position | Usage.Normal);
        disposables.add(floorModel);
        ModelInstance floorInstance = new ModelInstance(floorModel);
        instances.add(floorInstance);
        addStaticBody(floorInstance, new btBoxShape(new Vector3(10f, 0.5f, 10f)), null);

        // Murs
        Material wallMaterial = new Material(ColorAttribute.createDiffuse(Color.GRAY));
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

        // Porte
        Material doorMaterial = new Material(ColorAttribute.createDiffuse(Color.YELLOW));
        Model doorModel = modelBuilder.createBox(4f, 8f, 0.5f, doorMaterial, Usage.Position | Usage.Normal);
        disposables.add(doorModel);
        ModelInstance doorInstance = new ModelInstance(doorModel);
        doorInstance.transform.setTranslation(0, 4.5f, -9.75f);
        instances.add(doorInstance);
        addStaticBody(doorInstance, new btBoxShape(new Vector3(2f, 4f, 0.25f)), "Door");

        // Forgeron (PNJ)
        Material npcMaterial = new Material(ColorAttribute.createDiffuse(Color.GREEN));
        Model npcModel = modelBuilder.createBox(1f, 4f, 1f, npcMaterial, Usage.Position | Usage.Normal);
        disposables.add(npcModel);
        ModelInstance npcInstance = new ModelInstance(npcModel);
        npcInstance.transform.setTranslation(2, 2.5f, 0);
        instances.add(npcInstance);
        addStaticBody(npcInstance, new btBoxShape(new Vector3(0.5f, 2f, 0.5f)), "Blacksmith");
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

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(camera);
        modelBatch.render(instances, environment);
        modelBatch.end();

        spriteBatch.begin();
        drawHud();
        spriteBatch.end();

        if (inDialog) {
            stage.act(delta);
            stage.draw();
        }
    }

    private void update(float delta) {
        if (!inDialog) {
            playerController.update();
            dynamicsWorld.stepSimulation(delta, 5, 1 / 60f);
            playerController.updateCamera();
        }
    }

    private void drawHud() {
        if (!inDialog) {
            font.draw(spriteBatch, "+", Gdx.graphics.getWidth() / 2f - 5, Gdx.graphics.getHeight() / 2f + 5);

            btCollisionObject objectInView = playerController.getBodyInView(5f);
            if (objectInView != null) {
                if ("Blacksmith".equals(objectInView.userData)) {
                    String text = "Parler au forgeron (E)";
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

    @Override
    public boolean keyDown(int keycode) {
        if (inDialog) {
            if (keycode == Input.Keys.ESCAPE) {
                toggleDialog(null);
            }
            return true;
        }

        if (keycode == Input.Keys.E) {
            btCollisionObject objectInView = playerController.getBodyInView(5f);
            if (objectInView != null) {
                if ("Blacksmith".equals(objectInView.userData)) {
                    toggleDialog("Blacksmith");
                    return true;
                } else if ("Door".equals(objectInView.userData)) {
                    game.setScreen(new TownScreen(game));
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

        if (inDialog && "Blacksmith".equals(dialogType)) {
            setupBlacksmithDialog();
        } else {
            dialogTable.clear();
        }
    }

    private void setupBlacksmithDialog() {
        dialogTable.clear();
        dialogTable.add("Que puis-je faire pour vous ?").row();

        TextButton buyButton = new TextButton("Forger un objet (10 Po)", skin);
        dialogTable.add(buyButton).pad(10).row();
        buyButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.log("Forge", "Achat d'un objet...");
                toggleDialog(null);
            }
        });

        if (player.hasLearnedSkill("Attaque Puissante")) { // On vérifie une compétence de base pour le test
            TextButton craftButton = new TextButton("Utiliser l'enclume", skin);
            dialogTable.add(craftButton).pad(10).row();
            craftButton.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    Gdx.app.log("Forge", "Utilisation de l'enclume...");
                    toggleDialog(null);
                }
            });
        }

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
