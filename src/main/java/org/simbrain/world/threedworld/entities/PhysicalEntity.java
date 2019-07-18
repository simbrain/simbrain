package org.simbrain.world.threedworld.entities;

import com.jme3.bounding.BoundingVolume;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.util.CollisionShapeFactory;
import com.jme3.math.FastMath;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import org.simbrain.workspace.Consumable;
import org.simbrain.workspace.Producible;
import org.simbrain.world.threedworld.engine.ThreeDEngine;
import org.simbrain.world.threedworld.entities.EditorDialog.Editor;

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

    @Producible(defaultVisibility = false)
    public double getX() {
        return getPosition().x;
    }

    @Consumable(defaultVisibility = false)
    public void setX(double value) {
        Vector3f location = getPosition();
        location.x = (float) value;
        queuePosition(location);
    }

    @Producible(defaultVisibility = false)
    public double getY() {
        return getPosition().y;
    }

    @Consumable(defaultVisibility = false)
    public void setY(double value) {
        Vector3f location = getPosition();
        location.y = (float) value;
        queuePosition(location);
    }

    @Producible(defaultVisibility = false)
    public double getZ() {
        return getPosition().z;
    }

    @Consumable(defaultVisibility = false)
    public void setZ(double value) {
        Vector3f location = getPosition();
        location.z = (float) value;
        queuePosition(location);
    }

    @Producible(description = "getPosition")
    public double[] getPositionComponents() {
        Vector3f position = getPosition();
        return new double[]{position.x, position.y, position.z};
    }

    @Consumable(description = "setPosition", defaultVisibility = false)
    public void setPositionComponents(double[] value) {
        Vector3f position = new Vector3f((float) value[0], (float) value[1], (float) value[2]);
        queuePosition(position);
    }

    @Override
    public void move(Vector3f offset) {
        node.move(offset);
    }

    @Consumable()
    public void move(double[] offset) {
        engine.enqueue(() -> {
            move(new Vector3f((float) offset[0], (float) offset[1], (float) offset[2]));
        });
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

    @Producible(defaultVisibility = false)
    public double getYaw() {
        return FastMath.RAD_TO_DEG * getRotationAngles()[1];
    }

    @Consumable(defaultVisibility = false)
    public void setYaw(double value) {
        double[] rotation = getRotationAngles();
        rotation[1] = FastMath.DEG_TO_RAD * (float) value;
        setRotationAngles(rotation);
    }

    @Producible(defaultVisibility = false)
    public double getPitch() {
        return FastMath.RAD_TO_DEG * getRotationAngles()[0];
    }

    @Consumable(defaultVisibility = false)
    public void setPitch(double value) {
        double[] rotation = getRotationAngles();
        rotation[0] = FastMath.DEG_TO_RAD * (float) value;
        setRotationAngles(rotation);
    }

    @Producible(defaultVisibility = false)
    public double getRoll() {
        return FastMath.RAD_TO_DEG * getRotationAngles()[2];
    }

    @Consumable(defaultVisibility = false)
    public void setRoll(double value) {
        double[] rotation = getRotationAngles();
        rotation[2] = FastMath.DEG_TO_RAD * (float) value;
        setRotationAngles(rotation);
    }

    @Producible(description = "getRotation")
    private double[] getRotationAngles() {
        float[] angles = getRotation().toAngles(null);
        return new double[]{angles[0], angles[1], angles[2]};
    }

    @Consumable(description = "setRotation", defaultVisibility = false)
    private void setRotationAngles(double[] values) {
        Quaternion rotation = new Quaternion();
        float[] angles = new float[]{(float) values[0], (float) values[1], (float) values[2]};
        rotation.fromAngles(angles);
        queueRotation(rotation);
    }

    @Override
    public void rotate(Quaternion rotation) {
        node.rotate(rotation);
    }

    @Consumable()
    public void rotate(double[] values) {
        engine.enqueue(() -> {
            Quaternion rotation = new Quaternion();
            float[] angles = new float[]{(float) values[0], (float) values[1], (float) values[2]};
            rotation.fromAngles(angles);
            rotate(rotation);
        });
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

    @Override
    public String getId() {
        return getName();
    }
}
