package org.simbrain.world.threedworld.entities;

import com.jme3.bounding.BoundingVolume;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.jme3.scene.Node;
import org.simbrain.world.threedworld.engine.ThreeDEngine;
import org.simbrain.world.threedworld.entities.EditorDialog.Editor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Agent is an Entity which contains a Model, Sensors, and Effectors.
 */
public class Agent implements Entity {

    private ModelEntity model;
    private List<Sensor> sensors = new ArrayList<Sensor>();
    private List<Effector> effectors = new ArrayList<Effector>();

    /**
     * Construct a new Agent.
     *
     * @param model The model to attach to the Agent.
     */
    public Agent(ModelEntity model) {
        this.model = model;
    }

    public void addSensor(Sensor sensor) {
        sensors.add(sensor);
    }

    public void removeSensor(Sensor sensor) {
        sensors.remove(sensor);
    }

    /**
     * Returns the sensor of the specified class, if one exists.
     */
    public <T extends Sensor> Optional<T> getSensor(Class<T> sensorType) {
        for (Sensor sensor : sensors) {
            if (sensorType.isInstance(sensor)) {
                return Optional.of(sensorType.cast(sensor));
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the sensor of the specified type, if one exists.
     *
     * @param type The simple name of a class of sensor.
     */
    public Optional<Sensor> getSensor(String type) {
        for (Sensor sensor : sensors) {
            if (sensor.getClass().getSimpleName().equals(type)) {
                return Optional.of(sensor);
            }
        }
        return Optional.empty();
    }

    public List<Sensor> getSensors() {
        return sensors;
    }

    public void addEffector(Effector effector) {
        effectors.add(effector);
    }

    public void removeEffector(Effector effector) {
        effectors.remove(effector);
    }

    /**
     * Return the effector of the specified type, if one exists.
     */
    public <T extends Effector> Optional<T> getEffector(Class<T> effectorType) {
        for (Effector effector : effectors) {
            if (effectorType.isInstance(effector)) {
                return Optional.of(effectorType.cast(effector));
            }
        }
        return Optional.empty();
    }

    /**
     * Return the effector the specified type, if one exists.
     *
     * @param type The simple name of a class of effector.
     */
    public Optional<Effector> getEffector(String type) {
        for (Effector effector : effectors) {
            if (effector.getClass().getSimpleName().equals(type)) {
                return Optional.of(effector);
            }
        }
        return Optional.empty();
    }

    public List<Effector> getEffectors() {
        return effectors;
    }

    public ThreeDEngine getEngine() {
        return model.getEngine();
    }

    public ModelEntity getModel() {
        return model;
    }

    public void setModel(ModelEntity value) {
        model = value;
        model.getBody().setKinematic(true);
    }

    @Override
    public String getName() {
        return model.getName();
    }

    @Override
    public void setName(String value) {
        model.setName(value);
    }

    @Override
    public Node getNode() {
        return model.getNode();
    }

    @Override
    public Vector3f getPosition() {
        return model.getPosition();
    }

    @Override
    public void setPosition(Vector3f value) {
        model.setPosition(value);
    }

    @Override
    public void queuePosition(Vector3f value) {
        model.queuePosition(value);
    }

    @Override
    public void move(Vector3f offset) {
        model.move(offset);
    }

    @Override
    public Quaternion getRotation() {
        return model.getRotation();
    }

    @Override
    public void setRotation(Quaternion value) {
        model.setRotation(value);
    }

    @Override
    public void queueRotation(Quaternion value) {
        model.queueRotation(value);
    }

    @Override
    public void rotate(Quaternion rotation) {
        model.rotate(rotation);
    }

    @Override
    public BoundingVolume getBounds() {
        return model.getBounds();
    }

    @Override
    public void update(float t) {
        model.update(t);
        for (Sensor sensor : sensors) {
            sensor.update(t);
        }
        for (Effector effector : effectors) {
            effector.update(t);
        }
    }

    @Override
    public void delete() {
        model.delete();
    }

    @Override
    public Editor getEditor() {
        return model.getEditor();
    }
}
