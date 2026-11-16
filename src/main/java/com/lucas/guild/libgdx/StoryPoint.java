package com.lucas.guild.libgdx;

import com.lucas.guild.game.Story;

/**
 * Un endroit du décor où l'histoire se joue (un cairn, un sablier, un maître d'arène) : ce qu'on obtient en visant
 * une partie de décor d'action « story ». Son nom et ses dialogues sont dans story.json ({@code points}, {@code dialogs}).
 */
final class StoryPoint {

    final String id;

    StoryPoint(String id) {
        this.id = id;
    }

    /** « Le Cairn des Anciens (E) ». */
    String prompt() {
        String name = Story.pointName(id);
        return (name != null ? name : id) + " (E)";
    }
}
