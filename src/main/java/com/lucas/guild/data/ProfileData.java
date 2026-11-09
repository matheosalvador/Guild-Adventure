package com.lucas.guild.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Une classe ({@code classes.json}) ou une origine ({@code backgrounds.json}) du personnage :
 * des avantages et des inconvénients chiffrés, choisis à la création.
 */
public class ProfileData {

    public String id;
    public String label;
    public String description;

    /** Points de vie en plus (ou en moins si négatif). */
    public int healthBonus;
    /** Charge portable en plus, en kg. */
    public double carryBonus;
    /** Vitesse de déplacement en plus, en % (négatif = plus lent). */
    public int speedBonusPercent;
    /** Or de départ en plus (ou en moins), en Po. */
    public int goldBonus;
    /** Identifiants (items.json) des objets de départ. */
    public List<String> startingItems = new ArrayList<>();
    /** Compétences déjà débloquées au départ : identifiant -> niveau. */
    public Map<String, Integer> skillStart = new LinkedHashMap<>();
    /** Progression plus rapide (+) ou plus lente (-) d'une compétence, en % : identifiant -> %. */
    public Map<String, Integer> practiceBonusPercent = new LinkedHashMap<>();
}
