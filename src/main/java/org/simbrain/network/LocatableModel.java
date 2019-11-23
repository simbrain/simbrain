package org.simbrain.network;

import java.awt.geom.Point2D;

/**
 *  Model elements that have a center location should implement this interface.  Typically these
 *  will be at the top of a graphical hierarchy.  Graphical representations should be set up
 *  accordingly. As an example see {@link org.simbrain.network.gui.nodes.NeuronArrayNode}
 */
public interface LocatableModel extends NetworkModel {

    /**
     * Return the center x position of this item
     */
    double getCenterX();

    /**
     * Return the center y position of this item
     */
    double getCenterY();

    /**
     * Set the center x position of this item
     */
    void setCenterX(double newx);

    /**
     * Set the center y position of this item
     */
    void setCenterY(double newy);

    /**
     * Get the location of this model. By default the anchor point is the center of the model.
     */
    default Point2D getLocation() {
        return new Point2D.Double(getCenterX(), getCenterY());
    }

    /**
     * Set the location of this model. By default the anchor point is the center of the model.
     *
     * @param location the location to set
     */
    default void setLocation(Point2D location) {
        setCenterX(location.getX());
        setCenterY(location.getY());
    }

}
