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
import com.lucas.guild.data.MonsterData;
import com.lucas.guild.game.Combat;
import com.lucas.guild.game.Monster;
import com.lucas.guild.model.Adventurer;

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

        Entry(Monster monster, Model model, ModelInstance instance, Color baseColor) {
            this.monster = monster;
            this.model = model;
            this.instance = instance;
            this.baseColor = baseColor;
        }
    }

    private static final float FLOOR_TOP = 0.5f;      // le dessus du sol (boîte d'1 m centrée en 0)
    private static final float DAMAGE_FLASH = 0.35f;  // durée du flash rouge quand le joueur est touché
    private static final float TARGET_RANGE = 12f;    // distance max à laquelle on affiche le monstre visé

    private final Array<ModelInstance> worldInstances;
    private final List<Entry> entries = new ArrayList<>();
    private final Random random = new Random();
    private final Texture pixel;
    private final GlyphLayout layout = new GlyphLayout();
    private final BoundingBox box = new BoundingBox();
    private final Vector3 hit = new Vector3();

    private float minX = -1000, maxX = 1000, minZ = -1000, maxZ = 1000;
    private float playerCooldown;
    private float damageFlash;

    CombatScene(Array<ModelInstance> worldInstances) {
        this.worldInstances = worldInstances;
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(Color.WHITE);
        p.fill();
        pixel = new Texture(p);
        p.dispose();
    }

    /** Zone où les monstres peuvent aller (les murs de la pièce). */
    void setBounds(float minX, float maxX, float minZ, float maxZ) {
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
    }

    Monster spawn(String id, float x, float z) {
        Monster monster = Monster.spawn(id, x, z);
        MonsterData data = monster.data;
        Color color = ColorNames.parse(data.color, "monsters.json", "monstre \"" + data.id + "\"");
        Model model = new ModelBuilder().createBox(data.w, data.h, data.d,
                new Material(ColorAttribute.createDiffuse(new Color(color))), Usage.Position | Usage.Normal);
        ModelInstance instance = new ModelInstance(model);
        worldInstances.add(instance);
        Entry entry = new Entry(monster, model, instance, color);
        entries.add(entry);
        place(entry, x, z + 1f);
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
        return n;
    }

    private void remove(Entry e) {
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
        boolean dead = false;
        for (Entry e : entries) {
            Monster m = e.monster;
            boolean attacks = m.update(dt, playerPos.x, playerPos.z);
            m.clamp(minX, maxX, minZ, maxZ);
            if (attacks && player.estVivant()) {
                if (Combat.monsterHit(m, player) > 0) {
                    damageFlash = DAMAGE_FLASH;
                }
                if (!player.estVivant()) {
                    dead = true;
                }
            }
            place(e, playerPos.x, playerPos.z);
        }
        return dead;
    }

    /** Pose la boîte du monstre : position, orientation vers le joueur et clignotement quand il est touché. */
    private void place(Entry e, float lookX, float lookZ) {
        Monster m = e.monster;
        float angle = (float) Math.toDegrees(Math.atan2(lookX - m.x, lookZ - m.z));
        e.instance.transform.idt().translate(m.x, FLOOR_TOP + m.data.h / 2f, m.z).rotate(Vector3.Y, angle);
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
        Entry target = targetOf(eye, direction, Combat.PLAYER_RANGE);
        if (target == null) {
            return null;
        }
        Combat.playerHit(player, target.monster);
        if (target.monster.isDead()) {
            String loot = Combat.loot(player, target.monster, random);
            remove(target);
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
            box.set(new Vector3(m.x - m.data.w / 2f, FLOOR_TOP, m.z - m.data.d / 2f),
                    new Vector3(m.x + m.data.w / 2f, FLOOR_TOP + m.data.h, m.z + m.data.d / 2f));
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
        Entry target = targetOf(eye, direction, TARGET_RANGE);
        if (target != null) {
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
            it.next().model.dispose();
            it.remove();
        }
        pixel.dispose();
    }
}
