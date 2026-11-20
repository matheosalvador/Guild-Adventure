package com.lucas.guild.libgdx;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.MonsterData;
import com.lucas.guild.game.Combat;
import com.lucas.guild.game.Monster;
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Item;

/**
 * Les monstres d'un écran : leur dessin (une boîte colorée chacun), leur déplacement, les coups du
 * joueur (clic gauche) et ce qu'on voit à l'écran (cible visée, flash rouge quand on est touché).
 * Les règles sont dans {@link Combat} et {@link Monster} ; ici on relie ça au monde 3D.
 */
class CombatScene implements Disposable {

    /** Un monstre et son dessin. */
    private static final class Entry {
        final Monster monster;
        final Model model;
        final ModelInstance instance;
        final Color baseColor;
        /** Le modèle 3D articulé du monstre (models.json), ou null s'il n'est qu'une boîte. */
        final CharacterModel character;
        float lastX;
        float lastZ;
        float lastYaw;
        /** La reddition de ce monstre a déjà été signalée à l'écran. */
        boolean yieldReported;
        /** Où ce monstre peut aller (minX, maxX, minZ, maxZ), ou null : la zone de l'écran. */
        float[] bounds;

        Entry(Monster monster, Model model, ModelInstance instance, Color baseColor, CharacterModel character) {
            this.monster = monster;
            this.model = model;
            this.instance = instance;
            this.baseColor = baseColor;
            this.character = character;
            this.lastX = monster.x;
            this.lastZ = monster.z;
        }
    }

    /** Un monstre mort qui s'effondre, avant de disparaître. */
    private static final class Dying {
        final CharacterModel model;
        final float x;
        final float z;
        final float yaw;
        float time;

        Dying(CharacterModel model, float x, float z, float yaw) {
            this.model = model;
            this.x = x;
            this.z = z;
            this.yaw = yaw;
        }
    }

    private static final float DYING_SECONDS = 1.6f;
    private static final float FLOOR_TOP = 0.5f;      // le dessus du sol (boîte d'1 m centrée en 0)
    private static final float DAMAGE_FLASH = 0.35f;  // durée du flash rouge quand le joueur est touché
    private static final float TARGET_RANGE = 12f;    // distance max à laquelle on affiche le monstre visé
    private static final float BOSS_BAR_RANGE = 40f;  // distance max à laquelle on affiche la vie d'un adversaire nommé

    private final Array<ModelInstance> worldInstances;
    private final List<Entry> entries = new ArrayList<>();
    private final List<Dying> dying = new ArrayList<>();
    private final Sparks sparks;
    private final Random random = com.lucas.guild.game.GameRandom.create();
    private final Texture pixel;
    private final GlyphLayout layout = new GlyphLayout();
    private final BoundingBox box = new BoundingBox();
    private final Vector3 hit = new Vector3();
    private final Vector3 feet = new Vector3();

    /** Reçoit les objets qui tombent au sol à la mort d'un monstre. */
    interface DropSink {
        void drop(Item item, float x, float z);
    }

    private DropSink dropSink;

    void setDropSink(DropSink dropSink) {
        this.dropSink = dropSink;
    }

    /** Ce que la scène signale à l'écran : c'est lui qui prévient l'histoire et affiche les phrases. */
    interface Listener {
        /** Un monstre vient de mourir. */
        void killed(Monster monster);

        /** Un monstre vient de se rendre. */
        void yielded(Monster monster);

        /** Une phrase à montrer au joueur (début d'une phase de combat). */
        void message(String text);

        /** Les conditions d'histoire sont-elles vraies ? */
        boolean holds(java.util.List<String> conditions);
    }

    private Listener listener;

    void setListener(Listener listener) {
        this.listener = listener;
    }

    private float minX = -1000, maxX = 1000, minZ = -1000, maxZ = 1000;
    /** Distance en plus pour les coups et la cible affichée : la caméra à la 3e personne est derrière les yeux du joueur. */
    private float rangeBonus;
    private float playerCooldown;
    private float damageFlash;

    CombatScene(Array<ModelInstance> worldInstances) {
        this.worldInstances = worldInstances;
        this.sparks = new Sparks(worldInstances);
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fill();
        pixel = new Texture(p);
        p.dispose();
    }

    /** Le joueur peut-il porter un coup tout de suite (le délai entre deux coups est écoulé) ? */
    boolean ready() {
        return playerCooldown <= 0f;
    }

    void setRangeBonus(float rangeBonus) {
        this.rangeBonus = rangeBonus;
    }

