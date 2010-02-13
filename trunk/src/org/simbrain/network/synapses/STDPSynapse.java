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
package org.simbrain.network.synapses;

import org.simbrain.network.interfaces.Neuron;
import org.simbrain.network.interfaces.SpikingNeuron;
import org.simbrain.network.interfaces.Synapse;

/**
 * <b>STDPSynapse</b> models spike time dependent plasticity.
 *
 * Only works if source and target neurons are spiking neurons.
 *
 * Default parameter values and equations inspired by Jean-Philippe Thivierge
 * and Paul Cisek (2008), Journal of Neuroscience. Nonperiodic Synchronization in
 * Heterogeneous Networks of Spiking Neurons
 */
public class STDPSynapse extends Synapse {

    // TODO: Check all below.  I made this up! And elaborate..
    // TODO: explain / code relation to time constant.

    /** Time constant for LTP case.*/
    private double tau_plus = 30;

    /** Time constant for LTD case. */
    private double tau_minus = 60;

    /**
     * Learning rate for LTP case. Can tweak this separately from W_minus to
     * allow more LTP than LTD
     */
    private double W_plus = 10;

    /**
     * Learning rate for LTP case. Can tweak this separately from W_minus to
     * allow more LTP than LTD.   Default in paper is 100.
     */
    private double W_minus = 100;

    /** General learning rate. */
    private double learningRate = .001;

    /**
     * Creates a weight connecting source and target neurons.
     *
     * @param source source neuron
     * @param target target neuron
     */
    public STDPSynapse(final Neuron source, final Neuron target) {
        super(source, target);
    }

    /**
     * This constructor is used when creating a neuron of one type from another
     * neuron of another type Only values common to different types of neuron
     * are copied.
     *
     * @param s Synapse to make of the type
     */
    public STDPSynapse(final Synapse s) {
        super(s);
    }

    /**
     * @return duplicate ClampedSynapse (used, e.g., in copy/paste).
     */
    public Synapse duplicate() {
        STDPSynapse duplicateSynapse = new STDPSynapse(this.getSource(), this.getTarget());
        duplicateSynapse.setTau_minus(this.getTau_minus());
        duplicateSynapse.setTau_plus(this.getTau_plus());
        duplicateSynapse.setW_minus(this.getW_minus());
        duplicateSynapse.setW_plus(this.getW_plus());
        duplicateSynapse.setLearningRate(this.getLearningRate());
        return super.duplicate(duplicateSynapse);
    }

    /**
     * Update the synapse.
     */
    public void update() {

        double delta_t,delta_w;

        boolean sourceSpiking = this.getSource() instanceof SpikingNeuron;
        boolean targetSpiking = this.getTarget() instanceof SpikingNeuron;

        if (sourceSpiking && targetSpiking) {
            SpikingNeuron src = (SpikingNeuron) getSource();
            SpikingNeuron tar = (SpikingNeuron) getTarget();
            delta_t = src.lastSpikeTime - tar.lastSpikeTime;
            double timeStep = getRootNetwork().getTimeStep();
            if (delta_t > 0) {
                delta_w = W_plus * Math.exp(-delta_t / (tau_plus / timeStep));
            } else {
                delta_w = -W_minus * Math.exp(delta_t / (tau_minus / timeStep));
            }
            delta_w *= learningRate;
            //System.out.println(delta_t + "/" + delta_w);
            this.setStrength(this.getStrength() + delta_w);
        }

    }

    /**
     * @return Name of synapse type.
     */
    public static String getName() {
        return "Spike Timing Dependent Plasticity Synapse";
    }

    /**
     * @return the tau_plus
     */
    public double getTau_plus() {
        return tau_plus;
    }

    /**
     * @param tauPlus the tau_plus to set
     */
    public void setTau_plus(double tauPlus) {
        tau_plus = tauPlus;
    }

    /**
     * @return the tau_minus
     */
    public double getTau_minus() {
        return tau_minus;
    }

    /**
     * @param tauMinus the tau_minus to set
     */
    public void setTau_minus(double tauMinus) {
        tau_minus = tauMinus;
    }

    /**
     * @return the w_plus
     */
    public double getW_plus() {
        return W_plus;
    }

    /**
     * @param wPlus the w_plus to set
     */
    public void setW_plus(double wPlus) {
        W_plus = wPlus;
    }

    /**
     * @return the w_minus
     */
    public double getW_minus() {
        return W_minus;
    }

    /**
     * @param wMinus the w_minus to set
     */
    public void setW_minus(double wMinus) {
        W_minus = wMinus;
    }

    /**
     * @return the learningRate
     */
    public double getLearningRate() {
        return learningRate;
    }

    /**
     * @param learningRate the learningRate to set
     */
    public void setLearningRate(double learningRate) {
        this.learningRate = learningRate;
    }

}
