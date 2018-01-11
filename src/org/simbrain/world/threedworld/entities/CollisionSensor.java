package org.simbrain.world.threedworld.entities;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.simbrain.workspace.Producible;

import com.jme3.bullet.collision.PhysicsCollisionGroupListener;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.control.RigidBodyControl;

public class CollisionSensor implements Sensor, PhysicsCollisionGroupListener {
    private class CollisionSensorEditor extends SensorEditor {
        private JTextField collisionNameField = new JTextField();

        {
            collisionNameField.setColumns(25);
        }

        private CollisionSensorEditor() {
            super(agent, CollisionSensor.this);
        }

        @Override
        public JComponent layoutFields() {
            JComponent sensorComponent = super.layoutFields();

            getPanel().add(new JLabel("Collision Name"));
            getPanel().add(collisionNameField, "wrap");

            return sensorComponent;
        }

        @Override
        public void readValues() {
            collisionNameField.setText(collisionName);
        }

        @Override
        public void writeValues() { }

        @Override
        public void close() { }
    }

    private Agent agent;
    private PhysicsCollisionObject other;
    private double colliding, nextColliding;
    private String collisionName;

    public CollisionSensor(Agent agent) {
        this.agent = agent;
        agent.addSensor(this);
        agent.getEngine().getPhysicsSpace().addCollisionGroupListener(this, PhysicsCollisionObject.COLLISION_GROUP_02);
    }

    @Override
    public Agent getAgent() {
        return agent;
    }

    @Producible
    public double getColliding() {
        return colliding;
    }

    @Producible
    public String getCollisionName() {
        return collisionName;
    }

    @Override
    public boolean collide(PhysicsCollisionObject objectA, PhysicsCollisionObject objectB) {
        RigidBodyControl thisCollisionObject = agent.getNode().getControl(RigidBodyControl.class);
        if (objectA.equals(thisCollisionObject)) {
            other = objectB;
            nextColliding = objectA.getCollisionGroup();
        } else if (objectB.equals(thisCollisionObject)) {
            other = objectA;
            nextColliding = objectB.getCollisionGroup();
        }
        return true;
    }

    @Override
    public void update(float tpf) {
        colliding = nextColliding;
        nextColliding = PhysicsCollisionObject.COLLISION_GROUP_NONE;
        if (colliding != PhysicsCollisionObject.COLLISION_GROUP_NONE) {
            collisionName = other.toString();
        } else {
            other = null;
            collisionName = "";
        }
    }

    @Override
    public void delete() {
        agent.getEngine().getPhysicsSpace().removeCollisionGroupListener(PhysicsCollisionObject.COLLISION_GROUP_02);
        agent.removeSensor(this);
    }

    @Override
    public SensorEditor getEditor() {
        return new CollisionSensorEditor();
    }
}
