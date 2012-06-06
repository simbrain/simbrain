/*
 * Copyright (C) 2005,2007 The Authors. See http://www.simbrain.net/credits This
 * program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.simbrain.network.connections;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.util.Comparators;

/**
 * Connect each source neuron to a single target.
 *
 * @author jyoshimi
 */
public class OneToOne extends ConnectNeurons {

    /**
     * "Template" synapse to be used when applying the connection.
     */
    private Synapse baseSynapse = Synapse.getTemplateSynapse();

    /**
     * If true, synapses are added in both directions.
     */
    private boolean useBidirectionalConnections = false;

    /** Orientation of how to connect neurons. */
    private Comparator<Neuron> connectOrientation = Comparators.X_ORDER;

    /**
     * Used for populating combo box with orientation types.
     *
     * @return Array of connection types.
     */
    public static Comparator[] getOrientationTypes() {
        return new Comparator[] { Comparators.X_ORDER, Comparators.Y_ORDER };
    }

    /**
     * {@inheritDoc}
     */
    public OneToOne(final Network network, final List<Neuron> neurons,
            final List<Neuron> neurons2) {
        super(network, neurons, neurons2);
    }

    /** {@inheritDoc} */
    public OneToOne() {
    }

    @Override
    public String toString() {
        return "One to one";
    }

    /**
     * Returns a sorted list of neurons, given a comparator.
     *
     * @param neuronList the base list of neurons.
     * @param comparator the comparator.
     * @return the sorted list.
     */
    private List<Neuron> getSortedNeuronList(
            final List<? extends Neuron> neuronList,
            final Comparator<Neuron> comparator) {
        ArrayList<Neuron> list = new ArrayList<Neuron>();
        list.addAll(neuronList);
        Collections.sort(list, comparator);
        return list;
    }

    /** @inheritDoc */
    public List<Synapse> connectNeurons() {

        ArrayList<Synapse> syns = new ArrayList<Synapse>();

        // TODO: Flags for which comparator to use, including no comparator
        // (Some users might want random but 1-1 couplings)

        Iterator<Neuron> targetsX = getSortedNeuronList(targetNeurons,
                connectOrientation).iterator();

        for (Iterator<Neuron> sources = getSortedNeuronList(sourceNeurons,
                connectOrientation).iterator(); sources.hasNext();) {
            Neuron source = sources.next();
            if (targetsX.hasNext()) {
                Neuron target = targetsX.next();
                Synapse synapse = baseSynapse.instantiateTemplateSynapse(
                        source, target, network);
                network.addSynapse(synapse);
                syns.add(synapse);
                // Allow neurons to be connected back to source.
                if (useBidirectionalConnections) {
                    Synapse synapse2 = baseSynapse.instantiateTemplateSynapse(
                            source, target, network);
                    synapse2.setSource(target);
                    synapse2.setTarget(source);
                    network.addSynapse(synapse2);
                    syns.add(synapse2);
                }
            } else {
                break;
            }
        }
        return syns;
    }

    /**
     * @return the useBidirectionalConnections
     */
    public boolean isUseBidirectionalConnections() {
        return useBidirectionalConnections;
    }

    /**
     * @param useBidirectionalConnections the useBidirectionalConnections to set
     */
    public void setUseBidirectionalConnections(
            final boolean useBidirectionalConnections) {
        this.useBidirectionalConnections = useBidirectionalConnections;
    }

    /**
     * @return the connectOrientation
     */
    public Comparator<Neuron> getConnectOrientation() {
        return connectOrientation;
    }

    /**
     * @param connectOrientation the connectOrientation to set
     */
    public void setConnectOrientation(
            final Comparator<Neuron> connectOrientation) {
        this.connectOrientation = connectOrientation;
    }

    /**
     * @return the baseSynapse
     */
    public Synapse getBaseSynapse() {
        return baseSynapse;
    }

    /**
     * @param baseSynapse the baseSynapse to set
     */
    public void setBaseSynapse(Synapse baseSynapse) {
        this.baseSynapse = baseSynapse;
    }
}
