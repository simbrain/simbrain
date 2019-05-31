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
package org.simbrain.network.synapse_update_rules;

import org.simbrain.network.core.SpikingNeuronUpdateRule;
import org.simbrain.network.core.Synapse;
import org.simbrain.util.math.ProbDistributions.NormalDistribution;

/**
 * An implementation of Log-STDP as introduced in:
 * <p>
 * Gilson M, Fukai T (2011) Stability versus Neuronal Specialization for STDP:
 * Long-Tail Weight Distributions Solve the Dilemma. PLoS ONE 6(10): e25339
 * doi:10.1371/journal.pone.0025339
 * <p>
 * Log-STDP pushes weight values toward a log normal distribution. This can
 * help with weight divergence, which is common to Add and Mlt STDP. It also
 * allows for specialization of synapses, and fits very well with experimental
 * data concerning synaptic efficacy.
 *
 * @author Zoë Tosi
 */
public class LogSTDPRule extends STDPRule {

    /**
     * The value that for a given synapse, if the absolute value of the synapses
     * weight is below this threshold it is governed by a different LTD rule
     * than if it were being subjected to LTD, but had a weight greater than
     * this value. LTD for weights below this value linearly approach 0 as the
     * weight approaches 0. LTD for weights above this value are related
     * logarithmically to the synapse strength.
     * <p>
     * J_0 in the cited paper.
     */
    private double smallWtThreshold = 1;

    /**
     * A constant for LTP. c_+ in the cited paper.
     */
    private double w_plus = 2;

    /**
     * A constant for LTD. c_- in the cited paper.
     */
    private double w_minus = 1;

    /**
     * The degree to which the distribution is pushed logarithmically. Has an
     * effect on how strongly LTD pushes larger weights toward the small weight
     * threshold.
     * <p>
     * alpha in the cited paper.
     */
    private double logSaturation = 5;

    /**
     * A moderating constant for LTP, causing LTP to behave noticeably
     * differently for weights greater than ltpMod * smallWtThreshold.
     * <p>
     * beta in the cited paper.
     */
    private double ltpMod = 10;

    /**
     * The variance of the noise applied to weight changes.
     */
    private double noiseVar = 0.6;

    private double delta_w=0;

    /**
     * Updates the synapse's strength using Log-STDP.
     */
    @Override
    public void update(Synapse synapse) {
        boolean sourceSpiking = synapse.getSource().getUpdateRule().isSpikingNeuron();
        boolean targetSpiking = synapse.getTarget().getUpdateRule().isSpikingNeuron();
        if (!sourceSpiking || !targetSpiking) {
            return; // STDP is non-sensical if one of the units doesn't spike...
        }
        SpikingNeuronUpdateRule src = (SpikingNeuronUpdateRule) synapse.getSource().getUpdateRule();
        SpikingNeuronUpdateRule tar = (SpikingNeuronUpdateRule) synapse.getTarget().getUpdateRule();
        double delta_t;
        final double timeStep = synapse.getNetwork().getTimeStep();
        //        final double delay = synapse.getDelay() * timeStep;
        //        if (synapse.getStrength() >= 0) {
        delta_t = (src.getLastSpikeTime()) - (tar.getLastSpikeTime());
        //        } else {
        //        	delta_t = tar.getLastSpikeTime()
        //        			- (src.getLastSpikeTime());
        //        }

        if (synapse.getStrength() >= 0) {
            double noise =
                    (1 + NormalDistribution.builder()
                            .mean(0)
                            .standardDeviation(noiseVar)
                            .build().nextRand()
                    );
            if (delta_t < 0) {
                calcW_plusTerm(synapse);
                delta_w = timeStep * learningRate * (W_plus * Math.exp(delta_t / tau_plus)) * (1 + noise);
            } else if (delta_t > 0) {
                calcW_minusTerm(synapse);
                delta_w = timeStep * learningRate * (-W_minus * Math.exp(-delta_t / tau_minus)) * (1 + noise);
            } else {
                delta_w = 0;
            }
        }
//        } else {
//            if (delta_t > 0) {
//                delta_w = timeStep * learningRate * -1.0 * Math.exp(-delta_t / tau_plus);
//            } else if (delta_t < 0) {
//                delta_w = timeStep * learningRate * 1.5 * Math.exp(delta_t / tau_plus);
//            } else {
//                delta_w = 0;
//            }
//            if (synapse.getSource().isSpike()) {
//                synapse.setStrength(synapse.clip(synapse.getStrength() + (learningRate * delta_w)));
//            }
         else if (synapse.getStrength() <= 0) {
            if (delta_t > 0) {
                delta_w = learningRate * 1.5 * Math.exp(-delta_t / tau_plus);
            } else if (delta_t < 0) {
                delta_w = learningRate * -1 * Math.exp(delta_t / tau_minus);
            } else {
                delta_w = 0;
            }
        }
        synapse.setStrength(synapse.clip(synapse.getStrength() - delta_w));
    }


