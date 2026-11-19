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
        /** Le rôle de la forme (skin, hair, tunic, pants, boots...) : une variante de modèle peut le recolorer ou le cacher. */
        public String role;
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
        /** Idle : cycles par seconde du balancement. */
        public float freq = 0.4f;
    }

    public static class AttackData {
        public float duration = 0.35f;
        public Map<String, AnimData> joints = new LinkedHashMap<>();
    }

    public static class ModelData {
        /**
         * Variante : le modèle de base dont celui-ci reprend le corps, la marche, le repos et le coup (ce qu'il
         * écrit lui-même les remplace). Un modèle de base n'a pas de base lui-même.
         */
        public String base;
        /** Variante : couleur de chaque rôle de forme (« skin » : « #C98F6B »). */
        public Map<String, String> roles = new LinkedHashMap<>();
        /** Variante : les rôles dont les formes sont cachées (« cloak »). */
        public List<String> hide = new ArrayList<>();
        /** Variante : formes en plus (barbe, tablier, oreilles...). */
        public List<PartData> extra = new ArrayList<>();
        /** Variante : identifiants de « gear » portés en plus (un marteau, un bâton). */
        public List<String> gear = new ArrayList<>();
        /** Variante : étirement du modèle [x, y, z] (nain = large et petit). */
        public float[] scale;
        /** Au repos : léger balancement de certaines articulations (respiration, regard). */
        public Map<String, AnimData> idle = new LinkedHashMap<>();
        public Map<String, JointData> joints = new LinkedHashMap<>();
        public List<PartData> parts = new ArrayList<>();
        /** Pose de repos : angle fixe (« from », en degrés) de certaines articulations (bras un peu plié, arme inclinée). */
        public Map<String, AnimData> pose = new LinkedHashMap<>();
        /** Marche : balancement de chaque articulation, au rythme des pas. */
        public Map<String, AnimData> walk = new LinkedHashMap<>();
        public AttackData attack;
        /** Axe autour duquel le modèle bascule à sa mort : « x » (il tombe à la renverse) ou « z » (il tombe sur le côté). */
        public String fallAxis = "x";
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

    /**
     * Le modèle {@code id} tel qu'on le construit : une variante reprend le corps, la marche, le repos et le
     * coup de sa base (ce qu'elle écrit elle-même les remplace) et garde ses couleurs, ses formes en plus, son
     * équipement et son étirement.
     */
    public ModelData resolved(String id) {
        ModelData m = models.get(id);
        if (m == null || m.base == null) {
            return m;
        }
        ModelData base = models.get(m.base);
        ModelData r = new ModelData();
        r.base = m.base;
        r.roles = m.roles;
        r.hide = m.hide;
        r.extra = m.extra;
        r.gear = m.gear;
        r.scale = m.scale;
        r.joints = base.joints;
        r.parts = base.parts;
        r.strideLength = m.strideLength != 1.6f ? m.strideLength : base.strideLength;
        r.fallAxis = !"x".equals(m.fallAxis) ? m.fallAxis : base.fallAxis;
        r.pose = !m.pose.isEmpty() ? m.pose : base.pose;
        r.walk = !m.walk.isEmpty() ? m.walk : base.walk;
        r.idle = !m.idle.isEmpty() ? m.idle : base.idle;
        r.attack = m.attack != null ? m.attack : base.attack;
        return r;
    }

    void validate(String file) {
        if (models == null || models.isEmpty()) throw new IllegalStateException(file + " : aucun modele");
        if (gear == null) gear = new LinkedHashMap<>();
        if (armorColors == null) armorColors = new LinkedHashMap<>();
        for (Map.Entry<String, GearData> e : gear.entrySet()) {
            if (e.getValue() == null) throw new IllegalStateException(file + " : equipement \"" + e.getKey() + "\" : vide");
            validateParts(e.getValue().parts, null, file + " : equipement \"" + e.getKey() + "\"");
        }
        for (Map.Entry<String, ModelData> e : models.entrySet()) {
            String where = file + " : modele \"" + e.getKey() + "\"";
            ModelData m = e.getValue();
            if (m == null) throw new IllegalStateException(where + " : vide");
            if (m.joints == null) m.joints = new LinkedHashMap<>();
            if (m.roles == null) m.roles = new LinkedHashMap<>();
            if (m.hide == null) m.hide = new ArrayList<>();
            if (m.extra == null) m.extra = new ArrayList<>();
            if (m.gear == null) m.gear = new ArrayList<>();
            if (m.pose == null) m.pose = new LinkedHashMap<>();
            if (m.walk == null) m.walk = new LinkedHashMap<>();
            if (m.idle == null) m.idle = new LinkedHashMap<>();
            if (m.strideLength <= 0) throw new IllegalStateException(where + " : \"strideLength\" doit etre > 0");
            if (!"x".equals(m.fallAxis) && !"z".equals(m.fallAxis)) throw new IllegalStateException(where + " : \"fallAxis\" doit etre x ou z");
            if (m.scale != null && (m.scale.length != 3 || m.scale[0] <= 0 || m.scale[1] <= 0 || m.scale[2] <= 0)) {
                throw new IllegalStateException(where + " : \"scale\" doit avoir 3 nombres > 0 [x, y, z]");
            }
            for (Map.Entry<String, String> role : m.roles.entrySet()) {
                if (!validColor(role.getValue())) throw new IllegalStateException(where + " : roles \"" + role.getKey() + "\" : couleur invalide (" + role.getValue() + ")");
            }
            for (String g : m.gear) {
                if (!gear.containsKey(g)) throw new IllegalStateException(where + " : equipement inconnu \"" + g + "\" (voir \"gear\")");
            }
            Map<String, JointData> joints = m.joints;
            if (m.base != null) {
                ModelData base = models.get(m.base);
                if (base == null) throw new IllegalStateException(where + " : \"base\" inconnue \"" + m.base + "\"");
                if (base.base != null) throw new IllegalStateException(where + " : la base \"" + m.base + "\" est elle-meme une variante (une seule generation permise)");
                if (!m.joints.isEmpty() || (m.parts != null && !m.parts.isEmpty())) {
                    throw new IllegalStateException(where + " : une variante n'ecrit ni \"joints\" ni \"parts\" (utiliser \"extra\")");
                }
                joints = base.joints;
            } else {
                if (m.parts == null || m.parts.isEmpty()) throw new IllegalStateException(where + " : aucune forme (\"parts\")");
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
            }
            validateParts(m.extra, joints, where + " : extra");
            validateAnim("pose", m.pose, joints, where);
            validateAnim("walk", m.walk, joints, where);
            validateAnim("idle", m.idle, joints, where);
            if (m.attack != null) {
                if (m.attack.duration <= 0) throw new IllegalStateException(where + " : attack : \"duration\" doit etre > 0");
                validateAnim("attack", m.attack.joints, joints, where);
            }
        }
        for (Map.Entry<String, String> e : armorColors.entrySet()) {
            if (!validColor(e.getValue())) throw new IllegalStateException(file + " : armorColors \"" + e.getKey() + "\" : couleur invalide (" + e.getValue() + ")");
        }
    }
}
