package org.simbrain.world.odorworld.sensors;

import org.simbrain.util.environment.ScalarSmellSource;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.workspace.Producible;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

/**
 * Sensor that reacts when an object of a given type is near it.
 */
public class ObjectSensor extends Sensor {

    /**
     * Current value of the sensor.
     */
    private double value;

    /**
     * The type of the object represented, e.g. Swiss.gif.
     */
    private String objectType;

    public ObjectSensor(OdorWorldEntity parent, String label, String objectType) {
        super(parent, label);
        this.objectType = objectType;
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

    @Producible(idMethod = "getId")
    public double getCurrentValue() {
        return value;
    }

    @Override
    public String getTypeDescription() {
        return objectType;
    }
}
