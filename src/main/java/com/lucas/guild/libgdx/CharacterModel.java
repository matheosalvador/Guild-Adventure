package com.lucas.guild.libgdx;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.lucas.guild.data.ModelsData;

/**
 * Un personnage en 3D, construit d'après {@code models.json} : des formes (boîtes, cylindres, sphères...)
 * portées par des articulations qui bougent à la marche et au coup, plus l'équipement visible
 * (arme en main, armure, sac). Les règles du jeu n'en dépendent pas : c'est de l'affichage.
 *
 * <p>Le modèle regarde vers -Z, comme le joueur à l'angle 0. {@link #update} le déplace et l'anime.</p>
 */
class CharacterModel implements Disposable {

    /** Une articulation : son pivot, son parent et l'angle courant (degrés, par axe). */
    private static final class Joint {
        final String name;
        final Joint parent;
        final Vector3 pivot;
        final float[] angle = new float[3];
        final Matrix4 matrix = new Matrix4();

        Joint(String name, Joint parent, ModelsData.JointData data) {
            this.name = name;
            this.parent = parent;
            this.pivot = new Vector3(data.x, data.y, data.z);
        }
    }

    /** Une forme posée sur le modèle. */
    private static final class Piece {
        final ModelInstance instance;
        final Joint joint;
        final ModelsData.PartData data;
        final Color baseColor;

        Piece(ModelInstance instance, Joint joint, ModelsData.PartData data, Color baseColor) {
            this.instance = instance;
            this.joint = joint;
            this.data = data;
            this.baseColor = new Color(baseColor);
        }
    }

    private final Array<ModelInstance> sink;
    private final ModelsData data;
    private final ModelsData.ModelData def;
    private final String file;

    private final Map<String, Joint> joints = new HashMap<>();
    private final List<Joint> jointOrder = new ArrayList<>();
    private final List<Piece> pieces = new ArrayList<>();
    private final Matrix4 root = new Matrix4();
    private final Matrix4 tmp = new Matrix4();

    private boolean visible = false;
    private boolean flashing = false;
    private boolean falling = false;
    private float fallTime;
    private float idleTime;
    private String look = "";
    private float walkPhase;
    private float walkBlend;
    private float attackTimer;

    /**
     * @param sink  les instances à dessiner de l'écran : le modèle y ajoute et en retire ses formes
     * @param modelId le modèle dans models.json (« human »)
     */
    CharacterModel(Array<ModelInstance> sink, ModelsData data, String modelId) {
        this.sink = sink;
        this.data = data;
        this.def = data.resolved(modelId);
        this.file = "models.json (modele \"" + modelId + "\")";
        if (def == null) {
            throw new IllegalStateException("models.json : modele inconnu \"" + modelId + "\"");
        }
        for (Map.Entry<String, ModelsData.JointData> e : def.joints.entrySet()) {
            Joint joint = new Joint(e.getKey(), e.getValue().parent != null ? joints.get(e.getValue().parent) : null, e.getValue());
            joints.put(e.getKey(), joint);
            jointOrder.add(joint);
        }
        rebuild(null, null, null);
    }

    /**
     * Habille le modèle : la couleur et les formes de l'armure, l'arme en main et le sac, d'après leurs
     * identifiants d'objet (items.json), ou {@code null} s'il n'y en a pas. Ne reconstruit rien si rien n'a changé.
     */
    void setLook(String armorId, String weaponId, String bagId) {
        String signature = armorId + "|" + weaponId + "|" + bagId;
        if (!signature.equals(look)) {
            rebuild(armorId, weaponId, bagId);
        }
    }

    private void rebuild(String armorId, String weaponId, String bagId) {
        look = armorId + "|" + weaponId + "|" + bagId;
        clear();
        String armorColor = armorId != null ? data.armorColors.get(armorId) : null;
        for (ModelsData.PartData part : def.parts) {
            add(part, armorColor);
        }
        for (ModelsData.PartData part : def.extra) {
            add(part, null);
        }
        for (String id : def.gear) {
            for (ModelsData.PartData part : data.gear.get(id).parts) {
                add(part, null);
            }
        }
        for (String id : new String[] {armorId, weaponId, bagId}) {
            ModelsData.GearData gear = id != null ? data.gear.get(id) : null;
            if (gear != null) {
                for (ModelsData.PartData part : gear.parts) {
                    add(part, null);
                }
            }
        }
        if (visible) {
            for (Piece piece : pieces) {
                sink.add(piece.instance);
            }
        }
    }

    private void add(ModelsData.PartData part, String armorColor) {
        if (part.role != null && def.hide.contains(part.role)) {
            return;
        }
        String hex = part.color;
        if ("armor".equals(part.slot) && armorColor != null) {
            hex = armorColor;
        } else if (part.role != null && def.roles.containsKey(part.role)) {
            hex = def.roles.get(part.role);
        }
        Color color = ColorNames.parse(hex, file, "une forme (" + part.shape + ")");
        Model model = sharedModel(part, color);
        pieces.add(new Piece(new ModelInstance(model), part.joint != null ? joints.get(part.joint) : null, part, color));
        flashing = false;
    }

    private void clear() {
        for (Piece piece : pieces) {
            sink.removeValue(piece.instance, true);
        }
        pieces.clear();
    }

    /**
     * Les formes sont partagées par tous les personnages (même forme, taille et couleur : même modèle) : un loup de
     * plus ne construit plus rien. Chaque instance a sa propre copie du matériau, donc le clignotement d'un monstre
     * touché ne touche que lui. Libérées à la fermeture du jeu ({@link #disposeShared()}).
     */
    private static final Map<String, Model> SHARED = new java.util.HashMap<>();

