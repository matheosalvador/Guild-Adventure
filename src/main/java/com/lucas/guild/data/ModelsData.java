package com.lucas.guild.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Les modèles 3D tels qu'écrits dans {@code models.json} : un corps fait de formes simples (boîtes,
 * cylindres, sphères...) rattachées à des articulations qui bougent (marche, coup), l'équipement
 * visible (armes, armures, sacs) et les couleurs d'armure. Ne contient que des données.
 *
 * <p>Les positions sont en mètres, dans le repère du modèle : l'origine est entre les pieds, au sol ;
 * le modèle regarde vers -Z ; +X est à sa droite ; +Y est le haut.</p>
 */
public class ModelsData {

    public static final List<String> SHAPES = Arrays.asList("box", "cylinder", "sphere", "cone", "capsule");

    public Map<String, ModelData> models = new LinkedHashMap<>();
    /** Équipement visible, par identifiant d'objet (items.json) : des formes en plus sur le corps. */
    public Map<String, GearData> gear = new LinkedHashMap<>();
    /** Couleur du vêtement porté par-dessus (parties de « slot » : « armor »), par identifiant d'armure. */
    public Map<String, String> armorColors = new LinkedHashMap<>();

    /** Un point d'articulation : le modèle pivote autour de lui. {@code parent} doit être défini avant. */
    public static class JointData {
        public float x;
        public float y;
        public float z;
        public String parent;
    }

    /** Une forme : taille, position du centre, rotation en degrés (x, y, z), couleur, articulation qui la porte. */
    public static class PartData {
        public String shape = "box";
        public float w;
        public float h;
        public float d;
        public float x;
        public float y;
        public float z;
        public float[] rot;
        public String color;
        /** Articulation qui porte la forme ; absent = le corps lui-même. */
        public String joint;
        /** « armor » : la forme prend la couleur de l'armure portée, si le personnage en porte une. */
        public String slot;
    }

    /** Le mouvement d'une articulation : balancement (amp, phase) ou de « from » à « to » (en degrés). */
    public static class AnimData {
        public String axis = "x";
        public float amp;
        public float phase;
        public float from;
        public float to;
    }

    public static class AttackData {
        public float duration = 0.35f;
        public Map<String, AnimData> joints = new LinkedHashMap<>();
    }

    public static class ModelData {
        public Map<String, JointData> joints = new LinkedHashMap<>();
        public List<PartData> parts = new ArrayList<>();
        /** Pose de repos : angle fixe (« from », en degrés) de certaines articulations (bras un peu plié, arme inclinée). */
        public Map<String, AnimData> pose = new LinkedHashMap<>();
        /** Marche : balancement de chaque articulation, au rythme des pas. */
        public Map<String, AnimData> walk = new LinkedHashMap<>();
        public AttackData attack;
        /** Mètres parcourus pour un cycle de marche complet. */
        public float strideLength = 1.6f;
    }

    public static class GearData {
        public List<PartData> parts = new ArrayList<>();
    }

    private static boolean validColor(String c) {
        return c != null && (c.matches("#[0-9A-Fa-f]{6}") || c.matches("[A-Za-z_]+"));
    }

    private static void validateParts(List<PartData> parts, Map<String, JointData> joints, String where) {
        if (parts == null) {
            return;
        }
        for (int i = 0; i < parts.size(); i++) {
            PartData p = parts.get(i);
            String at = where + " : forme n°" + (i + 1);
            if (p == null) throw new IllegalStateException(at + " : vide");
            if (p.shape == null || !SHAPES.contains(p.shape)) throw new IllegalStateException(at + " : \"shape\" inconnue \"" + p.shape + "\" (attendu : " + SHAPES + ")");
            if (p.w <= 0 || p.h <= 0 || p.d <= 0) throw new IllegalStateException(at + " : \"w\", \"h\" et \"d\" doivent etre > 0");
            if (!validColor(p.color)) throw new IllegalStateException(at + " : \"color\" manquante ou invalide (" + p.color + ")");
            if (p.rot != null && p.rot.length != 3) throw new IllegalStateException(at + " : \"rot\" doit avoir 3 nombres [x, y, z]");
            if (p.joint != null && joints != null && !joints.containsKey(p.joint)) throw new IllegalStateException(at + " : articulation inconnue \"" + p.joint + "\"");
            if (p.slot != null && !"armor".equals(p.slot)) throw new IllegalStateException(at + " : \"slot\" inconnu \"" + p.slot + "\" (attendu : armor)");
        }
    }

    private static void validateAnim(String name, Map<String, AnimData> anims, Map<String, JointData> joints, String where) {
        if (anims == null) {
            return;
        }
        for (Map.Entry<String, AnimData> e : anims.entrySet()) {
            String at = where + " : " + name + " : \"" + e.getKey() + "\"";
            if (!joints.containsKey(e.getKey())) throw new IllegalStateException(at + " : articulation inconnue");
            AnimData a = e.getValue();
            if (a == null || a.axis == null || !Arrays.asList("x", "y", "z").contains(a.axis)) {
                throw new IllegalStateException(at + " : \"axis\" doit etre x, y ou z");
            }
        }
    }

    void validate(String file) {
        if (models == null || models.isEmpty()) throw new IllegalStateException(file + " : aucun modele");
        if (gear == null) gear = new LinkedHashMap<>();
        if (armorColors == null) armorColors = new LinkedHashMap<>();
        for (Map.Entry<String, ModelData> e : models.entrySet()) {
            String where = file + " : modele \"" + e.getKey() + "\"";
            ModelData m = e.getValue();
            if (m == null) throw new IllegalStateException(where + " : vide");
            if (m.joints == null) m.joints = new LinkedHashMap<>();
            if (m.parts == null || m.parts.isEmpty()) throw new IllegalStateException(where + " : aucune forme (\"parts\")");
            if (m.strideLength <= 0) throw new IllegalStateException(where + " : \"strideLength\" doit etre > 0");
            List<String> seen = new ArrayList<>();
            for (Map.Entry<String, JointData> j : m.joints.entrySet()) {
                JointData jd = j.getValue();
                if (jd == null) throw new IllegalStateException(where + " : articulation \"" + j.getKey() + "\" vide");
                if (jd.parent != null && !seen.contains(jd.parent)) {
                    throw new IllegalStateException(where + " : articulation \"" + j.getKey() + "\" : le parent \"" + jd.parent + "\" doit etre defini avant elle");
                }
                seen.add(j.getKey());
            }
            validateParts(m.parts, m.joints, where);
            validateAnim("pose", m.pose, m.joints, where);
            validateAnim("walk", m.walk, m.joints, where);
            if (m.attack != null) {
                if (m.attack.duration <= 0) throw new IllegalStateException(where + " : attack : \"duration\" doit etre > 0");
                validateAnim("attack", m.attack.joints, m.joints, where);
            }
        }
        for (Map.Entry<String, GearData> e : gear.entrySet()) {
            if (e.getValue() == null) throw new IllegalStateException(file + " : equipement \"" + e.getKey() + "\" : vide");
            validateParts(e.getValue().parts, null, file + " : equipement \"" + e.getKey() + "\"");
        }
        for (Map.Entry<String, String> e : armorColors.entrySet()) {
            if (!validColor(e.getValue())) throw new IllegalStateException(file + " : armorColors \"" + e.getKey() + "\" : couleur invalide (" + e.getValue() + ")");
        }
    }
}
