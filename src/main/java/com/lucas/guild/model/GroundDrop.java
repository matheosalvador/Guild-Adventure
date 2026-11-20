package com.lucas.guild.model;

/**
 * Un objet posé par terre (butin d'un monstre, objet jeté) : le lieu où il est, sa position (x, z) dans ce lieu et
 * le jour de jeu où il y a été posé. Sauvegardé avec le personnage ; il disparaît au bout de quelques jours.
 */
public class GroundDrop {

    /** Le lieu (voir {@code BaseWorldScreen.sceneKey()} : « OpenWorld », « ZoneScreen@mine_galeries »...). */
    public String scene;
    public Item item;
    public float x;
    public float z;
    /** Le jour de jeu où l'objet a été posé. */
    public int day;

    /** Pour Gson. */
    public GroundDrop() {
    }

    public GroundDrop(String scene, Item item, float x, float z, int day) {
        this.scene = scene;
        this.item = item;
        this.x = x;
        this.z = z;
        this.day = day;
    }

    public float distanceTo(float px, float pz) {
        return (float) Math.hypot(px - x, pz - z);
    }
}
