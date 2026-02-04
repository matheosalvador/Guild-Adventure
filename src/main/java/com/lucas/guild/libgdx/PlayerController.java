package com.lucas.guild.libgdx;
// tout les imports biblio etc
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
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
import com.lucas.guild.model.Adventurer;
import com.lucas.guild.model.Item;

public class PlayerController extends InputAdapter implements Disposable {

    private final Camera camera;
    private final btRigidBody playerBody;
    private final btDefaultMotionState motionState;
    private final btDiscreteDynamicsWorld dynamicsWorld;
    private final Adventurer player;

    private final IntIntMap keys = new IntIntMap();
    private final float moveSpeed = 8f;
    private final float jumpForce = 10f;
    private final float rotationSpeed = 0.8f;

    private final Vector3 moveDirection = new Vector3();
    private final Vector3 tmp = new Vector3();
    private final Vector3 tmp2 = new Vector3();
    private final Quaternion rotation = new Quaternion();
    private final Quaternion camRotation = new Quaternion();
    private final Quaternion pitchRotation = new Quaternion();

    private float yaw = 0f;
    private float pitch = 0f;
    
    private final static Vector3 rayFrom = new Vector3();
    private final static Vector3 rayTo = new Vector3();
    private final static ClosestRayResultCallback rayCallback = new ClosestRayResultCallback(Vector3.Zero, Vector3.Z);

    public PlayerController(Camera camera, btDiscreteDynamicsWorld dynamicsWorld, Adventurer player) {
        this.camera = camera;
        this.dynamicsWorld = dynamicsWorld;
        this.player = player;

        btCollisionShape playerShape = new btCapsuleShape(0.5f, 1f);
        Vector3 localInertia = new Vector3();
        playerShape.calculateLocalInertia(80f, localInertia);

        motionState = new btDefaultMotionState();
        motionState.setWorldTransform(new Matrix4().setTranslation(0, 10, 0));

        btRigidBody.btRigidBodyConstructionInfo playerInfo = new btRigidBody.btRigidBodyConstructionInfo(80f, motionState, playerShape, localInertia);
        playerBody = new btRigidBody(playerInfo);
        
        playerBody.setAngularFactor(0f);
        playerBody.setActivationState(Collision.DISABLE_DEACTIVATION);
        playerBody.setFriction(0.8f);
        playerBody.setRestitution(0f);

        dynamicsWorld.addRigidBody(playerBody);
    }

    @Override
    public boolean keyDown(int keycode) {
        keys.put(keycode, keycode);
        if (keycode == Input.Keys.E) {
            interact();
        }
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        keys.remove(keycode, 0);
        return true;
    }

    public void update() {
        handleRotation();
        handleMovement();
    }

    private void handleRotation() {
        float deltaX = -Gdx.input.getDeltaX() * rotationSpeed;
        float deltaY = -Gdx.input.getDeltaY() * rotationSpeed;

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
        if (!moveDirection.isZero()) {
            moveDirection.nor().scl(moveSpeed);
            playerBody.setLinearVelocity(tmp.set(moveDirection.x, vy, moveDirection.z));
        } else {
            playerBody.setLinearVelocity(tmp.set(0, vy, 0));
        }
    }

    public void updateCamera() {
        Vector3 playerPosition = playerBody.getCenterOfMassPosition();
        camera.position.set(playerPosition).add(0, 0.8f, 0);
        
        camRotation.set(Vector3.Y, yaw);
        pitchRotation.set(Vector3.X, pitch);
        camRotation.mul(pitchRotation);
        
        camera.direction.set(0, 0, -1).mul(camRotation);
        camera.up.set(0, 1, 0).mul(camRotation);
        
        camera.update();
    }
    
    private void interact() {
        btCollisionObject body = getBodyInView(3f);
        if (body != null && body.userData instanceof GameObject) {
            GameObject object = (GameObject) body.userData;
            if (object.isActive) {
                if (object.item != null) {
                    player.findItem(object.item);
                    object.isActive = false;
                } else {
                    if (body instanceof btRigidBody) {
                        ((btRigidBody) body).activate();
                        ((btRigidBody) body).applyCentralImpulse(tmp.set(camera.direction).scl(10f));
                    }
                }
            }
        }
    }
    
    public btCollisionObject getBodyInView(float maxDistance) {
        rayFrom.set(camera.position);
        rayTo.set(camera.direction).scl(maxDistance).add(rayFrom);

        rayCallback.setCollisionObject(null);
        rayCallback.setClosestHitFraction(1f);
        rayCallback.setCollisionFilterGroup((short)-1);
        rayCallback.setCollisionFilterMask((short)-1);

        dynamicsWorld.rayTest(rayFrom, rayTo, rayCallback);

        if (rayCallback.hasHit()) {
            return rayCallback.getCollisionObject();
        }
        return null;
    }

    private boolean isOnGround() {
        return Math.abs(playerBody.getLinearVelocity().y) < 0.1f;
    }

    @Override
    public void dispose() {
        dynamicsWorld.removeRigidBody(playerBody);
        playerBody.dispose();
        motionState.dispose();
    }
}
