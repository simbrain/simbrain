package org.simnet.layouts;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;

import org.simnet.interfaces.ComplexNetwork;
import org.simnet.interfaces.Network;
import org.simnet.interfaces.Neuron;

/**
 * Lay neurons out in successive layers.  Assumes a complex network with subnetworks.
 *
 * @author jyoshimi
 */
public class LayersLayout implements Layout {

    /** Lay neurons out vertically. */
    public static final int VERTICAL = 0;

    /** Lay neurons out horizontally. */
    public static final int HORIZONTAL = 1;

    /** Initial x position of line of neurons. */
    private double initialX;

    /** Initial y position of line of neuorns. */
    private double initialY;

    /** Horizontal spacing between neurons. */
    private double hSpacing;

    /** Vertical spacing between neurons. */
    private double vSpacing;

    /** What layout to use: vertical or horizontal. */
    private int layout;

    /**
     * Lay out a complex network into layers.
     *
     * @param hspacing
     * @param vspacing
     * @param layout
     */
    public LayersLayout(final double hspacing, final double vspacing, final int layout) {
        this.vSpacing = vspacing;
        this.hSpacing = hspacing;
        this.layout = layout;
    }

    /** @see Layout. */
    public void layoutNeurons(final Network network) {

        ArrayList layers = ((ComplexNetwork) network).getNetworkList();

        int baseCount = ((Network) layers.get(0)).getNeuronCount();
        double y = initialY + layers.size() * vSpacing;

            for (int i = 0; i <  layers.size(); i++) {
                Network currentLayer = (Network) layers.get(i);
                ArrayList neurons = currentLayer.getFlatNeuronList();
                for (int j = 0; j < neurons.size(); j++) {
                    double hOffset  =  ((baseCount - neurons.size()) * hSpacing) / 2;
                    Neuron neuron = (Neuron) neurons.get(j);
                    neuron.setX(initialX + hOffset + (j * hSpacing));
                    neuron.setY(y - (i * vSpacing));
                }
            }
    }

    /** @see Layout. */
    public void setInitialLocation(final Point2D initialPoint) {
        initialX = initialPoint.getX();
        initialY = initialPoint.getY();
    }

    /** @see Layout. */
    public String getLayoutName() {
        return "Line";
    }
}
