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

import java.util.Collections;
import java.util.Comparator;

import org.simbrain.network.interfaces.Network;
import org.simbrain.network.interfaces.RootNetwork;
import org.simbrain.network.layouts.Layout;
import org.simbrain.network.neurons.PointNeuron;


/**
 * <b>KwtaNetwork</b> implements a k Winner Take All network.  The k neurons receiving the most
 * excitatory input will become active. The network determines what constantl level of inhibition
 * across all network neurons will result in those k neurons being active about threshold.
 * From O'Reilley and Munakata, Computational Explorations in Cognitive Neuroscience, p. 110.
 * All page references below are are to this book.
 *
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

    /** Current threshold conducatnce. */
    private double currentThresholdConductance = 0;

    /**
     * Default connstructor.
     */
    public KwtaNetwork() {
    }

    /**
     * Default connstructor.
     * @param layout for layout of Neurons.
     * @param k for the number of Neurons in the Kwta Network.
     * @param root reference to RootNetwork.
     */
    public KwtaNetwork(final RootNetwork root, final int k, final Layout layout) {
        super();
        this.setRootNetwork(root);
        // When these neurons are added
        //  the root network notifies all observers.
        //  network panel addes the neuron
        for (int i = 0; i < k; i++) {
            addNeuron(new PointNeuron());
        }
        layout.layoutNeurons(this);
        getRootNetwork().fireNetworkChanged();
    }

    /**
     * The core update function of the neural network.  Calls the current update function on each neuron, decays all
     * the neurons, and checks their bounds.
     */
    public void update() {
        sortNeurons();
        setCurrentThresholdCurrent();
        updateAllNeurons();
        updateAllSynapses();
        //System.out.println("|-->" + currentThresholdConductance);
    }

    /**
     * See p. 101, equation 3.3
     *
     */
    private void setCurrentThresholdCurrent() {
        double kPlusOne = ((PointNeuron) getNeuronList().get(k)).getThresholdInhibitoryConductance();
        currentThresholdConductance = kPlusOne
            + q * (((PointNeuron) this.getNeuronList().get(k - 1)).getThresholdInhibitoryConductance() - kPlusOne);
    }

    /**
     * See p. 101.
     * They say complete sort not necessary.  But why not?
     */
    private void sortNeurons() {
        Collections.sort(this.getNeuronList(), new PointNeuronComparator());
    }

    /**
     * Used to sort PointNeurons by excitatory current.
     */
    class PointNeuronComparator implements Comparator {

        /**
         * @inheritDoc Comparator.
         */
        public int compare(Object arg0, Object arg1) {
            return (int) (((PointNeuron)arg0).getExcitatoryCurrent() - ((PointNeuron)arg1).getExcitatoryCurrent());
        }
    }

    /**
     * Returns threhsold conductance.
     *
     * @return threshold conductance.
     */
    public double getThresholdInhibitoryConductance() {
        return currentThresholdConductance;
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

    /** @Override */
    public Network duplicate() {
        KwtaNetwork net = new KwtaNetwork();
        net = (KwtaNetwork) super.duplicate(net);
        net.setK(this.getK());
        return net;
    }
}
