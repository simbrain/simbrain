package org.simbrain.network.layouts;

import java.awt.geom.Point2D;

import org.simbrain.network.interfaces.Network;

/**
 * Interface for all neuron layout managers, which arrange a set of neurons in different ways.
 *
 * @author jyoshimi
 *
 */
public interface Layout {

    /**
     * Perform the layout.
     * @param network reference to network whose nodes should be laid out
     */
    void layoutNeurons(Network network);

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
