package org.simbrain.world.odorworld.gui;

import org.piccolo2d.nodes.PPath;
import org.simbrain.world.odorworld.sensors.ObjectSensor;

import java.awt.*;
import java.awt.geom.GeneralPath;

public class ObjectSensorNode extends EntityAttributeNode {

    /**
     * Sensor diameter
     */
    private static final int SENSOR_RADIUS = 4;

    /**
     * Reference to the sensor this node is representing
     */
    private ObjectSensor sensor;

    /**
     * The shape of this node
     */
    private PPath shape;

    public ObjectSensorNode(ObjectSensor sensor) {
        this.sensor = sensor;
        GeneralPath diamondPath = new GeneralPath();
        diamondPath.moveTo(-SENSOR_RADIUS, 0);
        diamondPath.lineTo(0, -SENSOR_RADIUS);
        diamondPath.lineTo(SENSOR_RADIUS, 0);
        diamondPath.lineTo(0, SENSOR_RADIUS);
        diamondPath.closePath();
        this.shape = new PPath.Float(diamondPath);
        setPickable(false);
        addChild(shape);
    }

    @Override
    public void update() {
        shape.setOffset(sensor.getRelativeLocation());
        float saturation = (float) (sensor.getCurrentValue() / sensor.getBaseValue());
        if (saturation > 1) {
            saturation = 1;
        }
        if (saturation < 0) {
            saturation = 0;
        }
        shape.setPaint(Color.getHSBColor(maxColor, saturation, 1));
    }
}
