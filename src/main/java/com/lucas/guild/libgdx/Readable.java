package com.lucas.guild.libgdx;

import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.LoreData;
import com.lucas.guild.game.Lore;
import com.lucas.guild.model.Adventurer;

/**
 * Un texte à lire dans le décor (panneau, stèle, livre) : ce qu'on obtient en visant une partie de décor d'action
 * « read ». Le texte vient de lore.json ; sa première lecture le note dans le journal.
 */
final class Readable {

    private static final String DIALOG_PREFIX = "Text:";

    final String textId;

    Readable(String textId) {
        this.textId = textId;
    }

    /** Le texte de lore.json, ou null s'il n'existe pas. */
    LoreData.TextData text() {
        return GameContent.lore().texts.get(textId);
    }

    /** « Lire : Panneau de la porte (E) », ou null. */
    String prompt() {
        LoreData.TextData text = text();
        return text != null ? "Lire : " + text.title + " (E)" : null;
    }

    /** Note le texte dans le journal du personnage. @return true si c'était la première lecture */
    boolean learn(Adventurer player) {
        return Lore.learn(player, Lore.textKey(textId));
    }

    /** Le type de dialogue qui affiche ce texte (voir {@link #textOfDialog}). */
    String dialogType() {
        return DIALOG_PREFIX + textId;
    }

    /** Le texte qu'affiche le dialogue {@code dialogType}, ou null si ce n'est pas la lecture d'un texte. */
    static LoreData.TextData textOfDialog(String dialogType) {
        if (dialogType == null || !dialogType.startsWith(DIALOG_PREFIX)) {
            return null;
        }
        return GameContent.lore().texts.get(dialogType.substring(DIALOG_PREFIX.length()));
    }
}
