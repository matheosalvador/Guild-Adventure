package com.lucas.guild.libgdx;

/** Réglages d'affichage valables pour toute la session de jeu (la touche V bascule la vue). */
final class ViewSettings {

    /** Vue à la 3e personne (la caméra recule derrière le joueur) au lieu de la 1re personne. */
    static boolean thirdPerson = false;

    /** Ombres portées (en plein air seulement) ; coupées, le jeu demande moins à la carte graphique. */
    static boolean shadows = true;

    private ViewSettings() {
    }
}
