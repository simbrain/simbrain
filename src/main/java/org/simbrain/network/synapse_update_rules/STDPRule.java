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
import org.simbrain.util.UserParameter;

/**
 * <b>STDPSynapse</b> models spike time dependent plasticity.
 * <p>
 * Only works if source and target neurons are spiking neurons.
 * <p>
 * Drew on: Jean-Philippe Thivierge and Paul Cisek (2008), Journal of
 * Neuroscience. Nonperiodic Synchronization in Heterogeneous Networks of
 * Spiking Neurons. Also drew on the Scholarpedia article.
 */
public class STDPRule extends SynapseUpdateRule {

    // TODO: check description
    /**
     * Time constant for LTD.
     */
    @UserParameter(label = "Tau minus", description = "Time constant " + "for LTD.", increment = .1, order = 0)
    protected double tau_minus = 60;

    /**
     * Time constant for LTP.
     */
    @UserParameter(label = "Tau plus", description = "Time constant " + "for LTP.", increment = .1, order = 1)
    protected double tau_plus = 30;

    /**
     * Learning rate for LTP case. Controls magnitude of LTP changes.
     */
    @UserParameter(label = "W+", description = "Learning rate for " + "LTP case. Controls magnitude of LTP changes.", increment = .1, order = 2)
    protected double W_plus = 10;

    /**
     * Learning rate for LTP case. Controls magnitude of LTD changes.
     */
    @UserParameter(label = "W-", description = "Learning rate for " + "LTP case. Controls magnitude of LTD changes.", increment = .1, order = 3)
    protected double W_minus = 10;

    /**
     * General learning rate.
     */
    @UserParameter(label = "Learning rate", description = "General learning " + "rate.", increment = .1, order = 4)
    protected double learningRate = 0.01;

    /**
     * Sets whether or not STDP acts directly on W or dW/dt
     */
    @UserParameter(label = "Smooth STDP", description = "Whether STDP acts directly on weight or on its derivative instead", order = 5)
    protected boolean continuous = false;

    @Override
    public void init(Synapse synapse) {
    }

    @Override
    public String getName() {
        return "STDP";
    }

    public STDPRule(){

    }

    public STDPRule(STDPRule toCpy) {
        this(toCpy.getW_plus(), toCpy.getW_minus(), toCpy.getTau_plus(),
                toCpy.getTau_minus(), toCpy.learningRate, toCpy.continuous);
    }

    public STDPRule(double w_plus, double w_minus, double tau_plus, double tau_minus, double learningRate, boolean continuous) {
        this.W_plus=w_plus;
        this.W_minus=w_minus;
        this.tau_plus=tau_plus;
        this.tau_minus=tau_minus;
        this.learningRate=learningRate;
        this.continuous=continuous;
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
        final double str = synapse.getStrength();
        if (synapse.getSource().isSpike() || synapse.getTarget().isSpike()) {
            try {

                final double delta_t = ((((SpikingNeuronUpdateRule) synapse.getSource().getUpdateRule())
                        .getLastSpikeTime())
                        - ((SpikingNeuronUpdateRule) synapse.getTarget().getUpdateRule()).getLastSpikeTime())
                        * (hebbian ? 1 : -1);
                if (delta_t < 0) {
                    delta_w = W_plus * Math.exp(delta_t / tau_plus) * learningRate;
                } else if (delta_t > 0) {
                    delta_w = -W_minus * Math.exp(-delta_t / tau_minus) * learningRate;
                }

            } catch (ClassCastException cce) {
                cce.printStackTrace();
                System.out.println("Don't use non-spiking neurons with STDP!");
            }
            if ( !continuous && Math.signum(str) == -1 ) {
                synapse.setStrength(str - delta_w * synapse.getSource().getNetwork().getTimeStep());
            } else {
                synapse.setStrength(str + delta_w * synapse.getSource().getNetwork().getTimeStep());
            }
        }

        if ( continuous && Math.signum(str) == -1 ) {
            synapse.setStrength(str - delta_w * synapse.getSource().getNetwork().getTimeStep());
        } else {
            synapse.setStrength(str + delta_w * synapse.getSource().getNetwork().getTimeStep());
        }
    }

    public double getTau_plus() {
        return tau_plus;
    }

    public void setTau_plus(double tauPlus) {
        tau_plus = tauPlus;
    }

    public double getTau_minus() {
        return tau_minus;
    }

    public void setTau_minus(double tauMinus) {
        tau_minus = tauMinus;
    }

    public double getW_plus() {
        return W_plus;
    }

    public void setW_plus(double wPlus) {
        W_plus = wPlus;
    }

    public double getW_minus() {
        return W_minus;
    }

    public void setW_minus(double wMinus) {
        W_minus = wMinus;
    }

    public double getLearningRate() {
        return learningRate;
    }

    public void setLearningRate(double learningRate) {
        this.learningRate = learningRate;
    }

    public boolean isHebbian() {
        return hebbian;
    }

    public void setHebbian(boolean hebbian) {
        this.hebbian = hebbian;
    }

    public boolean isContinuous() {
        return continuous;
    }

    public void setContinuous(boolean continuous) {
        this.continuous=continuous;
    }

    public double getDelta_w() {
        return  delta_w;
    }

    public void setDelta_w(double delta_w) {
        this.delta_w = delta_w;
    }

}
