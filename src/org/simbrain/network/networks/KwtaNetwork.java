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
package org.simbrain.network.networks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.layouts.Layout;
import org.simbrain.network.neurons.PointNeuron;

/**
 * <b>KwtaNetwork</b> implements a k Winner Take All network. The k neurons
 * receiving the most excitatory input will become active. The network
 * determines what level of inhibition across all network neurons will result in
 * those k neurons being active about threshold. From O'Reilley and Munakata,
 * Computational Explorations in Cognitive Neuroscience, p. 110. All page
 * references below are are to this book.
 */
public class KwtaNetwork extends Network {

    //TODO: Make q settable
    //      Add average based version 

    /** k, that is, number of neurons to win a competition. */
    private int k = 1;

    /**
     * Determines the relative contribution of the k and
     * k+1 node to the threshold conductance.
     */
    private double q = 0.25;

    /**
     * Current inhibitory conductance to be applied to all neurons in the
     * subnetwork.
     */
    private double inhibitoryConductance;

    /**
     * Default constructor.
     */
    public KwtaNetwork() {
    }

    /**
     * Default constructor.
     *
     * @param layout for layout of Neurons.
     * @param k for the number of Neurons in the Kwta Network.
     * @param root reference to RootNetwork.
     */
    public KwtaNetwork(final RootNetwork root, final int k, final Layout layout) {
        super();
        this.setRootNetwork(root);
        for (int i = 0; i < k; i++) {
            addNeuron(new PointNeuron());
        }
        layout.layoutNeurons(this);
    }

    /**
     * Update the kwta network. Sort the neurons by excitatory conductance,
     * determine the threshold conductance, apply this conductance to all point
     * neurons, and update the point neurons.
     */
    public void update() {
        sortNeurons();
        setCurrentThresholdCurrent();
        updateAllNeurons();
    }

    /**
     * See p. 101, equation 3.3.
     */
    private void setCurrentThresholdCurrent() {

        double highest = ((PointNeuron) this.getNeuronList().get(k))
              .getInhibitoryThresholdConductance();
        double secondHighest = ((PointNeuron) this.getNeuronList().get(k-1))
            .getInhibitoryThresholdConductance();

        inhibitoryConductance = secondHighest + q * (highest - secondHighest);

        // System.out.println("highest " + highest + "  secondHighest "
        //  + secondHighest + " inhibitoryCondctance" + inhibitoryConductance);

        // Set inhibitory conductances in the layer
        for (PointNeuron neuron : this.getNeuronList()) {
            neuron.setInhibitoryConductance(inhibitoryConductance);
        }
    }

    /**
     * Sort neurons by their excitatory conductance. See p. 101.
     */
    private void sortNeurons() {
        Collections.sort(this.getNeuronList(), new PointNeuronComparator());
    }

    /**
     * Used to sort PointNeurons by excitatory conductance.
     */
    class PointNeuronComparator implements Comparator<PointNeuron> {

        /**
         * {@inheritDoc}
         */
        public int compare(PointNeuron neuron1, PointNeuron neuron2) {
            return (int) (neuron1.getExcitatoryConductance() - neuron2
                    .getExcitatoryConductance());
        }
    }

    @Override
    public ArrayList<PointNeuron> getNeuronList() {
        return (ArrayList<PointNeuron>) super.getNeuronList();
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
        } else if (k >= getNeuronCount()) {
            this.k = getNeuronCount() - 1;
        } else {
            this.k = k;
        }
    }

    @Override
    public Network duplicate() {
        KwtaNetwork net = new KwtaNetwork();
        net = (KwtaNetwork) super.duplicate(net);
        net.setK(this.getK());
        return net;
    }
}
