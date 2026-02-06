package com.lucas.guild.libgdx;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.lucas.guild.model.Adventurer;

public class Enemy extends GameObject {
    public int health;
    public int maxHealth;
    private float attackCooldown = 0f;
    private final float ATTACK_INTERVAL = 2f;
    private final int attackDamage = 10;
    private final float detectionRadius = 15f;
    private final float attackRadius = 2f;
    private final float moveSpeed = 4f;
    public ModelInstance modelInstance;

    public Enemy(String name, int health) {
        super(name);
        this.health = health;
        this.maxHealth = health;
    }

    public void takeDamage(int amount) {
        this.health -= amount;
        if (this.health <= 0) {
            this.health = 0;
            this.isActive = false;
            System.out.println(this.name + " a ete vaincu !");
        } else {
            System.out.println(this.name + " a subi " + amount + " degats! PV restants: " + this.health);
        }
    }

    public void update(Adventurer player, btRigidBody playerBody, float delta) {
        if (!isActive) return;

        Vector3 playerPosition = playerBody.getCenterOfMassPosition();
        Vector3 enemyPosition = ((btRigidBody) this.body).getCenterOfMassPosition();
        float distance = playerPosition.dst(enemyPosition);

        if (distance <= detectionRadius) {
            Vector3 direction = new Vector3(playerPosition).sub(enemyPosition).nor();
            ((btRigidBody) this.body).setLinearVelocity(direction.scl(moveSpeed));

            if (distance <= attackRadius) {
                attackCooldown -= delta;
                if (attackCooldown <= 0) {
                    player.perdreVie(attackDamage);
                    System.out.println(this.name + " attaque et inflige " + attackDamage + " degats !");
                    attackCooldown = ATTACK_INTERVAL;
                }
            }
        } else {
            ((btRigidBody) this.body).setLinearVelocity(Vector3.Zero);
        }
    }
}