    /** Zone où les monstres peuvent aller (les murs de la pièce). */
    void setBounds(float minX, float maxX, float minZ, float maxZ) {
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
    }

    Monster spawn(String id, float x, float z) {
        return spawn(id, x, z, null);
    }

    /** Un monstre retenu dans {@code bounds} (minX, maxX, minZ, maxZ) : son lieu, dans le monde ouvert. */
    Monster spawn(String id, float x, float z, float[] bounds) {
        Monster monster = Monster.spawn(id, x, z);
        monster.setConditions(conditions -> listener == null || listener.holds(conditions));
        MonsterData data = monster.data;
        Color color = ColorNames.parse(data.color, "monsters.json", "monstre \"" + data.id + "\"");
        Entry entry;
        if (data.model != null) {
            CharacterModel character = new CharacterModel(worldInstances, GameContent.models(), data.model);
            character.setVisible(true);
            entry = new Entry(monster, null, null, color, character);
        } else {
            Model model = new ModelBuilder().createBox(data.w, data.h, data.d,
                    new Material(ColorAttribute.createDiffuse(new Color(color))), Usage.Position | Usage.Normal);
            ModelInstance instance = new ModelInstance(model);
            worldInstances.add(instance);
            entry = new Entry(monster, model, instance, color, null);
        }
        entry.bounds = bounds;
        entries.add(entry);
        place(entry, x, z + 1f, 0f);
        return monster;
    }

    int count() {
        return entries.size();
    }

    /** Le monstre vivant le plus proche d'un point (pour les tests et le debug), ou null. */
    Monster nearest(float x, float z) {
        Monster best = null;
        for (Entry e : entries) {
            if (best == null || e.monster.distanceTo(x, z) < best.distanceTo(x, z)) {
                best = e.monster;
            }
        }
        return best;
    }

    /** Retire tous les monstres sans butin (debug). @return combien */
    int clearAll() {
        int n = entries.size();
        for (Entry e : entries) {
            remove(e);
        }
        entries.clear();
        for (Dying d : dying) {
            d.model.dispose();
        }
        dying.clear();
        sparks.clear();
        return n;
    }

    /** Retire ce monstre sans butin (son lieu est défait). */
    void remove(Monster monster) {
        for (java.util.Iterator<Entry> it = entries.iterator(); it.hasNext();) {
            Entry e = it.next();
            if (e.monster == monster) {
                remove(e);
                it.remove();
                return;
            }
        }
    }

    private void clamp(Entry e, Monster m) {
        if (e.bounds != null) {
            m.clamp(e.bounds[0], e.bounds[1], e.bounds[2], e.bounds[3]);
        } else {
            m.clamp(minX, maxX, minZ, maxZ);
        }
    }

    private void remove(Entry e) {
        if (e.character != null) {
            e.character.dispose();
            return;
        }
        worldInstances.removeValue(e.instance, true);
        e.model.dispose();
    }

    /**
     * Fait avancer les monstres et leurs coups.
     *
     * @return true si le joueur vient de tomber à 0 PV
     */
    boolean update(float dt, Adventurer player, Vector3 playerPos) {
        playerCooldown = Math.max(0f, playerCooldown - dt);
        damageFlash = Math.max(0f, damageFlash - dt);
        sparks.update(dt);
        for (java.util.Iterator<Dying> it = dying.iterator(); it.hasNext();) {
            Dying d = it.next();
            d.time += dt;
            d.model.update(dt, 0f, feet.set(d.x, FLOOR_TOP, d.z), d.yaw);
            if (d.time > DYING_SECONDS) {
                d.model.dispose();
                it.remove();
            }
        }
        boolean dead = false;
        for (Entry e : entries) {
            Monster m = e.monster;
            boolean attacks = m.update(dt, playerPos.x, playerPos.z);
            clamp(e, m);
            if (attacks && player.estVivant()) {
                if (Combat.monsterHit(m, player) > 0) {
                    damageFlash = DAMAGE_FLASH;
                }
                if (!player.estVivant()) {
                    dead = true;
                }
            }
            if (attacks && e.character != null) {
                e.character.startAttack();
            }
            place(e, playerPos.x, playerPos.z, dt);
        }
        reportPhasesAndYields();
        return dead;
    }

