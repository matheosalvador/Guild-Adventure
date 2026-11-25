package com.lucas.guild.game;

import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Arme;

/**
 * Ce que fait le joueur pendant un combat, sans interface : son endurance, ses coups (rythme et coût selon le poids
 * de l'arme, coup chargé), sa garde (bloquer, parer au bon moment) et son esquive. L'écran transmet les touches et
 * demande ici si une action est possible ; {@link Combat#monsterHit} demande ici comment le coup est reçu.
 */
public final class PlayerFight {

    // ------------------------------------------------------------------ réglages (voir docs/REGLAGE-COMBATS.md)

    /** Endurance de base (la compétence Endurance en ajoute). */
    public static final float BASE_STAMINA = 100f;
    /** L'endurance remonte de tant par seconde... */
    public static final float STAMINA_REGEN = 35f;
    /** ...mais seulement après ce temps sans effort (ni coup, ni esquive, ni coup bloqué). */
    public static final float REGEN_DELAY = 0.3f;
    /** Poids d'arme quand on se bat à mains nues, en kg. */
    public static final float BARE_HANDS_KG = 0.5f;

    /** Temps entre deux coups : {@code SWING_BASE + SWING_PER_KG * poids de l'arme}, en secondes. */
    public static final float SWING_BASE = 0.38f;
    public static final float SWING_PER_KG = 0.08f;
    /** Coût d'un coup : {@code COST_BASE + COST_PER_KG * poids de l'arme}. */
    public static final float COST_BASE = 6f;
    public static final float COST_PER_KG = 4f;

    /** Garder le clic au moins ce temps donne un coup chargé. */
    public static final float CHARGE_TIME = 0.6f;
    /** Un coup chargé fait {@code CHARGE_BASE + CHARGE_PER_KG * poids} fois les dégâts, et coûte 50 % de plus. */
    public static final float CHARGE_BASE = 1.5f;
    public static final float CHARGE_PER_KG = 0.15f;
    public static final float CHARGE_COST_FACTOR = 1.5f;
    /** Un coup chargé fait chanceler un monstre ordinaire ce temps-là (pas un adversaire nommé). */
    public static final float CHARGE_STAGGER = 0.8f;

    /** L'esquive : un bond de {@code DODGE_SPEED} m/s pendant {@code DODGE_TIME} s, intouchable pendant {@code DODGE_SAFE} s. */
    public static final float DODGE_COST = 25f;
    public static final float DODGE_TIME = 0.25f;
    public static final float DODGE_SPEED = 14f;
    public static final float DODGE_SAFE = 0.3f;
    public static final float DODGE_COOLDOWN = 0.6f;

    /** La garde : on ne prend que ce pourcentage des dégâts... */
    public static final float BLOCK_KEEP = 0.25f;
    /** ...mais chaque coup bloqué coûte {@code BLOCK_COST_PER_DAMAGE} fois ses dégâts en endurance. */
    public static final float BLOCK_COST_PER_DAMAGE = 1.0f;
    /** Lever la garde juste avant le coup (moins de ce temps) : une parade, sans dégât, et le monstre chancelle. */
    public static final float PARRY_WINDOW = 0.2f;
    public static final float PARRY_STAGGER = 1.2f;
    /** Un adversaire nommé chancelle moins longtemps après une parade. */
    public static final float PARRY_STAGGER_BOSS = 0.7f;
    /** Garde brisée (plus assez d'endurance) : on ne peut plus la lever pendant ce temps. */
    public static final float GUARD_BROKEN_TIME = 1.0f;
    /** En garde, on avance à cette fraction de sa vitesse. */
    public static final float BLOCK_SPEED_FACTOR = 0.5f;

    /** Comment un coup de monstre a été reçu. */
    public enum Outcome {
        /** En plein. */
        HIT,
        /** Bloqué : dégâts réduits. */
        BLOCKED,
        /** La garde a cédé : dégâts entiers, garde impossible un instant. */
        GUARD_BROKEN,
        /** Paré : aucun dégât, le monstre chancelle. */
        PARRIED,
        /** Esquivé : aucun dégât. */
        DODGED
    }

    private float stamina = BASE_STAMINA;
    private float sinceEffort = REGEN_DELAY;
    private float swingCooldown;
    private boolean charging;
    private float chargeTime;
    private boolean blocking;
    private float blockTime;
    private float guardBroken;
    private float dodgeTime;
    private float dodgeSafe;
    private float dodgeCooldown;

    // ------------------------------------------------------------------ le temps passe

    /** Fait avancer les délais et l'endurance de {@code dt} secondes. */
    public void update(float dt, Adventurer player) {
        swingCooldown = Math.max(0f, swingCooldown - dt);
        dodgeTime = Math.max(0f, dodgeTime - dt);
        dodgeSafe = Math.max(0f, dodgeSafe - dt);
        dodgeCooldown = Math.max(0f, dodgeCooldown - dt);
        guardBroken = Math.max(0f, guardBroken - dt);
        if (charging) {
            chargeTime += dt;
        }
        if (blocking) {
            blockTime += dt;
            return;   // en garde, on ne reprend pas son souffle
        }
        sinceEffort += dt;
        if (sinceEffort >= REGEN_DELAY) {
            stamina = Math.min(maxStamina(player), stamina + STAMINA_REGEN * dt);
        }
    }

    private void spend(float amount) {
        stamina = Math.max(0f, stamina - amount);
        sinceEffort = 0f;
    }