    private static Model sharedModel(ModelsData.PartData part, Color color) {
        String key = part.shape + "/" + part.w + "x" + part.h + "x" + part.d + "/" + color.toIntBits();
        Model model = SHARED.get(key);
        if (model == null) {
            model = Shapes.create(part.shape, part.w, part.h, part.d, new Material(ColorAttribute.createDiffuse(color)));
            SHARED.put(key, model);
        }
        return model;
    }

    /** Libère les formes partagées (à la fermeture du jeu). */
    static void disposeShared() {
        for (Model model : SHARED.values()) {
            model.dispose();
        }
        SHARED.clear();
    }

    void setVisible(boolean visible) {
        if (visible == this.visible) {
            return;
        }
        this.visible = visible;
        for (Piece piece : pieces) {
            if (visible) {
                sink.add(piece.instance);
            } else {
                sink.removeValue(piece.instance, true);
            }
        }
    }

    /** Fait clignoter le modèle en rouge (il vient d'être touché) ou le remet à ses couleurs. */
    void setFlash(boolean flash) {
        if (flash == flashing) {
            return;
        }
        flashing = flash;
        for (Piece piece : pieces) {
            ColorAttribute diffuse = (ColorAttribute) piece.instance.materials.first().get(ColorAttribute.Diffuse);
            if (flash) {
                diffuse.color.set(1f, 0.25f, 0.25f, 1f);
            } else {
                diffuse.color.set(piece.baseColor);
            }
        }
    }

    /** Le personnage s'effondre (mort) : en une demi-seconde, il bascule au sol. */
    void startFall() {
        falling = true;
        fallTime = 0f;
    }

    boolean isFalling() {
        return falling;
    }

    /** Lance le geste de coup (si le modèle en a un). */
    void startAttack() {
        if (def.attack != null) {
            attackTimer = def.attack.duration;
        }
    }

    /**
     * Pose et anime le modèle.
     *
     * @param dt    secondes écoulées
     * @param speed vitesse de déplacement au sol, en m/s (anime la marche)
     * @param feet  position des pieds
     * @param yaw   direction regardée, en degrés (0 = vers -Z)
     */
    void update(float dt, float speed, Vector3 feet, float yaw) {
        if (speed > 0.1f) {
            walkPhase = (walkPhase + 360f * speed * dt / def.strideLength) % 360f;
        }
        walkBlend += (Math.min(1f, speed / 3f) - walkBlend) * Math.min(1f, dt * 10f);
        attackTimer = Math.max(0f, attackTimer - dt);
        idleTime += dt;
        if (falling) {
            fallTime += dt;
        }

        for (Joint joint : jointOrder) {
            joint.angle[0] = joint.angle[1] = joint.angle[2] = 0f;
        }
        for (Map.Entry<String, ModelsData.AnimData> e : def.pose.entrySet()) {
            joints.get(e.getKey()).angle[axis(e.getValue().axis)] += e.getValue().from;
        }
        for (Map.Entry<String, ModelsData.AnimData> e : def.idle.entrySet()) {
            ModelsData.AnimData a = e.getValue();
            joints.get(e.getKey()).angle[axis(a.axis)] += a.amp * MathUtils.sinDeg(360f * a.freq * idleTime + a.phase) * (1f - walkBlend);
        }
        boolean attacking = attackTimer > 0f && def.attack != null;
        for (Map.Entry<String, ModelsData.AnimData> e : def.walk.entrySet()) {
            if (attacking && def.attack.joints.containsKey(e.getKey())) {
                continue;   // le geste du coup remplace la marche pour cette articulation
            }
            ModelsData.AnimData a = e.getValue();
            joints.get(e.getKey()).angle[axis(a.axis)] += a.amp * MathUtils.sinDeg(walkPhase + a.phase) * walkBlend;
        }
        if (attacking) {
            float t = 1f - attackTimer / def.attack.duration;
            for (Map.Entry<String, ModelsData.AnimData> e : def.attack.joints.entrySet()) {
                ModelsData.AnimData a = e.getValue();
                joints.get(e.getKey()).angle[axis(a.axis)] += a.from + (a.to - a.from) * t;
            }
        }

        root.idt().translate(feet).rotate(Vector3.Y, yaw);
        if (falling) {
            float t = Math.min(1f, fallTime / 0.5f);
            float eased = t * t * (3f - 2f * t);
            root.rotate("z".equals(def.fallAxis) ? Vector3.Z : Vector3.X, 90f * eased);
        }
        if (def.scale != null) {
            root.scale(def.scale[0], def.scale[1], def.scale[2]);
        }
        for (Joint joint : jointOrder) {
            joint.matrix.idt().translate(joint.pivot)
                    .rotate(Vector3.Y, joint.angle[1]).rotate(Vector3.X, joint.angle[0]).rotate(Vector3.Z, joint.angle[2])
                    .translate(-joint.pivot.x, -joint.pivot.y, -joint.pivot.z);
            if (joint.parent != null) {
                tmp.set(joint.parent.matrix).mul(joint.matrix);
                joint.matrix.set(tmp);
            }
        }
        for (Piece piece : pieces) {
            Matrix4 m = piece.instance.transform;
            m.set(root);
            if (piece.joint != null) {
                m.mul(piece.joint.matrix);
            }
            m.translate(piece.data.x, piece.data.y, piece.data.z);
            if (piece.data.rot != null) {
                m.rotate(Vector3.Y, piece.data.rot[1]).rotate(Vector3.X, piece.data.rot[0]).rotate(Vector3.Z, piece.data.rot[2]);
            }
        }
    }

    private static int axis(String name) {
        return "x".equals(name) ? 0 : "y".equals(name) ? 1 : 2;
    }

    @Override
    public void dispose() {
        clear();
    }
}
