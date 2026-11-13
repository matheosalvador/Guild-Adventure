package com.lucas.guild.libgdx;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
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
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionDispatcher;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btDbvtBroadphase;
import com.badlogic.gdx.physics.bullet.collision.btDefaultCollisionConfiguration;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.dynamics.btSequentialImpulseConstraintSolver;
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
import com.lucas.guild.game.GameSession;
import com.lucas.guild.game.SaveManager;
import com.lucas.guild.game.DebugMode;
import com.lucas.guild.game.Shop;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.GameClock;

/**
 * Base commune de tous les écrans « monde 3D » (ville et intérieurs de bâtiments).
 *
 * Elle regroupe ce qui était dupliqué dans chaque écran : caméra, éclairage, physique Bullet,
 * HUD, menu pause, inventaire, dialogues, boucle de rendu et libération des ressources.
 * Un écran concret ne définit plus que ce qui lui est propre :
 * <ul>
 *   <li>{@link #buildWorld()} : le décor 3D ;</li>
 *   <li>{@link #getSpawnPoint()} : où apparaît le joueur ;</li>
 *   <li>{@link #getInteractionPrompt(Object)} / {@link #onInteract(Object)} : ce qu'on peut viser et utiliser avec E ;</li>
 *   <li>{@link #buildDialog(String)} : le contenu des dialogues.</li>
 * </ul>
 */
public abstract class BaseWorldScreen extends InputAdapter implements Screen {

    protected final MainGame game;
    protected final String difficulty;

    // État de la partie (vient de la session en mémoire)
    protected Adventurer player;
    protected GameClock gameClock;
    protected SaveManager saveManager;

    // 3D
    protected PerspectiveCamera camera;
    protected ModelBatch modelBatch;
    protected Environment environment;
    /** Lumière ambiante et lumière directionnelle (soleil) : modifiables à chaque image par {@link #updateEnvironment()}. */
    protected ColorAttribute ambientLight;
    protected DirectionalLight sunLight;
    protected PlayerController playerController;

    // Physique
    protected btDefaultCollisionConfiguration collisionConfig;
    protected btCollisionDispatcher dispatcher;
    protected btDbvtBroadphase broadphase;
    protected btSequentialImpulseConstraintSolver solver;
    protected btDiscreteDynamicsWorld dynamicsWorld;

    protected final Array<Disposable> disposables = new Array<>();
    protected final Array<ModelInstance> instances = new Array<>();
    private final Map<String, Model> boxModels = new HashMap<>();

    // HUD
    protected SpriteBatch spriteBatch;
    protected BitmapFont font;
    protected final GlyphLayout layout = new GlyphLayout();
    private final StringBuilder hudText = new StringBuilder();

    // Message d'événement affiché quelques secondes (saison, anniversaire, réveil...)
    private static final float TOAST_SECONDS = 5f;
    private static final float TOAST_MIN_SECONDS = 2f; // durée minimale quand un autre message attend
    private String toast;
    private float toastTimer;

    // Interface (menus)
    protected Stage stage;
    protected Skin skin;
    protected Table dialogTable;
    protected Table inventoryTable;
    private InventoryPanel inventoryPanel;
    protected Table skillsTable;
    private SkillsPanel skillsPanel;
    protected Table pauseTable;
    protected boolean inDialog = false;
    protected boolean isInventoryOpen = false;
    protected boolean isSkillsOpen = false;
    protected Table debugTable;
    private DebugPanel debugPanel;
    protected boolean isDebugOpen = false;

    // Dernière position du joueur, pour mesurer la distance marchée (compétences Endurance et Portage)
    private float lastX;
    private float lastZ;
    private boolean hasLastPosition = false;
    protected boolean isPaused = false;

    protected BaseWorldScreen(MainGame game, String difficulty) {
        this.game = game;
        this.difficulty = difficulty;
    }

    // ================================================================== à définir par chaque écran

