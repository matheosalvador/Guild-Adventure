import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.lucas.guild.libgdx.*;
import com.lucas.guild.model.*;
import com.lucas.guild.game.GameSession;
import com.lucas.guild.data.GameContent;

import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;

/**
 * Banc de test "fumee" : fenetre invisible, parcours scripte du jeu, captures PNG.
 * Utilise uniquement l'API publique + reflexion (champs stage / playerController / yaw)
 * pour pouvoir tourner a l'identique avant et apres un refactoring.
 */
public class Smoke extends ApplicationAdapter {
    static final float DT = 1 / 60f;
    final String outDir;
    MainGame game;
    int step = 0;
    final List<Runnable> steps = new ArrayList<>();
    int failures = 0;

    /** Si non null : ne jouer que cette section (« relecture » = relectureSection), pour iterer vite. */
    final String only;

    Smoke(String outDir, String only) { this.outDir = outDir; this.only = only; }

    public static void main(String[] a) {
        Lwjgl3ApplicationConfiguration cfg = new Lwjgl3ApplicationConfiguration();
        cfg.setTitle("smoke");
        cfg.setWindowedMode(800, 600);
        cfg.setInitialVisible(false);
        cfg.useVsync(false);
        cfg.setForegroundFPS(0);
        new Lwjgl3Application(new Smoke(a[0], a.length > 1 && !a[1].isEmpty() ? a[1] : null), cfg);
    }

    // ------------------------------------------------------------------ cycle
    @Override
    public void create() {
        try {
            Files.createDirectories(Paths.get(outDir));
            com.lucas.guild.game.GameRandom.setSeedForTests(20261116L);   // meme hasard a chaque parcours : captures comparables
            game = new MainGame();
            game.create();
            game.resize(800, 600);
            script();
            log("script: " + steps.size() + " etapes");
        } catch (Throwable t) {
            fail("create", t);
        }
    }

    @Override
    public void render() {
        if (step >= steps.size()) {
            log(failures == 0 ? "FIN: AUCUNE ERREUR" : "FIN: " + failures + " ERREUR(S)");
            Gdx.app.exit();
            return;
        }
        try {
            steps.get(step++).run();
        } catch (Throwable t) {
            fail("etape " + step, t);
            step = steps.size();
        }
    }

    // ------------------------------------------------------------------ script
    void script() {
        if (only != null) {
            // une seule section (elle demarre sa propre partie)
            try {
                getClass().getDeclaredMethod(only + "Section").invoke(this);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException("section inconnue : " + only, e);
            }
            return;
        }
        // ---- MENU : vrais clics sur les boutons
        shot("00_menu");
        click("Start");   shot("01_menu_start");
        click("Nouvelle partie"); shot("02_menu_difficulty");
        step(() -> {
            // etat de depart deterministe : on met un objet dans l'inventaire APRES la creation de la partie
        });
        click("Normale");
        frames(3);
        check("apres 'Normale' on arrive sur la creation du personnage", () -> cur() instanceof CharacterCreationScreen);
        shot("03_creation_screen");
        // la suite du parcours attend un personnage sans classe (60 Po, 1 potion) : on demarre la partie directement
        step(() -> { game.startNewGame("normale"); game.setScreen(new TownScreen(game, "normale")); });
        frames(40);
        check("on est dans la ville", () -> cur() instanceof TownScreen);
        step(() -> game.getSession().getPlayer().addItem(new PotionSoin()));
        shot("10_town_spawn");

        // ---- inventaire / pause en ville
        key(Input.Keys.I); shot("11_town_inventory");
        key(Input.Keys.I);
        key(Input.Keys.ESCAPE); shot("12_town_pause");
        key(Input.Keys.ESCAPE);

        // ---- sauvegarde rapide F5
        step(() -> { try { Files.deleteIfExists(Paths.get("save.json")); } catch (Exception e) { throw new RuntimeException(e); } });
        key(Input.Keys.F5);
        check("F5 ecrit save.json", () -> Files.exists(Paths.get("save.json")));

        // ---- Forge / Guilde / Auberge : aller (en marchant), parler au PNJ, ressortir par la porte
        building("forge", 0, 0, -20, "ForgeScreen", 2f, 2.5f, 0f, 38, null,
                () -> new ForgeScreen(game, "normale"), 0f, -14f, "20");
        building("guild", -20, 0, 0, "GuildScreen", 0f, 1.5f, 10f, 105, () -> {
                    click("Voir les quêtes"); shot("33_guild_quest_board");
                    key(Input.Keys.ESCAPE);   // ferme le tableau
                    key(Input.Keys.E);        // rouvre le dialogue (le joueur regarde toujours le maitre)
                },
                () -> new GuildScreen(game, "normale"), -20f, 5f, "30");
        building("inn", 20, 0, 0, "InnScreen", 0f, 1.5f, 5f, 68, null,
                () -> new InnScreen(game, "normale"), 20f, 5f, "40");

        // ---- F9 : rechargement depuis le disque
        step(() -> game.setScreen(new TownScreen(game, "normale", new Vector3(0, 2, 0))));
        frames(5);
        key(Input.Keys.F9);
        frames(30);
        check("F9 -> nouvelle ville", () -> cur() instanceof TownScreen);
        shot("50_after_f9");

        // ---- Menu "Continuer"
        step(() -> game.setScreen(new MainMenuScreen(game)));
        frames(3);
        shot("60_menu_with_save");
        click("Start"); shot("61_menu_start_with_save");
        click("Continuer");
        frames(30);
        check("Continuer -> ville", () -> cur() instanceof TownScreen);
        shot("62_after_continue");

        // =================================================== TEMPS (section ajoutee avec le bloc temps)
        timeSection();

        // =================================================== INVENTAIRE (section ajoutee avec le bloc inventaire)
        inventorySection();

        // =================================================== CREATION DE PERSONNAGE ET COMPETENCES
        creationSection();

        // =================================================== MODE DEBUG
        debugSection();

        // =================================================== QUETES
        questSection();

        // =================================================== COMBAT
        combatSection();

        // =================================================== MONDE
        worldSection();

        // =================================================== ZONES SAUVAGES
        wildSection();

        // =================================================== LORE
        loreSection();

        // =================================================== EVENEMENTS DU MONDE
        eventSection();

        // =================================================== QUETES ENTRE VILLES
        quest7eSection();

        // =================================================== VUE 3E PERSONNE
        designSection();

        // =================================================== PNJ ET MONSTRES EN 3D
        designNpcSection();

        // =================================================== FINITIONS
        design8dSection();

        // =================================================== RELECTURE (chantier A : bugs corriges)
        relectureSection();
    }

    // ------------------------------------------------------------------ relecture (chantier A)

    int bulletBefore;
    float worstGap, maxY;

