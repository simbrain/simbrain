package org.simnet.layouts;

import java.awt.geom.Point2D;
import java.util.ArrayList;

/**
 * Interface for all neuron layout managers, which arrange a set of neurons in different ways.
 *
 * @author jyoshimi
 *
 */
public interface Layout {

    /**
     * Perform the layout.
     *
     * @param neurons the neurons to arrange.
     */
    void layoutNeurons(final ArrayList neurons);

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
