/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005 Jeff Yoshimi <www.jeffyoshimi.net>
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
package org.simnet.networks;

import java.util.Collection;
import java.util.Iterator;

import org.simnet.interfaces.ComplexNetwork;
import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.Synapse;


/**
 * @author yoshimi <b>ContainerNetwork</b> serves as a high-level container for other networks and neurons.   It
 *         contains a list of neurons as well as a list of networks.  When  building simulations in which multiple
 *         networks interact, this should be the top-level network which contains the rest.
 */
public class ContainerNetwork extends ComplexNetwork {
    /** Container network. */
    public ContainerNetwork() {
        super();
    }

    /**
     * The core update function of the neural network.  Calls the current update function on each neuron, decays all
     * the neurons, and checks their bounds.
     */
    public void update() {
        updateTime();
        updateAllNetworks();
        updateAllNeurons();
        updateAllWeights();

        if (getRoundingOff()) {
            roundAll();
        }
    }

}
