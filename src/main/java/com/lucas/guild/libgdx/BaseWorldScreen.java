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
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.RenderableProvider;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.math.Matrix4;
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
import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.LoreData;
import com.lucas.guild.data.StoryData;
import com.lucas.guild.data.TownData;
import com.lucas.guild.game.DeathPenalty;
import com.lucas.guild.game.DebugMode;
import com.lucas.guild.game.GameSession;
import com.lucas.guild.game.GroundItems;
import com.lucas.guild.game.Monster;
import com.lucas.guild.game.SaveManager;
import com.lucas.guild.game.Shop;
import com.lucas.guild.game.Story;
import com.lucas.guild.game.WorldEvents;
import com.lucas.guild.model.ActiveEvent;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.DayNightCycle;
import com.lucas.guild.model.GameClock;
import com.lucas.guild.model.Inventaire;
import com.lucas.guild.model.Item;

/**
 * Base commune de tous les écrans « monde 3D » (ville, zones sauvages et intérieurs de bâtiments).
 *
 * Elle tient la boucle de jeu : caméra, physique Bullet, construction du décor, HUD, touches, et l'ouverture
 * des menus (un seul à la fois). Le reste est confié à des collaborateurs : {@link WorldPanels} (les menus),
 * {@link NpcGroup} (les PNJ), {@link GroundItemViews} (les objets au sol), {@link SkyAndShadows} (ciel et ombres
 * en plein air), {@link WaterWaves} (l'eau), {@link Toasts} (les messages), {@link Readable} (les textes à lire)
 * et {@link CombatScene} (les monstres).
 *
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
    private ModelBatch modelBatch;
    private Environment environment;
    /** Lumière ambiante et lumière directionnelle (soleil) : modifiées à chaque image par {@link #applyDayNight}. */
    private ColorAttribute ambientLight;
    private DirectionalLight sunLight;
    /** La brume (couleur du ciel), si l'écran en a une ; et l'éclairage du ciel, qui n'en a pas. */
    private ColorAttribute fog;
    private Environment skyEnvironment;
    protected PlayerController playerController;
    /** Le corps du joueur, visible à la 3e personne (touche V). */
    private CharacterModel playerModel;
    private final Vector3 feet = new Vector3();

    // Physique
    private btDiscreteDynamicsWorld dynamicsWorld;
    /**
     * Les corps solides du décor. Il faut les libérer nous-mêmes en quittant l'écran : LibGDX garde chaque corps
     * Bullet dans une table globale jusqu'à son {@code dispose()}, sinon ils s'accumulent à chaque changement d'écran.
     */
    private final Array<btRigidBody> bodies = new Array<>();

    protected final Array<Disposable> disposables = new Array<>();
    protected final Array<ModelInstance> instances = new Array<>();
    private final Map<String, Model> shapeModels = new HashMap<>();

    // Collaborateurs
    /** Ciel et ombres : seulement en plein air ({@link #hasSky()}), null dans les intérieurs. */
    private SkyAndShadows outdoors;
    private NpcGroup npcs;
    private GroundItemViews groundItems;
    /** L'histoire dans cet écran : objets à ramasser, déclencheurs, avancement. */
    private StoryScene storyScene;
    private WorldPanels panels;
    private final WaterWaves water = new WaterWaves();
    private final Toasts toasts = new Toasts();
    /** Les monstres de l'écran (aucun dans la plupart des lieux). */
    protected CombatScene combat;

    // HUD
    private SpriteBatch spriteBatch;
    private BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();
    private final StringBuilder hudText = new StringBuilder();

    // Interface (menus) : un seul ouvert à la fois
    protected Stage stage;
    protected Skin skin;
    protected Table dialogTable;
    protected boolean inDialog = false;
    protected boolean isInventoryOpen = false;
    protected boolean isSkillsOpen = false;
    protected boolean isJournalOpen = false;
    protected boolean isDebugOpen = false;
    protected boolean isPaused = false;

    private boolean deathPending = false;
    /** Une fin de l'histoire vient d'être atteinte : l'écran de fin s'ouvre à la fin de l'image. */
    private boolean endingPending = false;
    /** Le lieu affiché sur le HUD (le nom de la ville ou de la zone). */
    protected String townName = "";

    // Dernière position du joueur, pour mesurer la distance marchée (compétences Endurance et Portage)
    private float lastX;
    private float lastZ;
    private boolean hasLastPosition = false;

    private final java.util.Random eventRandom = com.lucas.guild.game.GameRandom.create();

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
        if (VILLAGER_DIALOG.equals(dialogType) && talkingTo != null) {
            buildVillagerDialog(talkingTo);
            return;
        }
        LoreData.TextData text = Readable.textOfDialog(dialogType);
        if (text != null) {
            dialogTable.add(UiKit.label(skin, text.title)).pad(10).row();
            addWrappedText(text.text);
            addLeaveButton();
        }
    }

    // ------------------------------------------------------------------ habitants qui parlent

    private static final String VILLAGER_DIALOG = "Villager";
    /** L'habitant à qui l'on parle (dialogue ouvert), ou null. */
    private Villagers.Villager talkingTo;

    /** Ouvre la réplique d'un habitant. */
    protected void talkTo(Villagers.Villager villager) {
        talkingTo = villager;
        toggleDialog(VILLAGER_DIALOG);
    }

    /** L'habitant à qui l'on parle, tant que son dialogue est ouvert (sinon null). */
    protected Villagers.Villager talkingTo() {
        if (!isOverlayOpen()) {
            talkingTo = null;
        }
        return talkingTo;
    }

    /** La réplique d'un habitant : son nom, ce qu'il dit, et de quoi lui demander autre chose. */
    private void buildVillagerDialog(final Villagers.Villager v) {
        java.util.List<String> lines = Villagers.linesOf(v, player.getTownId(), story());
        com.badlogic.gdx.scenes.scene2d.ui.Label name = UiKit.label(skin, v.talk.name);
        name.setColor(Color.GOLD);
        dialogTable.add(name).pad(10).row();
        String line = com.lucas.guild.game.NpcTalk.line(lines, v.asked);
        addWrappedText("\"" + (line != null ? line : "...") + "\"");
        if (lines.size() > 1) {
            addDialogButton("Autre chose ?", () -> {
                v.asked++;
                dialogTable.clear();
                buildDialog(VILLAGER_DIALOG);
            });
        }
        addDialogButton("Au revoir", () -> {
            v.asked++;   // la prochaine fois, il dira autre chose
            toggleDialog(null);
        });
    }

    /** Ce qu'on lit en visant un habitant qui parle. */
    protected static String villagerPrompt(Villagers.Villager v) {
        return "Parler à " + v.talk.name + " (E)";
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

    /**
     * Un personnage qui se déplace (sans corps solide, comme les habitants de la ville) visé par le rayon
     * {@code eye + t * direction}, plus près que {@code maxDistance}.
     *
     * @return ce qu'on obtient en le visant (passé à {@link #getInteractionPrompt} et {@link #onInteract}), ou null
     */
    protected Object aimedWalker(Vector3 eye, Vector3 direction, float maxDistance) {
        return null;
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

    /** Appelé à chaque image après la mise à jour du jeu, même quand un menu est ouvert (animations du décor). */
    protected void afterUpdate(float delta) {
    }

    /** Distance de vue en m. Avec la brume ({@link #hasFog()}), ce qui est plus loin disparaît dans la couleur du ciel. */
    protected float getViewDistance() {
        return 300f;
    }

    /** La brume : le lointain se fond dans le ciel (le monde ouvert y cache les carreaux qui apparaissent). */
    protected boolean hasFog() {
        return false;
    }

    /** Ce qu'il faut dessiner en plus des {@link #instances} (des carreaux fusionnés du monde ouvert), ombres comprises. */
    protected Array<RenderableProvider> extraRenderables() {
        return NO_EXTRA;
    }

    private static final Array<RenderableProvider> NO_EXTRA = new Array<>();

    /** Un monstre vient d'être tué (après l'histoire). */
    protected void onMonsterKilled(Monster monster) {
    }

    /** Cet écran est-il le monde ouvert ? (prototype) */
    protected boolean isOpenWorld() {
        return false;
    }

    /** Cet écran garde-t-il le mode du monde ouvert tel qu'il est ? (les intérieurs : on y entre et on en sort) */
    protected boolean keepsWorldMode() {
        return false;
    }

    /** Ce lieu prévient-il du marché en cours quand on y arrive ? (la ville, pas les intérieurs) */
    protected boolean announcesEvents() {
        return false;
    }

    /** Identifie ce lieu pour les objets posés au sol : les objets jetés ici sont retrouvés ici. */
    protected String sceneKey() {
        return getClass().getSimpleName() + "@" + player.getTownId();
    }

    /** Le joueur est tombé à 0 PV : il se réveille à l'auberge, plus pauvre (voir {@link DeathPenalty}). */
    protected void onPlayerDeath() {
        // On ne change pas d'écran en plein milieu d'une image : celui-ci serait libéré avant la fin du dessin
        deathPending = true;
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
        townName = GameContent.world().place(player.getTownId()).name;   // lu une fois : le HUD le redessine à chaque image
        if (!keepsWorldMode()) {
            session.setInOpenWorld(isOpenWorld());   // la ville ou une zone classique : on quitte le monde ouvert
        }

        setup3DEnvironment();
        setupPhysics();
        setupUI();
        setupInput();

        combat = new CombatScene(instances);
        combat.setDropSink(this::dropOnGround);
        disposables.add(combat);
        npcs = new NpcGroup(instances);
        disposables.add(npcs);
        groundItems = new GroundItemViews(session.getGround(), sceneKey(), instances, this::addVisualBox);
        storyScene = new StoryScene(session, sceneKey(), instances, this::addVisualBox);
        combat.setListener(new CombatScene.Listener() {
            @Override
            public void killed(Monster monster) {
                String message = storyScene.story().onKill(monster.data);
                if (message != null) {
                    gameClock.postMessage(message);
                }
                onMonsterKilled(monster);
            }

            @Override
            public void yielded(Monster monster) {
                storyScene.story().onYield(monster.data);
                if (monster.data.yieldMessage != null) {
                    gameClock.postMessage(monster.data.yieldMessage);
                }
            }

            @Override
            public void message(String text) {
                gameClock.postMessage(text);
            }

            @Override
            public boolean holds(java.util.List<String> conditions) {
                return storyScene.story().holds(conditions);
            }
        });
        // les avantages de l'histoire propres à ce lieu (les sceaux, au château) ; aucun ailleurs
        int[] boons = storyScene.story().boonsIn(player.getTownId());
        player.setStoryBoons(boons[0], boons[1]);
        buildWorld();
        groundItems.showExisting();
        storyScene.showPickups();
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
    }

    private void setup3DEnvironment() {
        camera = new PerspectiveCamera(70, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.position.set(0f, 5f, 10f);
        camera.lookAt(0, 0, 0);
        camera.near = 0.1f;
        camera.far = getViewDistance();
        camera.update();

        modelBatch = new ModelBatch();
        disposables.add(modelBatch);

        float ambient = getAmbientLight();
        environment = new Environment();
        ambientLight = new ColorAttribute(ColorAttribute.AmbientLight, ambient, ambient, ambient, 1f);
        environment.set(ambientLight);
        if (hasSky()) {
            // en plein air : le soleil porte des ombres, et il y a un ciel
            outdoors = new SkyAndShadows();
            disposables.add(outdoors);
            sunLight = outdoors.sun();
        } else {
            sunLight = new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f);
        }
        environment.add(sunLight);
        if (hasFog()) {
            // le ciel garde un éclairage sans brume : sinon le soleil et les étoiles, loin, disparaîtraient
            skyEnvironment = new Environment();
            skyEnvironment.set(ambientLight);
            skyEnvironment.add(sunLight);
            fog = new ColorAttribute(ColorAttribute.Fog, 0.5f, 0.8f, 1f, 1f);
            environment.set(fog);
        } else {
            skyEnvironment = environment;
        }

        spriteBatch = new SpriteBatch();
        font = new BitmapFont();
        disposables.addAll(spriteBatch, font);
    }

    private void setupPhysics() {
        btDefaultCollisionConfiguration collisionConfig = new btDefaultCollisionConfiguration();
        btCollisionDispatcher dispatcher = new btCollisionDispatcher(collisionConfig);
        btDbvtBroadphase broadphase = new btDbvtBroadphase();
        btSequentialImpulseConstraintSolver solver = new btSequentialImpulseConstraintSolver();
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

        panels = new WorldPanels(stage, skin, game, this, difficulty, this::dropInFront, this::togglePause);
        disposables.add(panels);
        dialogTable = panels.dialog;

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
        body.setWorldTransform(new Matrix4().setTranslation(x, y, z));
        body.userData = userData;
        dynamicsWorld.addRigidBody(body);
        bodies.add(body);
    }

    /**
     * Pose un PNJ debout sur le sol en (x, z), de face (il regarde vers -Z, comme le joueur qui arrive par la
     * porte). {@code userData} est ce qu'on obtient en le visant.
     *
     * @param modelId son modèle dans models.json (« innkeeper »...)
     */
    protected void addNpc(String modelId, float x, float z, Object userData) {
        addInvisibleBody(0.9f, 2f, 0.9f, x, 1.5f, z, userData);
        npcs.add(modelId, x, z);
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
    protected void addDecorPart(TownData.PartData part, float ox, float oz, Color color, Object userData) {
        float x = ox + part.x;
        float z = oz + part.z;
        float centerY = 0.5f + part.y + part.h / 2f;   // le sol est une boîte d'1 m dont le dessus est à 0,5
        boolean plainBox = "box".equals(part.shape) && part.rot == null;
        if (part.decal || !plainBox) {
            ModelInstance instance = addVisualShape(part.shape, part.w, part.h, part.d, color, x, centerY, z, part.rot);
            if ("water".equals(part.anim)) {
                water.add(instance, centerY, color);
            }
        } else {
            addBox(part.w, part.h, part.d, color, x, centerY, z, userData);
        }
    }

    /** Le monde physique de l'écran (le monde ouvert y ajoute et retire ses corps lui-même). */
    protected btDiscreteDynamicsWorld physics() {
        return dynamicsWorld;
    }

    /** Modèle partagé par toutes les formes de même type, taille et couleur (libéré avec l'écran). */
    protected Model shapeModel(String shape, float w, float h, float d, Color color) {
        String key = shape + "/" + w + "x" + h + "x" + d + "/" + color.toIntBits();
        Model model = shapeModels.get(key);
        if (model == null) {
            model = Shapes.create(shape, w, h, d, new Material(ColorAttribute.createDiffuse(color)));
            disposables.add(model);
            shapeModels.put(key, model);
        }
        return model;
    }

    // ================================================================== boucle de jeu

    @Override
    public void render(float delta) {
        update(delta);
        npcs.update(delta, playerPosition());
        water.update(delta);
        afterUpdate(delta);
        updateEnvironment();

        Color bg = getBackgroundColor();
        Gdx.gl.glClearColor(bg.r, bg.g, bg.b, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        Array<RenderableProvider> extra = extraRenderables();
        if (outdoors != null) {
            outdoors.renderShadows(environment, instances, extra, playerPosition(), camera.direction, delta);
        }
        modelBatch.begin(camera);
        modelBatch.render(instances, environment);
        for (RenderableProvider provider : extra) {
            modelBatch.render(provider, environment);
        }
        if (outdoors != null) {
            outdoors.renderSky(modelBatch, skyEnvironment);
        }
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
            endingPending = false;
            gameClock.postMessage(DeathPenalty.apply(game.getSession()));
            game.setScreen(new InnScreen(game, difficulty));
        } else if (endingPending) {
            endingPending = false;
            game.setScreen(new EndingScreen(game, difficulty));
        }
    }

    /** Vrai quand un menu (pause, inventaire, dialogue, menu supplémentaire) bloque le jeu. */
    protected boolean isOverlayOpen() {
        return inDialog || isInventoryOpen || isSkillsOpen || isJournalOpen || isDebugOpen || isPaused || isExtraOverlayOpen();
    }

    protected Vector3 playerPosition() {
        return playerController.getPlayerBody().getCenterOfMassPosition();
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
            if (combat.update(delta, player, playerPosition())) {
                onPlayerDeath();
            }
            advanceStory();
        }
    }

    /** L'histoire avance (déclencheurs, chapitres) ; un texte d'histoire en attente s'ouvre, un épilogue a son écran. */
    private void advanceStory() {
        Vector3 p = playerPosition();
        storyScene.update(p.x, p.z);
        if (deathPending) {
            return;
        }
        if (StoryScene.textsEnabled && storyScene.hasSceneText()) {
            toggleDialog(SCENE_TEXT_DIALOG);
            return;
        }
        Story.Text text = storyScene.story().nextText();
        if (text == null || (!StoryScene.textsEnabled && !text.ending)) {
            return;
        }
        if (text.ending) {
            endingPending = true;   // jamais de changement d'écran au milieu d'une image
        } else {
            toggleDialog(STORY_TEXT_DIALOG);
        }
    }

    /** Habille le corps du joueur de son équipement, le place et l'anime. */
    private void updatePlayerModel(float delta) {
        Inventaire inventory = player.getInventaire();
        playerModel.setLook(
                inventory.getArmor() != null ? inventory.getArmor().getId() : null,
                inventory.getWeapon() != null ? inventory.getWeapon().getId() : null,
                inventory.getBag() != null ? inventory.getBag().getId() : null);
        // contre un mur, la caméra se colle au joueur : on cache alors son corps (comme à la 1re personne)
        playerModel.setVisible(ViewSettings.thirdPerson && playerController.getAimBonus() > 0.9f);
        feet.set(playerPosition()).sub(0, 1.0f, 0);
        playerModel.update(delta, playerController.getHorizontalSpeed(), feet, playerController.getYaw());
        combat.setRangeBonus(playerController.getAimBonus());
    }

    /** Applique le ciel, le soleil (puis la lune) et la lumière ambiante de l'heure et de la saison : à appeler dans {@link #updateEnvironment()}. */
    protected void applyDayNight(Color sky) {
        DayNightCycle.Light light = DayNightCycle.compute(
                gameClock.getTimeOfDay(), gameClock.getSunrise(), gameClock.getSunset(), getAmbientLight());
        sky.set(light.sky[0], light.sky[1], light.sky[2], 1f);
        if (fog != null) {
            fog.color.set(sky);
        }
        ambientLight.color.set(light.ambient[0], light.ambient[1], light.ambient[2], 1f);
        sunLight.color.set(light.lightColor[0], light.lightColor[1], light.lightColor[2], 1f);
        sunLight.direction.set(light.lightDirection[0], light.lightDirection[1], light.lightDirection[2]).nor();
        if (outdoors != null) {
            outdoors.updateSky(camera.position, light.daylight, light.lightDirection);
        }
    }

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

    /** La marche entraîne l'Endurance, et le Portage quand on porte au moins la moitié de sa charge maximale. */
    private void trainWalking() {
        Vector3 position = playerPosition();
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

    // ================================================================== objets au sol

    /** Pose un objet au sol (butin d'un monstre, objet jeté, trop-plein d'un coffre). */
    protected void dropOnGround(Item item, float x, float z) {
        groundItems.drop(item, x, z);
    }

    /** Un point à {@code distance} mètres devant le joueur, au sol. */
    private Vector3 inFront(float distance) {
        Vector3 d = new Vector3(camera.direction.x, 0, camera.direction.z);
        if (d.isZero()) {
            d.set(0, 0, -1);
        }
        return d.nor().scl(distance).add(playerPosition());
    }

    /** Le joueur jette {@code item} : il tombe devant lui, et on peut le ramasser. */
    private void dropInFront(Item item) {
        Vector3 spot = inFront(1.5f);
        dropOnGround(item, spot.x, spot.z);
    }

    private com.lucas.guild.model.GroundDrop nearestGroundItem() {
        Vector3 p = playerPosition();
        return groundItems.nearest(p.x, p.z);
    }

    /** Ramasse l'objet au sol le plus proche (E). @return false s'il n'y en a aucun à portée. */
    private boolean pickUpNearest() {
        Vector3 p = playerPosition();
        String message = groundItems.pickUpNearest(player, p.x, p.z);
        if (message == null) {
            return false;
        }
        gameClock.postMessage(message);
        return true;
    }

    // ================================================================== debug

    /** Debug : fait apparaître un monstre à 6 m devant le joueur. */
    String debugSpawn(String monsterId) {
        Vector3 spot = inFront(6f);
        Monster m = combat.spawn(monsterId, spot.x, spot.z);
        return m.getName() + " apparaît.";
    }

    /** Debug : retire tous les monstres. */
    String debugKillAll() {
        return combat.clearAll() + " monstre(s) retiré(s).";
    }

    // ================================================================== HUD

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
        hudText.append(gameClock.getDate()).append(" - ").append(gameClock.formatTime())
                .append(" - ").append(townName).append("\n");
        if (DebugMode.isEnabled()) {
            Vector3 p = playerPosition();
            hudText.append(String.format("[DEBUG] x %.1f  y %.1f  z %.1f  jour %d  temps x%.0f  vitesse x%.0f  (F10)\n",
                    p.x, p.y, p.z, gameClock.getCurrentDay(), gameClock.getTimeScale(), DebugMode.getSpeedMultiplier()));
        }
        hudText.append("Or : ").append(player.getGold()).append(" Po - ").append(UiKit.loadLine(player));
        font.draw(spriteBatch, hudText, 10, h - 10);

        font.draw(spriteBatch, "I : inventaire   K : personnage   J : journal   Q : potion   V : vue   Échap : pause", 10, 24);
        lifeBarClock += delta;
        drawLifeBar();

        combat.drawHud(spriteBatch, font, w, h, camera.position, camera.direction);
        toasts.draw(delta, gameClock, spriteBatch, font, w, h);

        String text = null;
        Object aimed = aimedUserData();
        if (aimed instanceof Readable) {
            text = ((Readable) aimed).prompt();
        } else if (aimed instanceof StoryPoint) {
            text = ((StoryPoint) aimed).prompt();
        } else if (aimed != null) {
            text = getInteractionPrompt(aimed);
        }
        if (text == null) {
            Vector3 p = playerPosition();
            StoryData.PickupData pickup = storyScene.nearestPickup(p.x, p.z);
            if (pickup != null) {
                text = "Ramasser : " + pickup.name + " (E)";
            }
        }
        if (text == null) {
            com.lucas.guild.model.GroundDrop drop = nearestGroundItem();
            if (drop != null) {
                text = "Ramasser : " + drop.item.getName() + " (E)";
            }
        }
        if (text != null) {
            layout.setText(font, text);
            font.draw(spriteBatch, layout, w / 2f - layout.width / 2f, h / 2f - 30);
        }
    }

    /** Portée pour parler à un habitant qui passe (depuis le joueur ; la caméra à la 3e personne s'y ajoute). */
    private static final float WALKER_TALK_RANGE = 4f;

    /** Ce que vise le joueur : un habitant qui passe s'il est devant le décor visé, sinon ce décor (ou null). */
    private Object aimedUserData() {
        btCollisionObject body = playerController.getBodyInView(getInteractionRange());
        float bodyDistance = playerController.getLastViewDistance();
        Object walker = aimedWalker(camera.position, camera.direction,
                Math.min(bodyDistance, WALKER_TALK_RANGE + playerController.getAimBonus()));
        if (walker != null) {
            return walker;
        }
        return body != null ? body.userData : null;
    }

    // ------------------------------------------------------------------ barre de vie

    private static final float LIFE_BAR_WIDTH = 220f;
    private static final float LIFE_BAR_HEIGHT = 16f;
    private Texture pixel;

    /** La barre de vie du joueur, en bas à gauche au-dessus de l'aide des touches : rouge, plus vive quand la vie est basse. */
    private void drawLifeBar() {
        if (pixel == null) {
            Pixmap white = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            white.setColor(Color.WHITE);
            white.fill();
            pixel = new Texture(white);
            white.dispose();
            disposables.add(pixel);
        }
        float x = 10f;
        float y = 36f;
        float share = Math.max(0f, Math.min(1f, player.getHealth() / (float) Math.max(1, player.getMaxHealth())));
        spriteBatch.setColor(0f, 0f, 0f, 0.6f);
        spriteBatch.draw(pixel, x - 2, y - 2, LIFE_BAR_WIDTH + 4, LIFE_BAR_HEIGHT + 4);
        spriteBatch.setColor(0.25f, 0.05f, 0.05f, 0.9f);
        spriteBatch.draw(pixel, x, y, LIFE_BAR_WIDTH, LIFE_BAR_HEIGHT);
        if (share <= 0.25f) {
            // vie basse : la barre clignote doucement
            float pulse = 0.75f + 0.25f * (float) Math.sin(lifeBarClock * 8f);
            spriteBatch.setColor(1f, 0.25f * pulse, 0.15f * pulse, 1f);
        } else {
            spriteBatch.setColor(0.8f, 0.12f, 0.1f, 1f);
        }
        spriteBatch.draw(pixel, x, y, LIFE_BAR_WIDTH * share, LIFE_BAR_HEIGHT);
        spriteBatch.setColor(Color.WHITE);
        layout.setText(font, "PV " + player.getHealth() + " / " + player.getMaxHealth());
        font.draw(spriteBatch, layout, x + (LIFE_BAR_WIDTH - layout.width) / 2f, y + LIFE_BAR_HEIGHT - 2f);
    }

    private float lifeBarClock;

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
        // Journal, inventaire et personnage ne s'ouvrent que depuis le jeu, jamais par-dessus un autre menu :
        // en se refermant, ils recaptureraient la souris alors que le menu du dessous attend encore un clic.
        if (keycode == Input.Keys.J) {
            if (isJournalOpen || !isOverlayOpen()) {
                toggleJournal();
            }
            return true;
        }
        if (keycode == Input.Keys.I) {
            if (isInventoryOpen || !isOverlayOpen()) {
                toggleInventory();
            }
            return true;
        }
        if (keycode == Input.Keys.K) {
            if (isSkillsOpen || !isOverlayOpen()) {
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
            Object aimed = aimedUserData();
            if (aimed instanceof Readable) {
                read((Readable) aimed);
                return true;
            }
            if (aimed instanceof StoryPoint) {
                toggleDialog(STORY_POINT_DIALOG + ((StoryPoint) aimed).id);
                return true;
            }
            if (aimed != null && onInteract(aimed)) {
                return true;
            }
            Vector3 p = playerPosition();
            String picked = storyScene.pickUpNearest(p.x, p.z);
            if (picked != null) {
                gameClock.postMessage(picked);
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
        panels.show(panels.pause, isPaused);
    }

    protected void toggleInventory() {
        isInventoryOpen = !isInventoryOpen;
        panels.show(panels.inventory, isInventoryOpen);
        if (isInventoryOpen) {
            panels.refreshInventory();
        }
    }

    protected void toggleDebug() {
        isDebugOpen = !isDebugOpen;
        panels.show(panels.debug, isDebugOpen);
        if (isDebugOpen) {
            panels.refreshDebug();
        }
    }

    protected void toggleJournal() {
        isJournalOpen = !isJournalOpen;
        panels.show(panels.journal, isJournalOpen);
        if (isJournalOpen) {
            panels.refreshJournal();
        }
    }

    protected void toggleSkills() {
        isSkillsOpen = !isSkillsOpen;
        panels.show(panels.skills, isSkillsOpen);
        if (isSkillsOpen) {
            panels.refreshSkills();
        }
    }

    /** Ouvre le dialogue {@code dialogType}, ou ferme le dialogue en cours. */
    protected void toggleDialog(String dialogType) {
        inDialog = !inDialog;
        panels.show(dialogTable, inDialog);

        dialogTable.clear();
        if (inDialog && !buildStoryDialog(dialogType)) {
            buildDialog(dialogType);
        }
    }

    // ================================================================== dialogues d'histoire

    private static final String STORY_TEXT_DIALOG = "StoryText";
    private static final String SCENE_TEXT_DIALOG = "SceneText";
    private static final String STORY_POINT_DIALOG = "StoryPoint:";

    /** Les dialogues que tous les écrans partagent : un texte d'histoire, un point d'histoire du décor. @return false si ce n'en est pas un */
    private boolean buildStoryDialog(String dialogType) {
        if (SCENE_TEXT_DIALOG.equals(dialogType)) {
            StoryData.TriggerData scene = storyScene.pollSceneText();
            if (scene != null) {
                if (scene.title != null) {
                    Label title = UiKit.label(skin, scene.title);
                    title.setColor(Color.GOLD);
                    dialogTable.add(title).pad(10).row();
                }
                addWrappedText(scene.text);
            }
            addDialogButton("Continuer", () -> toggleDialog(null));
            return true;
        }
        if (STORY_TEXT_DIALOG.equals(dialogType)) {
            final Story.Text text = storyScene.story().nextText();
            if (text == null) {
                addLeaveButton();
                return true;
            }
            Label title = UiKit.label(skin, text.title);
            title.setColor(Color.GOLD);
            dialogTable.add(title).pad(10).row();
            addWrappedText(text.body);
            storyScene.story().markShown(text);   // montré : il ne reviendra pas, même si on ferme avec Échap
            addDialogButton("Continuer", () -> toggleDialog(null));
            return true;
        }
        if (dialogType != null && dialogType.startsWith(STORY_POINT_DIALOG)) {
            final String point = dialogType.substring(STORY_POINT_DIALOG.length());
            java.util.List<StoryData.DialogData> dialogs = storyScene.story().dialogsAt(point);
            Runnable close = () -> toggleDialog(null);
            if (dialogs.size() == 1) {
                openStoryDialog(dialogs.get(0), close);
                return true;
            }
            String name = Story.pointName(point);
            dialogTable.add(UiKit.label(skin, name != null ? name : point)).pad(10).row();
            if (dialogs.isEmpty()) {
                addWrappedText("Rien à faire ici pour l'instant.");
            }
            for (final StoryData.DialogData dialog : dialogs) {
                addDialogButton(dialog.label, () -> openStoryDialog(dialog, close));
            }
            addLeaveButton();
            return true;
        }
        return false;
    }

    /**
     * Ajoute au dialogue d'un PNJ les boutons des dialogues d'histoire qu'il propose en ce moment.
     *
     * @param npc    son rôle dans story.json (« guildmaster », « innkeeper », « blacksmith », « shopkeeper »)
     * @param onBack ce que fait « Continuer » à la fin : en général, réafficher le dialogue du PNJ
     */
    protected void addStoryDialogs(String npc, final Runnable onBack) {
        for (final StoryData.DialogData dialog : storyScene.story().dialogsAt(npc + "@" + player.getTownId())) {
            TextButton button = addDialogButton(dialog.label, () -> openStoryDialog(dialog, onBack));
            button.getLabel().setColor(Color.GOLD);
        }
    }

    /** Affiche un dialogue d'histoire dans la fenêtre de dialogue : son texte, puis ses choix s'il en a. */
    private void openStoryDialog(final StoryData.DialogData dialog, final Runnable onBack) {
        final Story story = storyScene.story();
        dialogTable.clear();
        java.util.List<StoryData.OptionData> options = story.optionsOf(dialog);
        if (dialog.options.isEmpty()) {
            String refusal = story.apply(dialog);
            if (refusal != null) {
                dialogTable.add(UiKit.message(skin, refusal)).pad(10).row();
            } else {
                addWrappedText(dialog.text);
            }
            addDialogButton("Continuer", onBack);
            return;
        }
        addWrappedText(dialog.text);
        for (final StoryData.OptionData option : options) {
            addDialogButton(option.label, () -> {
                String refusal = story.choose(option);
                final Runnable then = option.next != null && refusal == null
                        ? () -> openStoryDialog(GameContent.story().dialog(option.next), onBack)
                        : onBack;
                if (refusal == null && option.text == null) {
                    then.run();
                    return;
                }
                dialogTable.clear();
                if (refusal != null) {
                    dialogTable.add(UiKit.message(skin, refusal)).pad(10).row();
                } else {
                    addWrappedText(option.text);
                }
                addDialogButton("Continuer", then);
            });
        }
        addDialogButton("Plus tard", onBack);
    }

    /** Ce qu'on obtient en visant une partie de décor d'action « read » (un texte) ou « story » (un point d'histoire), sinon null. */
    protected Object decorUserData(TownData.PartData part) {
        if ("read".equals(part.action)) {
            return new Readable(part.text);
        }
        if ("story".equals(part.action)) {
            return new StoryPoint(part.text);
        }
        return null;
    }

    /** L'histoire du personnage, pour les écrans qui en dépendent (portes fermées, épreuves...). */
    protected Story story() {
        return storyScene.story();
    }

    /** Ouvre la lecture d'un texte du décor ; la première lecture l'ajoute au journal. */
    private void read(Readable sign) {
        LoreData.TextData text = sign.text();
        if (text == null) {
            return;
        }
        if (sign.learn(player)) {
            gameClock.postMessage("Ajouté au journal : " + text.title + ".");
        }
        toggleDialog(sign.dialogType());
    }

    /** Ajoute un paragraphe qui revient à la ligne dans le dialogue en cours. */
    protected void addWrappedText(String text) {
        Label label = UiKit.label(skin, text);
        label.setWrap(true);
        float width = Math.max(300f, Math.min(640f, Gdx.graphics.getWidth() - 100f));
        dialogTable.add(label).width(width).pad(10).row();
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

    /**
     * Ouvre la boutique {@code shopId} (shops.json) dans la fenêtre de dialogue en cours.
     *
     * @param onBack appelé par « Retour » : en général, réafficher le dialogue du PNJ
     */
    protected void showShop(String shopId, Runnable onBack) {
        new ShopPanel(dialogTable, skin, player, new Shop(shopId), onBack).rebuild(null);
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
        // les corps du décor d'abord, tant que le monde physique existe encore
        for (btRigidBody body : bodies) {
            dynamicsWorld.removeRigidBody(body);
            body.dispose();
        }
        bodies.clear();
        for (Disposable disposable : disposables) {
            disposable.dispose();
        }
        disposables.clear();
        instances.clear();
        shapeModels.clear();
        groundItems.clear();
        storyScene.clear();
        skin.dispose();
    }
}
