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
package org.simbrain.network.synapse_update_rules;

import java.util.concurrent.ThreadLocalRandom;

import org.simbrain.network.core.SpikingNeuronUpdateRule;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseUpdateRule;
import org.simbrain.network.neuron_update_rules.IntegrateAndFireRule;

/**
 * <b>STDPSynapse</b> models spike time dependent plasticity.
 *
 * Only works if source and target neurons are spiking neurons.
 *
 * Drew on: Jean-Philippe Thivierge and Paul Cisek (2008), Journal of
 * Neuroscience. Nonperiodic Synchronization in Heterogeneous Networks of
 * Spiking Neurons. Also drew on the Scholarpedia article.
 *
 */
public class STDPRule extends SynapseUpdateRule {

    /** Default tau plus. */
    public static final double TAU_PLUS_DEFAULT = 30;

    /** Default tau minus. */
    public static final double TAU_MINUS_DEFAULT = 60;

    /** Default W plus. */
    public static final double W_PLUS_DEFAULT = 10;

    /** Default W - . */
    public static final double W_MINUS_DEFAULT = 10;

    /** Default Learning rate. */
    public static final double LEARNING_RATE_DEFAULT = .01;

    /** Time constant for LTP. */
    protected double tau_plus = TAU_PLUS_DEFAULT;

    /** Time constant for LTD. */
    protected double tau_minus = TAU_MINUS_DEFAULT;

    /**
     * Learning rate for LTP case. Controls magnitude of LTP changes.
     */
    protected double W_plus = W_PLUS_DEFAULT;

    /**
     * Learning rate for LTP case. Controls magnitude of LTD changes.
     */
    protected double W_minus = W_MINUS_DEFAULT;

    /** General learning rate. */
    protected double learningRate = LEARNING_RATE_DEFAULT;

    @Override
    public void init(Synapse synapse) {
    }

    @Override
    public String getName() {
        return "STDP";
    }

    @Override
    public SynapseUpdateRule deepCopy() {
        STDPRule duplicateSynapse = new STDPRule();
        duplicateSynapse.setTau_minus(this.getTau_minus());
        duplicateSynapse.setTau_plus(this.getTau_plus());
        duplicateSynapse.setW_minus(this.getW_minus());
        duplicateSynapse.setW_plus(this.getW_plus());
        duplicateSynapse.setLearningRate(this.getLearningRate());
        duplicateSynapse.setHebbian(hebbian);
        return duplicateSynapse;
    }

    private boolean hebbian = true;

    private double delta_w = 0;

    @Override
    public void update(Synapse synapse) {
        if (synapse.getSource().isSpike() || synapse.getTarget().isSpike()) {
            try {
                final double str = synapse.getStrength();
                final double delta_t = ((((SpikingNeuronUpdateRule) synapse
                        .getSource().getUpdateRule()).getLastSpikeTime())
                        - ((SpikingNeuronUpdateRule) synapse
                        .getTarget().getUpdateRule()).getLastSpikeTime())
                        * (hebbian ? 1 : -1);   // Reverse time window for
                                                // anti-hebbian
                if (delta_t < 0) {
                    delta_w = W_plus * Math.exp(delta_t / tau_plus)
                            * learningRate;
                } else if (delta_t > 0) {
                    delta_w = -W_minus * Math.exp(-delta_t / tau_minus)
                            * learningRate;
                }
                if(Math.signum(str) == -1) {
                    synapse.setStrength(str - delta_w);
                } else {
                    synapse.setStrength(str + delta_w);
                }
            } catch (ClassCastException cce) {
                cce.printStackTrace();
                System.out.println("Don't use non-spiking neurons with STDP!");
            }
        }
    }

    /**
     * @return the tau_plus
     */
    public double getTau_plus() {
        return tau_plus;
    }

    /**
     * @param tauPlus
     *            the tau_plus to set
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
     * @param tauMinus
     *            the tau_minus to set
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
     * @param wPlus
     *            the w_plus to set
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
     * @param wMinus
     *            the w_minus to set
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
     * @param learningRate
     *            the learningRate to set
     */
    public void setLearningRate(double learningRate) {
        this.learningRate = learningRate;
    }

    public boolean isHebbian() {
        return hebbian;
    }

    public void setHebbian(boolean hebbian) {
        this.hebbian = hebbian;
    }

}
