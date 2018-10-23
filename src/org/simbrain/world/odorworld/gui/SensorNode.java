package org.simbrain.world.odorworld.gui;

import org.piccolo2d.PNode;
import org.simbrain.world.odorworld.sensors.VisualizableSensor;

import java.awt.*;

public abstract class SensorNode<T extends VisualizableSensor> extends PNode {

    protected static float sensorColor = Color.RGBtoHSB(255, 0, 0, null)[0];

    public abstract T getSensor();

    public abstract void update();

}
