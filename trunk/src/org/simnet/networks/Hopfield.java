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

import org.simnet.interfaces.Network;
import org.simnet.interfaces.Neuron;
import org.simnet.interfaces.Synapse;
import org.simnet.synapses.ClampedSynapse;


/**
 * <b>Hopfield</b>.
 */
public class Hopfield extends Network {

    /**
     * Default constructor.
     */
    public Hopfield() {
        super();
    }

    /**
     * Create full symmetric connections without self-connections.
     */
    public void createConnections() {
        for (int i = 0; i < this.getNeuronCount(); i++) {
            for (int j = 0; j < i; j++) {
                ClampedSynapse w = new ClampedSynapse();
                w.setUpperBound(1);
                w.setLowerBound(-1);
                w.randomize();
                w.setStrength(Network.round(w.getStrength(), 0));
                w.setSource(this.getNeuron(i));
                w.setTarget(this.getNeuron(j));
                addWeight(w);

                ClampedSynapse w2 = new ClampedSynapse();
                w2.setUpperBound(1);
                w2.setLowerBound(-1);
                w2.setStrength(w.getStrength());
                w2.setSource(this.getNeuron(j));
                w2.setTarget(this.getNeuron(i));
                addWeight(w2);
            }
        }
    }

    /**
     * Randomize weights symmetrically.
     */
    public void randomizeWeights() {
        for (int i = 0; i < getNeuronCount(); i++) {
            for (int j = 0; j < i; j++) {
                Synapse w = Network.getWeight(getNeuron(i), getNeuron(j));
                w.randomize();
                w.setStrength(Network.round(w.getStrength(), 0));

                Synapse w2 = Network.getWeight(getNeuron(j), getNeuron(i));
                w2.setStrength(w.getStrength());
            }
        }
        this.fireNetworkChanged();
    }

    /**
     * Apply hopfield training rule to current activation pattern.
     */
    public void train() {
        //Assumes all neurons have the same upper and lower values
        double low = getNeuron(0).getLowerBound();
        double hi = getNeuron(0).getUpperBound();

        for (int i = 0; i < this.getWeightCount(); i++) {
            //Must use buffer
            Synapse w = this.getWeight(i);
            Neuron src = w.getSource();
            Neuron tar = w.getTarget();
            w.setStrength(w.getStrength()
                          + ((((2 * src.getActivation()) - hi - low) / (hi - low)) * (((2 * tar.getActivation()) - hi
                          - low) / (hi - low))));
        }
        fireNetworkChanged();
    }

    /**
     * Used for updating network.
     */
    public void update() {
    }
}
