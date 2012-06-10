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
package org.simbrain.network.subnetworks;

import java.util.Comparator;

import org.simbrain.network.core.Network;
import org.simbrain.network.core.Neuron;
import org.simbrain.network.groups.Subnetwork;
import org.simbrain.network.layouts.Layout;
import org.simbrain.network.neuron_update_rules.PointNeuronRule;

/**
 * <b>KwtaNetwork</b> implements a k Winner Take All network. The k neurons
 * receiving the most excitatory input will become active. The network
 * determines what level of inhibition across all network neurons will result in
 * those k neurons being active about threshold. From O'Reilley and Munakata,
 * Computational Explorations in Cognitive Neuroscience, p. 110. All page
 * references below are are to this book.
 */
public class KWTA extends Subnetwork {

    // TODO: Make q settable
    // Add average based version

    /** k, that is, number of neurons to win a competition. */
    private int k = 1;

    /**
     * Determines the relative contribution of the k and k+1 node to the
     * threshold conductance.
     */
    private double q = 0.25;

    /**
     * Current inhibitory conductance to be applied to all neurons in the
     * subnetwork.
     */
    private double inhibitoryConductance;

    // REDO
    //
    // /**
    // * Copy constructor.
    // *
    // * @param newRoot new root network
    // * @param oldNet old network.
    // */
    // public KWTA(Network newRoot, KWTA oldNet) {
    // super(newRoot, oldNet);
    // setK(oldNet.getK());
    // }

    /**
     * Default constructor.
     *
     * @param layout for layout of Neurons.
     * @param k for the number of Neurons in the Kwta Network.
     * @param root reference to Network.
     */
    public KWTA(final Network root, final int k, final Layout layout) {
        super(root, 1, 1);
        for (int i = 0; i < k; i++) {
            getNeuronGroup().addNeuron(
                    new Neuron(getParentNetwork(), new PointNeuronRule()));
        }
        layout.layoutNeurons(getNeuronGroup().getNeuronList());
        getSynapseGroup().setDeleteWhenEmpty(false);
        root.getSynapseRouter().associateSynapseGroupWithTargetNeuronGroup(
                getNeuronGroup(), getSynapseGroup());
        setLabel("K-Winner Take All");
    }

    @Override
    public void update() {
        sortNeurons();
        // setCurrentThresholdCurrent();
        getNeuronGroup().update();
    }

    /**
     * See p. 101, equation 3.3.
     */
    private void setCurrentThresholdCurrent() {

        double highest = ((PointNeuronRule) getNeuronGroup().getNeuronList()
                .get(k).getUpdateRule()).getInhibitoryThresholdConductance();
        double secondHighest = ((PointNeuronRule) getNeuronGroup()
                .getNeuronList().get(k - 1).getUpdateRule())
                .getInhibitoryThresholdConductance();

        inhibitoryConductance = secondHighest + q * (highest - secondHighest);

        // System.out.println("highest " + highest + "  secondHighest "
        // + secondHighest + " inhibitoryCondctance" + inhibitoryConductance);

        // Set inhibitory conductances in the layer
        for (Neuron neuron : getNeuronGroup().getNeuronList()) {
            ((PointNeuronRule) neuron.getUpdateRule())
                    .setInhibitoryConductance(inhibitoryConductance);
        }
    }

    /**
     * Sort neurons by their excitatory conductance. See p. 101.
     */
    private void sortNeurons() {

        // REDO
        // Collections.sort(this.getNeuronList(), new PointNeuronComparator());
    }

    /**
     * Used to sort PointNeurons by excitatory conductance.
     */
    class PointNeuronComparator implements Comparator<Neuron> {

        /**
         * {@inheritDoc}
         */
        public int compare(Neuron neuron1, Neuron neuron2) {
            return (int) ((PointNeuronRule) neuron1.getUpdateRule())
                    .getExcitatoryConductance()
                    - (int) ((PointNeuronRule) neuron1.getUpdateRule())
                            .getExcitatoryConductance();
        }
    }

    /**
     * Returns the initial number of neurons.
     *
     * @return the initial number of neurons
     */
    public int getK() {
        return k;
    }

    /**
     * @param k The k to set.
     */
    public void setK(final int k) {
        if (k < 1) {
            this.k = 1;
        } else if (k >= getNeuronGroup().getNeuronList().size()) {
            this.k = getNeuronGroup().getNeuronList().size() - 1;
        } else {
            this.k = k;
        }
    }
}
