package com.lucas.guild.libgdx;

import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.lucas.guild.model.Item;

public class GameObject {
    public String name;
    public boolean isActive = true;
    public Item item;
    
    public btCollisionObject body;

    public GameObject(String name) {
        this.name = name;
    }

    public GameObject(String name, Item item) {
        this.name = name;
        this.item = item;
    }
}
