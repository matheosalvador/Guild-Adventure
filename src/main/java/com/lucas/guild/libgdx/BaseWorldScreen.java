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
import com.badlogic.gdx.graphics.g3d.environment.DirectionalShadowLight;
import com.badlogic.gdx.graphics.g3d.utils.DepthShaderProvider;
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
import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.LoreData;
import com.lucas.guild.game.GroundItems;
import com.lucas.guild.game.Lore;
import com.lucas.guild.game.WorldEvents;
import com.lucas.guild.model.ActiveEvent;
import com.lucas.guild.model.Arme;
import com.lucas.guild.model.Armure;
import com.lucas.guild.model.Butin;
import com.lucas.guild.model.DayNightCycle;
import com.lucas.guild.model.Item;
import com.lucas.guild.model.ObjetDeQuete;
import com.lucas.guild.model.Sac;
import com.lucas.guild.game.SaveManager;
import com.lucas.guild.game.DeathPenalty;
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
    /** Le ciel (astre, étoiles, nuages) : dessiné sans ombres, hors de la liste du décor. */
    private final Array<ModelInstance> skyInstances = new Array<>();
    private SkyDome skyDome;
    private float skyClock;
    private DirectionalShadowLight shadowLight;
    private ModelBatch shadowBatch;
    private final Map<String, Model> boxModels = new HashMap<>();
    private final Map<GroundItems.Drop, ModelInstance> groundViews = new HashMap<>();

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
    protected Table journalTable;
    private JournalPanel journalPanel;
    protected boolean isJournalOpen = false;
    /** Les monstres de l'écran (aucun dans la plupart des lieux). */
    protected CombatScene combat;
    protected Table debugTable;
    private DebugPanel debugPanel;
    protected boolean isDebugOpen = false;

    private boolean deathPending = false;
    /** Le lieu affiché sur le HUD (le nom de la ville ou de la zone). */
    protected String townName = "";

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

    /**
     * Contenu d'un dialogue. {@code dialogTable} est déjà vidée. Les écrans qui le redéfinissent
     * peuvent appeler {@code super.buildDialog(dialogType)} pour garder la lecture des textes.
     */
    protected void buildDialog(String dialogType) {
        if (dialogType != null && dialogType.startsWith(TEXT_DIALOG)) {
            LoreData.TextData text = GameContent.lore().texts.get(dialogType.substring(TEXT_DIALOG.length()));
            if (text != null) {
                dialogTable.add(UiKit.label(skin, text.title)).pad(10).row();
                addWrappedText(text.text);
                addLeaveButton();
            }
        }
    }

    /** Un texte à lire (panneau, stèle, livre) : ce qu'on obtient en visant une partie de décor d'action « read ». */
    protected static final class Readable {
        final String textId;

        Readable(String textId) {
            this.textId = textId;
        }
    }

    private static final String TEXT_DIALOG = "Text:";

    /** Ouvre la lecture d'un texte de lore.json ; la première lecture l'ajoute au journal. */
    private void showText(String textId) {
        LoreData.TextData text = GameContent.lore().texts.get(textId);
        if (text == null) {
            return;
        }
        if (Lore.learn(player, Lore.textKey(textId))) {
            gameClock.postMessage("Ajouté au journal : " + text.title + ".");
        }
        toggleDialog(TEXT_DIALOG + textId);
    }

    /** Ajoute un paragraphe qui revient à la ligne dans le dialogue en cours. */
    protected void addWrappedText(String text) {
        Label label = UiKit.label(skin, text);
        label.setWrap(true);
        float width = Math.max(300f, Math.min(640f, Gdx.graphics.getWidth() - 100f));
        dialogTable.add(label).width(width).pad(10).row();
    }

    /** Cet écran est-il en plein air (ciel visible, ombres du soleil) ? */
    protected boolean hasSky() {
        return false;
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
        townName = com.lucas.guild.data.GameContent.world().place(player.getTownId()).name;   // lu une fois : le HUD le redessine à chaque image

        setup3DEnvironment();
        setupPhysics();
        setupUI();
        setupInput();

        combat = new CombatScene(instances);
        combat.setDropSink(this::dropOnGround);
        disposables.add(combat);
        buildWorld();
        for (GroundItems.Drop drop : game.getSession().getGround().in(sceneKey())) {
            addGroundView(drop);
        }
        WorldEvents.update(player, gameClock.getCurrentDay(), eventRandom);
        if (announcesEvents()) {
            String market = WorldEvents.describeAt(player, player.getTownId());
            if (market != null) {
                gameClock.postMessage(market);
            }
        }

        playerController = new PlayerController(camera, dynamicsWorld, player, getSpawnPoint());
        playerModel = new CharacterModel(instances, GameContent.models(), "human");
        disposables.add(playerModel);
        if (hasSky()) {
            skyDome = new SkyDome(skyInstances);
            disposables.add(skyDome);
        }
    }

    /** Le corps du joueur, visible à la 3e personne (touche V). */
    private CharacterModel playerModel;
    private final Vector3 feet = new Vector3();

    /** Habille le corps du joueur de son équipement, le place et l'anime. */
    private void updatePlayerModel(float delta) {
        com.lucas.guild.model.Inventaire inventory = player.getInventaire();
        playerModel.setLook(
                inventory.getArmor() != null ? inventory.getArmor().getId() : null,
                inventory.getWeapon() != null ? inventory.getWeapon().getId() : null,
                inventory.getBag() != null ? inventory.getBag().getId() : null);
        // contre un mur, la caméra se colle au joueur : on cache alors son corps (comme à la 1re personne)
        playerModel.setVisible(ViewSettings.thirdPerson && playerController.getAimBonus() > 0.9f);
        feet.set(playerController.getPlayerBody().getCenterOfMassPosition()).sub(0, 1.0f, 0);
        playerModel.update(delta, playerController.getHorizontalSpeed(), feet, playerController.getYaw());
        combat.setRangeBonus(playerController.getAimBonus());
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
        if (hasSky()) {
            // en plein air : le soleil porte des ombres (carte d'ombre de 70 m autour du joueur)
            shadowLight = new DirectionalShadowLight(2048, 2048, 70f, 70f, 1f, 140f);
            shadowLight.set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f);
            sunLight = shadowLight;
            environment.add(shadowLight);
            shadowBatch = new ModelBatch(new DepthShaderProvider());
            disposables.add(shadowLight);
            disposables.add(shadowBatch);
        } else {
            sunLight = new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f);
            environment.add(sunLight);
        }

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
        inventoryPanel.setDropHandler(this::dropInFront);

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

        journalTable = new Table(skin);
        journalTable.setFillParent(true);
        journalTable.setVisible(false);
        stage.addActor(journalTable);
        journalTable.setBackground(panelBackground);
        journalPanel = new JournalPanel(journalTable, skin, player);

        debugTable = new Table(skin);
        debugTable.setFillParent(true);
        debugTable.setVisible(false);
        stage.addActor(debugTable);
        debugTable.setBackground(panelBackground);
        debugPanel = new DebugPanel(debugTable, skin, game, this, game.getSession(), difficulty);

        // Menu pause
        pauseTable = new Table(skin);
        pauseTable.setFillParent(true);
        pauseTable.setVisible(false);
        stage.addActor(pauseTable);

        TextButton continueButton = new TextButton("Continuer", skin);
        TextButton menuButton = new TextButton("Retour au Menu", skin);

        final TextButton shadowsButton = new TextButton(shadowsLabel(), skin);
        shadowsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                ViewSettings.shadows = !ViewSettings.shadows;
                shadowsButton.setText(shadowsLabel());
            }
        });
        pauseTable.add(continueButton).pad(10).row();
        pauseTable.add(shadowsButton).pad(10).row();
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

    private static String shadowsLabel() {
        return "Ombres : " + (ViewSettings.shadows ? "oui" : "non");
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
        ModelInstance instance = addVisualBox(w, h, d, color, x, y, z);
        addInvisibleBody(w, h, d, x, y, z, userData);
        return instance;
    }

    /** Un volume solide qu'on ne voit pas (le corps d'un PNJ dessiné par un modèle) : on le heurte et on peut le viser. */
    protected void addInvisibleBody(float w, float h, float d, float x, float y, float z, Object userData) {
        btBoxShape shape = new btBoxShape(new Vector3(w / 2f, h / 2f, d / 2f));
        disposables.add(shape);
        btRigidBody.btRigidBodyConstructionInfo info = new btRigidBody.btRigidBodyConstructionInfo(0, null, shape, Vector3.Zero);
        disposables.add(info);
        btRigidBody body = new btRigidBody(info);
        body.setWorldTransform(new com.badlogic.gdx.math.Matrix4().setTranslation(x, y, z));
        body.userData = userData;
        dynamicsWorld.addRigidBody(body);
    }

    /** Un personnage non joueur : un modèle de models.json qui tourne la tête vers le joueur quand il approche. */
    private static final class Npc {
        final CharacterModel model;
        final float x;
        final float z;
        float yaw;

        Npc(CharacterModel model, float x, float z) {
            this.model = model;
            this.x = x;
            this.z = z;
        }
    }

    private final java.util.List<Npc> npcs = new java.util.ArrayList<>();

    /**
     * Pose un PNJ debout sur le sol en (x, z), de face (il regarde vers -Z, comme le joueur qui arrive par la
     * porte). {@code userData} est ce qu'on obtient en le visant.
     *
     * @param modelId son modèle dans models.json (« innkeeper »...)
     */
    protected void addNpc(String modelId, float x, float z, Object userData) {
        addInvisibleBody(0.9f, 2f, 0.9f, x, 1.5f, z, userData);
        CharacterModel model = new CharacterModel(instances, GameContent.models(), modelId);
        model.setVisible(true);
        disposables.add(model);
        npcs.add(new Npc(model, x, z));
    }

    private static final float NPC_NOTICE_RANGE = 9f;
    private static final float NPC_TURN_SPEED = 160f;

    /** Les PNJ respirent, et se tournent vers le joueur quand il est proche. */
    private void updateNpcs(float delta) {
        if (npcs.isEmpty()) {
            return;
        }
        Vector3 p = playerController.getPlayerBody().getCenterOfMassPosition();
        for (Npc npc : npcs) {
            float dx = p.x - npc.x;
            float dz = p.z - npc.z;
            float target = 0f;
            if (Math.hypot(dx, dz) < NPC_NOTICE_RANGE) {
                target = (float) Math.toDegrees(Math.atan2(-dx, -dz));
            }
            float diff = ((target - npc.yaw + 540f) % 360f) - 180f;
            float step = NPC_TURN_SPEED * delta;
            npc.yaw += Math.max(-step, Math.min(step, diff));
            feet.set(npc.x, 0.5f, npc.z);
            npc.model.update(delta, 0f, feet, npc.yaw);
        }
    }

    /** Une boîte seulement visible : le joueur la traverse (rues, eau, marquages au sol). */
    protected ModelInstance addVisualBox(float w, float h, float d, Color color, float x, float y, float z) {
        return addVisualShape("box", w, h, d, color, x, y, z, null);
    }

    /** Une forme seulement visible (voir {@link Shapes}), éventuellement tournée (degrés autour de x, y, z). */
    protected ModelInstance addVisualShape(String shape, float w, float h, float d, Color color, float x, float y, float z, float[] rot) {
        ModelInstance instance = new ModelInstance(shapeModel(shape, w, h, d, color));
        instance.transform.setTranslation(x, y, z);
        if (rot != null) {
            instance.transform.rotate(Vector3.Y, rot[1]).rotate(Vector3.X, rot[0]).rotate(Vector3.Z, rot[2]);
        }
        instances.add(instance);
        return instance;
    }

    /**
     * Une partie de décor d'un fichier de ville ou de zone : une boîte solide, ou une forme visuelle (toit, couronne
     * d'arbre, eau...). {@code (ox, oz)} est le centre du bâtiment pour ses détails.
     */
    protected void addDecorPart(com.lucas.guild.data.TownData.PartData part, float ox, float oz, Color color, Object userData) {
        float x = ox + part.x;
        float z = oz + part.z;
        float centerY = 0.5f + part.y + part.h / 2f;   // le sol est une boîte d'1 m dont le dessus est à 0,5
        boolean plainBox = "box".equals(part.shape) && part.rot == null;
        if (part.decal || !plainBox) {
            ModelInstance instance = addVisualShape(part.shape, part.w, part.h, part.d, color, x, centerY, z, part.rot);
            if ("water".equals(part.anim)) {
                waves.add(new Wave(instance, centerY, color));
            }
        } else {
            addBox(part.w, part.h, part.d, color, x, centerY, z, userData);
        }
    }

    /** Une surface d'eau qui ondule. */
    private static final class Wave {
        final ModelInstance instance;
        final float baseY;
        final Color base;
        final float phase;

        Wave(ModelInstance instance, float baseY, Color base) {
            this.instance = instance;
            this.baseY = baseY;
            this.base = new Color(base);
            this.phase = instance.transform.val[com.badlogic.gdx.math.Matrix4.M03] * 0.15f;
        }
    }

    private final java.util.List<Wave> waves = new java.util.ArrayList<>();
    private float waveTime;

    private void animateWater(float delta) {
        if (waves.isEmpty()) {
            return;
        }
        waveTime += delta;
        for (Wave wave : waves) {
            float s = com.badlogic.gdx.math.MathUtils.sin(waveTime * 1.1f + wave.phase);
            wave.instance.transform.val[com.badlogic.gdx.math.Matrix4.M13] = wave.baseY + 0.05f * s;
            ColorAttribute diffuse = (ColorAttribute) wave.instance.materials.first().get(ColorAttribute.Diffuse);
            float k = 1f + 0.12f * s;
            diffuse.color.set(Math.min(1f, wave.base.r * k), Math.min(1f, wave.base.g * k), Math.min(1f, wave.base.b * k), 1f);
        }
    }

    /** Appelé à chaque image après la mise à jour du jeu, même quand un menu est ouvert (animations du décor). */
    protected void afterUpdate(float delta) {
    }

    /** Modèle partagé par toutes les formes de même type, taille et couleur. */
    private Model shapeModel(String shape, float w, float h, float d, Color color) {
        String key = shape + "/" + w + "x" + h + "x" + d + "/" + color.toIntBits();
        Model model = boxModels.get(key);
        if (model == null) {
            model = Shapes.create(shape, w, h, d, new Material(ColorAttribute.createDiffuse(color)));
            disposables.add(model);
            boxModels.put(key, model);
        }
        return model;
    }

    // ================================================================== boucle de jeu

    @Override
    public void render(float delta) {
        update(delta);
        updateNpcs(delta);
        animateWater(delta);
        afterUpdate(delta);
        updateEnvironment();

        Color bg = getBackgroundColor();
        Gdx.gl.glClearColor(bg.r, bg.g, bg.b, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        skyClock += delta;
        boolean shadows = shadowLight != null && ViewSettings.shadows;
        environment.shadowMap = shadows ? shadowLight : null;
        if (shadows) {
            shadowLight.begin(playerController.getPlayerBody().getCenterOfMassPosition(), camera.direction);
            shadowBatch.begin(shadowLight.getCamera());
            shadowBatch.render(instances);
            shadowBatch.end();
            shadowLight.end();
        }

        modelBatch.begin(camera);
        modelBatch.render(instances, environment);
        modelBatch.render(skyInstances, environment);
        modelBatch.end();

        spriteBatch.begin();
        drawHud(delta);
        spriteBatch.end();

        if (isOverlayOpen()) {
            stage.act(delta);
            stage.draw();
        }

        if (deathPending) {
            deathPending = false;
            gameClock.postMessage(DeathPenalty.apply(game.getSession()));
            game.setScreen(new InnScreen(game, difficulty));
        }
    }

    /** Vrai quand un menu (pause, inventaire, dialogue, menu supplémentaire) bloque le jeu. */
    protected boolean isOverlayOpen() {
        return inDialog || isInventoryOpen || isSkillsOpen || isJournalOpen || isDebugOpen || isPaused || isExtraOverlayOpen();
    }

    private void update(float delta) {
        if (!isOverlayOpen()) {
            playerController.update();
            dynamicsWorld.stepSimulation(delta, 5, 1 / 60f);
            playerController.updateCamera();
            updatePlayerModel(delta);
            gameClock.update(delta);
            advanceWorldEvents();
            trainWalking();
            Vector3 position = playerController.getPlayerBody().getCenterOfMassPosition();
            if (combat.update(delta, player, position)) {
                onPlayerDeath();
            }
        }
    }

    // ================================================================== objets au sol

    /** Identifie ce lieu pour les objets posés au sol : les objets jetés ici sont retrouvés ici. */
    protected String sceneKey() {
        return getClass().getSimpleName() + "@" + player.getTownId();
    }

    private static Color colorOfItem(Item item) {
        if (item instanceof ObjetDeQuete) return Color.GOLD;
        if (item instanceof Butin) return Color.TAN;
        if (item instanceof Arme) return Color.LIGHT_GRAY;
        if (item instanceof Armure || item instanceof Sac) return Color.BROWN;
        if (item.estUtilisableHorsCombat()) return Color.SCARLET;
        return Color.ORANGE;
    }

    private void addGroundView(GroundItems.Drop drop) {
        groundViews.put(drop, addVisualBox(0.5f, 0.3f, 0.5f, colorOfItem(drop.item), drop.x, 0.65f, drop.z));
    }

    /** Pose un objet au sol (butin d'un monstre, objet jeté, trop-plein d'un coffre). */
    protected void dropOnGround(Item item, float x, float z) {
        addGroundView(game.getSession().getGround().drop(sceneKey(), item, x, z));
    }

    /** Le joueur jette {@code item} : il tombe devant lui, et on peut le ramasser. */
    private void dropInFront(Item item) {
        Vector3 p = playerController.getPlayerBody().getCenterOfMassPosition();
        Vector3 d = new Vector3(camera.direction.x, 0, camera.direction.z);
        if (d.isZero()) {
            d.set(0, 0, -1);
        }
        d.nor().scl(1.5f);
        dropOnGround(item, p.x + d.x, p.z + d.z);
    }

    private GroundItems.Drop nearestGroundItem() {
        Vector3 p = playerController.getPlayerBody().getCenterOfMassPosition();
        return game.getSession().getGround().nearest(sceneKey(), p.x, p.z);
    }

    /** Ramasse l'objet au sol le plus proche (E). @return false s'il n'y en a aucun à portée. */
    private boolean pickUpNearest() {
        GroundItems.Drop drop = nearestGroundItem();
        if (drop == null) {
            return false;
        }
        if (!player.addItem(drop.item)) {
            gameClock.postMessage("Trop lourd pour ramasser : " + drop.item.getName() + ".");
            return true;
        }
        game.getSession().getGround().remove(sceneKey(), drop);
        instances.removeValue(groundViews.remove(drop), true);
        gameClock.postMessage("Vous ramassez : " + drop.item.getName() + ".");
        return true;
    }

    /** Applique le ciel, le soleil (puis la lune) et la lumière ambiante de l'heure et de la saison : à appeler dans {@link #updateEnvironment()}. */
    protected void applyDayNight(Color sky) {
        DayNightCycle.Light light = DayNightCycle.compute(
                gameClock.getTimeOfDay(), gameClock.getSunrise(), gameClock.getSunset(), getAmbientLight());
        sky.set(light.sky[0], light.sky[1], light.sky[2], 1f);
        ambientLight.color.set(light.ambient[0], light.ambient[1], light.ambient[2], 1f);
        sunLight.color.set(light.lightColor[0], light.lightColor[1], light.lightColor[2], 1f);
        sunLight.direction.set(light.lightDirection[0], light.lightDirection[1], light.lightDirection[2]).nor();
        if (skyDome != null) {
            skyDome.update(camera.position, light.daylight, light.lightDirection, skyClock);
        }
    }

    private final java.util.Random eventRandom = new java.util.Random();

    /** Les événements du monde avancent avec les jours ; on prévient quand l'un d'eux démarre dans la ville où l'on se trouve. */
    private void advanceWorldEvents() {
        if (gameClock.getCurrentDay() == player.getEventsDay()) {
            return;
        }
        for (ActiveEvent event : WorldEvents.update(player, gameClock.getCurrentDay(), eventRandom)) {
            if (announcesEvents() && event.placeId.equals(player.getTownId())) {
                gameClock.postMessage(WorldEvents.describe(event));
            }
        }
    }

    /** Ce lieu prévient-il du marché en cours quand on y arrive ? (la ville, pas les intérieurs) */
    protected boolean announcesEvents() {
        return false;
    }

    /** Debug : fait apparaître un monstre à 6 m devant le joueur. */
    String debugSpawn(String monsterId) {
        Vector3 p = playerController.getPlayerBody().getCenterOfMassPosition();
        Vector3 d = new Vector3(camera.direction.x, 0, camera.direction.z);
        if (d.isZero()) {
            d.set(0, 0, -1);
        }
        d.nor().scl(6f);
        com.lucas.guild.game.Monster m = combat.spawn(monsterId, p.x + d.x, p.z + d.z);
        return m.getName() + " apparaît.";
    }

    /** Debug : retire tous les monstres. */
    String debugKillAll() {
        return combat.clearAll() + " monstre(s) retiré(s).";
    }

    /** Le joueur est tombé à 0 PV : il se réveille à l'auberge, plus pauvre (voir {@link DeathPenalty}). */
    protected void onPlayerDeath() {
        // On ne change pas d'écran en plein milieu d'une image : celui-ci serait libéré avant la fin du dessin
        deathPending = true;
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
        hudText.append(gameClock.getDate()).append(" - ").append(gameClock.formatTime())
                .append(" - ").append(townName).append("\n");
        if (DebugMode.isEnabled()) {
            com.badlogic.gdx.math.Vector3 p = playerController.getPlayerBody().getCenterOfMassPosition();
            hudText.append(String.format("[DEBUG] x %.1f  y %.1f  z %.1f  jour %d  temps x%.0f  vitesse x%.0f  (F10)\n",
                    p.x, p.y, p.z, gameClock.getCurrentDay(), gameClock.getTimeScale(), DebugMode.getSpeedMultiplier()));
        }
        hudText.append("Or : ").append(player.getGold()).append(" Po - ").append(UiKit.loadLine(player));
        font.draw(spriteBatch, hudText, 10, h - 10);

        font.draw(spriteBatch, "I : inventaire   K : personnage   J : journal   Q : potion   V : vue   Échap : pause", 10, 24);

        combat.drawHud(spriteBatch, font, w, h, camera.position, camera.direction);
        drawToast(delta, w, h);

        String text = null;
        btCollisionObject objectInView = playerController.getBodyInView(getInteractionRange());
        if (objectInView != null && objectInView.userData instanceof Readable) {
            LoreData.TextData readable = GameContent.lore().texts.get(((Readable) objectInView.userData).textId);
            text = readable != null ? "Lire : " + readable.title + " (E)" : null;
        } else if (objectInView != null) {
            text = getInteractionPrompt(objectInView.userData);
        }
        if (text == null) {
            GroundItems.Drop drop = nearestGroundItem();
            if (drop != null) {
                text = "Ramasser : " + drop.item.getName() + " (E)";
            }
        }
        if (text != null) {
            layout.setText(font, text);
            font.draw(spriteBatch, layout, w / 2f - layout.width / 2f, h / 2f - 30);
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
            } else if (isJournalOpen) {
                toggleJournal();
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
        if (keycode == Input.Keys.J) {
            // le journal ne s'ouvre que depuis le jeu (pas par-dessus un autre menu)
            if (isJournalOpen || !(inDialog || isInventoryOpen || isSkillsOpen || isPaused || isDebugOpen || isExtraOverlayOpen())) {
                toggleJournal();
            }
            return true;
        }
        if (keycode == Input.Keys.I) {
            if (!isSkillsOpen && !isJournalOpen) {
                toggleInventory();
            }
            return true;
        }
        if (keycode == Input.Keys.K) {
            // le panneau du personnage ne s'ouvre que depuis le jeu (pas par-dessus un autre menu)
            if (isSkillsOpen || !(inDialog || isInventoryOpen || isJournalOpen || isPaused || isExtraOverlayOpen())) {
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

        if (keycode == Input.Keys.Q) {
            gameClock.postMessage(player.drinkHealingPotion());
            return true;
        }

        if (keycode == Input.Keys.V) {
            ViewSettings.thirdPerson = !ViewSettings.thirdPerson;
            return true;
        }

        if (keycode == Input.Keys.E) {
            btCollisionObject objectInView = playerController.getBodyInView(getInteractionRange());
            if (objectInView != null && objectInView.userData instanceof Readable) {
                showText(((Readable) objectInView.userData).textId);
                return true;
            }
            if (objectInView != null && onInteract(objectInView.userData)) {
                return true;
            }
            if (pickUpNearest()) {
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
        if (button == Input.Buttons.LEFT && !isOverlayOpen()) {
            // clic gauche : un coup (sans effet s'il n'y a pas de monstre à portée)
            boolean swing = combat.ready();
            String loot = combat.attack(player, camera.position, camera.direction);
            if (swing) {
                playerModel.startAttack();
            }
            if (loot != null) {
                gameClock.postMessage(loot);
            }
            return true;
        }
        return false;
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

    protected void toggleJournal() {
        isJournalOpen = !isJournalOpen;
        journalTable.setVisible(isJournalOpen);
        Gdx.input.setCursorCatched(!isJournalOpen);
        if (isJournalOpen) {
            journalPanel.rebuild();
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
        skyInstances.clear();
        boxModels.clear();
        groundViews.clear();
        skin.dispose();
    }
}
