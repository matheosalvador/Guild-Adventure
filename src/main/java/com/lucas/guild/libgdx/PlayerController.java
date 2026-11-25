package com.lucas.guild.libgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.linearmath.btDefaultMotionState;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.IntIntMap;
import com.lucas.guild.game.Settings;
import com.lucas.guild.model.Adventurer;

public class PlayerController implements Disposable {

    private final Camera camera;
    private final btRigidBody playerBody;
    private final btDefaultMotionState motionState;
    private final btDiscreteDynamicsWorld dynamicsWorld;
    private final Adventurer player;

    private final IntIntMap keys = new IntIntMap();
    private final float moveSpeed = 8f;
    private final float jumpForce = 300f;
    private final float rotationSpeed = 0.25f;

    private final Vector3 moveDirection = new Vector3();
    private final Vector3 tmp = new Vector3();
    private final Vector3 tmp2 = new Vector3();
    private final Quaternion rotation = new Quaternion();
    private final Quaternion camRotation = new Quaternion();
    private final Quaternion pitchRotation = new Quaternion();

    private float yaw = 0f;
    private float pitch = 0f;
    /** Ce qui multiplie la marche en ce moment (la garde ralentit), et le bond d'une esquive en cours. */
    private float combatSpeedFactor = 1f;
    private boolean dashing;
    private float dashSpeed;
    private final Vector3 dashDirection = new Vector3();
    private float lastViewDistance = Float.MAX_VALUE;

    private final static Vector3 rayFrom = new Vector3();
    private final static Vector3 rayTo = new Vector3();
    private final static ClosestRayResultCallback rayCallback = new ClosestRayResultCallback(Vector3.Zero, Vector3.Z);

    /** Groupe de collision du joueur : les rayons de visée et de caméra l'ignorent (sinon, en 3e personne, on se viserait soi-même). */
    private static final short PLAYER_GROUP = 32;
    private static final short NOT_PLAYER = (short) ~PLAYER_GROUP;

    /** Distance de la caméra derrière le joueur en 3e personne, et décalage d'épaule. */
    private static final float THIRD_PERSON_DISTANCE = 3.4f;
    private static final float SHOULDER_RIGHT = 0.55f;
    private static final float SHOULDER_UP = 0.25f;

    private final Vector3 eye = new Vector3();
    private final Vector3 desired = new Vector3();
    private final Vector3 right = new Vector3();
    private float aimBonus;

    /**
     * La caméra à la 3e personne est une petite boule qu'on fait glisser des yeux du joueur jusqu'à sa place : elle
     * s'arrête au premier mur, donc reste toujours à son rayon de tout obstacle. Avec un simple rayon, dans un coin,
     * elle se retrouvait à quelques centimètres d'un mur voisin, que le plan proche de la caméra découpait.
     */
    private static final float CAMERA_RADIUS = 0.25f;
    /** Le sol est cherché sous les pieds par une boule un peu plus étroite que le joueur, jusqu'à cette distance. */
    private static final float GROUND_PROBE_RADIUS = 0.4f;
    private static final float GROUND_TOLERANCE = 0.12f;
    private static final float HALF_HEIGHT = 1f;   // du centre du corps à ses pieds (capsule : 0,5 + 0,5)

    private final btCollisionShape playerShape;
    private final btSphereShape cameraProbe = new btSphereShape(CAMERA_RADIUS);
    private final btSphereShape groundProbe = new btSphereShape(GROUND_PROBE_RADIUS);
    private final ClosestConvexResultCallback sweep = new ClosestConvexResultCallback(Vector3.Zero, Vector3.Z);
    private final Matrix4 sweepFrom = new Matrix4();
    private final Matrix4 sweepTo = new Matrix4();

    public PlayerController(Camera camera, btDiscreteDynamicsWorld dynamicsWorld, Adventurer player) {
        this(camera, dynamicsWorld, player, new Vector3(0, 10, 0));
    }