    // ------------------------------------------------------------------ endurance

    public static float maxStamina(Adventurer player) {
        return BASE_STAMINA + (float) player.getSkillBonus("staminaBonus");
    }

    public float getStamina() {
        return stamina;
    }

    /** Remet l'endurance au plein (nouvelle scène, repos). */
    public void refill(Adventurer player) {
        stamina = maxStamina(player);
    }

    // ------------------------------------------------------------------ les coups du joueur

    /** Poids de l'arme tenue, en kg (mains nues : {@link #BARE_HANDS_KG}). */
    public static float weaponKg(Adventurer player) {
        Arme weapon = player.getInventaire() != null ? player.getInventaire().getWeapon() : null;
        return weapon != null ? (float) weapon.getPoids() : BARE_HANDS_KG;
    }

    /** Temps entre deux coups avec cette arme, en secondes. */
    public static float swingTime(float kg) {
        return SWING_BASE + SWING_PER_KG * kg;
    }

    /** Endurance que coûte un coup avec cette arme. */
    public static float swingCost(float kg, boolean charged) {
        float cost = COST_BASE + COST_PER_KG * kg;
        return charged ? cost * CHARGE_COST_FACTOR : cost;
    }

    /** Ce que multiplie un coup chargé avec cette arme. */
    public static float chargeFactor(float kg) {
        return CHARGE_BASE + CHARGE_PER_KG * kg;
    }

    /** Le clic est enfoncé : le coup se prépare (il part au relâchement). */
    public void startCharge() {
        charging = true;
        chargeTime = 0f;
    }

    /** Le coup en préparation est perdu (un menu s'est ouvert). */
    public void cancelCharge() {
        charging = false;
        chargeTime = 0f;
    }

    public boolean isCharging() {
        return charging;
    }

    /** Le coup en préparation est-il assez long pour être chargé ? */
    public boolean isCharged() {
        return charging && chargeTime >= CHARGE_TIME;
    }

    /** La part de la charge déjà faite (0 à 1), pour l'affichage. */
    public float chargeShare() {
        return charging ? Math.min(1f, chargeTime / CHARGE_TIME) : 0f;
    }

    /** Peut-on frapper maintenant (délai écoulé, pas en garde, assez d'endurance) ? */
    public boolean canSwing(float kg, boolean charged) {
        return swingCooldown <= 0f && !blocking && stamina >= swingCost(kg, charged);
    }

    /**
     * Le clic est relâché : le coup part s'il le peut (normal ou chargé selon le temps gardé).
     *
     * @return 0 si aucun coup ne part, sinon ce par quoi multiplier les dégâts (1 = coup normal)
     */
    public float release(float kg) {
        boolean charged = isCharged();
        charging = false;
        chargeTime = 0f;
        if (!canSwing(kg, charged)) {
            return 0f;
        }
        swingCooldown = swingTime(kg);
        spend(swingCost(kg, charged));
        return charged ? chargeFactor(kg) : 1f;
    }

    /** Le joueur ne peut pas frapper pour l'instant (délai du dernier coup) : vrai tant que le délai court. */
    public boolean isRecovering() {
        return swingCooldown > 0f;
    }

    // ------------------------------------------------------------------ la garde

    /** Lève la garde (clic droit enfoncé), si elle n'est pas brisée. @return vrai si on est en garde */
    public boolean raiseGuard() {
        if (guardBroken > 0f || dodgeTime > 0f) {
            return false;
        }
        if (!blocking) {
            blocking = true;
            blockTime = 0f;
            charging = false;
        }
        return true;
    }

    /** Baisse la garde (clic droit relâché). */
    public void lowerGuard() {
        blocking = false;
    }

    public boolean isBlocking() {
        return blocking;
    }

    public boolean isGuardBroken() {
        return guardBroken > 0f;
    }

    // ------------------------------------------------------------------ l'esquive

    /** Tente une esquive. @return vrai si elle part (assez d'endurance, pas trop tôt après la précédente) */
    public boolean dodge() {
        if (dodgeCooldown > 0f || dodgeTime > 0f || stamina < DODGE_COST) {
            return false;
        }
        blocking = false;
        charging = false;
        dodgeTime = DODGE_TIME;
        dodgeSafe = DODGE_SAFE;
        dodgeCooldown = DODGE_COOLDOWN;
        spend(DODGE_COST);
        return true;
    }

    /** Le bond de l'esquive est-il en cours (le corps est lancé) ? */
    public boolean isDodging() {
        return dodgeTime > 0f;
    }

    // ------------------------------------------------------------------ recevoir un coup

    /**
     * Un monstre frappe : comment le coup est-il reçu ?
     * Le coût d'un blocage est payé ici ; les dégâts, eux, sont appliqués par {@link Combat#monsterHit}.
     *
     * @param damage les dégâts du monstre, avant la défense
     */
    public Outcome receive(int damage) {
        if (dodgeSafe > 0f) {
            return Outcome.DODGED;
        }
        if (!blocking) {
            return Outcome.HIT;
        }
        if (blockTime <= PARRY_WINDOW) {
            return Outcome.PARRIED;
        }
        float cost = damage * BLOCK_COST_PER_DAMAGE;
        if (stamina >= cost) {
            spend(cost);
            return Outcome.BLOCKED;
        }
        spend(stamina);
        blocking = false;
        guardBroken = GUARD_BROKEN_TIME;
        return Outcome.GUARD_BROKEN;
    }
}
