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
package org.simbrain.network.synapse_update_rules.spikeresponders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.simbrain.network.core.Synapse;

/**
 * <b>SpikeResponder</b>.
 */
public abstract class SpikeResponder {

    /** Value. */
    protected double value;

    /**
     * @return Spike responder to duplicate.
     */
    public abstract SpikeResponder deepCopy();

    /**
     * Update the synapse.
     *
     * @param s
     *            the synapse being updated
     */
    public abstract void update(final Synapse s);

    /**
     * @return the name of the spike responder
     */
    public abstract String getDescription();

    /**
     * @return the name of the class of this synapse
     */
    public String getType() {
        return this.getClass().getName()
                .substring(this.getClass().getName().lastIndexOf('.') + 1);
    }

    /**
     * A method which takes in a list of synapses and returns a list of their
     * spike responder, if they have any.
     *
     * @param synapses
     *            The list of synapses whose spike responders we want to query.
     * @return Returns a list of spike responders associated with the group of
     *         synapses
     */
    public static List<SpikeResponder> getResponderList(
            Collection<Synapse> synapses) {
        List<SpikeResponder> srList = new ArrayList<SpikeResponder>(
                synapses.size());
        for (Synapse s : synapses) {
            if (s.getSpikeResponder() != null) {
                srList.add(s.getSpikeResponder());
            }
        }
        return srList;
    }

    /**
     * @return Returns the value.
     */
    public double getValue() {
        return value;
    }

    /**
     * @param value
     *            The value to set.
     */
    public void setValue(final double value) {
        this.value = value;
    }

}
