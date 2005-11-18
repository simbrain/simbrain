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

import java.util.Iterator;

import org.simnet.interfaces.Network;
import org.simnet.interfaces.Synapse;
import org.simnet.neurons.LinearNeuron;


/**
 * <b>StandardNetwork</b> serves as a high-level container for other networks and neurons.   It
 * contains a list of neurons as well as a list of networks.  When  building simulations in which multiple
 * networks interact, this should be the top-level network which contains the rest.
 *
 * @author yoshimi
 */
public class StandardNetwork extends Network {
  
    /**
     * Default connstructor.
     */
    public StandardNetwork() {
        super();
    }

    /**
     * Construct a Standard Network with a specified number of units.
     * 
     * @param nUnits how many units this network should have.
     */
    public StandardNetwork(final int nUnits) {
        super();

        for (int i = 0; i < nUnits; i++) {
            this.addNeuron(new LinearNeuron());
        }
    }

    public void init() {
        super.init();
    }

    /**
     * The core update function of the neural network.  Calls the current update function on each neuron, decays all
     * the neurons, and checks their bounds.
     */
    public void update() {
        updateAllNeurons();
        updateAllWeights();
    }

    /**
     * Set delays on all synapses to this network.
     *
     * @param newDelay the delay to set.
     */
    public void setDelays(final int newDelay) {
        for (int i = 0; i < this.getNeuronCount(); i++) {
             for (Iterator iter = this.getNeuron(i).getFanIn().iterator(); iter.hasNext();) {
                Synapse syn = (Synapse) iter.next();
                syn.setDelay(newDelay);
            }
        }
    }
}
