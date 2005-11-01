package org.simbrain.world.visionworld;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

public class Pixel extends Rectangle {

    public static final Color ON_COLOR = Color.BLACK;
    public static final Color OFF_COLOR = Color.WHITE;
    public static final boolean ON = true;
    public static final boolean OFF = false;
    private boolean state = true;

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

    public void switchState() {
        state = !state;
    }

    public boolean getState() {
        return state;
    }

    public void setState(final boolean state) {
        this.state = state;
    }

}