    /** Les phases qui viennent de commencer (renforts, phrase) et les monstres qui viennent de se rendre. */
    private void reportPhasesAndYields() {
        List<float[]> reinforcements = null;
        List<String> reinforcementIds = null;
        List<float[]> reinforcementBounds = null;
        for (Entry e : entries) {
            Monster m = e.monster;
            MonsterData.Phase phase;
            while ((phase = m.pollStartedPhase()) != null) {
                if (phase.message != null && listener != null) {
                    listener.message(phase.message);
                }
                for (int i = 0; phase.summon != null && i < phase.summonCount; i++) {
                    if (reinforcements == null) {
                        reinforcements = new ArrayList<>();
                        reinforcementIds = new ArrayList<>();
                        reinforcementBounds = new ArrayList<>();
                    }
                    // de part et d'autre du monstre, à quelques pas
                    float side = (i % 2 == 0 ? 1f : -1f) * (2.5f + i / 2 * 1.5f);
                    reinforcements.add(new float[] {m.x + side, m.z + 1.5f});
                    reinforcementIds.add(phase.summon);
                    reinforcementBounds.add(e.bounds);
                }
            }
            if (m.hasYielded() && !e.yieldReported) {
                e.yieldReported = true;
                if (e.character != null) {
                    e.character.setFlash(false);
                }
                if (listener != null) {
                    listener.yielded(m);
                }
            }
        }
        // les renforts apparaissent après la boucle : on ne modifie pas la liste pendant qu'on la parcourt
        for (int i = 0; reinforcements != null && i < reinforcements.size(); i++) {
            float[] bounds = reinforcementBounds.get(i);
            Monster added = spawn(reinforcementIds.get(i), reinforcements.get(i)[0], reinforcements.get(i)[1], bounds);
            clamp(entries.get(entries.size() - 1), added);
            sparks.burst(added.x, FLOOR_TOP + added.data.h * 0.5f, added.z, 10, new Color(0.8f, 0.8f, 0.85f, 1f), 3f);
        }
    }

    /** Pose la boîte du monstre : position, orientation vers le joueur et clignotement quand il est touché. */
    private void place(Entry e, float lookX, float lookZ, float dt) {
        Monster m = e.monster;
        if (e.character != null) {
            // le modèle regarde vers -Z : l'angle qui le tourne vers le joueur
            float yaw = (float) Math.toDegrees(Math.atan2(-(lookX - m.x), -(lookZ - m.z)));
            float speed = dt > 0f ? (float) Math.hypot(m.x - e.lastX, m.z - e.lastZ) / dt : 0f;
            e.lastX = m.x;
            e.lastZ = m.z;
            e.lastYaw = yaw;
            e.character.setFlash(m.getHurtTimer() > 0f);
            e.character.update(dt, speed, feet.set(m.x, FLOOR_TOP + m.data.flyHeight, m.z), yaw);
            return;
        }
        float angle = (float) Math.toDegrees(Math.atan2(lookX - m.x, lookZ - m.z));
        e.instance.transform.idt().translate(m.x, FLOOR_TOP + m.data.flyHeight + m.data.h / 2f, m.z).rotate(Vector3.Y, angle);
        ColorAttribute diffuse = (ColorAttribute) e.instance.materials.first().get(ColorAttribute.Diffuse);
        if (m.getHurtTimer() > 0f) {
            diffuse.color.set(1f, 0.25f, 0.25f, 1f);
        } else {
            diffuse.color.set(e.baseColor);
        }
    }

    /**
     * Un coup du joueur (clic gauche) : touche le monstre visé s'il est à portée.
     *
     * @return le message de butin si le coup a tué un monstre, sinon null
     */
    String attack(Adventurer player, Vector3 eye, Vector3 direction) {
        if (playerCooldown > 0f) {
            return null;
        }
        playerCooldown = Combat.PLAYER_COOLDOWN;
        Entry target = targetOf(eye, direction, Combat.PLAYER_RANGE + rangeBonus);
        if (target == null) {
            return null;
        }
        Monster hitMonster = target.monster;
        if (hitMonster.hasYielded()) {
            return null;   // il s'est rendu : on ne frappe plus
        }
        Combat.playerHit(player, hitMonster);
        sparks.burst(hitMonster.x, FLOOR_TOP + hitMonster.data.flyHeight + hitMonster.data.h * 0.6f, hitMonster.z, 7, new Color(1f, 0.82f, 0.45f, 1f), 3.5f);
        if (target.monster.isDead()) {
            Monster m = target.monster;
            sparks.burst(m.x, FLOOR_TOP + m.data.flyHeight + m.data.h * 0.5f, m.z, 14, target.baseColor, 4.5f);
            String loot = Combat.loot(player, m, random);
            if (listener != null) {
                listener.killed(m);
            }
            // le reste du butin tombe au sol, là où le monstre est mort
            java.util.List<String> fallen = new ArrayList<>();
            for (Item item : Combat.rollDrops(m, random)) {
                if (dropSink != null) {
                    dropSink.drop(item, m.x + (random.nextFloat() - 0.5f), m.z + (random.nextFloat() - 0.5f));
                    fallen.add(item.getName());
                }
            }
            if (!fallen.isEmpty()) {
                loot += " Au sol : " + String.join(", ", fallen) + ".";
            }
            if (target.character != null) {
                // le corps s'effondre un instant avant de disparaître
                target.character.setFlash(false);
                target.character.startFall();
                dying.add(new Dying(target.character, m.x, m.z, target.lastYaw));
            } else {
                remove(target);
            }
            entries.remove(target);
            return loot;
        }
        return null;
    }

