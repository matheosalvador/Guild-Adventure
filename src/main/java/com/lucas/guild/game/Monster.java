package com.lucas.guild.game;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.function.Predicate;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.MonsterData;

/**
 * Un monstre en jeu : sa vie, sa position au sol (x, z) et son comportement simple — il reste
 * immobile tant que le joueur est loin, le poursuit quand il le repère, et le frappe quand il est
 * assez près. Aucune dépendance à LibGDX : l'écran se contente de dessiner l'état.
 *
 * <p>Un adversaire d'histoire peut avoir des <b>phases</b> (monsters.json) : quand sa vie passe sous un seuil, il
 * frappe ou court autrement, se soigne, appelle des renforts. Il peut aussi <b>se rendre</b> au lieu de mourir.</p>
 */
public class Monster {

    /** Délai avant le premier coup, pour laisser au joueur le temps de réagir. */
    private static final float FIRST_ATTACK_DELAY = 0.5f;
    /** Durée pendant laquelle le monstre « clignote » après un coup reçu. */
    public static final float HURT_FLASH = 0.2f;

    public final MonsterData data;
    public float x;
    public float z;

    private int health;
    private float cooldown = FIRST_ATTACK_DELAY;
    private float hurtTimer;

    // Les valeurs de combat courantes : celles des données, puis celles de la phase en cours
    private int damage;
    private float speed;
    private float attackCooldown;
    /** Numéro de la phase en cours dans {@code data.phases} (-1 : aucune phase commencée). */
    private int phase = -1;
    private float healTimer;
    private boolean yielded;
    /** Les phases qui viennent de commencer, en attente d'être lues par l'écran (renforts, phrase). */
    private final Deque<MonsterData.Phase> startedPhases = new ArrayDeque<>();
    /** Dit si des conditions d'histoire sont vraies (pour le soin d'une phase) ; par défaut, toujours. */
    private Predicate<List<String>> conditions = c -> true;

    public Monster(MonsterData data, float x, float z) {
        this.data = data;
        this.x = x;
        this.z = z;
        this.health = data.health;
        this.damage = data.damage;
        this.speed = data.speed;
        this.attackCooldown = data.attackCooldown;
    }

    /** Crée un monstre du type {@code id} (monsters.json) à la position donnée. */
    public static Monster spawn(String id, float x, float z) {
        return new Monster(GameContent.monster(id), x, z);
    }

    public String getName() {
        return data.name;
    }

    public int getHealth() {
        return health;
    }

    public int getMaxHealth() {
        return data.health;
    }

    public boolean isDead() {
        return health <= 0;
    }

    /** Les dégâts d'un de ses coups en ce moment (ils changent avec les phases), avant la défense du joueur. */
    public int getDamage() {
        return damage;
    }

    /** Vrai une fois qu'il s'est rendu : il ne bouge plus, ne frappe plus et ne prend plus de coups. */
    public boolean hasYielded() {
        return yielded;
    }

    /** Numéro de la phase en cours (0 = la première de monsters.json), ou -1 avant la première. */
    public int getPhase() {
        return phase;
    }

    /** Branche les conditions d'histoire (voir {@code Story.holds}) dont dépend le soin de certaines phases. */
    public void setConditions(Predicate<List<String>> conditions) {
        this.conditions = conditions;
    }

    /** La prochaine phase qui vient de commencer (renforts à faire apparaître, phrase à afficher), ou null. */
    public MonsterData.Phase pollStartedPhase() {
        return startedPhases.poll();
    }

    /** Reste du clignotement après un coup reçu (0 = ne clignote pas). */
    public float getHurtTimer() {
        return hurtTimer;
    }

    /** Distance au sol jusqu'à un point. */
    public float distanceTo(float px, float pz) {
        return (float) Math.hypot(px - x, pz - z);
    }

    /** Subit des dégâts (jamais en dessous de 0 PV ; un monstre qui peut se rendre s'arrête à son seuil). */
    public void takeDamage(int amount) {
        if (amount <= 0 || isDead() || yielded) {
            return;
        }
        health = Math.max(0, health - amount);
        hurtTimer = HURT_FLASH;
        if (data.yieldPercent > 0 && health * 100L <= (long) data.yieldPercent * data.health) {
            health = Math.max(1, health);
            yielded = true;
            return;
        }
        enterPhases();
    }

    /** Fait commencer les phases dont le seuil vient d'être franchi. */
    private void enterPhases() {
        while (phase + 1 < data.phases.size()
                && health * 100L < (long) data.phases.get(phase + 1).below * data.health) {
            phase++;
            MonsterData.Phase p = data.phases.get(phase);
            if (p.damage > 0) damage = p.damage;
            if (p.speed > 0) speed = p.speed;
            if (p.attackCooldown > 0) attackCooldown = p.attackCooldown;
            healTimer = p.healEvery;
            startedPhases.add(p);
        }
    }

    /** Garde le monstre dans la zone {@code [minX, maxX] x [minZ, maxZ]}. */
    public void clamp(float minX, float maxX, float minZ, float maxZ) {
        x = Math.max(minX, Math.min(maxX, x));
        z = Math.max(minZ, Math.min(maxZ, z));
    }

    /**
     * Fait avancer le monstre de {@code dt} secondes.
     *
     * @return true si le monstre frappe le joueur à cet instant
     */
    public boolean update(float dt, float playerX, float playerZ) {
        hurtTimer = Math.max(0f, hurtTimer - dt);   // le clignotement s'éteint même s'il ne se bat plus
        if (isDead() || yielded) {
            return false;
        }
        cooldown = Math.max(0f, cooldown - dt);
        heal(dt);

        float dist = distanceTo(playerX, playerZ);
        if (dist > data.aggroRange) {
            return false;   // trop loin : il ne bouge pas
        }
        // Il s'approche jusqu'a portee de coup, sans jamais traverser le joueur
        float stopAt = data.attackRange * 0.8f;
        if (dist > stopAt) {
            float step = Math.min(speed * dt, dist - stopAt);
            x += (playerX - x) / dist * step;
            z += (playerZ - z) / dist * step;
            dist = distanceTo(playerX, playerZ);
        }
        if (dist <= data.attackRange && cooldown <= 0f) {
            cooldown = attackCooldown;
            return true;
        }
        return false;
    }

    /** Le soin de la phase en cours, s'il y en a un et si l'histoire le permet. */
    private void heal(float dt) {
        if (phase < 0) {
            return;
        }
        MonsterData.Phase p = data.phases.get(phase);
        if (p.healEvery <= 0 || !conditions.test(p.healRequires)) {
            return;
        }
        healTimer -= dt;
        if (healTimer <= 0f) {
            healTimer += p.healEvery;
            health = Math.min(data.health, health + p.healAmount);
        }
    }
}
