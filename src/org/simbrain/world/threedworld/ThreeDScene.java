package org.simbrain.world.threedworld;

import com.jme3.asset.AssetManager;
import com.jme3.light.Light;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;
import org.simbrain.world.threedworld.engine.ThreeDEngine;

/**
 * ThreeDScene encapsulate scene loading and unloading behavior from the ThreeDWorld class.
 */
public class ThreeDScene {
    private String name;
    private transient Node node;

    /**
     * Construct a new ThreeDScene with the default name.
     */
    public ThreeDScene() {
        this("Scenes/GrassyPlain.j3o");
    }

    /**
     * Construct a new ThreeDScene from the given name.
     *
     * @param name The name of the scene to load.
     */
    public ThreeDScene(String name) {
        this.name = name;
    }

    /**
     * Load the scene specified by name, unloading the current scene if necessary.
     *
     * @param engine The engine to use to load the scene.
     */
    public void load(ThreeDEngine engine) {
        node = new Node("test");
        engine.getRootNode().attachChild(node);
        AssetManager assetManager = engine.getAssetManager();

        /** create a blue box at coordinates (1,-1,1) */
        Box box1 = new Box(1,1,1);
        Geometry blue = new Geometry("Box", box1);
        blue.setLocalTranslation(new Vector3f(1,-1,1));
        Material mat1 = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");
        mat1.setColor("Color", ColorRGBA.Blue);
        blue.setMaterial(mat1);

        /** create a red box straight above the blue one at (1,3,1) */
        Box box2 = new Box(1,1,1);
        Geometry red = new Geometry("Box", box2);
        red.setLocalTranslation(new Vector3f(1,3,1));
        Material mat2 = new Material(assetManager,
                "Common/MatDefs/Misc/Unshaded.j3md");
        mat2.setColor("Color", ColorRGBA.Red);
        red.setMaterial(mat2);

        /** Create a pivot node at (0,0,0) and attach it to the root node */
        Node pivot = new Node("pivot");
        node.attachChild(pivot); // put this node in the scene

        /** Attach the two boxes to the *pivot* node. (And transitively to the root node.) */
        pivot.attachChild(blue);
        pivot.attachChild(red);
        /** Rotate the pivot node: Note that both boxes have rotated! */
        pivot.rotate(.4f,.4f,0f);
//        if (node != null) {
//            unload(engine);
//        }
//        Spatial model = engine.getAssetManager().loadModel(name);
//        if (model instanceof Node) {
//            node = (Node) model;
//        } else {
//            node = new Node("scene");
//            node.attachChild(model);
//        }
//        engine.getRootNode().attachChild(node);
//        engine.getPhysicsSpace().addAll(node);
//        // HACK: lights must be attached to the root in order to light entities
//        for (Light light : node.getLocalLightList()) {
//            node.removeLight(light);
//            engine.getRootNode().addLight(light);
//        }
    }

    /**
     * Unload the current scene, detaching all geometry and physical bodies from the engine.
     *
     * @param engine The engine to unload the scene from.
     */
    public void unload(ThreeDEngine engine) {
        engine.getRootNode().detachChild(node);
        engine.getPhysicsSpace().removeAll(node);
        // HACK: lights must be detached from the root
        for (Light light : engine.getRootNode().getWorldLightList()) {
            engine.getRootNode().removeLight(light);
        }
        node = null;
    }

    /**
     * @return The name of the scene to load.
     */
    public String getName() {
        return name;
    }

    /**
     * @param value The name of a new scene to load.
     */
    public void setName(String value) {
        name = value;
    }

    /**
     * Return the node which serves as the root for the loaded scene.
     */
    public Node getNode() {
        return node;
    }
}
