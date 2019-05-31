package org.simbrain.world.odorworld.gui;

import org.piccolo2d.nodes.PPath;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.sensors.SmellSensor;

import java.awt.*;

/**
 * Visual representation of smell sensors.
 */
public class SmellSensorNode extends EntityAttributeNode {

    /**
     * Sensor diameter
     */
    private static final int SENSOR_DIAMETER = 6;

    /**
     * Reference to the sensor this node is representing
     */
    private SmellSensor sensor;

    /**
     * The shape of this node
     */
    private PPath shape;

    /**
     * Reference to the world this sensor is in.
     */
    private OdorWorld world;

    /**
     * Construct a SensorNode base for the given sensor.
     *
     * @param sensor the sensor to be represented with this node
     */
    public SmellSensorNode(SmellSensor sensor) {
        this.sensor = sensor;
        this.shape = PPath.createEllipse(
                - SENSOR_DIAMETER / 2,
                - SENSOR_DIAMETER / 2,
                SENSOR_DIAMETER,
                SENSOR_DIAMETER
        );
        setPickable(false);
        shape.setPickable(false);
        this.world = sensor.getParent().getParentWorld();
        addChild(shape);
    }

    @Override
    public void update() {
        shape.setOffset(sensor.getRelativeLocation());
        double val = SimbrainMath.getVectorNorm(sensor.getCurrentValues());
        float saturation = 0;
        if (world.getTotalSmellVectorLength() > 0) {
            saturation = (float) Math.abs(val / (1 * world.getTotalSmellVectorLength()));
        }
        if (saturation > 1) {
            saturation = 1;
        }
        if (saturation < 0) {
            saturation = 0;
        }
        shape.setPaint(Color.getHSBColor(maxColor, saturation, 1));
    }
}
