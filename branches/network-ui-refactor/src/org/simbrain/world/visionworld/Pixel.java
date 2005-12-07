package org.simbrain.world.visionworld;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * The <b>Pixel</b> class represents a unit in the <b>VisionWorld</b>.
 *
 * @author RJB
 */
public class Pixel extends Rectangle {

    /**
     * The color of the activated pixel.
     */
    public static final Color ON_COLOR = Color.BLACK;

    /**
     * The color of the inactive pixel.
     */
    public static final Color OFF_COLOR = Color.WHITE;

    /**
     * The boolean value representing the activated state of the pixel.
     */
    public static final boolean ON = true;

    /**
     * The boolean value representing the inactive state of the pixel.
     */
    public static final boolean OFF = false;

    /**
     * The current state of the pixel (active by default).
     */
    private boolean state = ON;

    /**
    * @param g the graphics for visionworld
    * @val if true, show this pixel, else don't
    */
    public void show(final Graphics g) {

        if (state == ON) {
            g.setColor(ON_COLOR);
        } else if (state == OFF) {
            g.setColor(OFF_COLOR);
        }

        g.fillRect(this.x, this.y, this.width, this.height);

        return;
    }

    /**
     * Switches the state of the pixel.
     */
    public void switchState() {
        state = !state;
    }

    /**
     * @return the current state of the pixel
     */
    public boolean getState() {
        return state;
    }

    /**
     * @param state desired state of the pixel
     */
    public void setState(final boolean state) {
        this.state = state;
    }
}
