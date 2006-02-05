package org.simnet.layouts;

import java.awt.geom.Point2D;

import org.simnet.interfaces.Network;

/**
 * Interface for all neuron layout managers, which arrange a set of neurons in different ways.
 *
 * @author jyoshimi
 *
 */
public interface Layout {

    /**
     * Perform the layout.
     * @param network TODO
     * @param reference to network who
     * se nodes should be laid out
     */
    void layoutNeurons(Network network);

    /**
     * @return the name of this layout type
     */
    public String getLayoutName();
    
    /**
     * Set the initial position.
     *
     * @param initialPoint initial position
     */
    public void setInitialLocation(final Point2D initialPoint);
  

}