    public PlayerController(Camera camera, btDiscreteDynamicsWorld dynamicsWorld, Adventurer player, Vector3 spawnPoint) {
        this.camera = camera;
        this.dynamicsWorld = dynamicsWorld;
        this.player = player;

        playerShape = new btCapsuleShape(0.5f, 1f);
        Vector3 localInertia = new Vector3();
        playerShape.calculateLocalInertia(80f, localInertia);

        motionState = new btDefaultMotionState();
        motionState.setWorldTransform(new Matrix4().setTranslation(spawnPoint));

        btRigidBody.btRigidBodyConstructionInfo playerInfo = new btRigidBody.btRigidBodyConstructionInfo(80f, motionState, playerShape, localInertia);
        playerBody = new btRigidBody(playerInfo);
        playerInfo.dispose();   // ne sert qu'à la construction

        playerBody.setAngularFactor(0f);
        playerBody.setActivationState(Collision.DISABLE_DEACTIVATION);
        playerBody.setFriction(0.8f);
        playerBody.setRestitution(0f);

        dynamicsWorld.addRigidBody(playerBody, PLAYER_GROUP, (short) -1);
    }

    public btRigidBody getPlayerBody() {
        return playerBody;
    }

    public boolean keyDown(int keycode) {
        switch (keycode) {
            case Input.Keys.W:
            case Input.Keys.A:
            case Input.Keys.S:
            case Input.Keys.D:
            case Input.Keys.SPACE:
                keys.put(keycode, keycode);
                return true;
        }
        return false;
    }

    public boolean keyUp(int keycode) {
        keys.remove(keycode, 0);
        return true;
    }

    public void update() {
        handleRotation();
        handleMovement();
    }

    /** La marche est multipliée par {@code factor} (1 = normale ; la garde la ralentit). */
    public void setCombatSpeedFactor(float factor) {
        this.combatSpeedFactor = factor;
    }

    /**
     * Lance le bond d'une esquive, dans le sens des touches de marche tenues (aucune : en arrière) ;
     * il dure tant que {@link #setDashing} le dit.
     */
    public void startDash(float speed) {
        Vector3 forward = tmp.set(0, 0, -1).mul(rotation).nor();
        Vector3 side = tmp2.set(forward).crs(Vector3.Y).nor();
        dashDirection.set(0, 0, 0);
        if (keys.containsKey(Input.Keys.W)) dashDirection.add(forward);
        if (keys.containsKey(Input.Keys.S)) dashDirection.sub(forward);
        if (keys.containsKey(Input.Keys.A)) dashDirection.sub(side);
        if (keys.containsKey(Input.Keys.D)) dashDirection.add(side);
        dashDirection.y = 0;
        if (dashDirection.isZero()) {
            dashDirection.set(forward).scl(-1f);
        }
        dashDirection.nor();
        dashSpeed = speed;
        dashing = true;
    }

    public void setDashing(boolean dashing) {
        this.dashing = dashing;
    }

    private void handleRotation() {
        // la sensibilité du menu Options multiplie la vitesse de base
        float speed = rotationSpeed * Settings.current().mouseSensitivity;
        float deltaX = -Gdx.input.getDeltaX() * speed;
        float deltaY = -Gdx.input.getDeltaY() * speed;

        yaw += deltaX;
        pitch += deltaY;
        pitch = MathUtils.clamp(pitch, -89f, 89f);

        rotation.set(Vector3.Y, yaw);
    }

    private void handleMovement() {
        Vector3 forward = tmp.set(0, 0, -1).mul(rotation).nor();
        Vector3 side = tmp2.set(forward).crs(Vector3.Y).nor();

        moveDirection.set(0, 0, 0);
        if (keys.containsKey(Input.Keys.W)) moveDirection.add(forward);
        if (keys.containsKey(Input.Keys.S)) moveDirection.sub(forward);
        if (keys.containsKey(Input.Keys.A)) moveDirection.sub(side);
        if (keys.containsKey(Input.Keys.D)) moveDirection.add(side);

        if (keys.containsKey(Input.Keys.SPACE) && isOnGround()) {
            playerBody.applyCentralImpulse(tmp.set(0, jumpForce, 0));
        }

        moveDirection.y = 0;
        float vy = playerBody.getLinearVelocity().y;
        if (dashing) {
            playerBody.setLinearVelocity(tmp.set(dashDirection.x * dashSpeed, vy, dashDirection.z * dashSpeed));
            return;
        }
        if (!moveDirection.isZero()) {
            moveDirection.nor().scl(moveSpeed * combatSpeedFactor * (float) player.getSpeedFactor() * com.lucas.guild.game.DebugMode.getSpeedMultiplier());
            playerBody.setLinearVelocity(tmp.set(moveDirection.x, vy, moveDirection.z));
        } else {
            playerBody.setLinearVelocity(tmp.set(0, vy, 0));
        }
    }

