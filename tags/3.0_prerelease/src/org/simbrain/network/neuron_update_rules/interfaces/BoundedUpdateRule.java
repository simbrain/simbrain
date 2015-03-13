/*
 * Part of Simbrain--a java-based neural network kit Copyright (C) 2005,2007 The
 * Authors. See http://www.simbrain.net/credits This program is free software;
 * you can redistribute it and/or modify it under the terms of the GNU General
 * Public License as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version. This program is
 * distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details. You
 * should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 59 Temple Place
 * - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.neuron_update_rules.interfaces;

/**
 *
 * An interface for updates rules that make use of an upper and lower bound,
 * either for clipping or for setting intrinsic bounds.
 *
 * @author Zach Tosi
 * @author Jeff Yoshimi
 *
 */
public interface BoundedUpdateRule {

    /**
     * Sets the upper bound of this neuron update rule's activation.
     *
     * @param upperBound the upper bound
     */
    void setUpperBound(double upperBound);

    /**
     * Sets the lower bound of this neuron update rule's activation.
     *
     * @param lowerBound the lower bound
     */
    void setLowerBound(double lowerBound);

    /**
     * Returns the upper bound.
     *
     * @return the upper boud
     */
    double getUpperBound();

    /**
     * Returns the lower bound.
     *
     * @return the lower bound
     */
    double getLowerBound();

}
