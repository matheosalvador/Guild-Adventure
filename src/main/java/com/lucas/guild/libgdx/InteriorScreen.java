package com.lucas.guild.libgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.lucas.guild.data.GameContent;
import com.lucas.guild.data.NpcsData;
import com.lucas.guild.data.TownData;
import com.lucas.guild.data.WorldData;
import com.lucas.guild.game.NpcTalk;
import com.lucas.guild.game.QuestLog;
import com.lucas.guild.game.WorldEvents;

/**
 * Intérieur d'un bâtiment (forge, guilde, auberge) : une pièce avec une porte de sortie
 * qui ramène en ville, et un ou plusieurs PNJ.
 *
 * Cette classe gère ce qui est commun à tous les intérieurs : le point d'apparition devant
 * la porte, la porte « Sortir (E) » avec sauvegarde automatique, et le gabarit de pièce.
 * L'écran concret décrit seulement ses PNJ et ses dialogues.
 */
public abstract class InteriorScreen extends BaseWorldScreen {

    private final Vector3 townSpawn;
    private final String placeName;

    /**
     * @param interiorId identifiant de l'intérieur dans town.json (« forge », « guild », « inn ») :
     *                   on y lit où réapparaît le joueur en ville quand il sort
     * @param placeName nom du lieu pour le journal (« la forge », « la guilde »...)
     */
    protected InteriorScreen(MainGame game, String difficulty, String interiorId, String placeName) {
        super(game, difficulty);
        TownData.BuildingData building = GameContent.town(game.getSession().getPlayer().getTownId()).findByInterior(interiorId);
        if (building == null) {
            throw new IllegalStateException("town.json : aucun batiment n'a \"interior\": \"" + interiorId + "\"");
        }
        this.townSpawn = new Vector3(building.exit.x, building.exit.y, building.exit.z);
        this.placeName = placeName;
    }

    // ------------------------------------------------------------------ comportement commun

    @Override
    protected Vector3 getSpawnPoint() {
        // On arrive toujours devant la porte
        return new Vector3(0, 2, -8);
    }

    @Override
    protected final String getInteractionPrompt(Object userData) {
        String npcPrompt = getNpcPrompt(userData);
        if (npcPrompt != null) {
            return npcPrompt;
        }
        return "Door".equals(userData) ? getDoorPrompt() : null;
    }

    @Override
    protected final boolean onInteract(Object userData) {
        if (onNpcInteract(userData)) {
            return true;
        }
        if ("Door".equals(userData)) {
            leave();
            return true;
        }
        return false;
    }

    /** Texte affiché quand on vise la porte de sortie. */
    protected String getDoorPrompt() {
        return "Sortir (E)";
    }

    /** Ce que fait la porte : par défaut, retour en ville avec sauvegarde automatique. */
    protected void leave() {
        leaveToTown();
    }

    /** Texte à afficher quand on vise un PNJ ou un objet propre à ce lieu, sinon null. */
    protected abstract String getNpcPrompt(Object userData);

    /** Action de E sur un PNJ ou un objet propre à ce lieu. @return true si géré. */
    protected abstract boolean onNpcInteract(Object userData);

    /** On entre et on sort d'un intérieur sans quitter le monde ouvert (s'il y était). */
    @Override
    protected boolean keepsWorldMode() {
        return true;
    }

    /** Sauvegarde automatique puis retour en ville, devant la porte du bâtiment (dans le monde ouvert si on en vient). */
    private void leaveToTown() {
        com.lucas.guild.game.WorldLayout layout = com.lucas.guild.game.WorldLayout.standard();
        if (game.getSession().isInOpenWorld() && layout.contains(player.getTownId())) {
            float[] w = layout.toWorld(player.getTownId(), townSpawn.x, townSpawn.z);
            saveManager.saveGame(player, gameClock, difficulty, w[0], townSpawn.y, w[1], OpenWorldScreen.SAVE_SCREEN);
            game.setScreen(new OpenWorldScreen(game, difficulty, new Vector3(w[0], townSpawn.y, w[1])));
            return;
        }
        // On sauvegarde la position DANS LA VILLE (devant la porte), pas celle de l'intérieur.
        Vector3 spawn = new Vector3(townSpawn);
        saveManager.saveGame(player, gameClock, difficulty, spawn.x, spawn.y, spawn.z, "Town");
        Gdx.app.log("SAVE", "Sauvegarde automatique en quittant " + placeName);
        game.setScreen(new TownScreen(game, difficulty, spawn));
    }

    // ------------------------------------------------------------------ PNJ de npcs.json

    /** Le texte quand on vise le PNJ {@code npcId} : « Parler à l'aubergiste (E) ». */
    protected String npcPrompt(String npcId) {
        return GameContent.npcs().npc(npcId).prompt + " (E)";
    }

