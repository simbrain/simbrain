package org.simbrain.world.odorworld.sensors;

import org.simbrain.util.UserParameter;
import org.simbrain.util.math.DecayFunction;
import org.simbrain.util.math.DecayFunctions.LinearDecayFunction;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.propertyeditor2.EditableObject;
import org.simbrain.workspace.Producible;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

/**
 * Sensor that reacts when an object of a given type is near it.
 * <br>
 * While the smell framework involves objects emitting smells, object type
 * sensors have a sensitivity, and are more "sensor" or "subject" based than
 * object based.
 * <br>
 * The sensor itself is currently fixed at the center of the agent. We may
 * make the location editable at some point, if uses cases emerge.
 */
public class ObjectSensor extends Sensor {

    /**
     * Current value of the sensor.
     */
    private double value = 0;

    @UserParameter(
            label = "Base Value",
            description = "Base value of the output before decay function applies",
            defaultValue = "1", order = 4)
    private double baseValue = 1;

    /**
     * Decay function
     */
    @UserParameter(label = "Decay Function", isObjectType = true, order = 5)
    DecayFunction decayFunction = LinearDecayFunction.create();

    /**
     * The type of the object represented, e.g. Swiss.gif.
     */
    @UserParameter(label = "Object Type",
        description = "What type of object this sensor responds to",
        order = 3)
    private OdorWorldEntity.EntityType objectType = OdorWorldEntity.EntityType.SWISS;

    /**
     * Instantiate an object sensor.
     *
     * @param parent     parent entity
     * @param objectType the type (e.g. Swiss.gif)
     */
    public ObjectSensor(OdorWorldEntity parent, OdorWorldEntity.EntityType objectType) {
        super(parent, objectType.toString());
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
        for (OdorWorldEntity entity : parent.getEntitiesInRadius(decayFunction.getDispersion())) {
            if (entity.getEntityType() == objectType) {
                value = baseValue * decayFunction.getScalingFactor(
                        SimbrainMath.distance(
                                new double[]{entity.getCenterX(), entity.getCenterY()},
                                new double[]{parent.getCenterX(), entity.getCenterY()}
                        )
                );
                break;
            }
        }
    }

    @Producible(idMethod = "getId", customDescriptionMethod = "getAttributeDescription")
    public double getCurrentValue() {
        return value;
    }

    @Override
    public String getTypeDescription() {
        return objectType.toString();
    }


    @Override
    public EditableObject copy() {
        return new ObjectSensor(parent, objectType);
    }

    @Override
    public String getName() {
        return "ObjectSensor";
    }
}
