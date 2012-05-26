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

import org.simbrain.network.core.SpikingNeuronUpdateRule;
import org.simbrain.network.core.Synapse;
import org.simbrain.network.core.SynapseUpdateRule;

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
    private static final double TAU_PLUS_DEFAULT = 30;

    /** Default tau minus. */
    private static final double TAU_MINUS_DEFAULT = 60;

    /** Default W plus. */
    private static final double W_PLUS_DEFAULT = 10;

    /** Default W - . */
    private static final double W_MINUS_DEFAULT = 10;

    /** Default Learning rate. */
    private static final double LEARNING_RATE_DEFAULT = .01;

    /** Time constant for LTP.*/
    private double tau_plus = TAU_PLUS_DEFAULT;

    /** Time constant for LTD. */
    private double tau_minus = TAU_MINUS_DEFAULT;

    /**
     * Learning rate for LTP case. Controls magnitude of LTP changes.
     */
    private double W_plus = W_PLUS_DEFAULT;

    /**
     * Learning rate for LTP case. Controls magnitude of LTD changes.
     */
    private double W_minus = W_MINUS_DEFAULT;

    /** General learning rate. */
    private double learningRate = LEARNING_RATE_DEFAULT;

    @Override
    public void init(Synapse synapse) {
    }

    @Override
    public String getDescription() {
        return "Spike Timing Dependent Plasticity Synapse";
    }

    @Override
    public SynapseUpdateRule deepCopy() {
        STDPRule duplicateSynapse = new STDPRule();
        duplicateSynapse.setTau_minus(this.getTau_minus());
        duplicateSynapse.setTau_plus(this.getTau_plus());
        duplicateSynapse.setW_minus(this.getW_minus());
        duplicateSynapse.setW_plus(this.getW_plus());
        duplicateSynapse.setLearningRate(this.getLearningRate());
        return duplicateSynapse;
    }

    @Override
    public void update(Synapse synapse) {

        double delta_t, delta_w;

        boolean sourceSpiking = synapse.getSource().getUpdateRule() instanceof SpikingNeuronUpdateRule;
        boolean targetSpiking = synapse.getTarget().getUpdateRule() instanceof SpikingNeuronUpdateRule;

        if (sourceSpiking && targetSpiking) {
            SpikingNeuronUpdateRule src = (SpikingNeuronUpdateRule) synapse
                    .getSource().getUpdateRule();
            SpikingNeuronUpdateRule tar = (SpikingNeuronUpdateRule) synapse
                    .getTarget().getUpdateRule();
            if (tar.hasSpiked() ) {
				delta_t = synapse.getNetwork().getTime()
						- src.getLastSpikeTime();
				delta_w = W_plus * Math.exp(-delta_t / tau_plus) * learningRate;
				//System.out.println("LTP: " + delta_t + "/" + delta_w);
				synapse.setStrength(synapse.clip(synapse.getStrength()
						+ delta_w));
            	
            }
            if (src.hasSpiked()) {
				delta_t = tar.getLastSpikeTime()
						- synapse.getNetwork().getTime();
				delta_w = -W_minus * Math.exp(delta_t / tau_minus)
						* learningRate;
				//System.out.println("LTD: " + delta_t + "/" + delta_w);
				synapse.setStrength(synapse.clip(synapse.getStrength()
						+ delta_w));
            
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
