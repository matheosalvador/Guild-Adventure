package com.lucas.guild.libgdx;

public class Enemy extends GameObject {
    public int health;
    public int maxHealth;

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
        }
    }
}
