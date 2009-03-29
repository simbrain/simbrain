package org.simbrain.network.layouts;

import java.awt.geom.Point2D;

import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;

/**
 * Lay neurons out in a line.
 *
 * @author jyoshimi
 */
public class LineLayout implements Layout {

    /** Lay neurons out vertically. */
    public static final int VERTICAL = 0;

    /** Lay neurons out horizontally. */
    public static final int HORIZONTAL = 1;

    /** Initial x position of line of neurons. */
    private double initialX;

    /** Initial y position of line of neurons. */
    private double initialY;

    /** Spacing between neurons. */
    private double spacing;

    /** What layout to use: vertical or horizontal. */
    private int layout;

    /**
     * Create a layout.
     *
     * @param initialx initial x position
     * @param initialy initial y position
     * @param spacing spacing between neurons
     * @param layout what layout to use
     */
    public LineLayout(final double initialx, final double initialy, final double spacing, final int layout) {
        initialX = initialx;
        initialY = initialy;
        this.spacing = spacing;
        this.layout = layout;
    }

    /**
     * Create a layout.
     *
     * @param spacing spacing between neurons
     * @param layout what layout to use
     */
    public LineLayout(final double spacing, final int layout) {
        this.spacing = spacing;
        this.layout = layout;
    }

    /** @see Layout */
    public void layoutNeurons(final Network network) {
        if (layout == VERTICAL) {
            double ypos = initialY;
            for (Neuron neuron : network.getFlatNeuronList()) {
                neuron.setX(initialX);
                neuron.setY(ypos);
                ypos += spacing;
            }
        } else if (layout == HORIZONTAL) {
            double xpos = initialX;
            for (Neuron neuron : network.getFlatNeuronList()) {
                neuron.setX(xpos);
                neuron.setY(initialY);
                xpos += spacing;
            }
        }
    }

    /** @see Layout */
    public void setInitialLocation(final Point2D initialPoint) {
        initialX = initialPoint.getX();
        initialY = initialPoint.getY();
    }

    /** @see Layout */
    public String getLayoutName() {
        return "Line";
    }
}
