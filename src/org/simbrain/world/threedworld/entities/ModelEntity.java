package org.simbrain.world.threedworld.entities;

import java.util.Collection;
import java.util.Collections;

import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;
import org.simbrain.world.threedworld.engine.ThreeDEngine;
import org.simbrain.world.threedworld.entities.EditorDialog.Editor;

import com.jme3.animation.AnimChannel;
import com.jme3.animation.AnimControl;
import com.jme3.bounding.BoundingBox;
import com.jme3.scene.Node;

public class ModelEntity extends PhysicalEntity {

    private static Node loadModel(ThreeDEngine engine, String fileName) {
        Node rootNode = (Node) engine.getAssetManager().loadModel(fileName);
        Node modelNode = (Node) rootNode.getChild("ModelNode");
        modelNode.setModelBound(new BoundingBox());
        modelNode.updateModelBound();
        rootNode.detachChild(modelNode);
        return modelNode;
    }

    public static ModelEntity load(ThreeDEngine engine, String name, String fileName) {
        Node node = loadModel(engine, fileName);
        node.setName(name);
        return new ModelEntity(engine, node, fileName);
    }

    private String fileName;

    private ModelEntity(ThreeDEngine engine, Node node, String fileName) {
        super(engine, node);
        this.fileName = fileName;
    }

    /**
     * @return Return the name of the model file.
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Load a new model for this entity.
     * @param fileName The filename of the model to load.
     */
    public void reload(String fileName) {
        Node node = loadModel(getEngine(), fileName);
        node.setName(getName());
        setNode(node);
        this.fileName = fileName;
    }

    /**
     * Load a new model for this entity on the next engine update.
     */
    public void queueReload(String fileName) {
        getEngine().enqueue(() -> {
            reload(fileName);
        });
    }

    public AnimControl getAnimator() {
        return getNode().getControl(AnimControl.class);
    }

    public Collection<String> getAnimations() {
        if (getAnimator() != null) {
            return getAnimator().getAnimationNames();
        } else {
            return Collections.emptyList();
        }
    }

    public boolean hasAnimation(String name) {
        return getAnimations().contains(name);
    }

    @Producible(idMethod="getName")
    public String getAnimation() {
        AnimControl animator = getAnimator();
        if (animator == null || animator.getNumChannels() == 0) {
            return "";
        } else {
            AnimChannel animation = animator.getChannel(0);
            return animation.getAnimationName();
        }
    }

    @Consumable(idMethod="getName")
    public void setAnimation(String name) {
        setAnimation(name, 1);
    }

    public void setAnimation(String name, float speed) {
        if (hasAnimation(name)) {
            AnimChannel animation;
            if (getAnimator().getNumChannels() == 0) {
                animation = getAnimator().createChannel();
            } else {
                animation = getAnimator().getChannel(0);
            }
            if (animation.getAnimationName() != name) {
                animation.setAnim(name);
            }
            if (animation.getSpeed() != speed) {
                animation.setSpeed(speed);
            }
        }
    }

    @Override
    public Editor getEditor() {
        return new ModelEditor(this);
    }
}
