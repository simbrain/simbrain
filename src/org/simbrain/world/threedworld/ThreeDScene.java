package org.simbrain.world.threedworld;

import org.simbrain.world.threedworld.engine.ThreeDEngine;

import com.jme3.light.Light;
import com.jme3.scene.Spatial;

/**
 * ThreeDScene encapsulate scene loading and unloading behavior from the ThreeDWorld class.
 */
public class ThreeDScene {
    private String name;
    private transient Spatial node;

    /**
     * Construct a new ThreeDScene with the default name.
     */
    public ThreeDScene() {
        this("Scenes/GrassyPlain.j3o");
    }

    /**
     * Construct a new ThreeDScene from the given name.
     * @param name The name of the scene to load.
     */
    public ThreeDScene(String name) {
        this.name = name;
    }

    /**
     * Load the scene specified by name, unloading the current scene if necessary.
     * @param engine The engine to use to load the scene.
     */
    public void load(ThreeDEngine engine) {
        if (node != null) {
            unload(engine);
        }
        node = engine.getAssetManager().loadModel(name);
        engine.getRootNode().attachChild(node);
        engine.getPhysicsSpace().addAll(node);
        // HACK: lights must be attached to the root in order to light entities
        for (Light light : node.getLocalLightList()) {
            node.removeLight(light);
            engine.getRootNode().addLight(light);
        }
    }

    /**
     * Unload the current scene, detaching all geometry and physical bodies from the engine.
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
}
