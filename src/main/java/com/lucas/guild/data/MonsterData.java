package com.lucas.guild.data;

/** Un type de monstre tel qu'écrit dans {@code monsters.json}. */
public class MonsterData {

    /** Identifiant (la clé dans monsters.json) : renseigné à la lecture. */
    public String id;
    public String name;
    public int health;
    /** Dégâts d'un coup, avant la défense du joueur. */
    public int damage;
    /** Vitesse de course, en m/s. */
    public float speed;
    /** Distance à partir de laquelle le monstre repère le joueur et le poursuit, en m. */
    public float aggroRange;
    /** Distance à laquelle il frappe, en m. */
    public float attackRange;
    /** Temps entre deux coups, en secondes. */
    public float attackCooldown;
    /** Or lâché à la mort : tiré entre ces deux valeurs. */
    public int goldMin;
    public int goldMax;
    /** Nom de l'objet de quête lâché si une quête active le demande comme preuve, ou null. */
    public String lootItem;
    /** Nom de couleur LibGDX ou « #RRGGBB ». */
    public String color;
    /** Taille de la boîte qui le représente, en m. */
    public float w;
    public float h;
    public float d;
}
