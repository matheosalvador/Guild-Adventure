package com.lucas.guild.model;

/**
 * Un objet du jeu. Les objets du catalogue (assets/data/items.json) ont un identifiant, un poids
 * en kg et un prix en Po ; voir {@link BaseItem}.
 */
public interface Item {
    /** Identifiant dans le catalogue (items.json), ou null pour un objet créé hors catalogue. */
    String getId();

    String getName();

    String getDescription();

    void use(Entite user, Entite target);

    /** Poids en kg. */
    double getPoids();

    /** Prix d'achat en Po (0 = ne se vend pas). */
    int getPrix();

    /** Peut-on s'en servir hors combat, sans cible (ex. une potion de soin) ? */
    default boolean estUtilisableHorsCombat() {
        return false;
    }
}
