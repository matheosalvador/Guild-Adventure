package com.lucas.guild.libgdx;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.utils.Array;
import com.lucas.guild.data.StoryData;
import com.lucas.guild.game.GameSession;
import com.lucas.guild.game.GroundItems;
import com.lucas.guild.game.Story;

/**
 * L'histoire dans un écran : les objets d'histoire à ramasser (une petite boîte dorée chacun), les déclencheurs
 * franchis en marchant, et l'avancement des chapitres. Les règles sont dans {@link Story}.
 */
final class StoryScene {

    /**
     * Les textes d'histoire (ouverture et conclusion des chapitres, textes de scène) s'ouvrent-ils tout seuls ?
     * Toujours en jeu. Le banc de test visuel les coupe pour ses sections qui ne parlent pas de l'histoire :
     * ils attendent alors, sans être perdus.
     */
    static boolean textsEnabled = true;

    private final Story story;
    private final GameSession session;
    private final String scene;
    private final Array<ModelInstance> instances;
    private final GroundItemViews.BoxFactory boxes;
    private final Map<StoryData.PickupData, ModelInstance> views = new HashMap<>();

    /** @param scene le lieu (voir {@code BaseWorldScreen.sceneKey()}) */
    StoryScene(GameSession session, String scene, Array<ModelInstance> instances, GroundItemViews.BoxFactory boxes) {
        this.session = session;
        this.story = new Story(session);
        this.scene = scene;
        this.instances = instances;
        this.boxes = boxes;
    }

    Story story() {
        return story;
    }

    /** Montre les objets d'histoire à ramasser ici en ce moment. */
    void showPickups() {
        for (StoryData.PickupData pickup : story.pickupsIn(scene)) {
            views.put(pickup, boxes.box(0.45f, 0.45f, 0.45f, Color.GOLD, pickup.x, 0.75f, pickup.z));
        }
    }

    /** L'objet d'histoire le plus proche du point, à portée de main, ou null. */
    StoryData.PickupData nearestPickup(float x, float z) {
        StoryData.PickupData best = null;
        double bestDistance = GroundItems.PICKUP_RANGE;
        for (StoryData.PickupData pickup : views.keySet()) {
            double distance = Math.hypot(x - pickup.x, z - pickup.z);
            if (distance <= bestDistance) {
                bestDistance = distance;
                best = pickup;
            }
        }
        return best;
    }

    /** Le joueur, en (x, z), ramasse l'objet d'histoire le plus proche. @return la phrase à afficher, ou null s'il n'y en a pas à portée */
    String pickUpNearest(float x, float z) {
        StoryData.PickupData pickup = nearestPickup(x, z);
        if (pickup == null) {
            return null;
        }
        instances.removeValue(views.remove(pickup), true);
        return story.pick(pickup);
    }

    /**
     * À chaque image : les déclencheurs franchis par le joueur, puis l'avancement de l'histoire. Les phrases vont
     * dans la file de messages de la partie.
     */
    void update(float x, float z) {
        for (StoryData.TriggerData trigger : story.enter(scene, x, z)) {
            if (trigger.message != null && !trigger.message.isEmpty()) {
                session.getClock().postMessage(trigger.message);
            }
            if (trigger.text != null && !trigger.text.isEmpty()) {
                sceneTexts.add(trigger);
            }
        }
        for (String message : story.advance()) {
            session.getClock().postMessage(message);
        }
    }

    /** Les textes de scène ouverts par des déclencheurs, en attente d'être montrés. */
    private final java.util.ArrayDeque<StoryData.TriggerData> sceneTexts = new java.util.ArrayDeque<>();

    boolean hasSceneText() {
        return !sceneTexts.isEmpty();
    }

    /** Le prochain texte de scène à montrer (il est retiré de la file), ou null. */
    StoryData.TriggerData pollSceneText() {
        return sceneTexts.poll();
    }

    void clear() {
        views.clear();
    }
}
