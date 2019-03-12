package org.simbrain.world.odorworld.gui;

import org.piccolo2d.nodes.PPath;
import org.simbrain.world.odorworld.sensors.GridSensor;

import java.awt.geom.GeneralPath;

public class LocationSensorNode extends EntityAttributeNode {

    /**
     * Sensor diameter
     */
    private static final int SENSOR_RADIUS = 4;

    /**
     * Reference to the sensor this node is representing
     */
    private GridSensor sensor;

    /**
     * The shape of this node
     */
    private PPath shape;

    public LocationSensorNode(GridSensor sensor) {
        this.sensor = sensor;
        GeneralPath crossPath = new GeneralPath();
        crossPath.moveTo(-SENSOR_RADIUS, 0);
        crossPath.lineTo(SENSOR_RADIUS, 0);
        crossPath.moveTo(0, -SENSOR_RADIUS);
        crossPath.lineTo(0, SENSOR_RADIUS);
        this.shape = new PPath.Float(crossPath);

        setPickable(false);
        shape.setPickable(false);
        addChild(shape);

    }

    @Override
    public void update() {
        shape.setOffset(sensor.getRelativeLocation());
    }
}