    /** Construit le décor 3D (sol, murs, PNJ...) avec {@link #addBox}. */
    protected abstract void buildWorld();

    /** Position d'apparition du joueur. */
    protected abstract Vector3 getSpawnPoint();

    /** Couleur de fond (ciel). */
    protected abstract Color getBackgroundColor();

    /**
     * Texte affiché quand le joueur vise {@code userData} (« Parler au forgeron (E) »...),
     * ou {@code null} s'il n'y a rien à afficher.
     */
    protected abstract String getInteractionPrompt(Object userData);

    /** Action de la touche E sur {@code userData}. @return true si l'interaction a été gérée. */
    protected abstract boolean onInteract(Object userData);

    // ================================================================== à surcharger si besoin

    /** Contenu d'un dialogue. {@code dialogTable} est déjà vidée. */
    protected void buildDialog(String dialogType) {
    }

    /** Intensité de la lumière ambiante. */
    protected float getAmbientLight() {
        return 0.6f;
    }

    /** Distance max (en mètres) à laquelle on peut viser / utiliser quelque chose. */
    protected float getInteractionRange() {
        return 5f;
    }

    /** Un menu supplémentaire est-il ouvert ? (ex. tableau des quêtes) Il bloque le jeu comme la pause. */
    protected boolean isExtraOverlayOpen() {
        return false;
    }

    /** Ferme le menu supplémentaire ouvert (appelé par Échap). @return true s'il y en avait un. */
    protected boolean closeExtraOverlay() {
        return false;
    }

    /** Touches propres au menu supplémentaire, traitées avant tout le reste. @return true si consommée. */
    protected boolean onOverlayKeyDown(int keycode) {
        return false;
    }

    /** Touches toujours actives, même en pause ou inventaire ouvert (ex. sauvegarde rapide). */
    protected boolean onGlobalKeyDown(int keycode) {
        return false;
    }

    /** Appelé à chaque image avant le dessin : permet de faire varier l'éclairage (ex. jour et nuit en ville). */
    protected void updateEnvironment() {
    }

    /** Ajoute des acteurs à l'interface, après les menus de base. */
    protected void setupExtraUI() {
    }

    // ================================================================== cycle de vie

    @Override
    public void show() {
        Bullet.init();

        // L'état de la partie vient de la session en mémoire (jamais relu depuis le disque ici)
        saveManager = new SaveManager();
        GameSession session = game.getSession();
        player = session.getPlayer();
        gameClock = session.getClock();

        setup3DEnvironment();
        setupPhysics();
        setupUI();
        setupInput();

        buildWorld();

        playerController = new PlayerController(camera, dynamicsWorld, player, getSpawnPoint());
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

        float ambient = getAmbientLight();
        environment = new Environment();
        ambientLight = new ColorAttribute(ColorAttribute.AmbientLight, ambient, ambient, ambient, 1f);
        environment.set(ambientLight);
        sunLight = new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f);
        environment.add(sunLight);

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
        skin.add("default", new Label.LabelStyle(font, Color.WHITE));

        dialogTable = new Table(skin);
        dialogTable.setFillParent(true);
        dialogTable.setVisible(false);
        stage.addActor(dialogTable);

        inventoryTable = new Table(skin);
        inventoryTable.setFillParent(true);
        inventoryTable.setVisible(false);
        stage.addActor(inventoryTable);
        inventoryPanel = new InventoryPanel(inventoryTable, skin, player);

        skillsTable = new Table(skin);
        skillsTable.setFillParent(true);
        skillsTable.setVisible(false);
        stage.addActor(skillsTable);

        // Fond sombre translucide derrière les panneaux : le texte blanc reste lisible sur le ciel clair
        Pixmap shade = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        shade.setColor(0f, 0f, 0f, 0.7f);
        shade.fill();
        Texture shadeTexture = new Texture(shade);
        shade.dispose();
        disposables.add(shadeTexture);
        Drawable panelBackground = new TextureRegionDrawable(new TextureRegion(shadeTexture));
        dialogTable.setBackground(panelBackground);
        inventoryTable.setBackground(panelBackground);
        skillsTable.setBackground(panelBackground);
        skillsPanel = new SkillsPanel(skillsTable, skin, player, gameClock);

