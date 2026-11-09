package com.lucas.guild.model;

/**
 * Base des objets : identifiant, poids et prix, lus dans items.json.
 *
 * Un objet créé en code (sans poids dans les données) garde le poids par défaut de son type,
 * donné par {@link #defaultPoids()}.
 */
public abstract class BaseItem implements Item {

    protected String id;
    protected double poids;
    protected int prix;

    /** Poids utilisé quand les données n'en précisent pas. */
    protected abstract double defaultPoids();

    @Override
    public String getId() {
        return id;
    }

    @Override
    public double getPoids() {
        return poids > 0 ? poids : defaultPoids();
    }

    @Override
    public int getPrix() {
        return prix;
    }
}
