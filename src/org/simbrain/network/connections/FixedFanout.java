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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.network.interfaces.SynapseUpdateRule;
import org.simbrain.network.synapses.ClampedSynapse;

/**
 * Connect every source neuron to every target neuron.
 * 
 */
public class FixedFanout extends ConnectNeurons {

    /**
     * The synapse to be used as a basis for the connection. Default to a
     * clamped synapse.
     */
    private static SynapseUpdateRule baseSynapseType = new ClampedSynapse();

    /** Allows neurons to have a self connection. */
    // TODO: add GUI for sigma and numOutbound
    private static boolean allowSelfConnection = false;
    private int numOutbound = 2;
    private double sigma = 1.1;
    private double[] outboundWeights = new double[numOutbound];

    public FixedFanout(final Network network,
            final List<? extends Neuron> neurons,
            final List<? extends Neuron> neurons2) {
        super(network, neurons, neurons2);
    }

    /** {@inheritDoc} */
    public FixedFanout() {
    }

    @Override
    public String toString() {
        return "Fixed Fanout";
    }

    @Override
    public List<Synapse> connectNeurons() {
        ArrayList<Synapse> syns = new ArrayList<Synapse>();
        for (Neuron source : sourceNeurons) {
            Random generator = new Random();

            Set<Neuron> fanOutSet = new HashSet<Neuron>();
            // TODO: Find better method for generating random sequence
            for (int i = 0; i < numOutbound; i++) {
                boolean foundUnique = false;
                while (foundUnique == false) {
                    int neuronNumber = generator.nextInt(sourceNeurons.size());
                    Neuron targetNeuron = sourceNeurons.get(neuronNumber);
                    if (targetNeuron != source) {
                        if (!fanOutSet.contains(targetNeuron)) {
                            fanOutSet.add(targetNeuron);
                            foundUnique = true;
                        }
                    }
                }
            }

            double[] outboundWeights = getOutboundWeights();
            int i = 0;
            for (Neuron target : fanOutSet) {
                Synapse synapse = new Synapse(source, target,
                        baseSynapseType.deepCopy());
                synapse.setSource(source);
                synapse.setTarget(target);
                synapse.setStrength(outboundWeights[i]);
                network.addSynapse(synapse);
                syns.add(synapse);
                i++;
            }
        }
        return syns;
    }

    private double[] getOutboundWeights() {
        Random randomGenerator = new Random();
        double[] randNumbers = new double[numOutbound - 1];
        for (int i = 0; i < numOutbound - 1; i++) {
            randNumbers[i] = randomGenerator.nextDouble() * sigma;
            // scales the random value between 0 and 1 by sigma
        }
        Arrays.sort(randNumbers);
        // Create array of outboundWeights
        for (int i = 0; i < numOutbound; i++) {
            // the first index is set equal to the first random number
            if (i == 0) {
                outboundWeights[0] = randNumbers[0];
            }
            // the last index is set equal to sigma minus the last random number
            else if (i == (numOutbound - 1)) {
                outboundWeights[numOutbound - 1] = (sigma - randNumbers[numOutbound - 2]);
            }
            // all intermittent indices
            else {
                outboundWeights[i] = (randNumbers[i] - randNumbers[i - 1]);
            }
        }
        return outboundWeights;
    }

    /**
     * @return the allowSelfConnection
     */
    public static boolean isAllowSelfConnection() {
        return allowSelfConnection;
    }

    /**
     * @param allowSelfConnection the allowSelfConnection to set
     */
    public static void setAllowSelfConnection(boolean allowSelfConnection) {
        FixedFanout.allowSelfConnection = allowSelfConnection;
    }
}
