/*
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
package org.simbrain.network.connections;

import java.util.Collection;

import org.simbrain.network.core.Synapse;

/**
 * A superclass for all connectors whose primary parameter is related to base
 * connection density, taking no other major factors into account insofar as
 * slecting which neurons should be connected goes.
 *
 * @author Zach Tosi
 */
public abstract class DensityBasedConnector extends ConnectNeurons {

    /**
     * The default preference as to whether or not self connections are allowed.
     */
    public static final boolean DEFAULT_SELF_CONNECT_PREF = false;

    /**
     * Generally speaking the connectionDensity parameter represents a
     * probability reflecting how many possible connections between a given
     * source neuron and all available target neurons will actually be made.
     */
    protected double connectionDensity;

    /**
     * Whether or not connections where the source and target are the same
     * neuron are allowed. Only applicable if the source and target neuron sets
     * are the same.
     */
    protected boolean selfConnectionAllowed = DEFAULT_SELF_CONNECT_PREF;

    /** Returns the {@link #connectionDensity}. */
    public abstract double getConnectionDensity();

    /**
     * Set how dense the connections are between source and target neurons,
     * generally speaking the connectionDensity parameter represents a
     * probability reflecting how many possible connections between a given
     * source neuron and all available target neurons will actually be made.
     *
     * @param connectionDensity
     * @return the connections added or removed to achieve the desired density
     *         if density is being changed after connections have already been
     *         made
     */
    public abstract Collection<Synapse> setConnectionDensity(
            double connectionDensity);

    /**
     * @return hether or not self connections (connections where the source and
     *         target neuron are the same neuron) are allowed.
     */
    public boolean isSelfConnectionAllowed() {
        return selfConnectionAllowed;
    }

    /**
     * Set whether or not self connections (connections where the source and
     * target neuron are the same neuron) are allowed.
     *
     * @param selfConnectionAllowed
     */
    public void setSelfConnectionAllowed(boolean selfConnectionAllowed) {
        this.selfConnectionAllowed = selfConnectionAllowed;
    }

}
