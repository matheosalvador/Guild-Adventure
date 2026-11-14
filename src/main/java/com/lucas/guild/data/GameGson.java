package com.lucas.guild.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lucas.guild.game.RuntimeTypeAdapterFactory;
import com.lucas.guild.model.Arme;
import com.lucas.guild.model.Butin;
import com.lucas.guild.model.Armure;
import com.lucas.guild.model.Attaque;
import com.lucas.guild.model.AttaqueDistance;
import com.lucas.guild.model.AttaqueMelee;
import com.lucas.guild.model.AttaqueSimple;
import com.lucas.guild.model.Item;
import com.lucas.guild.model.ObjetDeQuete;
import com.lucas.guild.model.PotionDeSoin;
import com.lucas.guild.model.PotionDps;
import com.lucas.guild.model.PotionMix;
import com.lucas.guild.model.PotionSoin;
import com.lucas.guild.model.Sac;

/**
 * Configuration Gson unique du jeu, partagée par la sauvegarde et le chargement des données.
 *
 * Les types abstraits ({@link Item}, {@link Attaque}) sont écrits avec un champ discriminant
 * ({@code itemType}, {@code attaqueType}) pour retrouver la bonne classe à la lecture.
 * Quand on ajoute une sous-classe d'objet ou d'attaque, il suffit de l'enregistrer ici.
 */
public final class GameGson {

    private GameGson() {
    }

    public static Gson create() {
        RuntimeTypeAdapterFactory<Item> itemAdapter = RuntimeTypeAdapterFactory.of(Item.class, "itemType")
                .registerSubtype(Arme.class)
                .registerSubtype(Armure.class)
                .registerSubtype(PotionSoin.class)
                .registerSubtype(PotionDps.class)
                .registerSubtype(PotionMix.class)
                .registerSubtype(PotionDeSoin.class)
                .registerSubtype(ObjetDeQuete.class)
                .registerSubtype(Butin.class)
                .registerSubtype(Sac.class);

        RuntimeTypeAdapterFactory<Attaque> attaqueAdapter = RuntimeTypeAdapterFactory.of(Attaque.class, "attaqueType")
                .registerSubtype(AttaqueSimple.class)
                .registerSubtype(AttaqueMelee.class)
                .registerSubtype(AttaqueDistance.class);

        return new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapterFactory(itemAdapter)
                .registerTypeAdapterFactory(attaqueAdapter)
                .create();
    }
}
