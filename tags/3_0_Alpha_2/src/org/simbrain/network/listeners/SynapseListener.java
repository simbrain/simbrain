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
package org.simbrain.network.listeners;

import org.simbrain.network.interfaces.Synapse;
import org.simbrain.network.interfaces.SynapseUpdateRule;

/**
 * Listener interface for receiving network events relating to synapses. Classes
 * interested in responding to synapse related events are registered with a
 * RootNetwork, which broadcasts synapse relevant events to registered observer
 * classes.
 */
public interface SynapseListener {

    /**
     * Notify this listener of a synapse removed event.
     *
     * @param networkEvent event
     */
    void synapseRemoved(NetworkEvent<Synapse> networkEvent);

    /**
     * Notify this listener of a synapse added event.
     *
     * @param networkEvent reference to new synapse
     */
    void synapseAdded(NetworkEvent<Synapse> networkEvent);

    /**
     * Notify this listener that synapse's state changed.
     *
     * @param networkEvent reference to synapse whose state changed
     */
    void synapseChanged(NetworkEvent<Synapse> networkEvent);

    /**
     * Invoked when a synapse's update rule changes.
     *
     * @param networkEvent reference to to old and new synapse update rule
     */
    void synapseTypeChanged(NetworkEvent<SynapseUpdateRule> networkEvent);

}