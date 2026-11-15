package com.lucas.guild.game;

import java.util.List;

/**
 * Un marcheur qui suit un parcours en boucle (les villageois d'une ville) : il va d'un point au suivant, puis
 * revient au premier. Sans dépendance à l'interface.
 *
 * <p>La direction regardée est donnée en degrés, avec la convention du jeu : 0 = vers -Z.</p>
 */
public final class RouteWalker {

    private final float[] xs;
    private final float[] zs;
    private final float speed;

    private int from = 0;
    private float travelled = 0f;
    private float x;
    private float z;
    private float yaw;

    /**
     * @param points les points du parcours, au moins deux ({x, z})
     * @param speed  vitesse en m/s, > 0
     */
    public RouteWalker(List<float[]> points, float speed) {
        if (points == null || points.size() < 2) {
            throw new IllegalArgumentException("un parcours a au moins deux points");
        }
        if (speed <= 0f) {
            throw new IllegalArgumentException("la vitesse doit etre > 0");
        }
        this.xs = new float[points.size()];
        this.zs = new float[points.size()];
        for (int i = 0; i < points.size(); i++) {
            xs[i] = points.get(i)[0];
            zs[i] = points.get(i)[1];
        }
        this.speed = speed;
        this.x = xs[0];
        this.z = zs[0];
        update(0f);
    }

    private int next() {
        return (from + 1) % xs.length;
    }

    private float segmentLength() {
        int n = next();
        return (float) Math.hypot(xs[n] - xs[from], zs[n] - zs[from]);
    }

    /** Avance de {@code dt} secondes. */
    public void update(float dt) {
        travelled += speed * dt;
        float length = segmentLength();
        int guard = 0;
        while (travelled >= length && guard++ < 1000) {
            travelled -= length;
            from = next();
            length = segmentLength();
        }
        int n = next();
        float t = length > 0f ? travelled / length : 0f;
        x = xs[from] + (xs[n] - xs[from]) * t;
        z = zs[from] + (zs[n] - zs[from]) * t;
        float dx = xs[n] - xs[from];
        float dz = zs[n] - zs[from];
        if (dx != 0f || dz != 0f) {
            yaw = (float) Math.toDegrees(Math.atan2(-dx, -dz));
        }
    }

    /** Avance d'une distance en mètres (pour répartir les marcheurs le long du parcours au départ). */
    public void advanceMeters(float meters) {
        update(meters / speed);
    }

    public float getX() {
        return x;
    }

    public float getZ() {
        return z;
    }

    /** Direction du déplacement, en degrés (0 = vers -Z). */
    public float getYaw() {
        return yaw;
    }

    public float getSpeed() {
        return speed;
    }
}