        debugTable = new Table(skin);
        debugTable.setFillParent(true);
        debugTable.setVisible(false);
        stage.addActor(debugTable);
        debugTable.setBackground(panelBackground);
        debugPanel = new DebugPanel(debugTable, skin, game, game.getSession(), difficulty);

        // Menu pause
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

        setupExtraUI();
    }

    private void setupInput() {
        InputMultiplexer multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(stage);
        multiplexer.addProcessor(this);
        Gdx.input.setInputProcessor(multiplexer);
        Gdx.input.setCursorCatched(true);
    }

    // ================================================================== construction du décor

    /**
     * Ajoute une boîte (visible + solide) au décor.
     *
     * @param w largeur (X)
     * @param h hauteur (Y)
     * @param d profondeur (Z)
     * @param x position du centre
     * @param userData ce qu'on obtient en visant la boîte (null pour du décor inerte)
     * @return l'instance 3D créée
     */
    protected ModelInstance addBox(float w, float h, float d, Color color, float x, float y, float z, Object userData) {
        ModelInstance instance = new ModelInstance(boxModel(w, h, d, color));
        instance.transform.setTranslation(x, y, z);
        instances.add(instance);

        btBoxShape shape = new btBoxShape(new Vector3(w / 2f, h / 2f, d / 2f));
        disposables.add(shape);
        btRigidBody.btRigidBodyConstructionInfo info = new btRigidBody.btRigidBodyConstructionInfo(0, null, shape, Vector3.Zero);
        disposables.add(info);
        btRigidBody body = new btRigidBody(info);
        body.setWorldTransform(instance.transform);
        body.userData = userData;
        dynamicsWorld.addRigidBody(body);

        return instance;
    }

    /** Modèle partagé par toutes les boîtes de même taille et couleur. */
    private Model boxModel(float w, float h, float d, Color color) {
        String key = w + "x" + h + "x" + d + "/" + color.toIntBits();
        Model model = boxModels.get(key);
        if (model == null) {
            Material material = new Material(ColorAttribute.createDiffuse(color));
            model = new ModelBuilder().createBox(w, h, d, material, Usage.Position | Usage.Normal);
            disposables.add(model);
            boxModels.put(key, model);
        }
        return model;
    }

    // ================================================================== boucle de jeu

    @Override
    public void render(float delta) {
        update(delta);
        updateEnvironment();

        Color bg = getBackgroundColor();
        Gdx.gl.glClearColor(bg.r, bg.g, bg.b, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        modelBatch.begin(camera);
        modelBatch.render(instances, environment);
        modelBatch.end();

        spriteBatch.begin();
        drawHud(delta);
        spriteBatch.end();

        if (isOverlayOpen()) {
            stage.act(delta);
            stage.draw();
        }
    }

    /** Vrai quand un menu (pause, inventaire, dialogue, menu supplémentaire) bloque le jeu. */
    protected boolean isOverlayOpen() {
        return inDialog || isInventoryOpen || isSkillsOpen || isDebugOpen || isPaused || isExtraOverlayOpen();
    }

    private void update(float delta) {
        if (!isOverlayOpen()) {
            playerController.update();
            dynamicsWorld.stepSimulation(delta, 5, 1 / 60f);
            playerController.updateCamera();
            gameClock.update(delta);
            trainWalking();
        }
    }

    /** La marche entraîne l'Endurance, et le Portage quand on porte au moins la moitié de sa charge maximale. */
    private void trainWalking() {
        Vector3 position = playerController.getPlayerBody().getCenterOfMassPosition();
        float x = position.x;
        float z = position.z;
        if (hasLastPosition) {
            float dx = x - lastX;
            float dz = z - lastZ;
            float distance = (float) Math.sqrt(dx * dx + dz * dz);
            // Un grand saut d'une image à l'autre (apparition, téléportation) n'est pas de la marche
            if (distance > 0.001f && distance < 2f) {
                player.train("athletics", distance);
                if (player.getInventaire().getPoidsActuel() >= 0.5 * player.getInventaire().getPoidsMax()) {
                    player.train("carrying", distance);
                }
            }
        }
        lastX = x;
        lastZ = z;
        hasLastPosition = true;
    }

    private void drawHud(float delta) {
        if (isOverlayOpen()) {
            return;
        }
        float w = Gdx.graphics.getWidth();
        float h = Gdx.graphics.getHeight();

        font.draw(spriteBatch, "+", w / 2f - 5, h / 2f + 5);

        hudText.setLength(0);
        hudText.append("FPS: ").append(Gdx.graphics.getFramesPerSecond()).append("\n");
        hudText.append("Player: ").append(player.getName())
                .append(" (").append(player.getAge(gameClock.getCurrentDay())).append(" ans)\n");
        hudText.append("HP: ").append(player.getHealth()).append(" / ").append(player.getMaxHealth()).append("\n");
        hudText.append(gameClock.getDate()).append(" - ").append(gameClock.formatTime()).append("\n");
        if (DebugMode.isEnabled()) {
            com.badlogic.gdx.math.Vector3 p = playerController.getPlayerBody().getCenterOfMassPosition();
            hudText.append(String.format("[DEBUG] x %.1f  y %.1f  z %.1f  jour %d  temps x%.0f  vitesse x%.0f  (F10)\n",
                    p.x, p.y, p.z, gameClock.getCurrentDay(), gameClock.getTimeScale(), DebugMode.getSpeedMultiplier()));
        }
        hudText.append("Or : ").append(player.getGold()).append(" Po - ").append(UiKit.loadLine(player));
        font.draw(spriteBatch, hudText, 10, h - 10);

        font.draw(spriteBatch, "I : inventaire   K : personnage   Échap : pause", 10, 24);

        drawToast(delta, w, h);

        btCollisionObject objectInView = playerController.getBodyInView(getInteractionRange());
        if (objectInView != null) {
            String text = getInteractionPrompt(objectInView.userData);
            if (text != null) {
                layout.setText(font, text);
                font.draw(spriteBatch, layout, w / 2f - layout.width / 2f, h / 2f - 30);
            }
        }
    }

    /** Affiche le prochain message d'événement en haut de l'écran pendant quelques secondes. */
    private void drawToast(float delta, float w, float h) {
        if (toast != null) {
            toastTimer -= delta;
            // Si d'autres messages attendent, on ne garde pas celui-ci les 5 s complètes : un refus
            // ("Pas assez d'or") doit apparaître vite, pas après le message précédent.
            boolean shownLongEnough = toastTimer <= TOAST_SECONDS - TOAST_MIN_SECONDS;
            if (toastTimer <= 0f || (shownLongEnough && gameClock.hasPendingMessage())) {
                toast = null;
            }
        }
        if (toast == null) {
            toast = gameClock.pollMessage(); // un message arrivé est affiché tout de suite
            toastTimer = TOAST_SECONDS;
        }
        if (toast != null) {
            layout.setText(font, toast);
            font.draw(spriteBatch, layout, w / 2f - layout.width / 2f, h - 20);
        }
    }

    private void drawInventory() {
        inventoryPanel.rebuild(null);
    }

    /**
     * Ouvre la boutique {@code shopId} (shops.json) dans la fenêtre de dialogue en cours.
     *
     * @param onBack appelé par « Retour » : en général, réafficher le dialogue du PNJ
     */
    protected void showShop(String shopId, Runnable onBack) {
        new ShopPanel(dialogTable, skin, player, new Shop(shopId), onBack).rebuild(null);
    }

    // ================================================================== entrées clavier / souris

    @Override
    public boolean keyDown(int keycode) {
        if (onOverlayKeyDown(keycode)) {
            return true;
        }

        if (keycode == Input.Keys.ESCAPE) {
            if (isDebugOpen) {
                toggleDebug();
            } else if (isSkillsOpen) {
                toggleSkills();
            } else if (isInventoryOpen) {
                toggleInventory();
            } else if (inDialog) {
                toggleDialog(null);
            } else if (!closeExtraOverlay()) {
                togglePause();
            }
            return true;
        }
        if (keycode == Input.Keys.F10 && DebugMode.isEnabled()) {
            if (isDebugOpen || !isOverlayOpen()) {
                toggleDebug();
            }
            return true;
        }
        if (keycode == Input.Keys.I) {
            if (!isSkillsOpen) {
                toggleInventory();
            }
            return true;
        }
        if (keycode == Input.Keys.K) {
            // le panneau du personnage ne s'ouvre que depuis le jeu (pas par-dessus un autre menu)
            if (isSkillsOpen || !(inDialog || isInventoryOpen || isPaused || isExtraOverlayOpen())) {
                toggleSkills();
            }
            return true;
        }

        if (onGlobalKeyDown(keycode)) {
            return true;
        }

        if (isOverlayOpen()) {
            return false;
        }

        if (keycode == Input.Keys.E) {
            btCollisionObject objectInView = playerController.getBodyInView(getInteractionRange());
            if (objectInView != null && onInteract(objectInView.userData)) {
                return true;
            }
        }

        return playerController.keyDown(keycode);
    }

    @Override
    public boolean keyUp(int keycode) {
        return playerController.keyUp(keycode);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return playerController.touchDown(screenX, screenY, pointer, button);
    }

    // ================================================================== menus

    protected void togglePause() {
        isPaused = !isPaused;
        pauseTable.setVisible(isPaused);
        Gdx.input.setCursorCatched(!isPaused);
    }

    protected void toggleInventory() {
        isInventoryOpen = !isInventoryOpen;
        inventoryTable.setVisible(isInventoryOpen);
        Gdx.input.setCursorCatched(!isInventoryOpen);
        if (isInventoryOpen) {
            drawInventory();
        }
    }

    protected void toggleDebug() {
        isDebugOpen = !isDebugOpen;
        debugTable.setVisible(isDebugOpen);
        Gdx.input.setCursorCatched(!isDebugOpen);
        if (isDebugOpen) {
            debugPanel.rebuild(null);
        }
    }

    protected void toggleSkills() {
        isSkillsOpen = !isSkillsOpen;
        skillsTable.setVisible(isSkillsOpen);
        Gdx.input.setCursorCatched(!isSkillsOpen);
        if (isSkillsOpen) {
            skillsPanel.rebuild();
        }
    }

    /** Ouvre le dialogue {@code dialogType}, ou ferme le dialogue en cours. */
    protected void toggleDialog(String dialogType) {
        inDialog = !inDialog;
        dialogTable.setVisible(inDialog);
        Gdx.input.setCursorCatched(!inDialog);

        dialogTable.clear();
        if (inDialog) {
            buildDialog(dialogType);
        }
    }

    /** Ajoute un bouton au dialogue en cours. */
    protected TextButton addDialogButton(String label, final Runnable action) {
        TextButton button = new TextButton(label, skin);
        dialogTable.add(button).pad(10).row();
        button.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                action.run();
            }
        });
        return button;
    }

    /** Ajoute le bouton « Partir » qui ferme le dialogue. */
    protected void addLeaveButton() {
        addDialogButton("Partir", new Runnable() {
            @Override
            public void run() {
                toggleDialog(null);
            }
        });
    }

    // ================================================================== fin de vie

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
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        playerController.dispose();
        for (Disposable disposable : disposables) {
            disposable.dispose();
        }
        disposables.clear();
        instances.clear();
        boxModels.clear();
        skin.dispose();
    }
}
