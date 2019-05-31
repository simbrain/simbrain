package org.simbrain.world.odorworld.gui;

import org.piccolo2d.nodes.PPath;
import org.simbrain.world.odorworld.sensors.BumpSensor;

import java.awt.*;

public class BumpSensorNode extends EntityAttributeNode {

    /**
     * Reference to the sensor this node is representing
     */
    private BumpSensor sensor;

    /**
     * The shape of this node
     */
    private PPath shape;

    /**
     * Construct a BumpSensorNode base for the given sensor.
     *
     * @param sensor the sensor to be represented with this node
     */
    public BumpSensorNode(BumpSensor sensor) {
        this.sensor = sensor;
        this.shape = PPath.createRectangle(
                - sensor.getSensorSize() / 2,
                - sensor.getSensorSize() / 2,
                sensor.getSensorSize(),
                sensor.getSensorSize()
        );
        update();
        setPickable(false);
        shape.setPickable(false);
        addChild(this.shape);
    }

    @Override
    public void update() {
        shape.setOffset(sensor.getRelativeLocation().getX(), sensor.getRelativeLocation().getY());
        shape.setPaint(Color.getHSBColor(maxColor, (float) sensor.getCurrentValue(), 1));
    }
}