    public void updateCamera() {
        Vector3 playerPosition = playerBody.getCenterOfMassPosition();
        eye.set(playerPosition).add(0, 0.8f, 0);

        camRotation.set(Vector3.Y, yaw);
        pitchRotation.set(Vector3.X, pitch);
        camRotation.mul(pitchRotation);

        camera.direction.set(0, 0, -1).mul(camRotation);
        camera.up.set(0, 1, 0).mul(camRotation);

        if (!ViewSettings.thirdPerson) {
            camera.position.set(eye);
            aimBonus = 0f;
        } else {
            // derrière le joueur, un peu sur l'épaule ; un mur entre les deux rapproche la caméra
            right.set(camera.direction).crs(camera.up).nor();
            desired.set(eye).mulAdd(camera.direction, -THIRD_PERSON_DISTANCE).mulAdd(right, SHOULDER_RIGHT).mulAdd(camera.up, SHOULDER_UP);
            float reached = sweepFraction(cameraProbe, eye, desired);
            if (reached < 1f) {
                desired.sub(eye).scl(reached).add(eye);
            }
            camera.position.set(desired);
            aimBonus = camera.position.dst(eye);
        }

        camera.update();
    }

    /** Vitesse de déplacement au sol, en m/s (anime la marche du personnage). */
    public float getHorizontalSpeed() {
        Vector3 v = playerBody.getLinearVelocity();
        return (float) Math.hypot(v.x, v.z);
    }

    /** Direction du regard, en degrés (0 = vers -Z). */
    public float getYaw() {
        return yaw;
    }

    /** Distance entre la caméra et les yeux du joueur : à ajouter à la portée de ce que la caméra vise. */
    public float getAimBonus() {
        return aimBonus;
    }
    
    public btCollisionObject getBodyInView(float maxDistance) {
        rayFrom.set(camera.position);
        rayTo.set(camera.direction).scl(maxDistance + aimBonus).add(rayFrom);

        rayCallback.setCollisionObject(null);
        rayCallback.setClosestHitFraction(1f);
        rayCallback.setCollisionFilterGroup((short)-1);
        rayCallback.setCollisionFilterMask(NOT_PLAYER);

        dynamicsWorld.rayTest(rayFrom, rayTo, rayCallback);

        if (rayCallback.hasHit()) {
            lastViewDistance = rayCallback.getClosestHitFraction() * (maxDistance + aimBonus);
            return rayCallback.getCollisionObject();
        }
        lastViewDistance = Float.MAX_VALUE;
        return null;
    }

    /** La distance (depuis la caméra) du corps trouvé par le dernier {@link #getBodyInView}, ou MAX_VALUE s'il n'y en avait pas. */
    public float getLastViewDistance() {
        return lastViewDistance;
    }

    /**
     * Fait glisser {@code probe} de {@code from} vers {@code to} dans le décor (le joueur est ignoré).
     *
     * @return la part du trajet parcourue avant le premier obstacle (1 = rien sur le chemin)
     */
    private float sweepFraction(btConvexShape probe, Vector3 from, Vector3 to) {
        sweep.setHitCollisionObject(null);
        sweep.setClosestHitFraction(1f);
        sweep.setCollisionFilterGroup((short) -1);
        sweep.setCollisionFilterMask(NOT_PLAYER);
        dynamicsWorld.convexSweepTest(probe, sweepFrom.setToTranslation(from), sweepTo.setToTranslation(to), sweep);
        return sweep.hasHit() ? Math.max(0f, sweep.getClosestHitFraction()) : 1f;
    }

    /**
     * Le joueur a-t-il les pieds sur quelque chose ? Une vitesse verticale nulle ne suffit pas : elle l'est aussi au
     * sommet d'un saut, et garder Espace enfoncé faisait alors resauter en l'air, sans fin.
     */
    private boolean isOnGround() {
        if (Math.abs(playerBody.getLinearVelocity().y) >= 0.1f) {
            return false;
        }
        Vector3 center = playerBody.getCenterOfMassPosition();
        tmp.set(center).sub(0, HALF_HEIGHT - GROUND_PROBE_RADIUS + GROUND_TOLERANCE, 0);
        return sweepFraction(groundProbe, center, tmp) < 1f;
    }

    @Override
    public void dispose() {
        dynamicsWorld.removeRigidBody(playerBody);
        playerBody.dispose();
        motionState.dispose();
        playerShape.dispose();
        cameraProbe.dispose();
        groundProbe.dispose();
        sweep.dispose();
    }
}
