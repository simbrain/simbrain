/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.gui.nodes;

import org.simbrain.network.groups.NeuronGroup;
import org.simbrain.network.groups.SynapseGroup;

import java.awt.geom.Point2D;

/**
 * An interface that any synapse group node which uses a curved arrow (not an
 * arc) should implement.
 *
 * @author Zoë Tosi
 */
public interface SynapseGroupArrow {

    /**
     * Lays out the arrow(s) between the two specified points. The main code to
     * layout the graphical arrows is expected to be placed here, and then if
     * implemented by a PNode, this method should be called in LayoutChildren.
     * It is also expected that {@link #layoutChildrenQuiet(Point2D, Point2D)}
     * should call this method.
     *
     * @param pt1
     * @param pt2
     */
    void layout(Point2D pt1, Point2D pt2);

    /**
     * Lays out the child nodes of the class implementing this interface in
     * a way that expressly ensures that layoutChildren() will not be called
     * concurrently until this method has fully completed. The specifics of
     * how this is accomplished varies according to specific implementation.
     * If either pt1 or pt2 are null (or both), this method should use
     * the already existing start and end points as appropriate.
     *
     * @param pt1 the start point, if null should be replaced by the arrow's
     *            current start point. If the arrow doesn't have a start point yet,
     *            {@link #getOpposingDefaultPosition(NeuronGroup)} should be called using
     *            the neuron group opposite to this point.
     * @param pt2 pt1 the end point, if null should be replaced by the arrow's
     *            current end point. If the arrow doesn't have an end point yet,
     *            {@link #getOpposingDefaultPosition(NeuronGroup)} should be called using
     *            the neuron group opposite to this point.
     */
    void layoutChildrenQuiet(Point2D pt1, Point2D pt2);

    /**
     * Returns the midpoint for for
     *
     * @param src the first point
     * @param tar the second point
     * @return the midpoint between pt1 and pt2
     */
    Point2D midpoint(Point2D src, Point2D tar);

    /**
     * Determines what the end points should be depending on what this arrow
     * connects.
     */
    void determineProperEndPoints();

    /**
     * Based on the NeuronGroup at either end of this arrow, gives a "dummy"
     * default opposing position. Used when one of the end-points of this arrow
     * has been determined, but the other remains undetermined, so that the
     * arrow can be laid out in the meantime.
     *
     * @param ng the neuron group for the known endpoint
     * @return a default position for the end opposite the neuron group
     */
    Point2D getOpposingDefaultPosition(NeuronGroup ng);

    /**
     * @return the synapse group underlying this arrow.
     */
    SynapseGroup getSynapseGroup();

    /**
     * @return the start point for the arrow
     */
    Point2D getStartPt();

    /**
     * @return the end point for the arrow
     */
    Point2D getEndPt();

    /**
     * @return the amount of space this arrow takes up (usually plus a buffer),
     * used by whatever kind of neuron group this arrow connects to determine
     * how much space to give the PNode implementing this arrow relative to
     * other arrows connecting to it.
     */
    float getRequiredSpacing();

}
