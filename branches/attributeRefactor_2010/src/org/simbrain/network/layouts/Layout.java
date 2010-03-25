package org.simbrain.network.layouts;

import java.awt.geom.Point2D;
import java.util.List;

import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;

/**
 * Interface for all neuron layout managers, which arrange a set of neurons in different ways.
 *
 * @author jyoshimi
 *
 */
public interface Layout {

    /**
     * Layout a network.
     * 
     * @param network reference to network whose nodes should be laid out
     */
    void layoutNeurons(Network network);
    
    /**
     * Layout a list of neurons.
     * 
     * @param neurons the list of neurons
     */
    void layoutNeurons(List<Neuron> neurons);

    /**
     * @return the name of this layout type
     */
    String getLayoutName();

    /**
     * Set the initial position.
     *
     * @param initialPoint initial position
     */
    void setInitialLocation(final Point2D initialPoint);

}
