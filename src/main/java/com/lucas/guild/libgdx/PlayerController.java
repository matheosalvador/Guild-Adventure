package com.lucas.guild.libgdx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.physics.bullet.collision.btCapsuleShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionObject;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.collision.Collision;
import com.badlogic.gdx.physics.bullet.dynamics.btDiscreteDynamicsWorld;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.linearmath.btDefaultMotionState;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.IntIntMap;

public class PlayerController extends InputAdapter implements Disposable {

    private final Camera camera;
    private final btRigidBody playerBody;
    private final btDefaultMotionState motionState;

    private final IntIntMap keys = new IntIntMap();
    private final float moveSpeed = 8f;
    private final float jumpForce = 10f;
    private final float rotationSpeed = 0.2f;
    private final float cameraSmoothing = 15.0f;

    private final Vector3 moveDirection = new Vector3();
    private final Vector3 tmp = new Vector3();
    private final Matrix4 playerTransform = new Matrix4();
    private final Quaternion rotation = new Quaternion();
    
    private float yaw = 0f;   // Horizontal rotation
    private float pitch = 0f; // Vertical rotation

    public PlayerController(Camera camera, btDiscreteDynamicsWorld dynamicsWorld) {
        this.camera = camera;

        btCollisionShape playerShape = new btCapsuleShape(0.5f, 1f);
        Vector3 localInertia = new Vector3();
        playerShape.calculateLocalInertia(80f, localInertia);

        motionState = new btDefaultMotionState();
        motionState.setWorldTransform(new Matrix4().setTranslation(0, 10, 0));

        btRigidBody.btRigidBodyConstructionInfo playerInfo = new btRigidBody.btRigidBodyConstructionInfo(80f, motionState, playerShape, localInertia);
        playerBody = new btRigidBody(playerInfo);
        playerBody.setCollisionFlags(playerBody.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_CHARACTER_OBJECT);
        playerBody.setAngularFactor(0f);
        playerBody.setActivationState(Collision.DISABLE_DEACTIVATION);
        playerBody.setFriction(0.8f);
        playerBody.setRestitution(0f); // Prevent bouncing

        dynamicsWorld.addRigidBody(playerBody);
    }

    @Override
    public boolean keyDown(int keycode) {
        keys.put(keycode, keycode);
        return true;
    }

    @Override
    public boolean keyUp(int keycode) {
        keys.remove(keycode, 0);
        return true;
    }

    public void update(float deltaTime) {
        handleRotation(deltaTime);
        handleMovement(deltaTime);
        updateCamera(deltaTime);
    }

    private void handleRotation(float deltaTime) {
        float deltaX = -Gdx.input.getDeltaX() * rotationSpeed;
        float deltaY = -Gdx.input.getDeltaY() * rotationSpeed;

        yaw += deltaX;
        pitch += deltaY;
        pitch = MathUtils.clamp(pitch, -89f, 89f);

        // Update the physics body's rotation based on yaw
        rotation.set(Vector3.Y, yaw);
        playerBody.getMotionState().getWorldTransform(playerTransform);
        playerTransform.set(playerBody.getCenterOfMassPosition(), rotation);
        playerBody.setWorldTransform(playerTransform);
    }

    private void handleMovement(float deltaTime) {
        // Create a forward vector based on the yaw
        Vector3 forward = tmp.set(0, 0, -1).mul(rotation).nor();
        // Create a side vector
        Vector3 side = new Vector3(forward).crs(Vector3.Y).nor();

        moveDirection.set(0, 0, 0);
        if (keys.containsKey(Input.Keys.W)) moveDirection.add(forward);
        if (keys.containsKey(Input.Keys.S)) moveDirection.sub(forward);
        if (keys.containsKey(Input.Keys.A)) moveDirection.sub(side);
        if (keys.containsKey(Input.Keys.D)) moveDirection.add(side);

        // Jump
        if (keys.containsKey(Input.Keys.SPACE) && isOnGround()) {
            playerBody.applyCentralImpulse(new Vector3(0, jumpForce, 0));
        }

        // Apply velocity
        moveDirection.y = 0;
        if (!moveDirection.isZero()) {
            moveDirection.nor().scl(moveSpeed);
            float vy = playerBody.getLinearVelocity().y;
            playerBody.setLinearVelocity(tmp.set(moveDirection.x, vy, moveDirection.z));
        } else {
            float vy = playerBody.getLinearVelocity().y;
            playerBody.setLinearVelocity(new Vector3(0, vy, 0));
        }
    }

    private void updateCamera(float deltaTime) {
        // Get player position
        Vector3 playerPosition = playerBody.getCenterOfMassPosition();
        
        // Smoothly move camera to player's head
        Vector3 targetPosition = tmp.set(playerPosition).add(0, 0.8f, 0);
        camera.position.lerp(targetPosition, deltaTime * cameraSmoothing);
        
        // Point the camera in the direction of our yaw and pitch
        Quaternion camRotation = new Quaternion().set(Vector3.Y, yaw);
        camRotation.mul(new Quaternion(Vector3.X, pitch));
        
        camera.direction.set(0, 0, -1).mul(camRotation);
        camera.up.set(0, 1, 0).mul(camRotation);
        
        camera.update();
    }

    public void render(ModelBatch modelBatch, Environment environment) {
        // We don't render the player model in first-person view
    }

    private boolean isOnGround() {
        return Math.abs(playerBody.getLinearVelocity().y) < 0.01f;
    }

    @Override
    public void dispose() {
        motionState.dispose();
    }
}
