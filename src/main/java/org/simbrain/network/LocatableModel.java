package org.simbrain.network;

import org.simbrain.network.core.Neuron;

import java.awt.geom.Point2D;

/**
 * Model elements that have a location should implement this interface.  Note that locations are mostly based on
 * {@link Neuron} location. Neurons have point locations but not width or height.  Thus the width  of a {@link org.simbrain.network.groups.NeuronGroup}
 * neuron group, for example, is the distance between the point locations of the neurons within it which are farthest
 * apart.
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
