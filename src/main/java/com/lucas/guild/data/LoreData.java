package com.lucas.guild.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Le savoir du monde tel qu'écrit dans {@code lore.json} : les rumeurs que l'aubergiste raconte
 * et les textes qu'on lit sur les panneaux, stèles et livres. Ne contient que des données.
 */
public class LoreData {

    /** Les rumeurs, dans l'ordre où l'aubergiste les raconte. */
    public List<RumorData> rumors = new ArrayList<>();
    /** Les textes à lire, par identifiant. */
    public Map<String, TextData> texts = new LinkedHashMap<>();

    /** Une rumeur : entendue à l'auberge de la ville {@code town} (de n'importe laquelle si absent). */
    public static class RumorData {
        public String id;
        public String town;
        public String text;
    }

    /** Un texte à lire. */
    public static class TextData {
        /** Identifiant (la clé dans lore.json) : renseigné à la lecture. */
        public String id;
        public String title;
        public String text;
    }

    void validate(String file) {
        if (rumors == null) rumors = new ArrayList<>();
        if (texts == null) texts = new LinkedHashMap<>();
        List<String> seen = new ArrayList<>();
        for (int i = 0; i < rumors.size(); i++) {
            RumorData r = rumors.get(i);
            String where = file + " : rumeur n°" + (i + 1) + (r != null && r.id != null ? " (" + r.id + ")" : "");
            if (r == null || r.id == null || r.id.isEmpty()) throw new IllegalStateException(where + " : \"id\" manquant");
            if (seen.contains(r.id)) throw new IllegalStateException(where + " : \"id\" en double");
            seen.add(r.id);
            if (r.text == null || r.text.isEmpty()) throw new IllegalStateException(where + " : \"text\" manquant");
        }
        for (Map.Entry<String, TextData> e : texts.entrySet()) {
            TextData t = e.getValue();
            String where = file + " : texte \"" + e.getKey() + "\"";
            if (t == null) throw new IllegalStateException(where + " : vide");
            t.id = e.getKey();
            if (t.title == null || t.title.isEmpty()) throw new IllegalStateException(where + " : \"title\" manquant");
            if (t.text == null || t.text.isEmpty()) throw new IllegalStateException(where + " : \"text\" manquant");
        }
    }
}
