package org.simbrain.world.threedworld;

import com.jme3.light.AmbientLight;
import com.jme3.light.Light;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Quad;
import com.jme3.util.SkyFactory;
import org.simbrain.world.threedworld.engine.ThreeDEngine;

/**
 * ThreeDScene encapsulate scene loading and unloading behavior from the ThreeDWorld class.
 */
public class ThreeDScene {

    /**
     * Name of the scene to be loaded
     */
    private String sceneName;

    /**
     * Node for the scene
     */
    private transient Node sceneNode;

    /**
     * Construct a new ThreeDScene with the default name.
     */
    public ThreeDScene() {
        this("Models/Grass.j3o");
    }

    /**
     * Construct a new ThreeDScene from the given name.
     *
     * @param sceneName The name of the scene to load.
     */
    public ThreeDScene(String sceneName) {
        this.sceneName = sceneName;
    }

    /**
     * Load the scene specified by name, unloading the current scene if necessary.
     *
     * @param engine The engine to use to load the scene.
     */
    public void load(ThreeDEngine engine) {

        if (sceneNode != null) {
            unload(engine);
        }

        // TODO: These are fixes just to get something, but they are not really integrated
        // into the framework Tim set up

        // Add a sky
        engine.getRootNode().attachChild(SkyFactory.createSky(
                engine. getAssetManager(), "Materials/BrightSky.dds",
                SkyFactory.EnvMapType.CubeMap));

        // Trying to add a floor. No idea how to orient it or make it solid.
        Geometry floor = new Geometry("OurMesh", new Quad(1000f,1000f));
        Material greenMat = new Material(engine.getAssetManager(), "Common/MatDefs/Misc/Unshaded.j3md");
        greenMat.setColor("Color", ColorRGBA.Green);
        floor.setMaterial(greenMat);
        floor.setLocalTranslation(10,-10,10);
        Quaternion rotate = new Quaternion();
        rotate.fromAngleAxis(FastMath.PI, new Vector3f(0,1,0));
        floor.setLocalRotation(rotate);
        engine.getRootNode().attachChild(floor);


        Spatial model = engine.getAssetManager().loadModel(sceneName);
        if (model instanceof Node) {
            sceneNode = (Node) model;
        } else {
            sceneNode = new Node("scene");
            sceneNode.attachChild(model);
        }
        engine.getRootNode().attachChild(sceneNode);
        engine.getPhysicsSpace().addAll(sceneNode);

        AmbientLight ambientLight = new AmbientLight();
        ambientLight.setColor(ColorRGBA.White);
        engine.getRootNode().addLight(ambientLight);


        //
        //
        //node = new Node("test");
        //engine.getRootNode().attachChild(node);
        //AssetManager assetManager = engine.getAssetManager();
        //
        ///** create a blue box at coordinates (1,-1,1) */
        //Box box1 = new Box(1,1,1);
        //Geometry blue = new Geometry("Box", box1);
        //blue.setLocalTranslation(new Vector3f(1,-1,1));
        //Material mat1 = new Material(assetManager,
        //        "Common/MatDefs/Misc/Unshaded.j3md");
        //mat1.setColor("Color", ColorRGBA.Blue);
        //blue.setMaterial(mat1);
        //
        ///** create a red box straight above the blue one at (1,3,1) */
        //Box box2 = new Box(1,1,1);
        //Geometry red = new Geometry("Box", box2);
        //red.setLocalTranslation(new Vector3f(1,3,1));
        //Material mat2 = new Material(assetManager,
        //        "Common/MatDefs/Misc/Unshaded.j3md");
        //mat2.setColor("Color", ColorRGBA.Red);
        //red.setMaterial(mat2);
        //
        ///** Create a pivot node at (0,0,0) and attach it to the root node */
        //Node pivot = new Node("pivot");
        //node.attachChild(pivot); // put this node in the scene
        //
        ///** Attach the two boxes to the *pivot* node. (And transitively to the root node.) */
        //pivot.attachChild(blue);
        //pivot.attachChild(red);
        //
        ///** Rotate the pivot node: Note that both boxes have rotated! */
        //pivot.rotate(.4f,.4f,0f);
        //if (node != null) {
        //    unload(engine);
        //}
        //Spatial model = engine.getAssetManager().loadModel(name);
        //engine.getRootNode().attachChild(model);
        //if (model instanceof Node) {
        //    node = (Node) model;
        //} else {
        //    node = new Node("scene");
        //    node.attachChild(model);
        //}
        //engine.getRootNode().attachChild(node);
        //engine.getPhysicsSpace().addAll(node);

        //// HACK: lights must be attached to the root in order to light entities
        //for (Light light : node.getLocalLightList()) {
        //    node.removeLight(light);
        //    engine.getRootNode().addLight(light);
        //}

    }

    /**
     * Unload the current scene, detaching all geometry and physical bodies from the engine.
     *
     * @param engine The engine to unload the scene from.
     */
    public void unload(ThreeDEngine engine) {
        engine.getRootNode().detachChild(sceneNode);
        engine.getPhysicsSpace().removeAll(sceneNode);
        // HACK: lights must be detached from the root
        for (Light light : engine.getRootNode().getWorldLightList()) {
            engine.getRootNode().removeLight(light);
        }
        sceneNode = null;
    }

    /**
     * @return The name of the scene to load.
     */
    public String getSceneName() {
        return sceneName;
    }

    /**
     * @param value The name of a new scene to load.
     */
    public void setSceneName(String value) {
        sceneName = value;
    }

    /**
     * Return the node which serves as the root for the loaded scene.
     */
    public Node getSceneNode() {
        return sceneNode;
    }
}
