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
 *
 * <p>Un coup s'<b>annonce</b> : le monstre s'arrête et se prépare un instant ({@code windUp}), puis frappe. Le coup ne
 * touche que si le joueur est encore à portée : on peut reculer ou esquiver. Paré, le monstre <b>chancelle</b> : il ne
 * bouge ni ne frappe un moment.</p>
 */
public class Monster {

    /** Temps de préparation d'un coup quand monsters.json n'en donne pas, en secondes. */
    public static final float DEFAULT_WIND_UP = 0.5f;
    /** Le coup touche si le joueur est encore à moins de tant de fois la portée quand il part. */
    public static final float REACH_MARGIN = 1.25f;
    /** Durée pendant laquelle le monstre « clignote » après un coup reçu. */
    public static final float HURT_FLASH = 0.2f;

    public final MonsterData data;
    public float x;
    public float z;

    private int health;
    private float cooldown;
    private float hurtTimer;
    /** Temps restant avant que le coup préparé parte (0 = il ne prépare rien). */
    private float windUpLeft;
    private float staggerLeft;
    /** Son dernier coup est parti dans le vide (le joueur s'était écarté), en attente d'être lu par l'écran. */
    private boolean whiffed;

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

    /** Temps de préparation de ses coups, en secondes. */
    public float windUp() {
        return windUpOf(data);
    }

    public static float windUpOf(MonsterData data) {
        return data.windUp > 0f ? data.windUp : DEFAULT_WIND_UP;
    }

    /** Prépare-t-il un coup en ce moment ? */
    public boolean isWindingUp() {
        return windUpLeft > 0f;
    }

    /** La part déjà faite de la préparation du coup (0 à 1), 0 s'il n'en prépare pas. */
    public float windUpShare() {
        return windUpLeft > 0f ? 1f - windUpLeft / windUp() : 0f;
    }

    /** Chancelle-t-il (paré, ou frappé d'un coup chargé) ? */
    public boolean isStaggered() {
        return staggerLeft > 0f;
    }

    /** Son dernier coup vient-il de partir dans le vide ? (vrai une seule fois par coup raté) */
    public boolean pollWhiff() {
        boolean w = whiffed;
        whiffed = false;
        return w;
    }

    /** Le fait chanceler {@code seconds} secondes : le coup qu'il préparait est perdu. */
    public void stagger(float seconds) {
        if (isDead() || yielded) {
            return;
        }
        staggerLeft = Math.max(staggerLeft, seconds);
        windUpLeft = 0f;
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
     * @return true si le coup qu'il préparait part à cet instant et touche le joueur (encore à portée)
     */
    public boolean update(float dt, float playerX, float playerZ) {
        hurtTimer = Math.max(0f, hurtTimer - dt);   // le clignotement s'éteint même s'il ne se bat plus
        if (isDead() || yielded) {
            return false;
        }
        cooldown = Math.max(0f, cooldown - dt);
        heal(dt);
        if (staggerLeft > 0f) {
            staggerLeft = Math.max(0f, staggerLeft - dt);
            return false;   // il chancelle : ni pas, ni coup
        }

        float dist = distanceTo(playerX, playerZ);
        if (windUpLeft > 0f) {
            return strike(dt, dist);   // il est planté, le coup part à la fin de la préparation
        }
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
            // le temps entre deux coups compte depuis le début de la préparation : le rythme reste celui des données
            cooldown = attackCooldown;
            windUpLeft = windUp();
            return strike(dt, dist);
        }
        return false;
    }

    /** La préparation avance ; à sa fin, le coup part : il touche si le joueur est encore à portée. */
    private boolean strike(float dt, float dist) {
        windUpLeft -= dt;
        if (windUpLeft > 0f) {
            return false;
        }
        windUpLeft = 0f;
        boolean reaches = dist <= data.attackRange * REACH_MARGIN;
        whiffed = !reaches;
        return reaches;
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