    /**
     * @param s
     * @return
     */
    private double calcW_plusTerm(Synapse s) {
        W_plus = w_plus * Math.exp(-Math.abs(s.getStrength()) / (smallWtThreshold * ltpMod));
        // if (s.getStrength() > 0) {
        // if (s.getStrength() >= s.getUpperBound()) {
        // W_plus = 0;
        // } else {
        // W_plus *= Math.exp(-20 * Math.pow(s.getStrength()
        // / (s.getUpperBound() - s.getStrength()), 2));
        // }
        // } else {
        // if (s.getStrength() <= s.getLowerBound()) {
        // W_plus = 0;
        // } else {
        // W_plus *= Math.exp(-20 * Math.pow(s.getStrength()
        // / (s.getLowerBound() - s.getStrength()), 2));
        // }
        // }
        return W_plus;
    }

    /**
     * @param s
     * @return
     */
    private double calcW_minusTerm(Synapse s) {
        double wt = Math.abs(s.getStrength());
        if (wt <= smallWtThreshold) {
            W_minus = w_minus * wt / smallWtThreshold;
        } else {

            double numerator = Math.log(1 + (logSaturation * ((wt / smallWtThreshold) - 1)));
            W_minus = w_minus * (1 + (numerator / logSaturation));
        }
        // if (s.getStrength() < 0) {
        // if (s.getStrength() >= s.getUpperBound()) {
        // W_minus = 0;
        // } else {
        // W_minus *= Math.exp(-0.25 * Math.pow((s.getLowerBound()
        // - s.getStrength()) / (s.getUpperBound()
        // - s.getStrength()), 2));
        // }
        // } else {
        // if (s.getStrength() <= s.getLowerBound()) {
        // W_minus = 0;
        // } else {
        // W_minus *= Math.exp(-0.25 * Math.pow((s.getUpperBound()
        // - s.getStrength()) / (s.getLowerBound()
        // - s.getStrength()), 2));
        // }
        // }
        return W_minus;
    }

    public double getSmallWtThreshold() {
        return smallWtThreshold;
    }

    public void setSmallWtThreshold(double smallWtThreshold) {
        this.smallWtThreshold = smallWtThreshold;
    }

    public double getW_plus() {
        return w_plus;
    }

    public void setW_plus(double w_plus) {
        this.w_plus = w_plus;
    }

    public double getW_minus() {
        return w_minus;
    }

    public void setW_minus(double w_minus) {
        this.w_minus = w_minus;
    }

    public double getLogSaturation() {
        return logSaturation;
    }

    public void setLogSaturation(double logSaturation) {
        this.logSaturation = logSaturation;
    }

    public double getLtpMod() {
        return ltpMod;
    }

    public void setLtpMod(double ltpMod) {
        this.ltpMod = ltpMod;
    }

    public double getNoiseVar() {
        return noiseVar;
    }

    public void setNoiseVar(double noiseVar) {
        this.noiseVar = noiseVar;
    }
}
