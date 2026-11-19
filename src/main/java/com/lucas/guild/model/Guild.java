package com.lucas.guild.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.lucas.guild.data.GameContent;

public class Guild {

    private List<Quest> availableQuests = new ArrayList<>();

    public Guild() {
        initializeQuests();
    }

    /** Les quêtes viennent de assets/data/quests.json (rang 0 = F, 1 = E...). */
    private void initializeQuests() {
        availableQuests.addAll(GameContent.quests());
    }

    /** Les quetes du tableau de la ville {@code townId} accessibles a ce rang. */
    public List<Quest> getQuestsFor(String townId, int rank) {
        return availableQuests.stream()
                .filter(q -> q.getTown().equals(townId) && q.getRank() <= rank)
                .collect(Collectors.toList());
    }
}