    /** Le monstre visé par le rayon, s'il est plus proche que {@code range}. */
    private Entry targetOf(Vector3 eye, Vector3 direction, float range) {
        Ray ray = new Ray(eye, direction);
        Entry best = null;
        float bestDist = range;
        for (Entry e : entries) {
            Monster m = e.monster;
            float base = FLOOR_TOP + m.data.flyHeight;
            box.set(new Vector3(m.x - m.data.w / 2f, base, m.z - m.data.d / 2f),
                    new Vector3(m.x + m.data.w / 2f, base + m.data.h, m.z + m.data.d / 2f));
            if (Intersector.intersectRayBounds(ray, box, hit)) {
                float dist = hit.dst(eye);
                if (dist <= bestDist) {
                    bestDist = dist;
                    best = e;
                }
            }
        }
        return best;
    }

    /** Le flash rouge quand le joueur est touché, et nom et vie du monstre visé. */
    void drawHud(SpriteBatch batch, BitmapFont font, float w, float h, Vector3 eye, Vector3 direction) {
        if (damageFlash > 0f) {
            batch.setColor(1f, 0f, 0f, 0.45f * damageFlash / DAMAGE_FLASH);
            batch.draw(pixel, 0, 0, w, h);
            batch.setColor(Color.WHITE);
        }
        // un adversaire nommé garde sa barre de vie en haut de l'écran tant qu'il est proche
        Entry boss = null;
        for (Entry e : entries) {
            if (e.monster.data.boss && e.monster.distanceTo(eye.x, eye.z) < BOSS_BAR_RANGE) {
                boss = e;
            }
        }
        if (boss != null) {
            Monster m = boss.monster;
            layout.setText(font, m.hasYielded() ? m.getName() + " se rend" : m.getName());
            // sous les lignes du HUD (en haut à gauche) et sous les messages (en haut au centre)
            font.draw(batch, layout, w / 2f - layout.width / 2f, h - 128);
            float barWidth = 320f;
            batch.setColor(0.15f, 0.15f, 0.15f, 0.9f);
            batch.draw(pixel, w / 2f - barWidth / 2f, h - 158, barWidth, 10);
            batch.setColor(0.75f, 0.12f, 0.12f, 1f);
            batch.draw(pixel, w / 2f - barWidth / 2f, h - 158, barWidth * m.getHealth() / m.getMaxHealth(), 10);
            batch.setColor(Color.WHITE);
        }
        Entry target = targetOf(eye, direction, TARGET_RANGE + rangeBonus);
        if (target != null && target != boss) {
            Monster m = target.monster;
            String text = m.getName() + "  " + m.getHealth() + " / " + m.getMaxHealth();
            layout.setText(font, text);
            float y = h / 2f + 60;
            font.draw(batch, layout, w / 2f - layout.width / 2f, y);
            float barWidth = 120f;
            batch.setColor(0.15f, 0.15f, 0.15f, 0.9f);
            batch.draw(pixel, w / 2f - barWidth / 2f, y - 22, barWidth, 8);
            batch.setColor(0.85f, 0.15f, 0.15f, 1f);
            batch.draw(pixel, w / 2f - barWidth / 2f, y - 22, barWidth * m.getHealth() / m.getMaxHealth(), 8);
            batch.setColor(Color.WHITE);
        }
    }

    @Override
    public void dispose() {
        Iterator<Entry> it = entries.iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            if (e.character != null) {
                e.character.dispose();
            } else {
                e.model.dispose();
            }
            it.remove();
        }
        for (Dying d : dying) {
            d.model.dispose();
        }
        dying.clear();
        sparks.dispose();
        pixel.dispose();
    }
}