    /** Nombre d'objets de collision Bullet encore vivants (LibGDX les garde tous dans une table statique jusqu'a leur dispose). */
    static int bulletObjects() {
        try {
            Field f = com.badlogic.gdx.physics.bullet.collision.btCollisionObject.class.getDeclaredField("instances");
            f.setAccessible(true);
            return ((com.badlogic.gdx.utils.LongMap<?>) f.get(null)).size;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    PlayerController pc() {
        try { return (PlayerController) findPlayerController(cur()); } catch (Exception e) { throw new RuntimeException(e); }
    }

    void teleport(float x, float y, float z) {
        com.badlogic.gdx.physics.bullet.dynamics.btRigidBody body = pc().getPlayerBody();
        body.proceedToTransform(new com.badlogic.gdx.math.Matrix4().setTranslation(x, y, z));
        body.setLinearVelocity(new Vector3());
    }

    void look(float yaw, float pitch) {
        try {
            Object pc = pc();
            Field f = PlayerController.class.getDeclaredField("yaw"); f.setAccessible(true); f.setFloat(pc, yaw);
            Field pf = PlayerController.class.getDeclaredField("pitch"); pf.setAccessible(true); pf.setFloat(pc, pitch);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    /**
     * Dans une piece carree de demi-cote {@code half} (faces interieures des murs), fait regarder le joueur dans toutes
     * les directions et renvoie la plus petite distance entre la camera et un mur ou le sol.
     */
    float sweepCamera(float half) {
        com.badlogic.gdx.graphics.Camera cam = (com.badlogic.gdx.graphics.Camera) fieldOf(cur(), "camera");
        float worst = Float.MAX_VALUE;
        for (float pitch : new float[] {-45f, -20f, 0f, 20f}) {
            for (float yaw = 0f; yaw < 360f; yaw += 7.5f) {
                look(yaw, pitch);
                pc().updateCamera();
                float gap = Math.min(Math.min(half - Math.abs(cam.position.x), half - Math.abs(cam.position.z)), cam.position.y - 0.5f);
                worst = Math.min(worst, gap);
            }
        }
        return worst;
    }

    /** Les bugs trouves a la relecture : chacun a ici la verification qui echouait avant sa correction. */
    void relectureSection() {
        step(() -> { game.startNewGame("facile"); com.lucas.guild.game.DebugMode.setEnabled(false); setThirdPerson(false); });
        step(() -> game.setScreen(new TownScreen(game, "facile", new Vector3(0, 2, 40))));
        frames(5);

        // ---------- 1) les corps Bullet d'un ecran sont liberes quand on le quitte
        step(() -> bulletBefore = bulletObjects());
        for (int i = 0; i < 4; i++) {
            step(() -> game.setScreen(new ForgeScreen(game, "facile")));
            frames(3);
            step(() -> game.setScreen(new TownScreen(game, "facile", new Vector3(0, 2, 40))));
            frames(3);
        }
        step(() -> log("corps Bullet vivants : " + bulletBefore + " avant, " + bulletObjects() + " apres 4 allers-retours ville/forge"));
        check("changer d'ecran ne laisse aucun corps Bullet derriere soi", () -> bulletObjects() == bulletBefore);

        // ---------- 2) l'inventaire ne s'ouvre pas par-dessus un autre menu (sinon, en le refermant, la souris est capturee menu ouvert)
        key(Input.Keys.ESCAPE);
        key(Input.Keys.I);
        check("pause : I n'ouvre pas l'inventaire par-dessus", () -> flag("isPaused") && !flag("isInventoryOpen"));
        key(Input.Keys.I);
        check("pause : la souris reste libre", () -> flag("isPaused") && !Gdx.input.isCursorCatched());
        shot("r00_pause_apres_I");
        key(Input.Keys.ESCAPE);
        check("pause fermee : la souris est de nouveau capturee", () -> !flag("isPaused") && Gdx.input.isCursorCatched());

        step(() -> game.setScreen(new InnScreen(game, "facile")));
        frames(5);
        step(() -> invokeInn("Innkeeper"));
        key(Input.Keys.I);
        key(Input.Keys.I);
        check("dialogue : apres I, I la souris reste libre pour cliquer", () -> flag("inDialog") && !flag("isInventoryOpen") && !Gdx.input.isCursorCatched());
        shot("r01_dialogue_apres_I");
        key(Input.Keys.ESCAPE);

        // ---------- 3) a la 3e personne, dans un coin, la camera ne passe pas a travers les murs
        step(() -> setThirdPerson(true));
        frames(3);
        step(() -> teleport(8.9f, 1.6f, 8.9f));
        frames(20);
        step(() -> { worstGap = sweepCamera(9.5f); log(String.format("3e personne dans un coin : la camera passe au plus pres a %.2f m d'un mur", worstGap)); });
        check("3e personne dans un coin : la camera reste a plus de 0,16 m des murs (plan proche)", () -> worstGap >= 0.16f);
        step(() -> look(205f, -12f));
        frames(3);
        shot("r10_coin_camera_a");
        step(() -> look(250f, 10f));
        frames(3);
        shot("r11_coin_camera_b");
        step(() -> look(30f, -30f));
        frames(3);
        shot("r12_coin_camera_c");
        step(() -> setThirdPerson(false));

        // ---------- 4) garder Espace enfonce : un saut a la fois, on ne s'envole pas
        step(() -> game.setScreen(new TownScreen(game, "facile", new Vector3(0, 2, 40))));
        frames(40);
        step(() -> maxY = 0f);
        keyDownHold(Input.Keys.SPACE);
        for (int i = 0; i < 300; i++) {
            step(() -> { render1(); maxY = Math.max(maxY, pc().getPlayerBody().getCenterOfMassPosition().y); });
        }
        keyUpHold(Input.Keys.SPACE);
        step(() -> log(String.format("Espace enfonce 5 s : hauteur max du joueur %.2f m (au sol : 1,5)", maxY)));
        check("Espace enfonce : on ne monte pas plus haut qu'un saut", () -> maxY < 3f);
        frames(60);
        shot("r20_apres_sauts");

        // ---------- 5) l'etage de l'auberge reste accessible a pied : escalier (W + Espace), palier, lit
        step(() -> game.setScreen(new InnScreen(game, "facile")));
        frames(5);
        step(() -> { teleport(-5.2f, 1.6f, 8.5f); look(90f, 0f); });   // au pied de l'escalier, de cote
        frames(20);
        shot("r30_pied_escalier");
        keyDownHold(Input.Keys.W); keyDownHold(Input.Keys.SPACE);
        frames(45);
        step(() -> look(0f, 0f));                                      // puis on monte, vers la porte
        frames(700);
        keyUpHold(Input.Keys.SPACE); keyUpHold(Input.Keys.W);
        frames(30);
        step(() -> { Vector3 p = pc().getPlayerBody().getCenterOfMassPosition(); log(String.format("haut de l'escalier : x %.1f  y %.1f  z %.1f", p.x, p.y, p.z)); });
        check("on monte l'escalier de l'auberge en sautant de marche en marche", () -> pc().getPlayerBody().getCenterOfMassPosition().y > 11.2f);
        shot("r31_haut_escalier");
        step(() -> look(-90f, 0f));                                    // le palier, vers l'etage
        keyDownHold(Input.Keys.W); frames(50); keyUpHold(Input.Keys.W);
        step(() -> faceAt(5f, 11.5f, 5f));                             // le lit
        keyDownHold(Input.Keys.W); frames(55); keyUpHold(Input.Keys.W);
        frames(10);
        step(() -> { faceAt(5f, 11.5f, 5f); intBefore1 = player().getGold(); });
        frames(3);
        shot("r32_devant_le_lit");
        key(Input.Keys.E);
        frames(3);
        check("on atteint le lit a pied et on y dort (10 Po)", () -> player().getGold() == intBefore1 - 10);
    }

    double distEmpty, distFull;
    int intBefore1, intBefore2;

    Adventurer player() { return game.getSession().getPlayer(); }

    static boolean hasItem(Adventurer p, String id) {
        for (Item i : p.getInventoryItems()) if (id.equals(i.getId())) return true;
        return false;
    }

    /** Boutique, inventaire, equipement, or, encombrement, auberge payante. */
    void inventorySection() {
        step(() -> game.startNewGame("normale"));   // partie neuve : 60 Po, 1 potion

        // ---------- Guilde : boutique de materiel
        step(() -> game.setScreen(new GuildScreen(game, "normale")));
        frames(5);
        turn(0, 1.5f, 10);
        keyDownHold(Input.Keys.W); frames(105); keyUpHold(Input.Keys.W);
        frames(2);
        key(Input.Keys.E);
        click("Acheter du matériel");
        shot("i01_guild_shop");
        clickNth("Acheter", 4);   // Besace, 20 Po
        check("besace achetee : or 60 -> 40", () -> player().getGold() == 40 && hasItem(player(), "pouch"));
        clickNth("Acheter", 5);   // Sac a dos, 60 Po : trop cher
        shot("i02_shop_not_enough_gold");
        check("achat refuse faute d'or : or inchange, pas de sac a dos", () -> player().getGold() == 40 && !hasItem(player(), "backpack"));
        clickNth("Acheter", 1);   // Grande potion de soin, 30 Po
        check("grande potion achetee : or 10", () -> player().getGold() == 10 && hasItem(player(), "greater_healing_potion"));
        shot("i03_shop_after_buys");
        clickNth("Vendre", 2);    // la grande potion (3e pile : potion, besace, grande potion)
        check("revente de la grande potion : +15 Po (50 % de 30)", () -> player().getGold() == 25 && !hasItem(player(), "greater_healing_potion"));
        click("Retour");
        key(Input.Keys.ESCAPE);   // ferme le dialogue du maitre de guilde

        // ---------- Inventaire : equiper le sac, utiliser une potion
        key(Input.Keys.I);
        shot("i04_inventory_before_equip");
        click("Équiper");
        check("besace equipee : capacite 30 -> 35 kg, la besace compte dans le poids (0,5 + 0,5)", () -> {
            Inventaire inv = player().getInventaire();
            return inv.getBag() != null && inv.getPoidsMax() == 35.0 && Math.abs(inv.getPoidsActuel() - 1.0) < 1e-9;
        });
        shot("i05_inventory_bag_equipped");
        key(Input.Keys.I);   // ferme
        step(() -> { player().perdreVie(50); intBefore1 = player().getHealth(); });
        key(Input.Keys.I);   // rouvre : le panneau se reconstruit
        click("Utiliser");
        check("potion de soin utilisee : +25 PV, potion consommee", () -> player().getHealth() == intBefore1 + 25 && !hasItem(player(), "healing_potion"));
        shot("i06_after_use_potion");
        key(Input.Keys.I);

        // ---------- Encombrement : vitesse de marche a vide puis a pleine charge
        step(() -> game.setScreen(new TownScreen(game, "normale", new Vector3(0, 2, 0))));
        frames(5);
        step(() -> face(0, 2, 100));
        frames(2);
        step(() -> posBefore = playerPos());
        keyDownHold(Input.Keys.W); frames(60); keyUpHold(Input.Keys.W);
        step(() -> distEmpty = dist(posBefore, playerPos()));
        step(() -> {
            Inventaire inv = player().getInventaire();
            while (inv.peutPorter(GameContent.newItem("healing_potion"))) player().addItem(GameContent.newItem("healing_potion"));
        });
        step(() -> game.setScreen(new TownScreen(game, "normale", new Vector3(0, 2, 0))));
        frames(5);
        step(() -> face(0, 2, 100));
        frames(2);
        step(() -> posBefore = playerPos());
        keyDownHold(Input.Keys.W); frames(60); keyUpHold(Input.Keys.W);
        step(() -> distFull = dist(posBefore, playerPos()));
        check("pleine charge (35/35 kg, normale) : on marche a ~75 % de la vitesse", () -> {
            double ratio = distFull / distEmpty;
            log(String.format("distance en 1 s : a vide = %.2f m, pleine charge = %.2f m, ratio = %.3f (attendu 0,75)", distEmpty, distFull, ratio));
            return distEmpty > 5 && ratio > 0.71 && ratio < 0.79;
        });
        key(Input.Keys.I);
        shot("i07_inventory_full_load");
        key(Input.Keys.I);
        step(() -> { // on se delleste pour la suite
            Inventaire inv = player().getInventaire();
            for (Item it : new ArrayList<>(inv.getItems())) if (it instanceof PotionSoin) inv.retirerItem(it);
        });

        // ---------- Forge : acheter une arme, l'equiper
        step(() -> player().addGold(200));
        step(() -> game.setScreen(new ForgeScreen(game, "normale")));
        frames(5);
        turn(2, 2.5f, 0);
        keyDownHold(Input.Keys.W); frames(38); keyUpHold(Input.Keys.W);
        frames(2);
        key(Input.Keys.E);
        click("Armes et armures");
        shot("i20_forge_shop");
        clickNth("Acheter", 1);   // Epee courte, 40 Po
        // 65 Po echanges plus haut ont debloque Marchandage 1 : -2 % -> epee 40 Po payee 39 Po
        check("epee courte achetee avec la reduction du Marchandage : or 225 -> 186", () -> player().getGold() == 186 && hasItem(player(), "short_sword"));
        click("Retour");
        key(Input.Keys.ESCAPE);
        key(Input.Keys.I);
        click("Équiper");
        check("epee courte equipee : +6 degats", () -> player().getBonusDegats() == 6 && player().getInventaire().getWeapon() != null);
        shot("i21_inventory_weapon_equipped");
        key(Input.Keys.I);

        // ---------- Auberge : le repas et la nuit sont payants
        step(() -> game.setScreen(new InnScreen(game, "normale")));
        frames(5);
        turn(0, 1.5f, 5);
        keyDownHold(Input.Keys.W); frames(68); keyUpHold(Input.Keys.W);
        frames(2);
        step(() -> { player().perdreVie(60); intBefore1 = player().getGold(); intBefore2 = player().getHealth(); });
        key(Input.Keys.E);
        shot("i30_inn_dialog_prices");
        click("Manger un repas (5 Po)");
        check("repas : -5 Po et +20 PV", () -> player().getGold() == intBefore1 - 5 && player().getHealth() == intBefore2 + 20);
        frames(2);
        shot("i31_after_meal_message");
        step(() -> { player().spendGold(player().getGold()); intBefore1 = game.getSession().getClock().getCurrentDay(); });
        step(() -> invokeInn("Bed"));
        check("sans or : la nuit est refusee, le temps n'avance pas", () -> player().getGold() == 0 && game.getSession().getClock().getCurrentDay() == intBefore1);
        frames(2);
        shot("i32_bed_not_enough_gold");
        step(() -> player().addGold(50));
        step(() -> invokeInn("Bed"));
        check("avec de l'or : -10 Po, jour suivant 07:00, vie au maximum", () -> {
            GameClock c = game.getSession().getClock();
            return player().getGold() == 40 && c.getCurrentDay() == intBefore1 + 1 && c.getCurrentHour() == 7 && player().getHealth() == player().getMaxHealth();
        });
        frames(2);
        shot("i33_bed_slept");

        // ---------- File de messages : un message en attente ne doit pas patienter 5 s derriere le precedent
        frames(320);   // 5,3 s : le message precedent (le reveil) a fini de s'afficher
        step(() -> {
            GameClock c = game.getSession().getClock();
            while (c.pollMessage() != null) { }   // vide la file
            c.postMessage("Premier message");
            c.postMessage("Pas assez d'or : message urgent");
        });
        frames(150);   // 2,5 s a 60 images/s : le premier a cede la place apres 2 s
        shot("i40_toast_queue_second_message_after_2s");
    }

    // ------------------------------------------------------------------ creation de personnage et competences

    static Object fieldOf(Object o, String name) {
        try {
            for (Class<?> c = o.getClass(); c != null; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (f.getName().equals(name)) { f.setAccessible(true); return f.get(o); }
                }
            }
        } catch (Exception e) { throw new RuntimeException(e); }
        throw new RuntimeException("champ introuvable : " + name);
    }

    void setNameField(String text) {
        com.badlogic.gdx.scenes.scene2d.ui.TextField tf = (com.badlogic.gdx.scenes.scene2d.ui.TextField) fieldOf(cur(), "nameField");
        tf.setText(text);
    }

    double athleticsBefore, carryingBefore;

    void creationSection() {
        // ---------- le menu mene a la creation, etape par etape
        step(() -> game.setScreen(new MainMenuScreen(game)));
        frames(3);
        click("Start");
        click("Nouvelle partie");
        click("Facile");
        frames(3);
        check("apres la difficulte : ecran de creation du personnage", () -> cur() instanceof CharacterCreationScreen);
        shot("c01_name_step");

        step(() -> setNameField(""));
        click("Suivant");
        frames(2);
        shot("c02_empty_name_refused");
        check("nom vide refuse : on reste a l'etape 1", () -> ((Integer) fieldOf(cur(), "step")) == 0);

        step(() -> setNameField("Ayla"));
        click("Suivant");
        frames(2);
        shot("c10_class_nothing_selected");
        click("Suivant");
        frames(2);
        shot("c11_class_required_message");
        check("sans classe choisie : on ne passe pas a l'etape suivante", () -> ((Integer) fieldOf(cur(), "step")) == 1);
        click("Guerrier");
        shot("c12_class_warrior");
        click("Marchand");
        shot("c13_class_merchant");
        click("Herboriste");
        shot("c14_class_herbalist");
        click("Éclaireur");
        shot("c15_class_scout");

        // revenir en arriere ne perd rien
        click("Précédent");
        frames(2);
        check("retour a l'etape 1 : le nom saisi est conserve", () -> "Ayla".equals(((com.badlogic.gdx.scenes.scene2d.ui.TextField) fieldOf(cur(), "nameField")).getText()));
        click("Suivant");
        frames(2);
        check("la classe choisie est conservee", () -> fieldOf(cur(), "chosenClass") != null);
        click("Suivant");
        frames(2);
        shot("c20_background_step");
        check("etape 3", () -> ((Integer) fieldOf(cur(), "step")) == 2);
        click("Commencer l'aventure");
        frames(2);
        shot("c21_background_required_message");
        check("sans origine choisie : la partie ne demarre pas", () -> cur() instanceof CharacterCreationScreen);
        click("Noble déchu");
        shot("c22_background_noble");
        click("Orphelin des rues");
        shot("c23_background_orphan_with_summary");
        click("Commencer l'aventure");
        frames(40);
        check("la partie demarre dans la ville", () -> cur() instanceof TownScreen);
        check("le personnage cree : Ayla, Eclaireur, Orphelin, 85 PV, 70 Po (facile 100 - 30)", () -> {
            Adventurer p = player();
            return p.getName().equals("Ayla") && "scout".equals(p.getClassId()) && "orphan".equals(p.getBackgroundId())
                    && p.getMaxHealth() == 85 && p.getGold() == 70 && p.getInventaire().getWeapon() != null;
        });
        check("difficulte facile conservee : 2 potions + besace portee", () -> player().getInventoryItems().size() == 2 && player().getInventaire().getBag() != null);
        shot("c30_town_after_creation");

        // ---------- panneau du personnage (K)
        key(Input.Keys.K);
        check("K ouvre le panneau du personnage", () -> (Boolean) fieldOf(cur(), "isSkillsOpen"));
        shot("c31_skills_panel_start");
        key(Input.Keys.I);
        check("I ne s'ouvre pas par-dessus le panneau du personnage", () -> !((Boolean) fieldOf(cur(), "isInventoryOpen")));
        key(Input.Keys.K);
        check("K ferme le panneau", () -> !((Boolean) fieldOf(cur(), "isSkillsOpen")));
        key(Input.Keys.I);
        key(Input.Keys.K);
        check("K ne s'ouvre pas par-dessus l'inventaire", () -> !((Boolean) fieldOf(cur(), "isSkillsOpen")));
        key(Input.Keys.I);
        key(Input.Keys.K);
        key(Input.Keys.ESCAPE);
        check("Echap ferme le panneau du personnage (sans ouvrir la pause)", () -> !((Boolean) fieldOf(cur(), "isSkillsOpen")) && !((Boolean) fieldOf(cur(), "isPaused")));

        // ---------- la marche entraine l'Endurance (pas le Portage a vide)
        step(() -> face(0, 2, 100));
        frames(2);
        step(() -> { athleticsBefore = player().getSkills().getPractice("athletics"); carryingBefore = player().getSkills().getPractice("carrying"); posBefore = playerPos(); });
        keyDownHold(Input.Keys.W); frames(90); keyUpHold(Input.Keys.W);
        check("1,5 s de marche : l'Endurance progresse de ~ la distance x 1,7 (bonus Eclaireur +50 %, Orphelin +20 %) ; Portage inchange (peu charge)", () -> {
            double walked = dist(posBefore, playerPos());
            double gained = player().getSkills().getPractice("athletics") - athleticsBefore;
            log(String.format("distance marchee = %.2f m, Endurance gagnee = %.2f (attendu ~ %.2f)", walked, gained, walked * 1.7));
            return walked > 5 && Math.abs(gained - walked * 1.7) < walked * 0.08 + 0.5 && player().getSkills().getPractice("carrying") == carryingBefore;
        });

        // ---------- chargee a plus de la moitie, la marche entraine aussi le Portage
        step(() -> {
            Inventaire inv = player().getInventaire();
            while (inv.getPoidsActuel() < inv.getPoidsMax() * 0.6 && inv.peutPorter(GameContent.newItem("healing_potion"))) player().addItem(GameContent.newItem("healing_potion"));
        });
        step(() -> { carryingBefore = player().getSkills().getPractice("carrying"); posBefore = playerPos(); });
        step(() -> face(0, 2, -100));
        frames(2);
        keyDownHold(Input.Keys.W); frames(60); keyUpHold(Input.Keys.W);
        check("avec une charge > 50 % : le Portage progresse aussi", () -> player().getSkills().getPractice("carrying") > carryingBefore + 3);

        // ---------- un niveau gagne : message a l'ecran et effets
        step(() -> player().train("athletics", 1100));   // x1,5 = 1650 : Endurance niveau 2 (2000)
        frames(3);
        shot("c40_level_up_toast");
        check("Endurance niveau 2 : message de niveau en file ou affiche, +4 % de vitesse de depart", () -> player().getSkills().getLevel("athletics") >= 2);
        key(Input.Keys.K);
        shot("c41_skills_panel_after");
        key(Input.Keys.K);

        // ---------- un marchand : prix reduits dans la boutique, competence deja debloquee
        step(() -> game.startNewGame("normale", "Marco", "merchant", "noble"));
        step(() -> game.setScreen(new GuildScreen(game, "normale")));
        frames(5);
        turn(0, 1.5f, 10);
        keyDownHold(Input.Keys.W); frames(105); keyUpHold(Input.Keys.W);
        frames(2);
        key(Input.Keys.E);
        click("Acheter du matériel");
        shot("c50_merchant_shop_discount");
        check("sac a dos : 60 Po -> 59 Po (-2 %, Marchandage 1)", () -> new com.lucas.guild.game.Shop("guild").buyPrice(player(), GameContent.newItem("backpack")) == 59);
        clickNth("Acheter", 5);   // sac a dos
        check("achat au prix reduit : 140 -> 81 Po", () -> player().getGold() == 81 && hasItem(player(), "backpack"));
        check("l'achat entraine le Marchandage (59 x 1,7 de bonus de progression)", () -> player().getSkills().getPractice("haggling") > 140);
        click("Retour");
        key(Input.Keys.ESCAPE);
        key(Input.Keys.K);
        shot("c51_merchant_skills_panel");
        key(Input.Keys.K);

        // ---------- liste longue : l'inventaire defile (zone limitee a la hauteur de l'ecran)
        step(() -> {
            player().getInventaire().setBaseBonus(500);
            for (Item it : GameContent.items()) player().addItem(GameContent.newItem(it.getId()));
        });
        key(Input.Keys.I);
        shot("c60_inventory_long_list_scrollable");
        key(Input.Keys.I);
    }

    void debugSection() {
        step(() -> { game.startNewGame("normale"); game.setScreen(new TownScreen(game, "normale")); });
        frames(40);
        // ---------- debug eteint : F10 ne fait rien
        key(Input.Keys.F10);
        check("debug eteint : F10 n'ouvre rien", () -> !((Boolean) fieldOf(cur(), "isDebugOpen")));
        step(() -> com.lucas.guild.game.DebugMode.setEnabled(true));
        frames(2);
        shot("d00_town_debug_hud");
        key(Input.Keys.F10);
        check("debug allume : F10 ouvre le panneau", () -> (Boolean) fieldOf(cur(), "isDebugOpen"));
        shot("d01_debug_panel");
        click("+1000 Po");
        check("+1000 Po", () -> player().getGold() == 1060);
        click("Vieillir +25 ans");
        check("vieillir de 25 ans : 45 ans", () -> player().getAge(game.getSession().getClock().getCurrentDay()) == 45);
        click("+1 saison");
        check("+1 saison : jour 29", () -> game.getSession().getClock().getCurrentDay() == 29);
        click("Endurance 0/5");
        check("Endurance niveau 1", () -> player().getSkills().getLevel("athletics") == 1);
        // les objets sont tout en bas du panneau (a faire defiler) : la logique est testee par DebugTest
        step(() -> new com.lucas.guild.game.DebugActions(game.getSession()).giveItem("long_sword"));
        check("epee longue donnee", () -> hasItem(player(), "long_sword"));
        click("Temps x60 : non");
        check("temps x60 actif", () -> game.getSession().getClock().getTimeScale() == 60f);
        click("Vitesse x3 : non");
        shot("d02_debug_panel_after_actions");
        key(Input.Keys.ESCAPE);
        check("Echap ferme le panneau sans ouvrir la pause", () -> !((Boolean) fieldOf(cur(), "isDebugOpen")) && !((Boolean) fieldOf(cur(), "isPaused")));
        // vitesse x3 : distance parcourue en 1 s
        step(() -> face(0, 2, 100));
        frames(2);
        step(() -> posBefore = playerPos());
        keyDownHold(Input.Keys.W); frames(60); keyUpHold(Input.Keys.W);
        check("vitesse x3 : on parcourt au moins 2,5 fois plus qu'a pied (~8 m/s)", () -> {
            double d = dist(posBefore, playerPos());
            log(String.format("debug vitesse x3 : %.2f m en 1 s", d));
            return d > 20;
        });
        key(Input.Keys.F10);
        click("Forge");
        frames(5);
        check("raccourci : Forge", () -> cur() instanceof ForgeScreen);
        key(Input.Keys.F10);
        click("Ville");
        frames(5);
        check("raccourci : Ville", () -> cur() instanceof TownScreen);
        step(() -> com.lucas.guild.game.DebugMode.setEnabled(false));
    }

    void questSection() {
        step(() -> game.startNewGame("normale"));   // 60 Po
        step(() -> game.setScreen(new GuildScreen(game, "normale")));
        frames(5);
        turn(0, 1.5f, 10);
        keyDownHold(Input.Keys.W); frames(105); keyUpHold(Input.Keys.W);
        frames(2);
        key(Input.Keys.E);
        click("Voir les quêtes");
        shot("q01_quest_board");
        click("> Nettoyage de la cave (20 Po)");
        shot("q02_quest_detail_available");
        click("Retour au tableau");
        click("Livraison express (10 Po)");
        click("Accepter la quête");
        shot("q03_quest_accepted");
        check("quete acceptee : active, lettre a livrer dans le sac", () -> player().getActiveQuests().contains("Livraison express")
                && player().getInventoryItems().stream().anyMatch(i -> "Lettre à livrer".equals(i.getName())));
        click("Rendre la quête");
        shot("q04_turn_in_refused_no_proof");
        check("rendre sans preuve : refuse, pas d'or", () -> player().getGold() == 60);
        key(Input.Keys.ESCAPE);

        // ---------- la lettre se remet au forgeron
        step(() -> game.setScreen(new ForgeScreen(game, "normale")));
        frames(5);
        turn(2, 2.5f, 0);
        keyDownHold(Input.Keys.W); frames(38); keyUpHold(Input.Keys.W);
        frames(2);
        key(Input.Keys.E);
        shot("q10_forge_dialog_with_letter");
        click("Remettre la lettre");
        check("lettre remise : preuve recue a la place", () -> player().getInventoryItems().stream().anyMatch(i -> "Lettre signée".equals(i.getName()))
                && player().getInventoryItems().stream().noneMatch(i -> "Lettre à livrer".equals(i.getName())));
        frames(2);
        shot("q11_forge_after_delivery_toast");

        // ---------- retour a la guilde : l'or
        step(() -> game.setScreen(new GuildScreen(game, "normale")));
        frames(5);
        turn(0, 1.5f, 10);
        keyDownHold(Input.Keys.W); frames(105); keyUpHold(Input.Keys.W);
        frames(2);
        key(Input.Keys.E);
        click("Voir les quêtes");
        shot("q20_board_ready_to_turn_in");
        click("Livraison express (10 Po) [à rendre]");
        click("Rendre la quête");
        shot("q21_quest_done_reward");
        check("quete rendue : +10 Po, terminee", () -> player().getGold() == 70 && player().getDoneQuests().contains("Livraison express") && player().getActiveQuests().isEmpty());
        click("Retour au tableau");
        shot("q22_board_done");
    }

    // ---- combat
    Object combatOf() { return fieldOf(cur(), "combat"); }

    int monsterCount() {
        try {
            Object c = combatOf();
            java.lang.reflect.Method m = c.getClass().getDeclaredMethod("count");
            m.setAccessible(true);
            return (Integer) m.invoke(c);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    /** Se tourne vers le monstre le plus proche (a hauteur des yeux, un peu vers le bas). */
    void faceNearestMonster() {
        try {
            Object c = combatOf();
            double[] p = playerPos();
            java.lang.reflect.Method m = c.getClass().getDeclaredMethod("nearest", float.class, float.class);
            m.setAccessible(true);
            Object mon = m.invoke(c, (float) p[0], (float) p[2]);
            if (mon == null) return;
            float mx = mon.getClass().getField("x").getFloat(mon);
            float mz = mon.getClass().getField("z").getFloat(mon);
            face(mx, 0.9f, mz);
            Object pc = findPlayerController(cur());
            double dx = mx - p[0], dz = mz - p[2];
            double horiz = Math.sqrt(dx * dx + dz * dz);
            float pitch = (float) Math.toDegrees(Math.atan2(0.9 - (p[1] + 0.8), horiz));
            Field pf = PlayerController.class.getDeclaredField("pitch");
            pf.setAccessible(true);
            pf.setFloat(pc, pitch);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    /** Se tourne vers un point (x, y, z), pitch compris. */
    void faceAt(float tx, float ty, float tz) {
        try {
            Object pc = findPlayerController(cur());
            Vector3 p = ((PlayerController) pc).getPlayerBody().getCenterOfMassPosition();
            double dx = tx - p.x, dz = tz - p.z;
            Field f = PlayerController.class.getDeclaredField("yaw"); f.setAccessible(true);
            f.setFloat(pc, (float) Math.toDegrees(Math.atan2(-dx, -dz)));
            Field pf = PlayerController.class.getDeclaredField("pitch"); pf.setAccessible(true);
            pf.setFloat(pc, (float) Math.toDegrees(Math.atan2(ty - (p.y + 0.8), Math.sqrt(dx * dx + dz * dz))));
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    void click1() {
        steps.add(() -> { ((InputProcessor) cur()).touchDown(400, 300, 0, Input.Buttons.LEFT); render1(); });
    }

    double meleeBefore, robustBefore;
    int goldBefore, hpBefore;
    int dayBefore, hourBefore;

    void combatSection() {
        step(() -> { game.startNewGame("normale"); com.lucas.guild.game.DebugMode.setEnabled(false); });
        // une quete de combat acceptee : la queue de rat tombera
        step(() -> new com.lucas.guild.game.QuestLog(player()).accept(GameContent.quests().get(0)));
        step(() -> player().addGold(0));
        step(() -> game.setScreen(new InnScreen(game, "normale")));
        frames(5);
        // la trappe de la cave est a (6, -4) : on s'en approche et on la vise
        step(() -> faceAt(6, 0.65f, -4));
        keyDownHold(Input.Keys.W); frames(30); keyUpHold(Input.Keys.W);
        step(() -> faceAt(6, 0.65f, -4));
        frames(2);
        shot("m01_inn_trapdoor_prompt");
        key(Input.Keys.E);
        frames(5);
        check("la trappe mene a la cave", () -> cur() instanceof CellarScreen);
        check("3 rats dans la cave", () -> monsterCount() == 3);
        shot("m02_cellar_arrival");

        // ---------- les rats approchent et mordent
        step(() -> { goldBefore = player().getGold(); hpBefore = player().getHealth(); robustBefore = player().getSkills().getPractice("robustness"); meleeBefore = player().getSkills().getPractice("melee"); });
        step(() -> face(0, 1.5f, 5));
        frames(150);
        shot("m03_rats_attacking_red_flash_maybe");
        check("au contact, les rats font perdre de la vie", () -> player().getHealth() < hpBefore);
        check("encaisser entraine Robustesse", () -> player().getSkills().getPractice("robustness") > robustBefore);

        // ---------- on les combat au clic gauche (cooldown 0,6 s : 40 images entre deux coups)
        for (int i = 0; i < 14; i++) {
            step(this::faceNearestMonster);
            frames(2);
            click1();
            frames(38);
            if (i == 1) shot("m10_fighting_target_bar");
        }
        check("des rats sont morts", () -> monsterCount() < 3);
        check("frapper entraine la Melee", () -> player().getSkills().getPractice("melee") > meleeBefore);
        check("or lache par les rats", () -> player().getGold() > goldBefore);
        check("la queue de rat est lachee (quete active)", () -> player().getInventoryItems().stream().anyMatch(i -> "Queue de rat".equals(i.getName())));
        shot("m11_after_fight_loot_toast");

        // ---------- potion rapide (Q)
        step(() -> { player().perdreVie(40); hpBefore = player().getHealth(); });
        key(Input.Keys.Q);
        check("Q boit la potion de soin : +25 PV", () -> player().getHealth() == hpBefore + 25 && player().getInventoryItems().stream().noneMatch(i -> "healing_potion".equals(i.getId())));

        // ---------- mort : reveil a l'auberge
        step(() -> { goldBefore = player().getGold(); dayBefore = game.getSession().getClock().getCurrentDay(); hourBefore = game.getSession().getClock().getCurrentHour(); });
        step(() -> game.setScreen(new CellarScreen(game, "normale")));
        frames(5);
        step(() -> player().perdreVie(player().getHealth() - 1));   // 1 PV
        step(() -> face(0, 1.5f, 5));
        frames(400);
        check("mort : on se reveille a l'auberge", () -> cur() instanceof InnScreen);
        check("mort : moitie des PV, 10 % d'or en moins", () -> player().getHealth() == player().getMaxHealth() / 2 && player().getGold() <= goldBefore - goldBefore / 10 + 15);
        check("mort : 6 heures ont passe", () -> {
            GameClock c = game.getSession().getClock();
            int elapsed = (c.getCurrentDay() - dayBefore) * 24 + (c.getCurrentHour() - hourBefore);
            return elapsed >= 6 && elapsed <= 7;
        });
        frames(3);
        shot("m20_woke_up_at_inn");

        // ---------- le debug : rat devant moi, invincible
        step(() -> com.lucas.guild.game.DebugMode.setEnabled(true));
        key(Input.Keys.F10);
        click("Rat devant moi");
        click("Invincible : non");
        shot("m30_debug_combat_buttons");
        key(Input.Keys.ESCAPE);
        step(() -> hpBefore = player().getHealth());
        frames(300);
        check("invincible : les coups du rat ne retirent rien", () -> player().getHealth() == hpBefore && monsterCount() == 1);
        step(() -> com.lucas.guild.game.DebugMode.setEnabled(false));
    }

    // ------------------------------------------------------------------ zones sauvages (7b)

    int groundCount(String key) {
        return game.getSession().getGround().in(key).size();
    }

    int tripStart;

    void wildSection() {
        step(() -> { game.startNewGame("facile"); com.lucas.guild.game.DebugMode.setEnabled(false); });

        // ---------- la carte montre les zones sauvages
        step(() -> game.setScreen(new TownScreen(game, "facile", new Vector3(0, 2, 52))));
        frames(5);
        step(() -> faceAt(0, 2.5f, 60));
        frames(2);
        key(Input.Keys.E);
        frames(3);
        check("la carte est ouverte", () -> cur() instanceof WorldMapScreen);
        shot("z00_map_with_wild_zones");

        // ---------- arrivee dans la foret des loups (en marchant 3 h)
        step(() -> { tripStart = minutesNow(); com.lucas.guild.game.World.travel(game.getSession(), "foret_loups"); game.setScreen(game.screenAt("foret_loups", "facile")); });
        frames(40);
        check("on est dans la zone sauvage", () -> cur() instanceof ZoneScreen);
        check("la foret est le lieu courant, la ville de reveil reste Aethelgard", () -> "foret_loups".equals(player().getTownId()) && "aethelgard".equals(player().getHomeTownId()));
        check("6 loups dans la foret", () -> monsterCount() == 6);
        shot("z01_forest_arrival");

        // ---------- la borne de route
        step(() -> faceAt(0, 4.4f, 76));
        frames(2);
        shot("z02_road_post_prompt");

        // ---------- loups : on les combat (invincible), le butin tombe au sol
        step(() -> com.lucas.guild.game.DebugMode.setEnabled(true));
        key(Input.Keys.F10);
        click("Loup devant moi");
        click("Loup devant moi");
        click("Loup devant moi");
        click("Invincible : non");
        shot("z10_debug_wolf_buttons");
        key(Input.Keys.ESCAPE);
        step(() -> { goldBefore = player().getGold(); meleeBefore = player().getSkills().getPractice("melee"); });
        frames(60);
        shot("z11_wolves_charging");
        for (int i = 0; i < 24; i++) {
            step(this::faceNearestMonster);
            frames(2);
            click1();
            frames(38);
        }
        check("les loups du joueur sont morts (il reste les 6 de la foret)", () -> monsterCount() <= 6);
        check("or lache", () -> player().getGold() >= goldBefore);
        check("du butin est tombe au sol", () -> groundCount("ZoneScreen@foret_loups") >= 1);
        frames(3);
        shot("z12_loot_on_ground");
        step(() -> com.lucas.guild.game.DebugMode.setInvincible(false));

        // ---------- ramasser (E) : on se met sur le butin
        step(() -> {
            com.lucas.guild.game.GroundItems.Drop d = game.getSession().getGround().in("ZoneScreen@foret_loups").get(0);
            game.setScreen(new ZoneScreen(game, "facile", new Vector3(d.x, 2, d.z + 1.5f)));
        });
        frames(40);
        check("le butin est toujours la apres avoir change d'ecran", () -> groundCount("ZoneScreen@foret_loups") >= 1);
        shot("z13_pickup_prompt");
        step(() -> { dayBefore = player().getInventoryItems().size(); hpBefore = groundCount("ZoneScreen@foret_loups"); });
        key(Input.Keys.E);
        check("E ramasse : un objet de plus dans le sac, un de moins au sol", () -> player().getInventoryItems().size() == dayBefore + 1 && groundCount("ZoneScreen@foret_loups") == hpBefore - 1);
        shot("z14_after_pickup");

        // ---------- jeter depuis l'inventaire : l'objet tombe devant soi
        key(Input.Keys.I);
        step(() -> { dayBefore = player().getInventoryItems().size(); hpBefore = groundCount("ZoneScreen@foret_loups"); });
        clickNth("Jeter", 0);
        check("Jeter : l'objet quitte le sac et tombe au sol", () -> player().getInventoryItems().size() == dayBefore - 1 && groundCount("ZoneScreen@foret_loups") == hpBefore + 1);
        shot("z20_inventory_after_drop");
        key(Input.Keys.I);
        frames(3);
        shot("z21_item_dropped_in_front");

        // ---------- le coffre (-36, 8) : or et potion, puis vide
        step(() -> game.setScreen(new ZoneScreen(game, "facile", new Vector3(-36, 2, 14))));
        frames(40);
        step(() -> faceAt(-36, 0.9f, 8));
        frames(2);
        shot("z30_chest_prompt");
        step(() -> { goldBefore = player().getGold(); dayBefore = player().getInventoryItems().size(); });
        key(Input.Keys.E);
        frames(3);
        check("le coffre donne 18 Po et une potion", () -> player().getGold() == goldBefore + 18 && player().getInventoryItems().size() == dayBefore + 1);
        shot("z31_chest_opened");
        step(() -> goldBefore = player().getGold());
        key(Input.Keys.E);
        check("le coffre ouvert est vide", () -> player().getGold() == goldBefore);
        // le coffre reste vide si on revient (etat sauvegarde dans le personnage)
        step(() -> game.setScreen(new ZoneScreen(game, "facile", new Vector3(-36, 2, 14))));
        frames(40);
        step(() -> faceAt(-36, 0.9f, 8));
        frames(2);
        shot("z32_chest_stays_empty");

        // ---------- sauvegarde dans la zone puis Continuer : on reprend dans la zone
        key(Input.Keys.F5);
        step(() -> game.setScreen(new MainMenuScreen(game)));
        frames(3);
        click("Start");
        click("Continuer");
        frames(40);
        check("Continuer reprend dans la zone sauvage", () -> cur() instanceof ZoneScreen && "foret_loups".equals(player().getTownId()));
        shot("z33_continue_in_forest");

        // ---------- les ruines gobelines (par la carte de debug)
        step(() -> { game.getSession().getPlayer().setTownId("ruines_gobelines"); game.getSession().getPlayer().markVisited("ruines_gobelines"); game.setScreen(game.screenAt("ruines_gobelines", "facile")); });
        frames(40);
        check("ruines : 5 gobelins", () -> monsterCount() == 5);
        shot("z35_ruins_arrival");
        step(() -> faceAt(0, 1.5f, -30));
        frames(2);
        shot("z36_ruins_view");

        // ---------- embuscade (forcee) entre Aethelgard et Port-Brume
        step(() -> { com.lucas.guild.game.DebugMode.setEnabled(true); com.lucas.guild.game.DebugMode.setForceAmbush(true); com.lucas.guild.game.DebugMode.setInvincible(true);
                     game.getSession().getPlayer().setTownId("aethelgard"); });
        step(() -> game.setScreen(new TownScreen(game, "facile", new Vector3(0, 2, 52))));
        frames(5);
        step(() -> faceAt(0, 2.5f, 60));
        frames(2);
        key(Input.Keys.E);
        frames(3);
        step(() -> tripStart = minutesNow());
        click("Port-Brume - 33 km - 6 h 35");
        frames(40);
        check("embuscade : on est dans le decor de route", () -> cur() instanceof ZoneScreen);
        check("embuscade : on n'est pas encore arrive", () -> "aethelgard".equals(player().getTownId()) && game.getSession().getPendingDestination() != null);
        check("embuscade : 1 ou 2 agresseurs", () -> monsterCount() >= 1 && monsterCount() <= 2);
        check("embuscade : la moitie du trajet a passe", () -> minutesNow() - tripStart > 100 && minutesNow() - tripStart < 395);
        shot("z40_ambush_start");
        step(() -> faceAt(0, 4.4f, 26));
        frames(2);
        shot("z41_ambush_road_blocked");
        key(Input.Keys.E);
        check("la route est barree tant que les agresseurs sont la", () -> cur() instanceof ZoneScreen && game.getSession().getPendingDestination() != null);
        key(Input.Keys.F5);
        check("F5 impossible pendant l'embuscade", () -> cur() instanceof ZoneScreen);
        for (int i = 0; i < 24; i++) {
            step(this::faceNearestMonster);
            frames(2);
            click1();
            frames(38);
        }
        check("agresseurs vaincus", () -> monsterCount() == 0);
        step(() -> faceAt(0, 4.4f, 26));
        frames(2);
        shot("z42_ambush_road_open");
        key(Input.Keys.E);
        frames(40);
        check("fin de l'embuscade : arrivee a Port-Brume", () -> cur() instanceof TownScreen && "portbrume".equals(player().getTownId()));
        check("le voyage a dure au moins 6 h 35 (le combat a fait passer un peu de temps)", () -> minutesNow() - tripStart >= 395 && minutesNow() - tripStart < 440);
        check("plus de voyage en cours", () -> game.getSession().getPendingDestination() == null);
        shot("z43_after_ambush_in_portbrume");

        // ---------- mort pendant une embuscade : reveil a l'auberge de la ville de depart (Port-Brume)
        step(() -> { com.lucas.guild.game.DebugMode.setInvincible(false); goldBefore = player().getGold(); });
        step(() -> com.lucas.guild.game.World.startAmbushedTravel(game.getSession(), "aethelgard"));
        step(() -> game.setScreen(ZoneScreen.ambush(game, "facile")));
        frames(40);
        step(() -> player().perdreVie(player().getHealth() - 1));
        step(() -> face(0, 1.5f, -10));
        frames(500);
        check("mort en embuscade : reveil a l'auberge de Port-Brume", () -> cur() instanceof InnScreen && "portbrume".equals(player().getTownId()));
        check("le voyage est abandonne", () -> game.getSession().getPendingDestination() == null);
        frames(3);
        shot("z50_woke_up_after_ambush");
        step(() -> { com.lucas.guild.game.DebugMode.setForceAmbush(false); com.lucas.guild.game.DebugMode.setEnabled(false); });
    }

    // ------------------------------------------------------------------ lore (7c)

    boolean flag(String name) {
        return (Boolean) fieldOf(cur(), name);
    }

    void loreSection() {
        step(() -> { game.startNewGame("facile"); com.lucas.guild.game.DebugMode.setEnabled(false); });

        // ---------- le panneau d'Aethelgard, a l'arrivee par la porte
        step(() -> game.setScreen(new TownScreen(game, "facile", new Vector3(0, 2, 52))));
        frames(5);
        step(() -> faceAt(7, 1.8f, 50));
        frames(2);
        shot("l01_sign_prompt");
        key(Input.Keys.E);
        frames(2);
        shot("l02_sign_text");
        check("lire le panneau l'ajoute au journal", () -> com.lucas.guild.game.Lore.isKnown(player(), com.lucas.guild.game.Lore.textKey("panneau_aethelgard")));
        key(Input.Keys.ESCAPE);
        check("Echap ferme le texte", () -> !flag("inDialog"));

        // ---------- les rumeurs de l'aubergiste
        step(() -> game.setScreen(new InnScreen(game, "facile")));
        frames(5);
        turn(0, 1.5f, 5);
        keyDownHold(Input.Keys.W); frames(68); keyUpHold(Input.Keys.W);
        frames(2);
        key(Input.Keys.E);
        shot("l10_innkeeper_dialog_with_rumors_button");
        step(() -> goldBefore = player().getGold());
        click("Offrir une bière et écouter les rumeurs (2 Po)");
        shot("l11_rumor_told");
        check("une rumeur coute une biere (2 Po) et est notee", () -> player().getGold() == goldBefore - 2 && com.lucas.guild.game.Lore.heardRumors(player()).size() == 1);
        click("Merci");
        for (int i = 0; i < 6; i++) {
            click("Offrir une bière et écouter les rumeurs (2 Po)");
            click("Merci");
        }
        check("les 7 rumeurs d'Aethelgard sont entendues (5 locales + 2 generales)", () -> com.lucas.guild.game.Lore.heardRumors(player()).size() == 7);
        step(() -> goldBefore = player().getGold());
        click("Offrir une bière et écouter les rumeurs (2 Po)");
        check("plus de rumeur : rien n'est depense", () -> player().getGold() == goldBefore && !flag("inDialog"));
        shot("l12_innkeeper_nothing_new");

        // ---------- le journal (J)
        step(() -> game.setScreen(new TownScreen(game, "facile", new Vector3(0, 2, 52))));
        frames(5);
        key(Input.Keys.J);
        check("J ouvre le journal", () -> flag("isJournalOpen"));
        shot("l20_journal");
        key(Input.Keys.ESCAPE);
        check("Echap ferme le journal", () -> !flag("isJournalOpen"));
        key(Input.Keys.J);
        key(Input.Keys.J);
        check("J ferme aussi le journal", () -> !flag("isJournalOpen"));
        key(Input.Keys.I);
        key(Input.Keys.J);
        check("J ne s'ouvre pas par-dessus l'inventaire", () -> !flag("isJournalOpen"));
        key(Input.Keys.I);

        // ---------- le panneau de la foret, puis la sauvegarde du journal
        step(() -> { player().setTownId("foret_loups"); player().markVisited("foret_loups"); game.setScreen(game.screenAt("foret_loups", "facile")); });
        frames(40);
        step(() -> faceAt(5, 1.8f, 66));
        frames(2);
        shot("l30_forest_sign_prompt");
        key(Input.Keys.E);
        frames(2);
        shot("l31_forest_sign_text");
        key(Input.Keys.ESCAPE);
        check("deux textes dans le journal", () -> com.lucas.guild.game.Lore.readTexts(player()).size() == 2);
        key(Input.Keys.F5);
        step(() -> game.setScreen(new MainMenuScreen(game)));
        frames(3);
        click("Start");
        click("Continuer");
        frames(40);
        check("le journal survit a la sauvegarde : 7 rumeurs, 2 textes", () -> com.lucas.guild.game.Lore.heardRumors(player()).size() == 7 && com.lucas.guild.game.Lore.readTexts(player()).size() == 2);
        key(Input.Keys.J);
        shot("l40_journal_after_reload");
        key(Input.Keys.J);
    }

    // ------------------------------------------------------------------ evenements du monde (7d)

    void eventSection() {
        step(() -> { game.startNewGame("facile"); com.lucas.guild.game.DebugMode.setEnabled(false);
                     com.lucas.guild.game.WorldEvents.update(player(), game.getSession().getClock().getCurrentDay(), new Random(1)); });

        // ---------- les actions debug des evenements (le panneau les range tout en bas, on les appelle directement)
        step(() -> {
            com.lucas.guild.game.DebugActions actions = new com.lucas.guild.game.DebugActions(game.getSession());
            actions.startEvent("fete_marche");
        });
        check("debug : la fete du marche demarre a Aethelgard", () -> player().getActiveEvents().size() == 1 && "aethelgard".equals(player().getActiveEvents().get(0).placeId));
        step(() -> new com.lucas.guild.game.DebugActions(game.getSession()).clearEvents());
        check("debug : tout effacer", () -> player().getActiveEvents().isEmpty());

        // ---------- une penurie de potions a Port-Brume : la boutique le dit et les prix suivent
        step(() -> {
            player().setTownId("portbrume"); player().markVisited("portbrume");
            com.lucas.guild.game.WorldEvents.start(player(), "penurie_potions", "portbrume", game.getSession().getClock().getCurrentDay());
        });
        step(() -> game.setScreen(new TownScreen(game, "facile", new Vector3(0, 2, 52))));
        frames(5);
        shot("e05_town_event_toast");
        step(() -> game.setScreen(new StoreScreen(game, "facile", "Comptoir du Port", "portbrume_store")));
        frames(5);
        turn(0, 1.5f, 6);
        keyDownHold(Input.Keys.W); frames(70); keyUpHold(Input.Keys.W);
        frames(2);
        key(Input.Keys.E);
        click("Voir la marchandise");
        shot("e10_shop_with_shortage");
        check("penurie : potion de soin a 13 Po a Port-Brume (9 x 1,4)", () -> new com.lucas.guild.game.Shop("portbrume_store").buyPrice(player(), GameContent.newItem("healing_potion")) == 13);
        click("Retour");
        key(Input.Keys.ESCAPE);

        // ---------- l'aubergiste d'Aethelgard donne des nouvelles de Port-Brume
        step(() -> { player().setTownId("aethelgard");
                     com.lucas.guild.game.WorldEvents.start(player(), "caravane", "hautevigne", game.getSession().getClock().getCurrentDay()); });
        step(() -> game.setScreen(new InnScreen(game, "facile")));
        frames(5);
        turn(0, 1.5f, 5);
        keyDownHold(Input.Keys.W); frames(68); keyUpHold(Input.Keys.W);
        frames(2);
        key(Input.Keys.E);
        click("Demander des nouvelles des autres villes");
        shot("e20_inn_news");
        click("Merci");
        key(Input.Keys.ESCAPE);

        // ---------- le journal liste les evenements des villes connues
        step(() -> game.setScreen(new TownScreen(game, "facile", new Vector3(0, 2, 52))));
        frames(5);
        key(Input.Keys.J);
        shot("e30_journal_events");
        key(Input.Keys.J);

        // ---------- un evenement demarre tout seul quand les jours passent
        step(() -> { player().getActiveEvents().clear(); player().setEventsDay(game.getSession().getClock().getCurrentDay()); });
        step(() -> {
            // 40 jours d'un coup : le rattrapage tire des evenements (15 % par jour)
            game.getSession().getClock().setCurrentDay(game.getSession().getClock().getCurrentDay() + 40);
        });
        frames(5);
        check("les jours qui passent font demarrer des evenements tout seuls", () -> player().getEventsDay() == game.getSession().getClock().getCurrentDay());
        step(() -> log("evenements apres 40 jours : " + player().getActiveEvents().size()));
    }

    // ------------------------------------------------------------------ quetes entre villes (7e)

    void quest7eSection() {
        step(() -> { game.startNewGame("facile"); com.lucas.guild.game.DebugMode.setEnabled(false); });

        // ---------- la guilde d'Aethelgard : accepter le colis pour Port-Brume
        step(() -> game.setScreen(new GuildScreen(game, "facile")));
        frames(5);
        turn(0, 1.5f, 10);
        keyDownHold(Input.Keys.W); frames(105); keyUpHold(Input.Keys.W);
        frames(2);
        key(Input.Keys.E);
        click("Voir les quêtes");
        shot("q01_guild_board_with_new_quests");
        click("Colis pour Port-Brume (45 Po)");
        shot("q02_parcel_quest_detail");
        click("Accepter la quête");
        check("le colis est remis a l'acceptation", () -> player().getInventoryItems().stream().anyMatch(i -> "Colis scellé".equals(i.getName())));
        shot("q03_parcel_accepted");
        key(Input.Keys.ESCAPE);

        // ---------- arrivee a Port-Brume : le tableau d'annonces
        step(() -> { com.lucas.guild.game.World.travel(game.getSession(), "portbrume"); game.setScreen(game.screenAt("portbrume", "facile")); });
        frames(40);
        step(() -> faceAt(-7, 1.7f, 50));
        frames(2);
        shot("q10_portbrume_board_prompt");
        key(Input.Keys.E);
        shot("q11_portbrume_board");
        click("> Trophées de gobelins (70 Po)");
        shot("q12_collect_quest_detail");
        click("Accepter la quête");
        key(Input.Keys.ESCAPE);
        check("Echap ferme le tableau", () -> !flag("isPaused"));

        // ---------- remettre le colis au Comptoir du Port : paye sur place
        step(() -> game.setScreen(new StoreScreen(game, "facile", "Comptoir du Port", "portbrume_store")));
        frames(5);
        turn(0, 1.5f, 6);
        keyDownHold(Input.Keys.W); frames(70); keyUpHold(Input.Keys.W);
        frames(2);
        key(Input.Keys.E);
        shot("q20_store_dialog_with_delivery");
        step(() -> goldBefore = player().getGold());
        click("Remettre la livraison");
        check("livraison payee a la remise : +45 Po, quete terminee", () -> player().getGold() == goldBefore + 45 && player().getDoneQuests().contains("Colis pour Port-Brume"));
        shot("q21_parcel_delivered_toast");

        // ---------- collecter des oreilles de gobelin : le tableau les prend
        step(() -> { for (int i = 0; i < 4; i++) player().addItem(GameContent.newItem("goblin_ear")); });
        step(() -> game.setScreen(new TownScreen(game, "facile", new Vector3(0, 2, 52))));
        frames(5);
        step(() -> faceAt(-7, 1.7f, 50));
        frames(2);
        key(Input.Keys.E);
        click("> Trophées de gobelins (70 Po) [à rendre]");
        shot("q30_collect_ready_to_turn_in");
        step(() -> goldBefore = player().getGold());
        click("Rendre la quête");
        check("collecte rendue : +70 Po, les 4 oreilles reprises", () -> player().getGold() == goldBefore + 70
                && player().getInventoryItems().stream().noneMatch(i -> "goblin_ear".equals(i.getId())));
        shot("q31_collect_done");
        key(Input.Keys.ESCAPE);
    }

    // ------------------------------------------------------------------ vue a la 3e personne (8a)

    boolean thirdPerson() {
        try {
            java.lang.reflect.Field f = Class.forName("com.lucas.guild.libgdx.ViewSettings").getDeclaredField("thirdPerson");
            f.setAccessible(true);
            return f.getBoolean(null);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    void setThirdPerson(boolean on) {
        try {
            java.lang.reflect.Field f = Class.forName("com.lucas.guild.libgdx.ViewSettings").getDeclaredField("thirdPerson");
            f.setAccessible(true);
            f.setBoolean(null, on);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    void designSection() {
        step(() -> { game.startNewGame("facile"); com.lucas.guild.game.DebugMode.setEnabled(false); setThirdPerson(false); });
        step(() -> game.setScreen(new TownScreen(game, "facile", new Vector3(0, 2, 40))));
        frames(5);
        shot("d00_first_person");
        key(Input.Keys.V);
        check("V passe a la 3e personne", () -> thirdPerson());
        frames(3);
        shot("d01_third_person_standing");

        // ---------- marcher : le corps s'anime
        keyDownHold(Input.Keys.W); frames(25);
        shot("d02_walking_a");
        frames(9);
        shot("d03_walking_b");
        keyUpHold(Input.Keys.W);

        // ---------- equipement visible
        step(() -> {
            for (String id : new String[] {"long_sword", "plate_armor", "backpack"}) {
                com.lucas.guild.model.Item item = GameContent.newItem(id);
                player().addItem(item);
                player().equip(item);
            }
        });
        frames(4);
        shot("d10_equipped_standing");
        keyDownHold(Input.Keys.W); frames(20); keyUpHold(Input.Keys.W);

        // ---------- coup : le bras se leve et s'abat
        click1();
        frames(5);
        shot("d11_attack_swing");
        frames(40);

        // ---------- en ville a l'arret, face a un mur : la camera ne traverse pas
        step(() -> game.setScreen(new InnScreen(game, "facile")));
        frames(5);
        shot("d20_third_person_inn");
        step(() -> turn180());
        frames(3);
        shot("d21_camera_against_wall");

        // ---------- zone sauvage : un loup, a la 3e personne, on le vise toujours bien
        step(() -> { player().setTownId("foret_loups"); player().markVisited("foret_loups"); game.setScreen(game.screenAt("foret_loups", "facile")); });
        frames(40);
        step(() -> new com.lucas.guild.game.DebugActions(game.getSession()).addGold(0));
        step(() -> com.lucas.guild.game.DebugMode.setEnabled(true));
        key(Input.Keys.F10);
        click("Loup devant moi");
        key(Input.Keys.ESCAPE);
        step(() -> com.lucas.guild.game.DebugMode.setInvincible(true));
        frames(10);
        shot("d30_wolf_third_person");
        step(() -> hpBefore = monsterCount());
        for (int i = 0; i < 10; i++) {
            step(this::aimFromCamera);
            click1();
            frames(38);
        }
        check("a la 3e personne, on tue le loup (visee et portee corrects)", () -> monsterCount() < 7 && monsterCount() <= 6);
        shot("d31_after_wolf");
        step(() -> { com.lucas.guild.game.DebugMode.setInvincible(false); com.lucas.guild.game.DebugMode.setEnabled(false); });

        // ---------- retour a la 1re personne
        key(Input.Keys.V);
        check("V revient a la 1re personne", () -> !thirdPerson());
        frames(3);
        shot("d40_back_to_first_person");
        step(() -> setThirdPerson(false));
    }

    /** Vise le monstre le plus proche depuis la CAMERA (a la 3e personne elle est derriere et sur l'epaule du joueur), en quelques ajustements. */
    void aimFromCamera() {
        try {
            Object c = combatOf();
            for (int pass = 0; pass < 4; pass++) {
                com.badlogic.gdx.graphics.Camera cam = (com.badlogic.gdx.graphics.Camera) fieldOf(cur(), "camera");
                java.lang.reflect.Method m = c.getClass().getDeclaredMethod("nearest", float.class, float.class);
                m.setAccessible(true);
                Object mon = m.invoke(c, cam.position.x, cam.position.z);
                if (mon == null) return;
                float mx = mon.getClass().getField("x").getFloat(mon);
                float mz = mon.getClass().getField("z").getFloat(mon);
                double dx = mx - cam.position.x, dz = mz - cam.position.z, dy = (0.5 + 0.45) - cam.position.y;
                Object pc = findPlayerController(cur());
                Field f = PlayerController.class.getDeclaredField("yaw"); f.setAccessible(true);
                f.setFloat(pc, (float) Math.toDegrees(Math.atan2(-dx, -dz)));
                Field pf = PlayerController.class.getDeclaredField("pitch"); pf.setAccessible(true);
                pf.setFloat(pc, (float) Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
                render1();
            }
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    void turn180() {
        try {
            Object pc = findPlayerController(cur());
            Field f = PlayerController.class.getDeclaredField("yaw"); f.setAccessible(true);
            f.setFloat(pc, f.getFloat(pc) + 180f);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    // ------------------------------------------------------------------ PNJ et monstres en modeles 3D (8b)

    void npcShot(String name, com.badlogic.gdx.Screen screen, float walkFrames, float x, float z) {
        step(() -> game.setScreen((Screen) screen));
        frames(5);
        keyDownHold(Input.Keys.W); frames((int) walkFrames); keyUpHold(Input.Keys.W);
        step(() -> faceAt(x, 2.0f, z));
        frames(30);
        shot(name);
    }

    void designNpcSection() {
        step(() -> { game.startNewGame("facile"); com.lucas.guild.game.DebugMode.setEnabled(false); });
        step(() -> game.setScreen(new InnScreen(game, "facile")));
        frames(5);
        turn(0, 1.5f, 5);
        keyDownHold(Input.Keys.W); frames(68); keyUpHold(Input.Keys.W);
        step(() -> faceAt(0, 2.0f, 5));
        frames(30);
        shot("n01_innkeeper");
        key(Input.Keys.E);
        check("dialogue de l'aubergiste ouvert", () -> flag("inDialog"));
        shot("n02_innkeeper_dialog");
        key(Input.Keys.ESCAPE);

        step(() -> game.setScreen(new ForgeScreen(game, "facile")));
        frames(5);
        turn(2, 1.5f, 0);
        keyDownHold(Input.Keys.W); frames(40); keyUpHold(Input.Keys.W);
        step(() -> faceAt(2, 2.0f, 0));
        frames(30);
        shot("n03_blacksmith");
        key(Input.Keys.E);
        check("dialogue du forgeron ouvert", () -> flag("inDialog"));
        key(Input.Keys.ESCAPE);

        step(() -> game.setScreen(new GuildScreen(game, "facile")));
        frames(5);
        turn(0, 1.5f, 10);
        keyDownHold(Input.Keys.W); frames(105); keyUpHold(Input.Keys.W);
        step(() -> faceAt(0, 2.0f, 10));
        frames(30);
        shot("n04_guildmaster");

        step(() -> { player().setTownId("portbrume"); player().markVisited("portbrume"); game.setScreen(new StoreScreen(game, "facile", "Comptoir du Port", "portbrume_store")); });
        frames(5);
        turn(0, 1.5f, 6);
        keyDownHold(Input.Keys.W); frames(70); keyUpHold(Input.Keys.W);
        step(() -> faceAt(0, 2.0f, 6));
        frames(30);
        shot("n05_merchant");

        // ---------- les monstres en modeles : cave, foret, ruines
        step(() -> game.setScreen(new CellarScreen(game, "facile")));
        frames(5);
        step(() -> face(0, 1.5f, 5));
        frames(60);
        step(this::faceNearestMonster);
        frames(2);
        shot("n10_giant_rats");

        step(() -> { player().setTownId("foret_loups"); player().markVisited("foret_loups"); game.setScreen(game.screenAt("foret_loups", "facile")); });
        frames(40);
        step(() -> com.lucas.guild.game.DebugMode.setEnabled(true));
        key(Input.Keys.F10);
        click("Loup devant moi");
        click("Gobelin devant moi");
        key(Input.Keys.ESCAPE);
        step(() -> com.lucas.guild.game.DebugMode.setInvincible(true));
        step(this::faceNearestMonster);
        frames(30);
        shot("n11_wolf_and_goblin_approach");
        frames(90);
        step(this::faceNearestMonster);
        frames(2);
        shot("n12_wolf_and_goblin_close");
        for (int i = 0; i < 6; i++) {
            step(this::faceNearestMonster);
            frames(2);
            click1();
            frames(10);
            if (i == 2) shot("n13_hit_flash");
            frames(28);
        }
        check("les monstres en modeles meurent toujours sous les coups", () -> monsterCount() <= 7);
        key(Input.Keys.V);
        step(() -> com.lucas.guild.game.DebugMode.setInvincible(true));
        frames(3);
        shot("n20_third_person_vs_monsters");
        key(Input.Keys.V);
        step(() -> { com.lucas.guild.game.DebugMode.setInvincible(false); com.lucas.guild.game.DebugMode.setEnabled(false); });
    }

    // ------------------------------------------------------------------ finitions : ciel, ombres, eclats, chute (8d)

    boolean shadowsOn() {
        try {
            java.lang.reflect.Field f = Class.forName("com.lucas.guild.libgdx.ViewSettings").getDeclaredField("shadows");
            f.setAccessible(true);
            return f.getBoolean(null);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    void design8dSection() {
        step(() -> { game.startNewGame("facile"); com.lucas.guild.game.DebugMode.setEnabled(false); setThirdPerson(true); });
        step(() -> game.setScreen(new TownScreen(game, "facile", new Vector3(0, 2, 40))));
        frames(8);
        shot("f01_day_shadows_third_person");
        step(() -> faceAt(0, 30f, -60));
        frames(3);
        shot("f02_day_sky_looking_up");
        // la nuit : etoiles et lune
        step(() -> new com.lucas.guild.game.DebugActions(game.getSession()).advanceToHour(23));
        frames(8);
        shot("f03_night_looking_up");
        step(() -> faceAt(0, 2f, -60));
        frames(3);
        shot("f04_night_town");
        // l'aube
        step(() -> new com.lucas.guild.game.DebugActions(game.getSession()).advanceToHour(7));
        frames(8);
        shot("f05_dawn");
        step(() -> new com.lucas.guild.game.DebugActions(game.getSession()).advanceToHour(12));
        frames(5);

        // ombres coupees depuis le menu pause
        key(Input.Keys.ESCAPE);
        shot("f06_pause_menu_shadows_button");
        click("Ombres : oui");
        check("le bouton coupe les ombres", () -> !shadowsOn());
        key(Input.Keys.ESCAPE);
        step(() -> { try { java.lang.reflect.Field f = Class.forName("com.lucas.guild.libgdx.ViewSettings").getDeclaredField("shadows"); f.setAccessible(true); f.setBoolean(null, true); } catch (Exception e) { throw new RuntimeException(e); } });

        // combat : eclats et chute des monstres (cave)
        step(() -> game.setScreen(new CellarScreen(game, "facile")));
        frames(5);
        step(() -> face(0, 1.5f, 5));
        frames(70);
        for (int i = 0; i < 4; i++) {
            step(this::faceNearestMonster);
            frames(2);
            click1();
            frames(3);
            if (i == 2) shot("f10_hit_sparks");
            frames(35);
        }
        shot("f11_after_kills");
        step(() -> com.lucas.guild.game.DebugMode.setEnabled(true));
        step(() -> { player().setTownId("foret_loups"); game.setScreen(game.screenAt("foret_loups", "facile")); });
        frames(30);
        key(Input.Keys.F10);
        click("Loup devant moi");
        key(Input.Keys.ESCAPE);
        step(() -> com.lucas.guild.game.DebugMode.setInvincible(true));
        step(this::aimFromCamera);
        for (int i = 0; i < 4; i++) {
            step(this::aimFromCamera);
            click1();
            frames(4);
            if (i == 2) shot("f12_wolf_dying");
            frames(34);
        }
        frames(10);
        shot("f13_wolf_fallen");
        step(() -> { com.lucas.guild.game.DebugMode.setInvincible(false); com.lucas.guild.game.DebugMode.setEnabled(false); setThirdPerson(false); });
    }

    int minutesNow() {
        GameClock c = game.getSession().getClock();
        return c.getCurrentDay() * 1440 + c.getCurrentHour() * 60 + c.getCurrentMinute();
    }

    int worldStart;

    void worldSection() {
        step(() -> { game.startNewGame("facile"); com.lucas.guild.game.DebugMode.setEnabled(false); });
        // ---------- Aethelgard : la porte sud mene a la carte
        step(() -> game.setScreen(new TownScreen(game, "normale", new Vector3(0, 2, 52))));
        frames(5);
        step(() -> faceAt(0, 2.5f, 60));
        frames(2);
        shot("w01_gate_prompt");
        key(Input.Keys.E);
        frames(3);
        check("E sur la porte : carte du monde", () -> cur() instanceof WorldMapScreen);
        shot("w02_world_map_from_aethelgard");

        // ---------- voyage vers Port-Brume
        step(() -> worldStart = minutesNow());
        click("Port-Brume - 33 km - 6 h 35");
        frames(5);
        check("arrivee dans la ville de Port-Brume", () -> cur() instanceof TownScreen && "portbrume".equals(player().getTownId()));
        check("le voyage a pris 6 h 35", () -> minutesNow() - worldStart == 395);
        check("Port-Brume est marquee visitee", () -> player().getVisitedPlaces().contains("portbrume"));
        check("sauvegarde automatique a l'arrivee", () -> Files.exists(Paths.get("save.json")));
        frames(3);
        shot("w10_portbrume_arrival");

        // ---------- la boutique de Port-Brume : prix 10 % moins chers
        step(() -> game.setScreen(new StoreScreen(game, "normale", "Comptoir du Port", "portbrume_store")));
        frames(5);
        turn(0, 1.5f, 6);
        keyDownHold(Input.Keys.W); frames(70); keyUpHold(Input.Keys.W);
        frames(2);
        key(Input.Keys.E);
        shot("w20_portbrume_shopkeeper_dialog");
        click("Voir la marchandise");
        shot("w21_portbrume_store_prices");
        check("Port-Brume : potion de soin a 9 Po (10 % de moins)", () -> new com.lucas.guild.game.Shop("portbrume_store").buyPrice(player(), GameContent.newItem("healing_potion")) == 9);
        click("Retour");
        key(Input.Keys.ESCAPE);

        // ---------- l'auberge de Port-Brume porte son propre nom
        step(() -> game.setScreen(new InnScreen(game, "normale")));
        frames(5);
        turn(0, 1.5f, 5);
        keyDownHold(Input.Keys.W); frames(68); keyUpHold(Input.Keys.W);
        frames(2);
        key(Input.Keys.E);
        shot("w30_portbrume_inn_dialog_name");
        key(Input.Keys.ESCAPE);

        // ---------- la carte depuis Port-Brume : routes vers Aethelgard et Hautevigne
        step(() -> game.setScreen(new TownScreen(game, "normale", new Vector3(0, 2, 52))));
        frames(5);
        step(() -> faceAt(0, 2.5f, 60));
        frames(2);
        key(Input.Keys.E);
        frames(3);
        shot("w40_world_map_from_portbrume");
        click("Hautevigne - 52 km - 10 h 25 (arrivée de nuit)");
        frames(5);
        check("arrivee a Hautevigne", () -> cur() instanceof TownScreen && "hautevigne".equals(player().getTownId()));
        frames(3);
        shot("w41_hautevigne_arrival");

        // ---------- retour par la porte : Echap revient dans la ville
        step(() -> faceAt(0, 2.5f, 60));
        frames(2);
        key(Input.Keys.E);
        frames(3);
        step(() -> Gdx.input.getInputProcessor().keyDown(Input.Keys.ESCAPE));   // la carte est un ecran a Stage : les touches passent par le processeur d'entree
        frames(3);
        check("Echap sur la carte : retour dans la ville, sans voyager", () -> cur() instanceof TownScreen && "hautevigne".equals(player().getTownId()));

        // ---------- Continuer : la sauvegarde reprend dans la bonne ville
        step(() -> game.setScreen(new MainMenuScreen(game)));
        frames(3);
        step(() -> { try { Files.deleteIfExists(Paths.get("save.json")); } catch (Exception e) { throw new RuntimeException(e); } });
        step(() -> new com.lucas.guild.game.SaveManager().saveGame(player(), game.getSession().getClock(), "normale", 0, 2, 52, "Town"));
        click("Start");
        click("Continuer");
        frames(30);
        check("Continuer : on reprend a Hautevigne", () -> cur() instanceof TownScreen && "hautevigne".equals(player().getTownId()));
        shot("w50_continue_in_hautevigne");

        // ---------- debug : voyager sans duree
        step(() -> com.lucas.guild.game.DebugMode.setEnabled(true));
        key(Input.Keys.F10);
        shot("w60_debug_panel_with_places");
        click("Aethelgard");
        frames(5);
        check("debug : teleportation a Aethelgard", () -> cur() instanceof TownScreen && "aethelgard".equals(player().getTownId()));
        step(() -> com.lucas.guild.game.DebugMode.setEnabled(false));
    }

    /** Clique le n-ieme (a partir de 0) bouton visible portant ce texte. */
    void clickNth(String text, int n) {
        steps.add(this::render1);
        steps.add(() -> {
            try {
                Stage st = null;
                for (Class<?> c = cur().getClass(); c != null && st == null; c = c.getSuperclass()) {
                    for (Field f : c.getDeclaredFields()) {
                        if (f.getType() == Stage.class) { f.setAccessible(true); st = (Stage) f.get(cur()); break; }
                    }
                }
                List<TextButton> found = new ArrayList<>();
                collectButtons(st.getRoot(), text, found);
                if (n >= found.size()) { log("CHECK ECHEC  bouton " + n + " de '" + text + "' introuvable (" + found.size() + " visibles)"); failures++; return; }
                TextButton b = found.get(n);
                Vector2 v = b.localToStageCoordinates(new Vector2(b.getWidth() / 2, b.getHeight() / 2));
                Vector2 s = st.stageToScreenCoordinates(v);
                st.touchDown((int) s.x, (int) s.y, 0, Input.Buttons.LEFT);
                st.touchUp((int) s.x, (int) s.y, 0, Input.Buttons.LEFT);
                log("clic: " + text + " #" + n);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            render1();
        });
        steps.add(this::render1);
    }

    static void collectButtons(Group g, String text, List<TextButton> out) {
        for (Actor a : g.getChildren()) {
            if (!a.isVisible()) continue;
            if (a instanceof TextButton && ((TextButton) a).getText().toString().equals(text)) out.add((TextButton) a);
            if (a instanceof Group) collectButtons((Group) a, text, out);
        }
    }

    /** Appelle InnScreen.onNpcInteract(userData) (protegee) : simule E sur le lit sans monter l'escalier. */
    void invokeInn(String userData) {
        try {
            java.lang.reflect.Method m = InnScreen.class.getDeclaredMethod("onNpcInteract", Object.class);
            m.setAccessible(true);
            m.invoke(cur(), userData);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        render1();
    }

    double distAt20, distAt70;
    double[] posBefore = new double[3];

    /** Jour / nuit, saisons, sommeil, vieillissement. Le joueur regarde la forge (vers -Z). */
    void timeSection() {
        // heures de la journee au printemps (lever 6h, coucher 19h30)
        String[][] moments = {
                {"t01_spring_0530_before_dawn", "14", "5", "30"},
                {"t02_spring_0600_sunrise", "14", "6", "0"},
                {"t03_spring_0800_morning", "14", "8", "0"},
                {"t04_spring_1245_noon", "14", "12", "45"},
                {"t05_spring_1915_sunset", "14", "19", "15"},
                {"t06_spring_2000_dusk", "14", "20", "0"},
                {"t07_spring_2200_night", "14", "22", "0"},
                {"t08_spring_0200_deep_night", "14", "2", "0"},
                {"t09_summer_1200", "42", "12", "0"},
                {"t10_winter_1200", "98", "12", "0"},
                {"t11_winter_0830_late_sunrise", "98", "8", "30"},
                {"t12_winter_1730_early_sunset", "98", "17", "30"},
        };
        // point de vue representatif : depart de la ville (0,2,0), face a la forge (yaw 0 = vers -Z)
        step(() -> game.setScreen(new TownScreen(game, "normale", new Vector3(0, 2, 0))));
        frames(5);
        for (String[] m : moments) {
            final int day = Integer.parseInt(m[1]), hour = Integer.parseInt(m[2]), minute = Integer.parseInt(m[3]);
            step(() -> {
                GameClock c = game.getSession().getClock();
                c.setCurrentDay(day); c.setCurrentHour(hour); c.setCurrentMinute(minute);
            });
            frames(2);
            shot(m[0]);
        }

        // sommeil : on dort a 22h -> reveil a 7h le lendemain, message affiche
        step(() -> {
            GameClock c = game.getSession().getClock();
            c.setCurrentDay(1); c.setCurrentHour(22); c.setCurrentMinute(0);
            c.sleepUntilNextMorning();
        });
        check("apres le sommeil : jour 2, 07:00", () -> {
            GameClock c = game.getSession().getClock();
            return c.getCurrentDay() == 2 && c.getCurrentHour() == 7 && c.getCurrentMinute() == 0;
        });
        frames(2);
        shot("t20_after_sleep_with_message");

        // vitesse de marche : a 20 ans puis a 70 ans (palier "Grand age" : 82 %)
        step(() -> game.setScreen(new TownScreen(game, "normale", new Vector3(0, 2, 0))));
        frames(5);
        step(() -> face(0, 2, 100));
        frames(2);
        step(() -> posBefore = playerPos());
        keyDownHold(Input.Keys.W);
        frames(60);
        keyUpHold(Input.Keys.W);
        step(() -> distAt20 = dist(posBefore, playerPos()));

        step(() -> {
            GameSession s = game.getSession();
            int day = s.getClock().getCurrentDay();
            s.getPlayer().setBirthDay(day - 70 * 112);
            s.getPlayer().refreshAging(day);
            game.setScreen(new TownScreen(game, "normale", new Vector3(0, 2, 0)));
        });
        frames(5);
        step(() -> face(0, 2, 100));
        frames(2);
        step(() -> posBefore = playerPos());
        keyDownHold(Input.Keys.W);
        frames(60);
        keyUpHold(Input.Keys.W);
        step(() -> distAt70 = dist(posBefore, playerPos()));
        check("a 70 ans on marche moins vite (ratio ~0.82)", () -> {
            double ratio = distAt70 / distAt20;
            log(String.format("distance en 1 s : 20 ans = %.2f m, 70 ans = %.2f m, ratio = %.3f", distAt20, distAt70, ratio));
            return distAt20 > 5 && ratio > 0.78 && ratio < 0.86;
        });
        shot("t30_age70_town");
        key(Input.Keys.I);
        shot("t31_age70_inventory_reduced_weight");
        key(Input.Keys.I);
    }

    double[] playerPos() {
        try {
            Object pc = findPlayerController(cur());
            Vector3 p = ((PlayerController) pc).getPlayerBody().getCenterOfMassPosition();
            return new double[]{p.x, p.y, p.z};
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static double dist(double[] a, double[] b) {
        return Math.sqrt((a[0] - b[0]) * (a[0] - b[0]) + (a[2] - b[2]) * (a[2] - b[2]));
    }

    /**
     * Parcours complet d'un batiment : depuis la ville on marche vers lui, E pour entrer,
     * on parle au PNJ, on ouvre inventaire/pause, puis on ressort par la porte
     * (ecran recree devant la porte) et on verifie la sauvegarde automatique.
     */
    void building(String name, float bx, float by, float bz, String expectedClass,
                  float nx, float ny, float nz, int walkToNpc, Runnable dialogHook,
                  java.util.function.Supplier<Screen> freshInterior, float townX, float townZ, String num) {
        step(() -> game.setScreen(new TownScreen(game, "normale", new Vector3(0, 2, 0))));
        frames(5);
        turn(bx, 5.5f, bz);
        keyDownHold(Input.Keys.W);
        frames(50);
        keyUpHold(Input.Keys.W);
        shot(num + "_" + name + "_0_near");
        key(Input.Keys.E);
        frames(30);
        check("entree dans " + expectedClass, () -> cur().getClass().getSimpleName().equals(expectedClass));
        shot(num + "_" + name + "_1_inside");

        // dialogue avec le PNJ
        turn(nx, ny, nz);
        keyDownHold(Input.Keys.W);
        frames(walkToNpc);
        keyUpHold(Input.Keys.W);
        frames(2);
        shot(name + "_2_facing_npc");
        key(Input.Keys.E);
        shot(name + "_3_dialog");
        if (dialogHook != null) dialogHook.run();
        key(Input.Keys.ESCAPE);
        key(Input.Keys.I); shot(name + "_4_inventory");
        key(Input.Keys.I);
        key(Input.Keys.ESCAPE); shot(name + "_5_pause");
        key(Input.Keys.ESCAPE);
        frames(2);
        check(name + " : le joueur est le meme que celui de la ville (potion presente)",
                () -> game.getSession().getPlayer().getInventoryItems().size() == 2); // potion de depart + celle ajoutee par le test

        // sortie : on recree l'interieur (spawn devant la porte), on regarde la porte, E
        step(() -> game.setScreen(freshInterior.get()));
        frames(5);
        turn(0, 2, -60);
        keyDownHold(Input.Keys.W);
        frames(20);                       // on s'approche de la porte (portee d'interaction : 5 m)
        keyUpHold(Input.Keys.W);
        frames(2);
        key(Input.Keys.E);
        frames(30);
        check("sortie " + name + " -> ville", () -> cur() instanceof TownScreen);
        check("autosave en sortant de " + name + " : position devant la porte en ville",
                () -> {
                    String j = new String(Files.readAllBytes(Paths.get("save.json")), java.nio.charset.StandardCharsets.UTF_8);
                    return j.contains("\"playerX\": " + townX) && j.contains("\"playerZ\": " + townZ);
                });
        shot(num + "_" + name + "_9_back_in_town");
    }

    // ------------------------------------------------------------------ primitives de script
    Screen cur() { return game.getScreen(); }

    void step(Runnable r) { steps.add(r); }

    /** Se tourner vers un point puis laisser la camera se mettre a jour (2 rendus). */
    void turn(float x, float y, float z) { steps.add(() -> face(x, y, z)); frames(2); }

    void render1() { cur().render(DT); }

    void frames(int n) { for (int i = 0; i < n; i++) steps.add(this::render1); }

    void shot(String name) { steps.add(() -> { render1(); capture(name); }); }

    void key(int k) {
        steps.add(() -> {
            Screen s = cur();
            if (s instanceof InputProcessor) {
                ((InputProcessor) s).keyDown(k);
                ((InputProcessor) s).keyUp(k);
            }
            render1();
        });
    }

    void keyDownHold(int k) { steps.add(() -> { if (cur() instanceof InputProcessor) ((InputProcessor) cur()).keyDown(k); render1(); }); }

    void keyUpHold(int k) { steps.add(() -> { if (cur() instanceof InputProcessor) ((InputProcessor) cur()).keyUp(k); render1(); }); }

    void check(String what, java.util.concurrent.Callable<Boolean> c) {
        steps.add(() -> {
            boolean ok;
            try { ok = c.call(); } catch (Exception e) { ok = false; }
            log((ok ? "CHECK OK     " : "CHECK ECHEC  ") + what);
            if (!ok) failures++;
        });
    }

    /** Oriente le joueur vers un point (yaw de PlayerController, pitch 0). */
    void face(float tx, float ty, float tz) {
        try {
            Object pc = findPlayerController(cur());
            Vector3 p = ((PlayerController) pc).getPlayerBody().getCenterOfMassPosition();
            double dx = tx - p.x, dz = tz - p.z;
            float yaw = (float) Math.toDegrees(Math.atan2(-dx, -dz));
            Field f = PlayerController.class.getDeclaredField("yaw");
            f.setAccessible(true);
            f.setFloat(pc, yaw);
            Field pf = PlayerController.class.getDeclaredField("pitch");
            pf.setAccessible(true);
            pf.setFloat(pc, 0f);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static Object findPlayerController(Object screen) throws Exception {
        for (Class<?> c = screen.getClass(); c != null; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getType() == PlayerController.class) {
                    f.setAccessible(true);
                    return f.get(screen);
                }
            }
        }
        throw new IllegalStateException("pas de PlayerController dans " + screen.getClass());
    }

    // ---- clic reel sur un TextButton visible (menu)
    void click(String text) {
        steps.add(this::render1);
        steps.add(() -> {
            try {
                Stage st = null;
                for (Class<?> c = cur().getClass(); c != null && st == null; c = c.getSuperclass()) {
                    for (Field f : c.getDeclaredFields()) {
                        if (f.getType() == Stage.class) { f.setAccessible(true); st = (Stage) f.get(cur()); break; }
                    }
                }
                TextButton b = findButton(st.getRoot(), text);
                if (b == null) { log("CHECK ECHEC  bouton introuvable/visible : " + text); failures++; return; }
                Vector2 v = b.localToStageCoordinates(new Vector2(b.getWidth() / 2, b.getHeight() / 2));
                Vector2 s = st.stageToScreenCoordinates(v);
                st.touchDown((int) s.x, (int) s.y, 0, Input.Buttons.LEFT);
                st.touchUp((int) s.x, (int) s.y, 0, Input.Buttons.LEFT);
                log("clic: " + text);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            render1();
        });
        steps.add(this::render1);
    }

    static TextButton findButton(Group g, String text) {
        for (Actor a : g.getChildren()) {
            if (!a.isVisible()) continue;
            if (a instanceof TextButton) {
                TextButton tb = (TextButton) a;
                if (tb.getText().toString().equals(text) && tb.getTouchable() != Touchable.disabled) return tb;
            }
            if (a instanceof Group) {
                TextButton r = findButton((Group) a, text);
                if (r != null) return r;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ divers
    void capture(String name) {
        Pixmap pm = Pixmap.createFromFrameBuffer(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight());
        PixmapIO.PNG writer = new PixmapIO.PNG((int) (pm.getWidth() * pm.getHeight() * 1.5f));
        try {
            writer.setFlipY(true);
            writer.write(Gdx.files.absolute(outDir + "/" + name + ".png"), pm);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        } finally {
            writer.dispose();
            pm.dispose();
        }
    }

    void log(String s) { System.out.println("[smoke] " + s); }

    void fail(String where, Throwable t) {
        failures++;
        System.out.println("[smoke] EXCEPTION a " + where + " : " + t);
        t.printStackTrace(System.out);
    }
}
