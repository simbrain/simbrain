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

import java.util.Collections;
import java.util.Comparator;

import org.simnet.interfaces.Network;
import org.simnet.layouts.Layout;
import org.simnet.neurons.PointNeuron;


/**
 * <b>KwtaNetwork</b> implements a k Winner Take All network.
 *
 */
public class KwtaNetwork extends Network {

    /** k Field. */
    private int k = 1;

    /** q value for Threshold Inhibitory Current. */
    private double q = 0.25;

    private boolean useAverageBased = false;

    private double currentThresholdConductance = 0;
    
    /**
     * Default connstructor.
     */
    public KwtaNetwork() {

    }


    /**
     * Initializes K Winner Take All network.
     */
    public void init() {
        super.init();
    }

    /**
     * Default connstructor.
     * @param layout for layout of Neurons.
     * @param k for the number of Neurons in the Kwta Network.
     */
    public KwtaNetwork(final int k, final Layout layout) {
        super();
        for (int i = 0; i < k; i++) {
            this.addNeuron(new PointNeuron());
        }
        layout.layoutNeurons(this);
    }

    /**
     * The core update function of the neural network.  Calls the current update function on each neuron, decays all
     * the neurons, and checks their bounds.
     */
    public void update() {
        sortNeurons();
        setCurrentThresholdCurrent();
        updateAllNeurons();
        updateAllWeights();
        System.out.println("|-->" + currentThresholdConductance);
    }

    /**
     * See p. 101, equation 3.3
     *
     */
    private void setCurrentThresholdCurrent() {
        double kPlusOne = ((PointNeuron) getNeuronList().get(k)).getThresholdInhibitoryConductance();
        currentThresholdConductance = kPlusOne + q * (((PointNeuron) this.getNeuronList().get(k-1)).getThresholdInhibitoryConductance() - kPlusOne);    
    }
    /**
     * See p. 101.  
     * They say complete sort not necessary.  But why not?
     *
     */
    private void sortNeurons() {
        Collections.sort(this.getNeuronList(), new PointNeuronComparator());
    }
    
    class PointNeuronComparator implements Comparator {

        public int compare(Object arg0, Object arg1) {
            return (int) (((PointNeuron)arg0).getExcitatoryCurrent() - ((PointNeuron)arg1).getExcitatoryCurrent());
        }
    }

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
    public void setK(int k) {
        this.k = k;
    }
}
