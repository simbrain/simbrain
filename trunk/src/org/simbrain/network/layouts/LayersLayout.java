package org.simbrain.network.layouts;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;

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

    /** 
     * {@inheritDoc}
     */
    public void layoutNeurons(final Network network) {

        ArrayList<Network> layers = network.getNetworkList();

        int baseCount = (layers.get(0)).getNeuronCount();
        double y = initialY + layers.size() * vSpacing;

            for (int i = 0; i <  layers.size(); i++) {
                Network currentLayer = layers.get(i);
                ArrayList<Neuron> neurons = currentLayer.getFlatNeuronList();
                for (int j = 0; j < neurons.size(); j++) {
                    double hOffset  =  ((baseCount - neurons.size()) * hSpacing) / 2;
                    Neuron neuron = (Neuron) neurons.get(j);
                    neuron.setX(initialX + hOffset + (j * hSpacing));
                    neuron.setY(y - (i * vSpacing));
                }
            }
    }

    /** 
     * {@inheritDoc}
     */
    public void setInitialLocation(final Point2D initialPoint) {
        initialX = initialPoint.getX();
        initialY = initialPoint.getY();
    }

    /** 
     * {@inheritDoc}
     */
    public String getLayoutName() {
        return "Layers";
    }

    /** 
     * {@inheritDoc}
     */
    public void layoutNeurons(List<Neuron> neurons) {
        //  TODO: No implementation yet; this will requires
        //      specifying number of layers and a size for each.
    }
}
