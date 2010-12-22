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

import java.util.Iterator;
import java.util.List;

import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.Synapse;
import org.simbrain.network.interfaces.SynapseUpdateRule;
import org.simbrain.network.synapses.ClampedSynapse;

/**
 * Connect neurons sparsely with some probabilities.
 *
 * @author jyoshimi
 *
 * TODO:    - Rename this class; it does not necessarily produce "sparse" connectivity.
 *          - Set weights strengths.
 */
public class Sparse extends ConnectNeurons {

    /** Probability connection will be an excitatory weight. */
    public static double excitatoryProbability = .1;

    /** Probability connection will be an inhibitory weight. */
    public static double inhibitoryProbability = .1;

    /** Template synapse for excitatory synapses. */
    private static Synapse baseExcitatorySynapse = Synapse.getTemplateSynapse();

    /** Template synapse for inhibitory synapses. */
    private static Synapse baseInhibitorySynapse = Synapse.getTemplateSynapse();

    // Initialize base synapses
    {
        baseExcitatorySynapse.setStrength(10);
        baseInhibitorySynapse.setStrength(-10);
    }

    /**
     * See super class description.
     *
     * @param network network with neurons to be connected.
     * @param neurons source neurons.
     * @param neurons2 target neurons.
     */
    public Sparse(final Network network, final List<? extends Neuron> neurons, final List<? extends Neuron> neurons2) {
        super(network, neurons, neurons2);
    }

    /** {@inheritDoc} */
    public Sparse() {}

    @Override
    public String toString() {
        return "Sparse";
    }

    /** @inheritDoc */
    public void connectNeurons() {
        for (Iterator i = sourceNeurons.iterator(); i.hasNext(); ) {
            Neuron source = (Neuron) i.next();
            for (Iterator j = targetNeurons.iterator(); j.hasNext(); ) {
                Neuron target = (Neuron) j.next();

                // Don't add a connection if there is already one present
                //  TODO: Add option to turn this off?
                if (Network.getSynapse(source, target) != null) {
                    continue;
                }

                if (Math.random() < excitatoryProbability) {
                    Synapse synapse = baseExcitatorySynapse
                            .instantiateTemplateSynapse(source, target, network);
                    network.addSynapse(synapse);
                    continue;
                }
                if (Math.random() < inhibitoryProbability) {
                    Synapse synapse = baseInhibitorySynapse
                            .instantiateTemplateSynapse(source, target, network);
                    network.addSynapse(synapse);
                }
            }
        }
    }

    /**
     * @return the excitatoryProbability
     */
    public static double getExcitatoryProbability() {
        return excitatoryProbability;
    }

    /**
     * @param excitatoryProbability the excitatoryProbability to set
     */
    public static void setExcitatoryProbability(final double excitatoryProbability) {
        Sparse.excitatoryProbability = excitatoryProbability;
    }

    /**
     * @return the inhibitoryProbability
     */
    public static double getInhibitoryProbability() {
        return inhibitoryProbability;
    }

    /**
     * @param inhibitoryProbability the inhibitoryProbability to set
     */
    public static void setInhibitoryProbability(
            final double inhibitoryProbability) {
        Sparse.inhibitoryProbability = inhibitoryProbability;
    }

    /**
     * @return the baseExcitatorySynapse
     */
    public static Synapse getBaseExcitatorySynapse() {
        return baseExcitatorySynapse;
    }

    /**
     * @param baseExcitatorySynapse the baseExcitatorySynapse to set
     */
    public static void setBaseExcitatorySynapse(Synapse baseExcitatorySynapse) {
        Sparse.baseExcitatorySynapse = baseExcitatorySynapse;
    }

    /**
     * @return the baseInhibitorySynapse
     */
    public static Synapse getBaseInhibitorySynapse() {
        return baseInhibitorySynapse;
    }

    /**
     * @param baseInhibitorySynapse the baseInhibitorySynapse to set
     */
    public static void setBaseInhibitorySynapse(Synapse baseInhibitorySynapse) {
        Sparse.baseInhibitorySynapse = baseInhibitorySynapse;
    }
}
