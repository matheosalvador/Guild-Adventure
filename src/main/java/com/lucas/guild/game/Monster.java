package com.lucas.guild.game;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.MonsterData;

/**
 * Un monstre en jeu : sa vie, sa position au sol (x, z) et son comportement simple — il reste
 * immobile tant que le joueur est loin, le poursuit quand il le repère, et le frappe quand il est
 * assez près. Aucune dépendance à LibGDX : l'écran se contente de dessiner l'état.
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

    public Monster(MonsterData data, float x, float z) {
        this.data = data;
        this.x = x;
        this.z = z;
        this.health = data.health;
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

    /** Reste du clignotement après un coup reçu (0 = ne clignote pas). */
    public float getHurtTimer() {
        return hurtTimer;
    }

    /** Distance au sol jusqu'à un point. */
    public float distanceTo(float px, float pz) {
        return (float) Math.hypot(px - x, pz - z);
    }

    /** Subit des dégâts (jamais en dessous de 0 PV). */
    public void takeDamage(int amount) {
        if (amount <= 0 || isDead()) {
            return;
        }
        health = Math.max(0, health - amount);
        hurtTimer = HURT_FLASH;
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
        if (isDead()) {
            return false;
        }
        hurtTimer = Math.max(0f, hurtTimer - dt);
        cooldown = Math.max(0f, cooldown - dt);

        float dist = distanceTo(playerX, playerZ);
        if (dist > data.aggroRange) {
            return false;   // trop loin : il ne bouge pas
        }
        // Il s'approche jusqu'a portee de coup, sans jamais traverser le joueur
        float stopAt = data.attackRange * 0.8f;
        if (dist > stopAt) {
            float step = Math.min(data.speed * dt, dist - stopAt);
            x += (playerX - x) / dist * step;
            z += (playerZ - z) / dist * step;
            dist = distanceTo(playerX, playerZ);
        }
        if (dist <= data.attackRange && cooldown <= 0f) {
            cooldown = data.attackCooldown;
            return true;
        }
        return false;
    }
}
