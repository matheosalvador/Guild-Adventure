package com.lucas.guild.libgdx;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Colors;

/** Couleurs des fichiers de données : un nom LibGDX (« BROWN ») ou un code hexadécimal (« #5B8C3A »). */
final class ColorNames {

    private ColorNames() {
    }

    /**
     * @param where ce qu'on colorie, pour le message d'erreur (« batiment "Forge" »)
     * @throws IllegalStateException si la couleur est inconnue
     */
    static Color parse(String name, String file, String where) {
        Color color = null;
        try {
            color = name.startsWith("#") ? Color.valueOf(name) : Colors.get(name);
        } catch (RuntimeException e) {
            // code hexadécimal invalide : message ci-dessous
        }
        if (color == null) {
            throw new IllegalStateException(file + " : " + where + " : couleur inconnue \"" + name + "\"");
        }
        return color;
    }
}
