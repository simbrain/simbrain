package org.simbrain.world.odorworld.sensors;

import org.simbrain.util.UserParameter;
import org.simbrain.util.environment.ScalarSmellSource;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.propertyeditor2.EditableObject;
import org.simbrain.workspace.Producible;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

/**
 * Sensor that reacts when an object of a given type is near it.
 */
public class ObjectSensor extends Sensor {

    /**
     * Current value of the sensor.
     */
    @UserParameter(label = "Output Amount",
            description = "The amount of activation to be sent to a neuron coupled with this sensor.",
            defaultValue = "" + 1, order = 4)
    private double value = 1;

    /**
     * The type of the object represented, e.g. Swiss.gif.
     */
    @UserParameter(label = "Object Type",
            description = "The type of the object represented, e.g. Swiss.gif.",
            order = 5)
    private String objectType = "Swiss.gif";

    /**
     * Instantiate an object sensor.
     *
     * @param parent parent entity
     * @param label label for the sensor
     * @param objectType the type (e.g. Swiss.gif)
     */
    public ObjectSensor(OdorWorldEntity parent, String label, String objectType) {
        super(parent, label);
        this.objectType = objectType;
    }

    /**
     * Instantiate an object sensor.
     *
     * @param parent parent entity
     */
    public ObjectSensor(OdorWorldEntity parent) {
        super(parent, "Object Sensor");
    }

    @Override
    public void update() {
        value = 0;
        for (OdorWorldEntity entity : parent.getParentWorld().getObjectList()) {
            ScalarSmellSource smell = entity.getScalarSmell();
            if(entity.getObjectType().equals(objectType)) {
                double distance = SimbrainMath.distance(parent.getCenterLocation(), entity.getCenterLocation());
                value += smell.getValue(distance);
            }
        }
    }

    @Producible(idMethod = "getId", customDescriptionMethod = "getSensorDescription")
    public double getCurrentValue() {
        return value;
    }

    @Override
    public String getTypeDescription() {
        return objectType.substring(0, objectType.lastIndexOf('.'));
    }

    /**
     * Called by reflection to return a custom description for the {@link org.simbrain.workspace.gui.couplingmanager.AttributePanel.ProducerOrConsumer}
     * corresponding to object sensors.
     */
    public String getSensorDescription() {
        return getId() + ":" + getTypeDescription() + " sensor";
    }

    @Override
    public EditableObject copy() {
        return new ObjectSensor(parent, getLabel(), objectType);
    }

    @Override
    public String getName() {
        return "ObjectSensor";
    }
}
