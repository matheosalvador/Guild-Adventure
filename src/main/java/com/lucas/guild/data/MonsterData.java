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
    /**
     * Temps pendant lequel il prépare un coup avant de frapper (on le voit venir), en secondes ; absent = 0,5 s.
     * Toujours plus court que le temps entre deux coups.
     */
    public float windUp;
    /** Or lâché à la mort : tiré entre ces deux valeurs. */
    public int goldMin;
    public int goldMax;
    /** Nom de l'objet de quête lâché si une quête active le demande comme preuve, ou null. */
    public String lootItem;
    /** Objets qui tombent au sol à la mort (items.json), chacun avec sa chance en %. */
    public java.util.List<Drop> drops = new java.util.ArrayList<>();
    /** Le modèle 3D qui le représente (models.json) ; absent = une simple boîte de la couleur ci-dessous. */
    public String model;
    /** Nom de couleur LibGDX ou « #RRGGBB ». */
    public String color;
    /** Taille de la boîte qui le représente, en m. */
    public float w;
    public float h;
    public float d;

    /** Hauteur de vol au-dessus du sol, en m (une chauve-souris) ; 0 = il marche. Affichage seulement. */
    public float flyHeight;
    /** Un adversaire nommé : sa barre de vie reste affichée en haut de l'écran tant qu'il est proche. */
    public boolean boss;
    /** Drapeau d'histoire posé à sa mort (voir story.json), et la phrase affichée alors. */
    public String killFlag;
    public String killMessage;
    /**
     * Il se rend au lieu de mourir quand sa vie passe sous ce pourcentage (0 = il se bat jusqu'au bout) : il cesse
     * de se battre et le drapeau {@code yieldFlag} est posé.
     */
    public int yieldPercent;
    public String yieldFlag;
    /** La phrase affichée quand il se rend. */
    public String yieldMessage;
    /** Les phases du combat, de la première à la dernière : chacune commence quand sa vie passe sous {@code below} %. */
    public java.util.List<Phase> phases = new java.util.ArrayList<>();

    /** Une phase de combat : ce qui change quand la vie du monstre passe sous un seuil. */
    public static class Phase {
        /** La phase commence quand la vie passe sous ce pourcentage. */
        public int below;
        /** Nouvelles valeurs (absent ou 0 = inchangé). */
        public int damage;
        public float speed;
        public float attackCooldown;
        /** Il reprend {@code healAmount} PV toutes les {@code healEvery} secondes pendant cette phase. */
        public float healEvery;
        public int healAmount;
        /** Conditions d'histoire pour que ce soin ait lieu (absent = toujours). */
        public java.util.List<String> healRequires = new java.util.ArrayList<>();
        /** Des renforts apparaissent au début de la phase. */
        public String summon;
        public int summonCount = 1;
        /** Phrase affichée au début de la phase. */
        public String message;
    }

    /** Un objet qui peut tomber au sol. */
    public static class Drop {
        /** Identifiant dans items.json. */
        public String item;
        /** Chance de tomber, de 1 à 100 %. */
        public int chance = 100;
    }
}
