package com.lucas.guild.libgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.lucas.guild.data.LoreData;
import com.lucas.guild.data.GameContent;
import com.lucas.guild.game.Lore;
import com.lucas.guild.game.QuestLog;
import com.lucas.guild.game.WorldEvents;

/** Intérieur de l'auberge « La Pinte qui Chante » : l'aubergiste, un lit à l'étage. */
public class InnScreen extends InteriorScreen {

    private static final String INNKEEPER = "Innkeeper";
    private static final String BED = "Bed";
    private static final String CELLAR = "Cellar";

    // Prix en Po
    private static final int MEAL_PRICE = 5;
    private static final int BEER_PRICE = 2;
    private static final int BED_PRICE = 10;

    public InnScreen(MainGame game, String difficulty) {
        super(game, difficulty, "inn", "l'auberge");
    }

    @Override
    protected Color getBackgroundColor() {
        return new Color(0.3f, 0.2f, 0.1f, 1f);
    }

    @Override
    protected void buildWorld() {
        Color floor = Color.MAROON;
        Color wall = Color.TAN;

        buildRoom(20f, floor, wall);

        // Aubergiste (PNJ)
        addNpc("innkeeper", 0, 5f, INNKEEPER);

        // Trappe de la cave
        addBox(3f, 0.2f, 3f, new Color(0.23f, 0.14f, 0.07f, 1f), 6f, 0.6f, -4f, CELLAR);

        // Étage : deux planchers, le lit
        addBox(12f, 1f, 20f, floor, 4, 10f, 0, null);
        addBox(8f, 1f, 12f, floor, -6, 10f, 4, null);
        addBox(4f, 2f, 8f, Color.RED, 5, 11.5f, 5, BED);

        // Murs de l'étage
        addBox(1f, 10f, 20f, wall, 10f, 15f, 0, null);
        addBox(1f, 10f, 20f, wall, -10f, 15f, 0, null);
        addBox(20f, 10f, 1f, wall, 0, 15f, 10f, null);
        addBox(20f, 10f, 1f, wall, 0, 15f, -10f, null);

        // Toit
        addBox(20f, 1f, 20f, floor, 0, 20f, 0, null);

        // Escalier
        for (int i = 0; i < 10; i++) {
            addBox(4f, 1f, 2f, Color.BROWN, -8, 1f + i, 8f - i * 2f, null);
        }
    }

    @Override
    protected String getNpcPrompt(Object userData) {
        if (INNKEEPER.equals(userData)) {
            return "Parler à l'aubergiste (E)";
        }
        if (CELLAR.equals(userData)) {
            return "Descendre à la cave (E)";
        }
        if (BED.equals(userData)) {
            return "Dormir (" + BED_PRICE + " Po) (E)";
        }
        return null;
    }

    @Override
    protected boolean onNpcInteract(Object userData) {
        if (INNKEEPER.equals(userData)) {
            toggleDialog(INNKEEPER);
            return true;
        }
        if (CELLAR.equals(userData)) {
            game.setScreen(new CellarScreen(game, difficulty));
            return true;
        }
        if (BED.equals(userData)) {
            if (!player.spendGold(BED_PRICE)) {
                gameClock.postMessage("Pas assez d'or : une nuit coûte " + BED_PRICE + " Po.");
                return true;
            }
            Gdx.app.log("Auberge", "Le joueur va dormir");
            gameClock.sleepUntilNextMorning();
            player.train("vigor", 2); // une vie max plus haute doit être soignée aussi
            player.heal(player.getMaxHealth());
            return true;
        }
        return false;
    }

    @Override
    protected void buildDialog(String dialogType) {
        if (!INNKEEPER.equals(dialogType)) {
            return;
        }
        dialogTable.add("Bienvenue à " + com.lucas.guild.data.GameContent.town(player.getTownId()).findByInterior("inn").name
                + " ! Que désirez-vous ?").row();

        final String innName = com.lucas.guild.data.GameContent.town(player.getTownId()).findByInterior("inn").name;
        final QuestLog questLog = new QuestLog(player);
        if (questLog.canDeliver(innName, GameContent.quests())) {
            addDialogButton("Remettre la livraison", () -> {
                String message = questLog.deliver(innName, GameContent.quests());
                toggleDialog(null);
                gameClock.postMessage(message);
            });
        }

        addDialogButton("Manger un repas (" + MEAL_PRICE + " Po)", () -> {
            toggleDialog(null);
            if (!player.spendGold(MEAL_PRICE)) {
                gameClock.postMessage("Pas assez d'or : un repas coûte " + MEAL_PRICE + " Po.");
                return;
            }
            Gdx.app.log("Auberge", "Le joueur mange un repas.");
            player.heal(20);
            gameClock.postMessage("Vous mangez un bon repas (+20 PV).");
            player.train("vigor", 1);
        });

        addDialogButton("Boire une bière (" + BEER_PRICE + " Po)", () -> {
            toggleDialog(null);
            if (!player.spendGold(BEER_PRICE)) {
                gameClock.postMessage("Pas assez d'or : une bière coûte " + BEER_PRICE + " Po.");
                return;
            }
            Gdx.app.log("Auberge", "Le joueur boit une bière.");
            gameClock.postMessage("Vous buvez une bière. Ça fait du bien.");
        });

        addDialogButton("Offrir une bière et écouter les rumeurs (" + BEER_PRICE + " Po)", () -> {
            LoreData.RumorData rumor = Lore.nextRumor(player, player.getTownId());
            if (rumor == null) {
                toggleDialog(null);
                gameClock.postMessage("L'aubergiste n'a plus rien de neuf à raconter, pour l'instant.");
                return;
            }
            if (!player.spendGold(BEER_PRICE)) {
                toggleDialog(null);
                gameClock.postMessage("Pas assez d'or : une bière coûte " + BEER_PRICE + " Po.");
                return;
            }
            Lore.learn(player, Lore.rumorKey(rumor.id));
            dialogTable.clear();
            dialogTable.add("L'aubergiste se penche vers vous :").pad(10).row();
            addWrappedText("\"" + rumor.text + "\"");
            dialogTable.add(UiKit.message(skin, "(Ajouté au journal : J)")).pad(5).row();
            addDialogButton("Merci", () -> {
                dialogTable.clear();
                buildDialog(INNKEEPER);
            });
        });

        addDialogButton("Demander des nouvelles des autres villes", () -> {
            dialogTable.clear();
            dialogTable.add("L'aubergiste réfléchit :").pad(10).row();
            boolean any = false;
            for (com.lucas.guild.data.WorldData.PlaceData place : com.lucas.guild.data.GameContent.world().places) {
                if (place.zoneFile != null || place.id.equals(player.getTownId())) {
                    continue;
                }
                String news = WorldEvents.describeAt(player, place.id);
                if (news != null) {
                    addWrappedText(place.name + " : " + news);
                    any = true;
                }
            }
            if (!any) {
                addWrappedText("Rien de spécial ailleurs, d'après les voyageurs. Les prix sont normaux.");
            }
            addDialogButton("Merci", () -> {
                dialogTable.clear();
                buildDialog(INNKEEPER);
            });
        });

        addLeaveButton();
    }
}
