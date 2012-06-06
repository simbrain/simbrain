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
import java.util.List;
import java.util.Random;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;

/**
 * Connect neurons sparsely.
 *
 * @author ztosi
 * @author jyoshimi
 */
public class Sparse extends ConnectNeurons {

    /** The default sparsity (between 0 and 1). */
    private static double DEFAULT_SPARSITY = 0.1;

    /** The overall sparsity of the connections. */
    private double sparsity = DEFAULT_SPARSITY;

    /**
     * Whether or not sparsity applies a constant number of synapses to each
     * source neuron.
     */
    private boolean sparseSpecific;

    /** A switch determining if self connections are possible. */
    private boolean allowSelfConnection;

    /**
     * See super class description.
     *
     * @param network network with neurons to be connected.
     * @param neurons source neurons.
     * @param neurons2 target neurons.
     */
    public Sparse(final Network network, final List<? extends Neuron> neurons,
            final List<? extends Neuron> neurons2) {
        super(network, neurons, neurons2);
    }

    /** {@inheritDoc} */
    public Sparse() {
    }

    @Override
    public String toString() {
        return "Sparse";
    }

    /** @inheritDoc */
    public List<Synapse> connectNeurons() {

        ArrayList<Synapse> syns = new ArrayList<Synapse>();

        // TODO: percent excititory currently not guaranteed for recurrent
        // connections (source list == target list) when self connection is
        // not allowed

        int numSyns = (int) (sparsity * sourceNeurons.size() * targetNeurons
                .size());
        int numExcite = (int) (excitatoryRatio * numSyns);
        Neuron source;
        Neuron target;
        Synapse synapse;
        Random randGen = new Random();

        if (!sparseSpecific) {

            for (int i = 0; i < numSyns; i++) {
                do {
                    source = sourceNeurons.get(randGen.nextInt(sourceNeurons
                            .size()));
                    target = targetNeurons.get(randGen.nextInt(targetNeurons
                            .size()));
                } while (Network.getSynapse(source, target) != null
                        || (!allowSelfConnection && (source == target)));

                if (i < numExcite) {
                    synapse = baseExcitatorySynapse.instantiateTemplateSynapse(
                            source, target, network);
                    if (enableExcitatoryRandomization) {
                        synapse.setStrength(excitatoryRandomizer.getRandom());
                    } else {
                        synapse.setStrength(DEFAULT_EXCITATORY_STRENGTH);
                    }
                } else {
                    synapse = baseInhibitorySynapse.instantiateTemplateSynapse(
                            source, target, network);
                    if (enableInhibitoryRandomization) {
                        synapse.setStrength(inhibitoryRandomizer.getRandom());
                    } else {
                        synapse.setStrength(DEFAULT_INHIBITORY_STRENGTH);
                    }
                }

                network.addSynapse(synapse);
                syns.add(synapse);
            }
        } else {
            int synsPerSource = numSyns / sourceNeurons.size();
            Random rGen = new Random();
            int numEx = (int) (excitatoryRatio * numSyns);
            int numIn = numSyns - numEx;

            for (int i = 0; i < sourceNeurons.size(); i++) {
                source = sourceNeurons.get(i);
                for (int j = 0; j < synsPerSource; j++) {

                    do {
                        target = targetNeurons.get(randGen
                                .nextInt(targetNeurons.size()));
                    } while (Network.getSynapse(source, target) != null
                            || (!allowSelfConnection && (source == target)));
                    int ex = rGen.nextInt(numEx + numIn);
                    if (ex < numEx) {
                        numEx--;
                        synapse = baseExcitatorySynapse
                                .instantiateTemplateSynapse(source, target,
                                        network);
                        if (enableExcitatoryRandomization) {
                            synapse.setStrength(excitatoryRandomizer
                                    .getRandom());
                        } else {
                            synapse.setStrength(DEFAULT_EXCITATORY_STRENGTH);
                        }

                    } else {
                        numIn--;
                        synapse = baseInhibitorySynapse
                                .instantiateTemplateSynapse(source, target,
                                        network);
                        if (enableInhibitoryRandomization) {
                            synapse.setStrength(inhibitoryRandomizer
                                    .getRandom());
                        } else {
                            synapse.setStrength(DEFAULT_INHIBITORY_STRENGTH);
                        }
                    }
                    network.addSynapse(synapse);
                    syns.add(synapse);
                }

            }
        }
        return syns;
    }

    public static double getDEFAULT_SPARSITY() {
        return DEFAULT_SPARSITY;
    }

    public static void setDEFAULT_SPARSITY(double dEFAULTSPARSITY) {
        DEFAULT_SPARSITY = dEFAULTSPARSITY;
    }

    public double getSparsity() {
        return sparsity;
    }

    public void setSparsity(double sparsity) {
        this.sparsity = sparsity;
    }

    public boolean isSparseSpecific() {
        return sparseSpecific;
    }

    public void setSparseSpecific(boolean sparseSpecific) {
        this.sparseSpecific = sparseSpecific;
    }

    public boolean isAllowSelfConnection() {
        return allowSelfConnection;
    }

    public void setAllowSelfConnection(boolean allowSelfConnection) {
        this.allowSelfConnection = allowSelfConnection;
    }

}
