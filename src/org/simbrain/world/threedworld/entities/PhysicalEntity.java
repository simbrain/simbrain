package org.simbrain.world.threedworld.entities;

import org.simbrain.world.threedworld.ThreeDWorldComponent;
import org.simbrain.world.threedworld.engine.ThreeDEngine;
import org.simbrain.world.threedworld.entities.EditorDialog.Editor;

import com.jme3.bounding.BoundingVolume;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;

public class PhysicalEntity implements Entity {
    private ThreeDEngine engine;
    private Node node;

    protected PhysicalEntity(ThreeDEngine engine, Node node) {
        this.engine = engine;
        setNode(node);
    }

    public ThreeDEngine getEngine() {
        return engine;
    }

    protected void setEngine(ThreeDEngine value) {
        engine = value;
    }

    @Override
    public String getName() {
        return node.getName();
    }

    @Override
    public void setName(String value) {
        node.setName(value);
    }

    @Override
    public Node getNode() {
        return node;
    }

    protected void setNode(Node value) {
        if (node != null) {
            engine.getRootNode().detachChild(node);
            engine.getPhysicsSpace().remove(getBody());
        }
        node = value;
        engine.getRootNode().attachChild(node);
        if (getBody() == null) {
            CollisionShape shape = CollisionShapeFactory.createDynamicMeshShape(node);
            RigidBodyControl body = new RigidBodyControl(shape, 1);
            body.setCollisionGroup(PhysicsCollisionObject.COLLISION_GROUP_02);
            body.setCollideWithGroups(PhysicsCollisionObject.COLLISION_GROUP_01 + PhysicsCollisionObject.COLLISION_GROUP_02);
            node.addControl(body);
        }
        engine.getPhysicsSpace().add(getBody());
    }

    public RigidBodyControl getBody() {
        return getNode().getControl(RigidBodyControl.class);
    }

    public void setBody(RigidBodyControl body) {
        if (getBody() != null) {
            getEngine().getPhysicsSpace().remove(getBody());
            getNode().removeControl(RigidBodyControl.class);
        }
        getEngine().getPhysicsSpace().add(body);
        getNode().addControl(body);
    }

    public boolean isKinematic() {
        return getBody().isKinematic();
    }

    public void setKinematic(boolean value) {
        RigidBodyControl body = getBody();
        if (value && !body.isKinematic()) {
            Vector3f position = body.getPhysicsLocation();
            Quaternion rotation = body.getPhysicsRotation();
            body.setKinematic(true);
            setPosition(position);
            setRotation(rotation);
        } else if (!value && getBody().isKinematic()) {
            body.setKinematic(false);
        }
    }

    @Override
    public Vector3f getPosition() {
        return node.getLocalTranslation();
    }

    @Override
    public void setPosition(Vector3f value) {
        if (getBody().isKinematic())
            getNode().setLocalTranslation(value);
        else {
            getBody().setPhysicsLocation(value);
            getBody().activate();
        }
        update(0);
    }

    @Override
    public void queuePosition(Vector3f value) {
        engine.enqueue(() -> {
            setPosition(value);
        });
    }

    @Override
    public void move(Vector3f offset) {
        node.move(offset);
    }

    @Override
    public Quaternion getRotation() {
        return node.getLocalRotation();
    }

    @Override
    public void setRotation(Quaternion value) {
        if (getBody().isKinematic()) {
            getNode().setLocalRotation(value);
        } else {
            getBody().setPhysicsRotation(value);
            getBody().activate();
        }
        update(0);
    }

    @Override
    public void queueRotation(Quaternion value) {
        engine.enqueue(() -> {
            setRotation(value);
        });
    }

    @Override
    public void rotate(Quaternion rotation) {
        node.rotate(rotation);
    }

    @Override
    public BoundingVolume getBounds() {
        return node.getWorldBound();
    }

    @Override
    public void update(float t) {
    }

    @Override
    public void delete() {
        engine.getRootNode().detachChild(node);
        engine.getPhysicsSpace().remove(getBody());
    }

    @Override
    public Editor getEditor() {
        return new EntityEditor(this);
    }
}
