package org.simbrain.world.odorworld.gui;

import org.piccolo2d.PNode;

import java.awt.*;

/**
 * Interface for visual representation of entity attributes (sensors and effectors).
 */
public abstract class EntityAttributeNode extends PNode {

    /**
     * The color to show when the output value of the attribute is at max.
     */
    static float maxColor = Color.RGBtoHSB(255, 0, 0, null)[0];

    /**
     * Update the visual representation base on the attribute status.
     */
    public abstract void update();

}
