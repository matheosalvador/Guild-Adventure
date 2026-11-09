package com.lucas.guild.model;

import java.util.ArrayList;
import java.util.List;

import com.lucas.guild.data.GameContent;

public class Inventaire {
    private final List<PotionSoin> potionsSoin = new ArrayList<>();
    private final List<PotionDps> potionsDps = new ArrayList<>();
    private final List<PotionMix> potionsMix = new ArrayList<>();
    private double poidsMax = 25.0; // Poids de base pour un chevalier

    public Inventaire() {
    }

    /** Poids max selon la difficulté, lu dans assets/data/difficulties.json (inchangé si la difficulté est inconnue). */
    public void setPoidsMax(String difficulte) {
        Double max = GameContent.maxWeight(difficulte);
        if (max != null) {
            this.poidsMax = max;
        }
    }

    public double getPoidsActuel() {
        double poidsActuel = 0;
        for (Item item : getItems()) {
            poidsActuel += item.getPoids();
        }
        return poidsActuel;
    }

    public double getPoidsMax() {
        return poidsMax;
    }

    public boolean ajouterItem(Item item) {
        if (getPoidsActuel() + item.getPoids() <= poidsMax) {
            if (item instanceof PotionSoin) {
                potionsSoin.add((PotionSoin) item);
            } else if (item instanceof PotionDps) {
                potionsDps.add((PotionDps) item);
            } else if (item instanceof PotionMix) {
                potionsMix.add((PotionMix) item);
            }
            return true;
        }
        return false;
    }

    public void retirerItem(Item item) {
        if (item instanceof PotionSoin) {
            potionsSoin.remove(item);
        } else if (item instanceof PotionDps) {
            potionsDps.remove(item);
        } else if (item instanceof PotionMix) {
            potionsMix.remove(item);
        }
    }

    public List<Item> getItems() {
        List<Item> allItems = new ArrayList<>();
        allItems.addAll(potionsSoin);
        allItems.addAll(potionsDps);
        allItems.addAll(potionsMix);
        return allItems;
    }

    public boolean estVide() {
        return potionsSoin.isEmpty() && potionsDps.isEmpty() && potionsMix.isEmpty();
    }
}
