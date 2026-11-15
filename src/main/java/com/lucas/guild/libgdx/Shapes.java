package com.lucas.guild.libgdx;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;

/**
 * Les formes de base des décors et des modèles : boîte, cylindre, sphère, cône, capsule et « gable » (toit à
 * deux pentes : un prisme triangulaire dont la crête court le long de X).
 */
final class Shapes {

    private static final long ATTRIBUTES = Usage.Position | Usage.Normal;

    private Shapes() {
    }

    /** @param w largeur (X), h hauteur (Y), d profondeur (Z) ; la forme est centrée sur l'origine. */
    static Model create(String shape, float w, float h, float d, Material material) {
        ModelBuilder builder = new ModelBuilder();
        switch (shape) {
            case "cylinder":
                return builder.createCylinder(w, h, d, 14, material, ATTRIBUTES);
            case "sphere":
                return builder.createSphere(w, h, d, 14, 10, material, ATTRIBUTES);
            case "cone":
                return builder.createCone(w, h, d, 14, material, ATTRIBUTES);
            case "capsule":
                return builder.createCapsule(Math.min(w, d) / 2f, h, 14, material, ATTRIBUTES);
            case "gable":
                return gable(w, h, d, material);
            default:
                return builder.createBox(w, h, d, material, ATTRIBUTES);
        }
    }

    /** Le toit à deux pentes : base w x d, hauteur h, crête le long de X. */
    private static Model gable(float w, float h, float d, Material material) {
        float hw = w / 2f;
        float hh = h / 2f;
        float hd = d / 2f;
        Vector3 a = new Vector3(-hw, -hh, -hd);
        Vector3 b = new Vector3(hw, -hh, -hd);
        Vector3 c = new Vector3(hw, -hh, hd);
        Vector3 dd = new Vector3(-hw, -hh, hd);
        Vector3 ridgeL = new Vector3(-hw, hh, 0);
        Vector3 ridgeR = new Vector3(hw, hh, 0);

        ModelBuilder builder = new ModelBuilder();
        builder.begin();
        MeshPartBuilder part = builder.part("gable", GL20.GL_TRIANGLES, ATTRIBUTES, material);
        Vector3 front = new Vector3(0, hd, -2f * hh).nor();
        Vector3 back = new Vector3(0, hd, 2f * hh).nor();
        triangle(part, a, ridgeR, b, front);
        triangle(part, a, ridgeL, ridgeR, front);
        triangle(part, dd, c, ridgeR, back);
        triangle(part, dd, ridgeR, ridgeL, back);
        triangle(part, a, dd, ridgeL, new Vector3(-1, 0, 0));
        triangle(part, b, ridgeR, c, new Vector3(1, 0, 0));
        triangle(part, a, b, c, new Vector3(0, -1, 0));
        triangle(part, a, c, dd, new Vector3(0, -1, 0));
        return builder.end();
    }

    /** Un triangle dont les sommets sont ordonnés pour être vus de face depuis le côté de {@code normal}. */
    private static void triangle(MeshPartBuilder part, Vector3 p1, Vector3 p2, Vector3 p3, Vector3 normal) {
        Vector3 cross = new Vector3(p2).sub(p1).crs(new Vector3(p3).sub(p1));
        if (cross.dot(normal) < 0f) {
            Vector3 swap = p2;
            p2 = p3;
            p3 = swap;
        }
        part.triangle(vertex(p1, normal), vertex(p2, normal), vertex(p3, normal));
    }

    private static MeshPartBuilder.VertexInfo vertex(Vector3 p, Vector3 n) {
        return new MeshPartBuilder.VertexInfo().setPos(p).setNor(n);
    }
}
