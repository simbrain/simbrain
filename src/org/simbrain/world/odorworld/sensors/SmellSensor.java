package org.simbrain.world.odorworld.sensors;

import java.util.List;

import org.simbrain.util.SimbrainMath;
import org.simbrain.util.environment.SmellSource;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

/**
 * A sensor which is updated based on the presence of SmellSources near it.
 * 
 * @see org.simbrain.util.environment.SmellSource
 */
public class SmellSensor implements Sensor {

    /** Angle of whisker in radians. */
    public static double DEFAULT_THETA = Math.PI / 4;

    /** Initial length of mouse whisker. */
    private final double DEFAULT_RADIUS = 23;

    /** Relative location of the sensor in polar coordinates. */
    private double theta = DEFAULT_THETA;

    /** Relative location of the sensor in polar coordinates. */
    private double radius = DEFAULT_RADIUS;

    /** Reference to parent entity. */
    private OdorWorldEntity parent;

    /** Current value of this sensor, as an array of doubles. */
    private double[] currentValue = new double[5];

    /**
     * Construct a sensor.
     * 
     * @param parent reference
     * @param sensorName name
     * @param dim stimulus dimension
     */
    public SmellSensor(final OdorWorldEntity parent) {
        this.parent = parent;
    }

    /**
     * @return the location
     */
    public double[] getLocation() {
        // int x = (int) (getParent().getSuggestedLocation()[0] + (radius *
        // Math.cos(theta)));
        // int y = (int) (getParent().getSuggestedLocation()[1] - (radius *
        // Math.sin(theta)));
        return new double[] { 1, 1 };
    }

    /**
     * Update the current value of this sensor.
     */
    public void update() {
        double[] temp = SimbrainMath.zeroVector(currentValue.length);
        for (OdorWorldEntity entity : parent.getParentWorld().getObjectList()) {
            
            if (entity != parent) {
                
                SmellSource smell = entity.getSmellSource();
                if (smell != null) {
                    temp = SimbrainMath.addVector(temp, smell
                            .getStimulus(SimbrainMath.distance(parent
                                    .getLocation(), entity.getLocation())));
                }
            }
        }
        currentValue = temp;
    }

    public List<Class> getApplicableTypes() {
        return null;
    }

    /**
     * @return the currentValue
     */
    public double[] getCurrentValue() {
        return currentValue;
    }
}
