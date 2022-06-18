package org.simbrain.world.odorworld.gui;

import org.piccolo2d.nodes.PPath;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.world.odorworld.OdorWorld;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;
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
        addChild(shape);
    }

    @Override
    public void update(OdorWorldEntity entity) {
        shape.setOffset(sensor.computeLocationFrom(entity));
        double val = SimbrainMath.getVectorNorm(sensor.getSmellVector());
        float saturation = 0;
        if (entity.getWorld().getMaxVectorNorm() > 0) {
            saturation = (float) SimbrainMath.rescale(val, 0, entity.getWorld().getMaxVectorNorm(),
                    0,1);
        }
        shape.setPaint(Color.getHSBColor(maxColor, saturation, 1));
    }
}
