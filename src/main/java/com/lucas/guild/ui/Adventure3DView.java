package com.lucas.guild.ui;

import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SubScene;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.SceneAntialiasing;
import javafx.scene.transform.Rotate;

public class Adventure3DView extends StackPane {
    private final SubScene subScene;
    private final PerspectiveCamera camera;
    private final Group worldRoot;
    
    private final double SPEED = 10.0;
    private final double ROT_SPEED = 5.0;

    public Adventure3DView(double width, double height) {
        worldRoot = new Group();
        
        // Sol (Ground)
        Box ground = new Box(2000, 10, 2000);
        ground.setMaterial(new PhongMaterial(Color.FORESTGREEN));
        ground.setTranslateY(100); 
        worldRoot.getChildren().add(ground);
        
        // Obstacles
        for (int i = 0; i < 20; i++) {
            Box box = new Box(50, 100 + Math.random() * 100, 50);
            box.setMaterial(new PhongMaterial(Color.BROWN));
            box.setTranslateX((Math.random() - 0.5) * 1500);
            box.setTranslateZ((Math.random() - 0.5) * 1500);
            box.setTranslateY(50);
            worldRoot.getChildren().add(box);
        }
        
        // Repère central
        Box centerMark = new Box(20, 200, 20);
        centerMark.setMaterial(new PhongMaterial(Color.RED));
        centerMark.setTranslateX(0);
        centerMark.setTranslateZ(200);
        centerMark.setTranslateY(0);
        worldRoot.getChildren().add(centerMark);

        // Caméra
        camera = new PerspectiveCamera(true);
        camera.setNearClip(0.1);
        camera.setFarClip(5000.0);
        camera.setTranslateZ(-500);
        camera.setTranslateY(-50);
        camera.setRotationAxis(Rotate.Y_AXIS);
        
        subScene = new SubScene(worldRoot, width, height, true, SceneAntialiasing.BALANCED);
        subScene.setCamera(camera);
        
        // Redimensionnement
        subScene.widthProperty().bind(widthProperty());
        subScene.heightProperty().bind(heightProperty());
        
        // HUD (Overlay)
        Label controlsLabel = new Label("Contrôles:\nZ/S : Avancer/Reculer\nQ/D : Pas latéraux\nFlèches G/D : Tourner\nEspace/Shift : Monter/Descendre");
        controlsLabel.setStyle("-fx-text-fill: white; -fx-background-color: rgba(0,0,0,0.5); -fx-padding: 10;");
        controlsLabel.setTranslateX(10);
        controlsLabel.setTranslateY(10);
        // Alignement en haut à gauche via StackPane par défaut ou ajustement manuel si nécessaire
        // Ici on ajoute simplement au StackPane, le Label sera par dessus la SubScene
        
        getChildren().addAll(subScene, controlsLabel);
        controlsLabel.setMouseTransparent(true); // Clics traversent le label
        
        // Gestion des événements
        setFocusTraversable(true);
        setOnKeyPressed(this::handleKeyPress);
        setOnMouseClicked(e -> requestFocus());
    }
    
    private void handleKeyPress(KeyEvent event) {
        switch (event.getCode()) {
            case Z: // Avancer
                moveCamera(SPEED);
                break;
            case S: // Reculer
                moveCamera(-SPEED);
                break;
            case Q: // Strafe Gauche
                strafeCamera(-SPEED);
                break;
            case D: // Strafe Droite
                strafeCamera(SPEED);
                break;
            case LEFT: // Tourner Gauche
                camera.setRotate(camera.getRotate() - ROT_SPEED);
                break;
            case RIGHT: // Tourner Droite
                camera.setRotate(camera.getRotate() + ROT_SPEED);
                break;
            case SPACE: // Monter
                camera.setTranslateY(camera.getTranslateY() - SPEED);
                break;
            case SHIFT: // Descendre
                camera.setTranslateY(camera.getTranslateY() + SPEED);
                break;
        }
    }
    
    private void moveCamera(double distance) {
        double rad = Math.toRadians(camera.getRotate());
        double z = distance * Math.cos(rad);
        double x = distance * Math.sin(rad);
        camera.setTranslateZ(camera.getTranslateZ() + z);
        camera.setTranslateX(camera.getTranslateX() + x);
    }

    private void strafeCamera(double distance) {
        double rad = Math.toRadians(camera.getRotate());
        double z = distance * Math.cos(rad + Math.PI/2);
        double x = distance * Math.sin(rad + Math.PI/2);
        camera.setTranslateZ(camera.getTranslateZ() + z);
        camera.setTranslateX(camera.getTranslateX() + x);
    }
    
    public void requestFocusForGame() {
        requestFocus();
    }
}