    /**
     * Le dialogue d'un PNJ de npcs.json : son accueil, les dialogues d'histoire qu'il propose, ses choix, puis le bouton
     * qui ferme.
     *
     * @param npcId      le PNJ (« innkeeper »)
     * @param dialogType le dialogue à rouvrir pour revenir à l'accueil
     * @param place      le nom du bâtiment : remplace {@code {lieu}}, et reçoit les livraisons par défaut
     * @param shopId     la boutique du bâtiment (chez le commerçant), sinon null
     * @param openQuests ouvre le tableau des quêtes (à la guilde), sinon null
     */
    protected void buildNpcDialog(String npcId, final String dialogType, String place, String shopId, Runnable openQuests) {
        NpcsData.NpcData npc = GameContent.npcs().npc(npcId);
        Runnable back = () -> {
            dialogTable.clear();
            buildDialog(dialogType);
        };
        dialogTable.add(NpcTalk.fill(npc.greeting, place, 0)).row();
        addStoryDialogs(npcId, back);
        for (NpcsData.OptionData option : NpcTalk.options(npc, story())) {
            addNpcOption(option, place, shopId, openQuests, back);
        }
        addDialogButton(npc.leave, () -> toggleDialog(null));
    }

    private void addNpcOption(final NpcsData.OptionData option, String place, String shopId, final Runnable openQuests, final Runnable back) {
        String label = NpcTalk.label(option);
        switch (option.action) {
            case "quests":
                if (story().guildClosed()) {
                    // une fin de l'histoire : la Guilde ne confie plus rien à ce personnage
                    if (option.closedText != null) {
                        dialogTable.add(UiKit.message(skin, option.closedText)).pad(10).row();
                    }
                } else if (openQuests != null) {
                    addDialogButton(label, () -> {
                        toggleDialog(null);
                        openQuests.run();
                    });
                }
                break;
            case "shop":
                final String shop = option.shop != null ? option.shop : shopId;
                if (shop != null) {
                    addDialogButton(label, () -> showShop(shop, back));
                }
                break;
            case "deliver":
                final String target = option.target != null ? option.target : place;
                final QuestLog questLog = new QuestLog(player);
                if (questLog.canDeliver(target, GameContent.quests())) {
                    addDialogButton(label, () -> {
                        String message = questLog.deliver(target, GameContent.quests());
                        toggleDialog(null);
                        gameClock.postMessage(message);
                    });
                }
                break;
            case "serve":
                addDialogButton(label, () -> {
                    toggleDialog(null);
                    gameClock.postMessage(NpcTalk.serve(player, option));
                });
                break;
            case "rumor":
                addDialogButton(label, () -> {
                    NpcTalk.RumorResult result = NpcTalk.buyRumor(player, player.getTownId(), option);
                    if (result.rumor == null) {
                        toggleDialog(null);
                        gameClock.postMessage(result.refusal);
                        return;
                    }
                    dialogTable.clear();
                    dialogTable.add(option.intro).pad(10).row();
                    addWrappedText("\"" + result.rumor.text + "\"");
                    dialogTable.add(UiKit.message(skin, "(Ajouté au journal : J)")).pad(5).row();
                    addDialogButton(option.back, back);
                });
                break;
            case "news":
                addDialogButton(label, () -> {
                    dialogTable.clear();
                    dialogTable.add(option.intro).pad(10).row();
                    boolean any = false;
                    for (WorldData.PlaceData other : GameContent.world().places) {
                        if (other.zoneFile != null || other.id.equals(player.getTownId())) {
                            continue;
                        }
                        String news = WorldEvents.describeAt(player, other.id);
                        if (news != null) {
                            addWrappedText(other.name + " : " + news);
                            any = true;
                        }
                    }
                    if (!any) {
                        addWrappedText(option.emptyText);
                    }
                    addDialogButton(option.back, back);
                });
                break;
            case "talk":
                addDialogButton(label, () -> {
                    dialogTable.clear();
                    addWrappedText(option.text);
                    addDialogButton(option.back, back);
                });
                break;
            default:
                throw new IllegalStateException("npcs.json : action inconnue \"" + option.action + "\"");
        }
    }

    // ------------------------------------------------------------------ gabarit de pièce

    /**
     * Construit une pièce carrée : sol, trois murs et la porte de sortie (face au joueur à
     * l'apparition). Le quatrième mur, côté porte, est fermé par la porte elle-même.
     *
     * @param size côté de la pièce (20 pour la forge et l'auberge, 30 pour la guilde)
     */
    protected void buildRoom(float size, Color floorColor, Color wallColor) {
        float half = size / 2f;

        addBox(size, 1f, size, floorColor, 0, 0, 0, null);                    // sol
        addBox(1f, 10f, size, wallColor, half, 5f, 0, null);                  // mur est
        addBox(1f, 10f, size, wallColor, -half, 5f, 0, null);                 // mur ouest
        addBox(size, 10f, 1f, wallColor, 0, 5f, half, null);                  // mur du fond
        addBox(4f, 8f, 0.5f, Color.YELLOW, 0, 4.5f, -(half - 0.25f), "Door"); // porte
    }
}
