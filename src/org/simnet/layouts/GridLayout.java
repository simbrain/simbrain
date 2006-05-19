package org.simnet.layouts;

import java.awt.geom.Point2D;
import java.util.ArrayList;

import org.simnet.interfaces.Network;
import org.simnet.interfaces.Neuron;

/**
 * Lay neurons out in a line.
 *
 * @author jyoshimi
 */
public class GridLayout implements Layout {

    /** Initial x position of line of neurons. */
    private double initialX;

    /** Initial y position of line of neuorns. */
    private double initialY;

    /** Number of columns in the layout. */
    private int numColumns;

    /** Horizontal spacing between neurons. */
    private double hSpacing;

    /** Vertical spacing between neurons. */
    private double vSpacing;

    /**
     * Create a layout.
     *
     * @param hSpacing horizontal spacing between neurons
     * @param vSpacing vertical spacing between neurons
     * @param numColumns number of columns of neurons
     */
    public GridLayout(final double hSpacing, final double vSpacing,
            final int numColumns) {
        this.hSpacing = hSpacing;
        this.vSpacing = vSpacing;
        this.numColumns = numColumns;
    }

    /** @see Layout. */
    public void layoutNeurons(final Network network) {
        ArrayList neurons = network.getFlatNeuronList();

        int rowNum = 0;
        for (int i = 0; i < neurons.size(); i++) {
            Neuron neuron = (Neuron) neurons.get(i);
            if (i % numColumns == 0) {
                rowNum++;
            }
            neuron.setX(initialX + (i % numColumns) * hSpacing);
            neuron.setY(initialY + rowNum * vSpacing);
        }
    }

    /** @see Layout. */
    public void setInitialLocation(final Point2D initialPoint) {
        initialX = initialPoint.getX();
        initialY = initialPoint.getY();
    }

    /** @see Layout. */
    public String getLayoutName() {
        return "Grid";
    }
}
