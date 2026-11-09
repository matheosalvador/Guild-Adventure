package com.lucas.guild.data;

import java.util.ArrayList;
import java.util.List;

/** Une boutique telle qu'écrite dans {@code shops.json}. */
public class ShopData {

    public String title;
    /** Pourcentage du prix payé quand le joueur revend un objet à la boutique. */
    public int sellPercent = 50;
    /** Identifiants (items.json) des objets en vente. */
    public List<String> stock = new ArrayList<>();
}
